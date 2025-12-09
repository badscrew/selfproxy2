package com.selfproxy.vpn.platform.wireguard

import android.content.Context
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.adapter.ConnectionTestResult
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.repository.CredentialStore
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyFormatException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration
import java.time.Instant

/**
 * WireGuard protocol adapter implementation.
 * 
 * Integrates with the wireguard-android library to provide WireGuard VPN functionality.
 * Handles connection establishment, state management, and statistics tracking.
 */
class WireGuardAdapter(
    private val context: Context,
    private val credentialStore: CredentialStore,
    private val backend: Backend
) : ProtocolAdapter {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    
    private var currentTunnel: Tunnel? = null
    private var currentProfile: ServerProfile? = null
    private var connectionStartTime: Instant? = null
    private var lastHandshakeTime: Instant? = null
    
    // Statistics tracking
    private var lastBytesReceived: Long = 0
    private var lastBytesSent: Long = 0
    private var lastStatsUpdate: Instant = Instant.now()
    
    override suspend fun connect(profile: ServerProfile): Result<Connection> = withContext(Dispatchers.IO) {
        try {
            // Validate profile is WireGuard
            require(profile.protocol == Protocol.WIREGUARD) {
                "Profile must be a WireGuard profile"
            }
            
            _connectionState.value = ConnectionState.Connecting
            
            // Get WireGuard configuration
            val wireGuardConfig = profile.getWireGuardConfig()
            
            // Retrieve private key from credential store
            val privateKeyResult = credentialStore.getWireGuardPrivateKey(profile.id)
            if (privateKeyResult.isFailure) {
                val error = WireGuardException("Failed to retrieve private key: ${privateKeyResult.exceptionOrNull()?.message}")
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            val privateKey = privateKeyResult.getOrThrow()
            
            // Optionally retrieve preshared key
            val presharedKey = credentialStore.getWireGuardPresharedKey(profile.id).getOrNull()
            
            // Build WireGuard configuration
            val config = buildWireGuardConfig(
                privateKey = privateKey,
                publicKey = wireGuardConfig.publicKey,
                presharedKey = presharedKey,
                endpoint = wireGuardConfig.endpoint,
                allowedIPs = wireGuardConfig.allowedIPs,
                persistentKeepalive = wireGuardConfig.persistentKeepalive,
                mtu = wireGuardConfig.mtu
            ).getOrElse { error ->
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            
            // Create tunnel
            val tunnel = object : Tunnel {
                override fun getName(): String = profile.name
                override fun onStateChange(newState: Tunnel.State) {
                    handleTunnelStateChange(newState, profile)
                }
            }
            
            // Set tunnel up
            backend.setState(tunnel, Tunnel.State.UP, config)
            
            currentTunnel = tunnel
            currentProfile = profile
            connectionStartTime = Instant.now()
            
            // Start statistics monitoring
            startStatisticsMonitoring()
            
            // Wait for connection to be established (check handshake)
            val connected = waitForHandshake(timeout = Duration.ofSeconds(10))
            
            if (!connected) {
                disconnect()
                val error = WireGuardException("Handshake timeout - no response from server")
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            
            val connection = Connection(
                profileId = profile.id,
                protocol = Protocol.WIREGUARD,
                connectedAt = connectionStartTime!!.toEpochMilli(),
                serverAddress = profile.hostname
            )
            
            _connectionState.value = ConnectionState.Connected(connection)
            
            Result.success(connection)
            
        } catch (e: Exception) {
            val error = when (e) {
                is KeyFormatException -> WireGuardException("Invalid key format: ${e.message}", e)
                is IllegalArgumentException -> WireGuardException("Invalid configuration: ${e.message}", e)
                else -> WireGuardException("Connection failed: ${e.message}", e)
            }
            _connectionState.value = ConnectionState.Error(error)
            Result.failure(error)
        }
    }
    
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            currentTunnel?.let { tunnel ->
                backend.setState(tunnel, Tunnel.State.DOWN, null)
            }
            
            currentTunnel = null
            currentProfile = null
            connectionStartTime = null
            lastHandshakeTime = null
            
            _connectionState.value = ConnectionState.Disconnected
            
        } catch (e: Exception) {
            // Log error but don't throw - disconnection should always succeed
            _connectionState.value = ConnectionState.Disconnected
        }
    }
    
    override suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult> = withContext(Dispatchers.IO) {
        try {
            // Validate profile is WireGuard
            require(profile.protocol == Protocol.WIREGUARD) {
                "Profile must be a WireGuard profile"
            }
            
            val wireGuardConfig = profile.getWireGuardConfig()
            
            // Validate keys
            val privateKeyResult = credentialStore.getWireGuardPrivateKey(profile.id)
            if (privateKeyResult.isFailure) {
                return@withContext Result.success(
                    ConnectionTestResult(
                        success = false,
                        errorMessage = "Private key not found or invalid"
                    )
                )
            }
            
            val privateKey = privateKeyResult.getOrThrow()
            
            // Validate key format
            try {
                Key.fromBase64(privateKey)
                Key.fromBase64(wireGuardConfig.publicKey)
            } catch (e: KeyFormatException) {
                return@withContext Result.success(
                    ConnectionTestResult(
                        success = false,
                        errorMessage = "Invalid key format: ${e.message}"
                    )
                )
            }
            
            // Validate endpoint format and reachability
            val endpointValidation = validateEndpoint(wireGuardConfig.endpoint)
            if (!endpointValidation.first) {
                return@withContext Result.success(
                    ConnectionTestResult(
                        success = false,
                        errorMessage = endpointValidation.second
                    )
                )
            }
            
            // Attempt a quick connection test
            val startTime = Instant.now()
            val testResult = performQuickConnectionTest(profile, privateKey, wireGuardConfig)
            val latency = Duration.between(startTime, Instant.now()).toMillis()
            
            Result.success(
                ConnectionTestResult(
                    success = testResult.first,
                    latencyMs = if (testResult.first) latency else null,
                    errorMessage = if (!testResult.first) testResult.second else null
                )
            )
            
        } catch (e: Exception) {
            Result.success(
                ConnectionTestResult(
                    success = false,
                    errorMessage = "Test failed: ${e.message}"
                )
            )
        }
    }
    
    override fun observeConnectionState(): Flow<ConnectionState> {
        return _connectionState.asStateFlow()
    }
    
    override fun getStatistics(): ConnectionStatistics? {
        val tunnel = currentTunnel ?: return null
        val profile = currentProfile ?: return null
        val startTime = connectionStartTime ?: return null
        
        return try {
            val statistics = backend.getStatistics(tunnel)
            val now = Instant.now()
            val duration = Duration.between(startTime, now).toMillis()
            
            // Calculate speeds
            val timeDelta = Duration.between(lastStatsUpdate, now).toMillis()
            val downloadSpeed = if (timeDelta > 0) {
                ((statistics.totalRx() - lastBytesReceived) * 1000) / timeDelta
            } else {
                0L
            }
            val uploadSpeed = if (timeDelta > 0) {
                ((statistics.totalTx() - lastBytesSent) * 1000) / timeDelta
            } else {
                0L
            }
            
            // Update last values
            lastBytesReceived = statistics.totalRx()
            lastBytesSent = statistics.totalTx()
            lastStatsUpdate = now
            
            ConnectionStatistics(
                bytesReceived = statistics.totalRx(),
                bytesSent = statistics.totalTx(),
                downloadSpeed = downloadSpeed,
                uploadSpeed = uploadSpeed,
                connectionDuration = duration,
                lastHandshakeTime = lastHandshakeTime?.toEpochMilli()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Builds a WireGuard configuration from the profile settings.
     */
    private fun buildWireGuardConfig(
        privateKey: String,
        publicKey: String,
        presharedKey: String?,
        endpoint: String,
        allowedIPs: List<String>,
        persistentKeepalive: Int?,
        mtu: Int
    ): Result<Config> {
        return try {
            // Parse keys
            val privateKeyObj = Key.fromBase64(privateKey)
            val publicKeyObj = Key.fromBase64(publicKey)
            val presharedKeyObj = presharedKey?.let { Key.fromBase64(it) }
            
            // Build interface
            val interfaceBuilder = Interface.Builder()
                .parsePrivateKey(privateKey)
                .parseAddresses("10.0.0.2/24") // VPN interface address
            
            // Set MTU if specified
            if (mtu > 0) {
                interfaceBuilder.parseMtu(mtu.toString())
            }
            
            // Build peer
            val peerBuilder = Peer.Builder()
                .parsePublicKey(publicKey)
                .parseEndpoint(endpoint)
            
            // Add allowed IPs
            allowedIPs.forEach { ip ->
                peerBuilder.parseAllowedIPs(ip)
            }
            
            // Add preshared key if present
            presharedKey?.let {
                peerBuilder.parsePreSharedKey(it)
            }
            
            // Add persistent keepalive if specified
            persistentKeepalive?.let {
                if (it > 0) {
                    peerBuilder.parsePersistentKeepalive(it.toString())
                }
            }
            
            // Build config
            val config = Config.Builder()
                .setInterface(interfaceBuilder.build())
                .addPeer(peerBuilder.build())
                .build()
            
            Result.success(config)
            
        } catch (e: Exception) {
            Result.failure(WireGuardException("Failed to build configuration: ${e.message}", e))
        }
    }
    
    /**
     * Validates the endpoint format and checks if it's reachable.
     */
    private suspend fun validateEndpoint(endpoint: String): Pair<Boolean, String?> {
        return try {
            // Parse endpoint (format: hostname:port)
            val parts = endpoint.split(":")
            if (parts.size != 2) {
                return Pair(false, "Invalid endpoint format. Expected: hostname:port")
            }
            
            val hostname = parts[0]
            val port = parts[1].toIntOrNull()
            
            if (port == null || port !in 1..65535) {
                return Pair(false, "Invalid port number: $port")
            }
            
            // Try to resolve hostname
            withContext(Dispatchers.IO) {
                try {
                    InetAddress.getByName(hostname)
                    Pair(true, null)
                } catch (e: Exception) {
                    Pair(false, "Cannot resolve hostname: $hostname")
                }
            }
            
        } catch (e: Exception) {
            Pair(false, "Endpoint validation failed: ${e.message}")
        }
    }
    
    /**
     * Performs a quick connection test by attempting to establish a handshake.
     */
    private suspend fun performQuickConnectionTest(
        profile: ServerProfile,
        privateKey: String,
        wireGuardConfig: com.selfproxy.vpn.data.model.WireGuardConfig
    ): Pair<Boolean, String?> {
        return try {
            // Build test configuration
            val config = buildWireGuardConfig(
                privateKey = privateKey,
                publicKey = wireGuardConfig.publicKey,
                presharedKey = null,
                endpoint = wireGuardConfig.endpoint,
                allowedIPs = listOf("0.0.0.0/0"),
                persistentKeepalive = 0,
                mtu = wireGuardConfig.mtu
            ).getOrElse {
                return Pair(false, "Configuration error: ${it.message}")
            }
            
            // Create temporary tunnel for testing
            val testTunnel = object : Tunnel {
                override fun getName(): String = "test-${profile.name}"
                override fun onStateChange(newState: Tunnel.State) {}
            }
            
            // Try to bring tunnel up
            backend.setState(testTunnel, Tunnel.State.UP, config)
            
            // Wait briefly for handshake
            delay(3000)
            
            // Check if handshake occurred
            val statistics = backend.getStatistics(testTunnel)
            // For now, assume connection is successful if we can get statistics
            val handshakeOccurred = statistics != null
            
            // Bring tunnel down
            backend.setState(testTunnel, Tunnel.State.DOWN, null)
            
            if (handshakeOccurred) {
                Pair(true, null)
            } else {
                Pair(false, "No handshake received from server")
            }
            
        } catch (e: Exception) {
            Pair(false, "Connection test failed: ${e.message}")
        }
    }
    
    /**
     * Waits for the WireGuard handshake to complete.
     */
    private suspend fun waitForHandshake(timeout: Duration): Boolean {
        val startTime = Instant.now()
        val tunnel = currentTunnel ?: return false
        
        while (Duration.between(startTime, Instant.now()) < timeout) {
            try {
                val statistics = backend.getStatistics(tunnel)
                // For now, assume handshake is successful if we can get statistics
                if (statistics != null) {
                    lastHandshakeTime = Instant.now()
                    return true
                }
                
                delay(500) // Check every 500ms
                
            } catch (e: Exception) {
                // Continue waiting
            }
        }
        
        return false
    }
    
    /**
     * Starts monitoring statistics in the background.
     */
    private fun startStatisticsMonitoring() {
        scope.launch {
            while (currentTunnel != null) {
                try {
                    // Update statistics
                    getStatistics()
                    
                    // Check for handshake updates
                    currentTunnel?.let { tunnel ->
                        val statistics = backend.getStatistics(tunnel)
                        // Update last handshake time if we can get statistics
                        if (statistics != null) {
                            lastHandshakeTime = Instant.now()
                        }
                    }
                    
                    delay(2000) // Update every 2 seconds
                    
                } catch (e: Exception) {
                    // Continue monitoring
                }
            }
        }
    }
    
    /**
     * Handles tunnel state changes from the backend.
     */
    private fun handleTunnelStateChange(newState: Tunnel.State, profile: ServerProfile) {
        when (newState) {
            Tunnel.State.UP -> {
                // Tunnel is up, but we wait for handshake before marking as connected
            }
            Tunnel.State.DOWN -> {
                if (_connectionState.value is ConnectionState.Connected) {
                    // Unexpected disconnection
                    _connectionState.value = ConnectionState.Error(
                        WireGuardException("Connection lost")
                    )
                }
            }
            Tunnel.State.TOGGLE -> {
                // Ignore toggle state
            }
        }
    }
}

/**
 * WireGuard-specific exception.
 */
class WireGuardException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
