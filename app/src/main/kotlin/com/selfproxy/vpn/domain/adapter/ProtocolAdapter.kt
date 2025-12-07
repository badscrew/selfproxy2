package com.selfproxy.vpn.domain.adapter

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow

/**
 * Protocol adapter interface that all VPN protocols must implement.
 * Provides a unified interface for different VPN protocols (WireGuard, VLESS).
 */
interface ProtocolAdapter {
    /**
     * Establishes a connection using this protocol.
     * @param profile The server profile containing connection details
     * @return Result containing Connection on success or error on failure
     */
    suspend fun connect(profile: ServerProfile): Result<Connection>
    
    /**
     * Disconnects the current connection.
     */
    suspend fun disconnect()
    
    /**
     * Tests if the server is reachable and credentials are valid.
     * @param profile The server profile to test
     * @return Result containing connection test result
     */
    suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult>
    
    /**
     * Observes the connection state.
     * @return Flow of connection states
     */
    fun observeConnectionState(): Flow<ConnectionState>
    
    /**
     * Gets current connection statistics.
     * @return Current statistics or null if not connected
     */
    fun getStatistics(): ConnectionStatistics?
}

/**
 * Result of a connection test.
 */
data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long? = null,
    val errorMessage: String? = null
)

/**
 * Connection statistics.
 */
data class ConnectionStatistics(
    val bytesReceived: Long,
    val bytesSent: Long,
    val downloadSpeed: Long,  // bytes per second
    val uploadSpeed: Long,    // bytes per second
    val connectionDuration: Long,  // milliseconds
    
    // WireGuard-specific
    val lastHandshakeTime: Long? = null,
    
    // VLESS-specific
    val latency: Long? = null  // milliseconds
)
