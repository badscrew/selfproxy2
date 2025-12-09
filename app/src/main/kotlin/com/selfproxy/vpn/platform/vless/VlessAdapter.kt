package com.selfproxy.vpn.platform.vless

import android.content.Context
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
import java.net.Socket
import java.time.Duration
import java.time.Instant
import java.util.UUID

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
    private val credentialStore: CredentialStore
) : ProtocolAdapter {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    
    // TODO: Replace with actual Xray-core instance when library is integrated
    private var xrayInstance: Any? = null
    private var currentProfile: ServerProfile? = null
    private var connectionStartTime: Instant? = null
    
    // Statistics tracking
    private var lastBytesReceived: Long = 0
    private var lastBytesSent: Long = 0
    private var lastStatsUpdate: Instant = Instant.now()
    private var currentLatency: Long? = null
    
    override suspend fun connect(profile: ServerProfile): Result<Connection> = withContext(Dispatchers.IO) {
        try {
            // Validate profile is VLESS
            require(profile.protocol == Protocol.VLESS) {
                "Profile must be a VLESS profile"
            }
            
            _connectionState.value = ConnectionState.Connecting
            
            // Get VLESS configuration
            val vlessConfig = profile.getVlessConfig()
            
            // Retrieve UUID from credential store
            val uuidResult = credentialStore.getVlessUuid(profile.id)
            if (uuidResult.isFailure) {
                val error = VlessException("Failed to retrieve UUID: ${uuidResult.exceptionOrNull()?.message}")
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            val uuid = uuidResult.getOrThrow()
            
            // Validate UUID format
            val uuidValidation = validateUuid(uuid)
            if (!uuidValidation.first) {
                val error = VlessException(uuidValidation.second ?: "Invalid UUID format")
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            
            // Validate transport protocol configuration
            val transportValidation = validateTransportConfig(vlessConfig)
            if (!transportValidation.first) {
                val error = VlessException(transportValidation.second ?: "Invalid transport configuration")
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            
            // Validate TLS configuration if present
            vlessConfig.tlsSettings?.let { tls ->
                val tlsValidation = validateTlsConfig(tls)
                if (!tlsValidation.first) {
                    val error = VlessException(tlsValidation.second ?: "Invalid TLS configuration")
                    _connectionState.value = ConnectionState.Error(error)
                    return@withContext Result.failure(error)
                }
            }
            
            // Build Xray-core configuration
            val xrayConfig = buildXrayConfig(
                hostname = profile.hostname,
                port = profile.port,
                uuid = uuid,
                vlessConfig = vlessConfig
            ).getOrElse { error ->
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            
            // TODO: Initialize Xray-core with configuration
            // This requires AndroidLibXrayLite library integration
            // Example (pseudo-code):
            // xrayInstance = XrayCore.create(context, xrayConfig)
            // xrayInstance.start()
            
            // For now, simulate connection establishment
            val connected = establishConnection(profile, uuid, vlessConfig)
            
            if (!connected.first) {
                disconnect()
                val error = VlessException(connected.second ?: "Connection failed")
                _connectionState.value = ConnectionState.Error(error)
                return@withContext Result.failure(error)
            }
            
            currentProfile = profile
            connectionStartTime = Instant.now()
            
            // Start statistics monitoring
            startStatisticsMonitoring()
            
            val connection = Connection(
                profileId = profile.id,
                protocol = Protocol.VLESS,
                connectedAt = connectionStartTime!!.toEpochMilli(),
                serverAddress = profile.hostname
            )
            
            _connectionState.value = ConnectionState.Connected(connection)
            
            Result.success(connection)
            
        } catch (e: Exception) {
            val error = when (e) {
                is IllegalArgumentException -> VlessException("Invalid configuration: ${e.message}", e)
                else -> VlessException("Connection failed: ${e.message}", e)
            }
            _connectionState.value = ConnectionState.Error(error)
            Result.failure(error)
        }
    }
    
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            // TODO: Stop Xray-core instance
            // xrayInstance?.stop()
            
            xrayInstance = null
            currentProfile = null
            connectionStartTime = null
            currentLatency = null
            
            _connectionState.value = ConnectionState.Disconnected
            
        } catch (e: Exception) {
            // Log error but don't throw - disconnection should always succeed
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
            
            // Test server reachability
            val reachabilityTest = testServerReachability(profile.hostname, profile.port)
            if (!reachabilityTest.first) {
                return@withContext Result.success(
                    ConnectionTestResult(
                        success = false,
                        errorMessage = reachabilityTest.second
                    )
                )
            }
            
            // Perform quick connection test
            val startTime = Instant.now()
            val testResult = performQuickConnectionTest(profile, uuid, vlessConfig)
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
     */
    private suspend fun testServerReachability(hostname: String, port: Int): Pair<Boolean, String?> {
        return try {
            withContext(Dispatchers.IO) {
                // Try to resolve hostname
                val address = try {
                    InetAddress.getByName(hostname)
                } catch (e: Exception) {
                    return@withContext Pair(false, "Cannot resolve hostname: $hostname")
                }
                
                // Try to connect to port
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(address, port), 5000)
                    socket.close()
                    Pair(true, null)
                } catch (e: Exception) {
                    Pair(false, "Cannot connect to $hostname:$port - ${e.message}")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Reachability test failed: ${e.message}")
        }
    }
    
    /**
     * Builds Xray-core configuration from profile settings.
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
            // TODO: Use Xray-core to establish connection
            // This is a placeholder that simulates connection
            
            // Test server reachability
            val reachable = testServerReachability(profile.hostname, profile.port)
            if (!reachable.first) {
                return Pair(false, reachable.second)
            }
            
            // Simulate connection establishment delay
            delay(1000)
            
            // Measure latency
            val startTime = Instant.now()
            testServerReachability(profile.hostname, profile.port)
            currentLatency = Duration.between(startTime, Instant.now()).toMillis()
            
            Pair(true, null)
            
        } catch (e: Exception) {
            Pair(false, "Connection establishment failed: ${e.message}")
        }
    }
    
    /**
     * Performs a quick connection test.
     */
    private suspend fun performQuickConnectionTest(
        profile: ServerProfile,
        uuid: String,
        vlessConfig: com.selfproxy.vpn.data.model.VlessConfig
    ): Pair<Boolean, String?> {
        return try {
            // Test server reachability
            val reachable = testServerReachability(profile.hostname, profile.port)
            if (!reachable.first) {
                return Pair(false, reachable.second)
            }
            
            // TODO: Perform actual VLESS handshake test with Xray-core
            // For now, just verify reachability
            
            Pair(true, null)
            
        } catch (e: Exception) {
            Pair(false, "Connection test failed: ${e.message}")
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
}

/**
 * VLESS-specific exception.
 */
class VlessException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
