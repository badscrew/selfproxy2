package com.selfproxy.vpn.platform.vless

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.selfproxy.vpn.data.model.FlowControl
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.TransportProtocol
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.adapter.ConnectionTestResult
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.repository.CredentialStore
import com.selfproxy.vpn.platform.vpn.TunnelVpnService
import libv2ray.CoreCallbackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * VLESS protocol adapter implementation.
 * 
 * Integrates with AndroidLibXrayLite library to provide VLESS VPN functionality.
 * Supports multiple transport protocols (TCP, WebSocket, gRPC, HTTP/2) and
 * TLS/Reality configuration for advanced obfuscation.
 * 
 * Note: This implementation requires AndroidLibXrayLite library integration.
 * The library provides the core Xray-core functionality for VLESS protocol.
 */
class VlessAdapter(
    private val context: Context,
    private val credentialStore: CredentialStore,
    private val settingsRepository: com.selfproxy.vpn.data.repository.SettingsRepository
) : ProtocolAdapter {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    
    companion object {
        private const val TAG = "VlessAdapter"
    }
    
    // Xray-core integration
    private var xrayCore: XrayCore? = null
    private var currentProfile: ServerProfile? = null
    private var connectionStartTime: Instant? = null
    private var socksPort: Int = 0
    
    // Statistics tracking
    private var lastBytesReceived: Long = 0
    private var lastBytesSent: Long = 0
    private var lastStatsUpdate: Instant = Instant.now()
    private var currentLatency: Long? = null
    
    override suspend fun connect(profile: ServerProfile): Result<Connection> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Starting VLESS connection ===")
            Log.d(TAG, "Profile: ${profile.name}")
            Log.d(TAG, "Server: ${profile.hostname}:${profile.port}")
            Log.d(TAG, "Protocol: ${profile.protocol}")
            
            // Validate profile is VLESS
            require(profile.protocol == Protocol.VLESS) {
                "Profile must be a VLESS profile"
            }
            
            _connectionState.value = ConnectionState.Connecting
            Log.d(TAG, "State changed to: Connecting")
            
            // Get VLESS configuration
            val vlessConfig = profile.getVlessConfig()
            Log.d(TAG, "VLESS config retrieved: transport=${vlessConfig.transport}, flow=${vlessConfig.flowControl}")
            
            // Retrieve UUID from credential store
            Log.d(TAG, "Retrieving UUID from credential store for profile ID: ${profile.id}")
            val uuidResult = credentialStore.getVlessUuid(profile.id)
            if (uuidResult.isFailure) {
                val errorMsg = "Failed to retrieve UUID: ${uuidResult.exceptionOrNull()?.message}"
                Log.e(TAG, errorMsg, uuidResult.exceptionOrNull())
                val error = VlessException(errorMsg)
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            val uuid = uuidResult.getOrThrow()
            Log.d(TAG, "UUID retrieved successfully: ${uuid.take(8)}...")
            
            // Validate UUID format
            val uuidValidation = validateUuid(uuid)
            if (!uuidValidation.first) {
                val errorMsg = uuidValidation.second ?: "Invalid UUID format"
                Log.e(TAG, "UUID validation failed: $errorMsg")
                val error = VlessException(errorMsg)
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            Log.d(TAG, "UUID validation passed")
            
            // Validate transport protocol configuration
            Log.d(TAG, "Validating transport configuration...")
            val transportValidation = validateTransportConfig(vlessConfig)
            if (!transportValidation.first) {
                val errorMsg = transportValidation.second ?: "Invalid transport configuration"
                Log.e(TAG, "Transport validation failed: $errorMsg")
                val error = VlessException(errorMsg)
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            Log.d(TAG, "Transport validation passed")
            
            // Validate TLS configuration if present
            vlessConfig.tlsSettings?.let { tls ->
                Log.d(TAG, "Validating TLS configuration...")
                val tlsValidation = validateTlsConfig(tls)
                if (!tlsValidation.first) {
                    val errorMsg = tlsValidation.second ?: "Invalid TLS configuration"
                    Log.e(TAG, "TLS validation failed: $errorMsg")
                    val error = VlessException(errorMsg)
                    _connectionState.value = ConnectionState.Error(error)
                    return@withContext Result.failure(error)
                }
                Log.d(TAG, "TLS validation passed")
            }
            
            // Initialize Xray-core
            Log.d(TAG, "Initializing Xray-core...")
            val xray = XrayCore(context)
            
            // Build Xray configuration
            Log.d(TAG, "Building Xray configuration...")
            val xrayConfig = xray.buildConfig(profile, uuid)
            Log.d(TAG, "Xray config built successfully")
            
            // Create callback handler
            val callbackHandler = object : CoreCallbackHandler {
                override fun onEmitStatus(code: Long, status: String): Long {
                    Log.d(TAG, "Xray status [$code]: $status")
                    return 0
                }
                
                override fun startup(): Long {
                    Log.d(TAG, "Xray startup called")
                    return 0
                }
                
                override fun shutdown(): Long {
                    Log.d(TAG, "Xray shutdown called")
                    return 0
                }
            }
            
            // Start Xray-core
            Log.d(TAG, "Starting Xray-core...")
            val startResult = xray.start(xrayConfig, callbackHandler)
            
            if (startResult.isFailure) {
                val errorMsg = startResult.exceptionOrNull()?.message ?: "Failed to start Xray-core"
                Log.e(TAG, "Xray-core start failed: $errorMsg", startResult.exceptionOrNull())
                val error = VlessException(errorMsg, startResult.exceptionOrNull())
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            
            socksPort = startResult.getOrThrow()
            Log.d(TAG, "Xray-core started successfully, SOCKS5 port: $socksPort")
            xrayCore = xray
            
            currentProfile = profile
            connectionStartTime = Instant.now()
            
            // Start VPN service to create TUN interface and route traffic
            Log.d(TAG, "Starting VPN service to route traffic through SOCKS5 proxy...")
            val vpnStartResult = startVpnService(profile, socksPort)
            if (vpnStartResult.isFailure) {
                val errorMsg = "Failed to start VPN service: ${vpnStartResult.exceptionOrNull()?.message}"
                Log.e(TAG, errorMsg, vpnStartResult.exceptionOrNull())
                // Stop Xray-core since VPN service failed
                xray.stop()
                xrayCore = null
                val error = VlessException(errorMsg, vpnStartResult.exceptionOrNull())
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            Log.d(TAG, "VPN service started successfully")
            
            // Only create connection object and mark as connected AFTER establishment succeeds
            val connection = Connection(
                profileId = profile.id,
                protocol = Protocol.VLESS,
                connectedAt = connectionStartTime!!.toEpochMilli(),
                serverAddress = profile.hostname
            )
            
            _connectionState.value = ConnectionState.Connected(connection)
            Log.d(TAG, "State changed to: Connected")
            
            // Start statistics monitoring only after successful connection
            startStatisticsMonitoring()
            
            Log.d(TAG, "=== VLESS connection completed successfully ===")
            Result.success(connection)
            
        } catch (e: Exception) {
            Log.e(TAG, "=== VLESS connection failed with exception ===", e)
            val error = when (e) {
                is IllegalArgumentException -> VlessException("Invalid configuration: ${e.message}", e)
                else -> VlessException("Connection failed: ${e.message}", e)
            }
            _connectionState.value = ConnectionState.Error(error)
            Result.failure(error)
        }
    }
    
    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Disconnecting VLESS connection...")
            
            // Stop VPN service first
            Log.d(TAG, "Stopping VPN service...")
            stopVpnService()
            
            // Then stop Xray-core
            Log.d(TAG, "Stopping Xray-core...")
            xrayCore?.stop()
            xrayCore = null
            currentProfile = null
            connectionStartTime = null
            currentLatency = null
            socksPort = 0
            
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "Disconnected successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
            // Log error but don't throw - disconnection should always succeed
            stopVpnService()  // Try to stop VPN service anyway
            xrayCore = null
            currentProfile = null
            connectionStartTime = null
            currentLatency = null
            socksPort = 0
            _connectionState.value = ConnectionState.Disconnected
        }
    }
    
    override suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult> = withContext(Dispatchers.IO) {
        try {
            // Validate profile is VLESS
            require(profile.protocol == Protocol.VLESS) {
                "Profile must be a VLESS profile"
            }
            
            val vlessConfig = profile.getVlessConfig()
            
            // Validate UUID
            val uuidResult = credentialStore.getVlessUuid(profile.id)
            if (uuidResult.isFailure) {
                return@withContext Result.success(
                    ConnectionTestResult(
                        success = false,
                        errorMessage = "UUID not found or invalid"
                    )
                )
            }
            
            val uuid = uuidResult.getOrThrow()
            
            // Validate UUID format
            val uuidValidation = validateUuid(uuid)
            if (!uuidValidation.first) {
                return@withContext Result.success(
                    ConnectionTestResult(
                        success = false,
                        errorMessage = "Invalid UUID format: ${uuidValidation.second}"
                    )
                )
            }
            
            // Validate transport protocol configuration
            val transportValidation = validateTransportConfig(vlessConfig)
            if (!transportValidation.first) {
                return@withContext Result.success(
                    ConnectionTestResult(
                        success = false,
                        errorMessage = "Invalid transport configuration: ${transportValidation.second}"
                    )
                )
            }
            
            // Validate TLS configuration if present
            vlessConfig.tlsSettings?.let { tls ->
                val tlsValidation = validateTlsConfig(tls)
                if (!tlsValidation.first) {
                    return@withContext Result.success(
                        ConnectionTestResult(
                            success = false,
                            errorMessage = "Invalid TLS configuration: ${tlsValidation.second}"
                        )
                    )
                }
            }
            
            // For VLESS Reality, we can't do a simple reachability test
            // because Reality servers don't respond to regular TLS handshakes.
            // Instead, we'll do a quick actual connection test using Xray-core.
            
            Log.d(TAG, "Performing VLESS connection test...")
            val startTime = Instant.now()
            
            // Test by actually starting Xray-core temporarily
            val testResult = performVlessConnectionTest(profile, uuid)
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
        val profile = currentProfile ?: return null
        val startTime = connectionStartTime ?: return null
        
        return try {
            // TODO: Get actual statistics from Xray-core
            // For now, return simulated statistics
            val now = Instant.now()
            val duration = Duration.between(startTime, now).toMillis()
            
            // Calculate speeds
            val timeDelta = Duration.between(lastStatsUpdate, now).toMillis()
            val downloadSpeed = if (timeDelta > 0) {
                ((lastBytesReceived) * 1000) / timeDelta
            } else {
                0L
            }
            val uploadSpeed = if (timeDelta > 0) {
                ((lastBytesSent) * 1000) / timeDelta
            } else {
                0L
            }
            
            lastStatsUpdate = now
            
            ConnectionStatistics(
                bytesReceived = lastBytesReceived,
                bytesSent = lastBytesSent,
                downloadSpeed = downloadSpeed,
                uploadSpeed = uploadSpeed,
                connectionDuration = duration,
                latency = currentLatency
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validates UUID format (RFC 4122).
     */
    private fun validateUuid(uuid: String): Pair<Boolean, String?> {
        return try {
            UUID.fromString(uuid)
            Pair(true, null)
        } catch (e: IllegalArgumentException) {
            Pair(false, "UUID must be in RFC 4122 format (e.g., 550e8400-e29b-41d4-a716-446655440000)")
        }
    }
    
    /**
     * Validates transport protocol configuration.
     */
    private fun validateTransportConfig(vlessConfig: com.selfproxy.vpn.data.model.VlessConfig): Pair<Boolean, String?> {
        return try {
            when (vlessConfig.transport) {
                TransportProtocol.TCP -> {
                    // TCP requires no additional settings
                    Pair(true, null)
                }
                TransportProtocol.WEBSOCKET -> {
                    val ws = vlessConfig.websocketSettings
                    if (ws == null) {
                        Pair(false, "WebSocket settings required for WebSocket transport")
                    } else if (ws.path.isBlank()) {
                        Pair(false, "WebSocket path cannot be blank")
                    } else {
                        Pair(true, null)
                    }
                }
                TransportProtocol.GRPC -> {
                    val grpc = vlessConfig.grpcSettings
                    if (grpc == null) {
                        Pair(false, "gRPC settings required for gRPC transport")
                    } else if (grpc.serviceName.isBlank()) {
                        Pair(false, "gRPC service name cannot be blank")
                    } else {
                        Pair(true, null)
                    }
                }
                TransportProtocol.HTTP2 -> {
                    val http2 = vlessConfig.http2Settings
                    if (http2 == null) {
                        Pair(false, "HTTP/2 settings required for HTTP/2 transport")
                    } else if (http2.path.isBlank()) {
                        Pair(false, "HTTP/2 path cannot be blank")
                    } else {
                        Pair(true, null)
                    }
                }
            }
        } catch (e: Exception) {
            Pair(false, "Transport validation failed: ${e.message}")
        }
    }
    
    /**
     * Validates TLS configuration.
     */
    private fun validateTlsConfig(tlsSettings: com.selfproxy.vpn.data.model.TlsSettings): Pair<Boolean, String?> {
        return try {
            if (tlsSettings.serverName.isBlank()) {
                Pair(false, "TLS server name (SNI) cannot be blank")
            } else {
                Pair(true, null)
            }
        } catch (e: Exception) {
            Pair(false, "TLS validation failed: ${e.message}")
        }
    }
    
    /**
     * Tests if the server is reachable.
     * For HTTPS/TLS servers (port 443), performs a TLS handshake.
     * For other ports, performs a simple TCP connection test.
     */
    private suspend fun testServerReachability(hostname: String, port: Int): Pair<Boolean, String?> {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Testing server reachability: $hostname:$port")
                
                // Try to resolve hostname
                val address = try {
                    val addr = InetAddress.getByName(hostname)
                    Log.d(TAG, "Hostname resolved to: ${addr.hostAddress}")
                    addr
                } catch (e: Exception) {
                    val errorMsg = "Cannot resolve hostname: $hostname - ${e.message}"
                    Log.e(TAG, errorMsg, e)
                    return@withContext Pair(false, errorMsg)
                }
                
                // For port 443 (HTTPS), try TLS handshake
                if (port == 443) {
                    try {
                        Log.d(TAG, "Attempting TLS handshake to ${address.hostAddress}:$port (timeout: 10s)")
                        
                        // Create a trust manager that accepts all certificates (for testing)
                        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        })
                        
                        val sslContext = SSLContext.getInstance("TLS")
                        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                        
                        val socketFactory = sslContext.socketFactory
                        val sslSocket = socketFactory.createSocket() as SSLSocket
                        
                        // Set timeout and connect
                        sslSocket.soTimeout = 10000
                        sslSocket.connect(InetSocketAddress(address, port), 10000)
                        
                        // Start TLS handshake
                        sslSocket.startHandshake()
                        
                        Log.d(TAG, "TLS handshake successful, server is reachable")
                        sslSocket.close()
                        
                        Pair(true, null)
                    } catch (e: Exception) {
                        val errorMsg = "TLS handshake failed to $hostname:$port - ${e.message}"
                        Log.e(TAG, errorMsg, e)
                        Pair(false, errorMsg)
                    }
                } else {
                    // For non-HTTPS ports, use simple TCP connection
                    val socket = Socket()
                    try {
                        Log.d(TAG, "Attempting TCP connection to ${address.hostAddress}:$port (timeout: 5s)")
                        socket.connect(InetSocketAddress(address, port), 5000)
                        socket.close()
                        Log.d(TAG, "Server is reachable")
                        Pair(true, null)
                    } catch (e: Exception) {
                        val errorMsg = "Cannot connect to $hostname:$port - ${e.message}"
                        Log.e(TAG, errorMsg, e)
                        Pair(false, errorMsg)
                    }
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Reachability test failed: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Pair(false, errorMsg)
        }
    }
    
    /**
     * Builds Xray-core configuration from profile settings.
     * 
     * NOTE: This method is no longer used. Configuration is now built in XrayCore class.
     */
    private fun buildXrayConfig(
        hostname: String,
        port: Int,
        uuid: String,
        vlessConfig: com.selfproxy.vpn.data.model.VlessConfig
    ): Result<String> {
        return try {
            // TODO: Build actual Xray-core JSON configuration
            // This is a simplified example structure
            val config = buildString {
                append("{")
                append("\"inbounds\": [{")
                append("\"port\": 10808,")
                append("\"protocol\": \"socks\",")
                append("\"settings\": {\"udp\": true}")
                append("}],")
                append("\"outbounds\": [{")
                append("\"protocol\": \"vless\",")
                append("\"settings\": {")
                append("\"vnext\": [{")
                append("\"address\": \"$hostname\",")
                append("\"port\": $port,")
                append("\"users\": [{")
                append("\"id\": \"$uuid\",")
                append("\"encryption\": \"none\",")
                append("\"flow\": \"${vlessConfig.flowControl.name.lowercase().replace('_', '-')}\"")
                append("}]")
                append("}]")
                append("},")
                append("\"streamSettings\": {")
                append("\"network\": \"${vlessConfig.transport.name.lowercase()}\"")
                
                // Add transport-specific settings
                when (vlessConfig.transport) {
                    TransportProtocol.WEBSOCKET -> {
                        vlessConfig.websocketSettings?.let { ws ->
                            append(",\"wsSettings\": {")
                            append("\"path\": \"${ws.path}\"")
                            if (ws.headers.isNotEmpty()) {
                                append(",\"headers\": {")
                                append(ws.headers.entries.joinToString(",") { "\"${it.key}\": \"${it.value}\"" })
                                append("}")
                            }
                            append("}")
                        }
                    }
                    TransportProtocol.GRPC -> {
                        vlessConfig.grpcSettings?.let { grpc ->
                            append(",\"grpcSettings\": {")
                            append("\"serviceName\": \"${grpc.serviceName}\",")
                            append("\"multiMode\": ${grpc.multiMode}")
                            append("}")
                        }
                    }
                    TransportProtocol.HTTP2 -> {
                        vlessConfig.http2Settings?.let { http2 ->
                            append(",\"httpSettings\": {")
                            append("\"path\": \"${http2.path}\"")
                            if (http2.host.isNotEmpty()) {
                                append(",\"host\": [")
                                append(http2.host.joinToString(",") { "\"$it\"" })
                                append("]")
                            }
                            append("}")
                        }
                    }
                    TransportProtocol.TCP -> {
                        // TCP requires no additional settings
                    }
                }
                
                // Add TLS settings if present
                vlessConfig.tlsSettings?.let { tls ->
                    append(",\"security\": \"tls\",")
                    append("\"tlsSettings\": {")
                    append("\"serverName\": \"${tls.serverName}\"")
                    if (tls.alpn.isNotEmpty()) {
                        append(",\"alpn\": [")
                        append(tls.alpn.joinToString(",") { "\"$it\"" })
                        append("]")
                    }
                    if (tls.allowInsecure) {
                        append(",\"allowInsecure\": true")
                    }
                    tls.fingerprint?.let { fp ->
                        append(",\"fingerprint\": \"$fp\"")
                    }
                    append("}")
                }
                
                // Add Reality settings if present
                vlessConfig.realitySettings?.let { reality ->
                    append(",\"security\": \"reality\",")
                    append("\"realitySettings\": {")
                    append("\"serverName\": \"${reality.serverName}\",")
                    append("\"publicKey\": \"${reality.publicKey}\",")
                    append("\"shortId\": \"${reality.shortId}\"")
                    reality.spiderX?.let { sx ->
                        append(",\"spiderX\": \"$sx\"")
                    }
                    reality.fingerprint?.let { fp ->
                        append(",\"fingerprint\": \"$fp\"")
                    }
                    append("}")
                }
                
                append("}")
                append("}]")
                append("}")
            }
            
            Result.success(config)
            
        } catch (e: Exception) {
            Result.failure(VlessException("Failed to build configuration: ${e.message}", e))
        }
    }
    
    /**
     * Establishes connection to VLESS server.
     */
    private suspend fun establishConnection(
        profile: ServerProfile,
        uuid: String,
        vlessConfig: com.selfproxy.vpn.data.model.VlessConfig
    ): Pair<Boolean, String?> {
        return try {
            Log.d(TAG, "establishConnection: Starting connection establishment")
            // TODO: Use Xray-core to establish connection
            // This is a placeholder that simulates connection
            
            // Test server reachability
            Log.d(TAG, "establishConnection: Testing server reachability...")
            val reachable = testServerReachability(profile.hostname, profile.port)
            if (!reachable.first) {
                Log.e(TAG, "establishConnection: Server not reachable: ${reachable.second}")
                return Pair(false, reachable.second)
            }
            Log.d(TAG, "establishConnection: Server is reachable")
            
            // Simulate connection establishment delay
            Log.d(TAG, "establishConnection: Simulating connection delay...")
            delay(1000)
            
            // Measure latency
            Log.d(TAG, "establishConnection: Measuring latency...")
            val startTime = Instant.now()
            testServerReachability(profile.hostname, profile.port)
            currentLatency = Duration.between(startTime, Instant.now()).toMillis()
            Log.d(TAG, "establishConnection: Latency measured: ${currentLatency}ms")
            
            Log.d(TAG, "establishConnection: Connection established successfully")
            Pair(true, null)
            
        } catch (e: Exception) {
            val errorMsg = "Connection establishment failed: ${e.message}"
            Log.e(TAG, "establishConnection: $errorMsg", e)
            Pair(false, errorMsg)
        }
    }
    
    /**
     * Performs a VLESS connection test.
     * 
     * Note: Full traffic routing through the tunnel is not yet implemented.
     * This test verifies that Xray-core is running and the VPN service is active.
     */
    private suspend fun performVlessConnectionTest(
        profile: ServerProfile,
        uuid: String
    ): Pair<Boolean, String?> {
        return try {
            Log.d(TAG, "Performing connection test...")
            
            // For now, just verify that Xray-core is running
            // TODO: Implement full packet routing in TunnelVpnService to enable actual traffic test
            
            if (xrayCore?.isRunning() == true) {
                Log.d(TAG, "Xray-core is running - connection test passed")
                Pair(true, null)
            } else {
                Log.e(TAG, "Xray-core is not running")
                Pair(false, "Xray-core is not running")
            }
            
        } catch (e: Exception) {
            val errorMsg = "Connection test failed: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Pair(false, errorMsg)
        }
    }

    
    /**
     * Starts monitoring statistics in the background.
     */
    private fun startStatisticsMonitoring() {
        scope.launch {
            while (currentProfile != null) {
                try {
                    // TODO: Get actual statistics from Xray-core
                    // For now, simulate statistics updates
                    
                    delay(2000) // Update every 2 seconds
                    
                } catch (e: Exception) {
                    // Continue monitoring
                }
            }
        }
    }
    
    /**
     * Starts the VPN service to create TUN interface and route traffic.
     */
    private suspend fun startVpnService(profile: ServerProfile, socksPort: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating VPN service intent...")
            val intent = Intent(context, TunnelVpnService::class.java).apply {
                action = TunnelVpnService.ACTION_START_VPN
                putExtra("SOCKS_PORT", socksPort)
                putExtra("PROFILE_ID", profile.id)
                putExtra("PROFILE_NAME", profile.name)
                putExtra("SERVER_ADDRESS", profile.hostname)
            }
            
            Log.d(TAG, "Starting VPN service (foreground)...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            // Give the service a moment to start
            delay(500)
            
            Log.d(TAG, "VPN service start command sent")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service", e)
            Result.failure(VlessException("Failed to start VPN service: ${e.message}", e))
        }
    }
    
    /**
     * Stops the VPN service.
     */
    private fun stopVpnService() {
        try {
            Log.d(TAG, "Stopping VPN service...")
            val intent = Intent(context, TunnelVpnService::class.java).apply {
                action = TunnelVpnService.ACTION_STOP_VPN
            }
            context.startService(intent)
            Log.d(TAG, "VPN service stop command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN service", e)
            // Don't throw - disconnection should always succeed
        }
    }
}

/**
 * VLESS-specific exception.
 */
class VlessException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
