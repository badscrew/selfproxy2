package com.selfproxy.vpn.domain.manager

import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors traffic statistics for VPN connections.
 * 
 * Tracks bytes sent/received, calculates speeds, and monitors connection duration.
 * Provides real-time statistics updates via StateFlow.
 * 
 * Requirements: 7.2, 7.3, 7.4, 7.5, 7.10
 */
class TrafficMonitor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val updateIntervalMs: Long = 1500
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    
    // Current statistics
    private val _statistics = MutableStateFlow<ConnectionStatistics?>(null)
    val statistics: StateFlow<ConnectionStatistics?> = _statistics.asStateFlow()
    
    // Tracking variables
    private var totalBytesReceived: Long = 0
    private var totalBytesSent: Long = 0
    private var connectionStartTime: Long = 0
    
    // Speed calculation variables
    private var lastBytesReceived: Long = 0
    private var lastBytesSent: Long = 0
    private var lastUpdateTime: Long = 0
    
    // Protocol-specific data
    private var lastHandshakeTime: Long? = null
    private var latency: Long? = null
    
    // Update job
    private var updateJob: Job? = null
    
    /**
     * Starts monitoring traffic.
     * 
     * Begins tracking connection duration and calculating speeds.
     * Updates statistics in real-time every 1-2 seconds.
     * 
     * Requirement 7.5: Real-time statistics updates
     */
    fun start() {
        if (updateJob?.isActive == true) {
            return // Already started
        }
        
        connectionStartTime = System.currentTimeMillis()
        lastUpdateTime = connectionStartTime
        lastBytesReceived = 0
        lastBytesSent = 0
        
        updateJob = scope.launch {
            while (isActive) {
                updateStatistics()
                delay(updateIntervalMs)
            }
        }
    }
    
    /**
     * Stops monitoring traffic.
     * 
     * Cancels the update job and clears current statistics.
     */
    fun stop() {
        updateJob?.cancel()
        updateJob = null
        _statistics.value = null
    }
    
    /**
     * Updates byte counters from TUN interface.
     * 
     * Should be called whenever bytes are read from or written to the TUN interface.
     * 
     * Requirements 7.2, 7.3: Track bytes sent/received
     * 
     * @param bytesReceived Number of bytes received
     * @param bytesSent Number of bytes sent
     */
    fun updateBytes(bytesReceived: Long, bytesSent: Long) {
        totalBytesReceived += bytesReceived
        totalBytesSent += bytesSent
    }
    
    /**
     * Sets the last handshake time for WireGuard connections.
     * 
     * Requirement 7.7: Display last handshake time for WireGuard
     * 
     * @param timestamp The handshake timestamp in milliseconds
     */
    fun setLastHandshakeTime(timestamp: Long) {
        lastHandshakeTime = timestamp
    }
    
    /**
     * Sets the latency for VLESS connections.
     * 
     * Requirement 7.8: Display latency for VLESS
     * 
     * @param latencyMs The latency in milliseconds
     */
    fun setLatency(latencyMs: Long) {
        latency = latencyMs
    }
    
    /**
     * Resets accumulated bandwidth data while maintaining the active connection.
     * 
     * Requirement 7.10: Statistics reset
     */
    fun reset() {
        totalBytesReceived = 0
        totalBytesSent = 0
        lastBytesReceived = 0
        lastBytesSent = 0
        connectionStartTime = System.currentTimeMillis()
        lastUpdateTime = connectionStartTime
        
        // Immediately update statistics to reflect reset
        updateStatistics()
    }
    
    /**
     * Updates statistics including speed calculations.
     * 
     * Calculates upload/download speeds based on bytes transferred since last update.
     * 
     * Requirement 7.3: Calculate upload/download speeds
     * Requirement 7.4: Track connection duration
     */
    private fun updateStatistics() {
        val currentTime = System.currentTimeMillis()
        val timeDeltaMs = currentTime - lastUpdateTime
        
        // Calculate speeds (bytes per second)
        // If time delta is 0 or negative, set speeds to 0 to avoid division by zero
        val bytesReceivedDelta = totalBytesReceived - lastBytesReceived
        val bytesSentDelta = totalBytesSent - lastBytesSent
        
        val downloadSpeed = if (timeDeltaMs > 0) {
            (bytesReceivedDelta * 1000) / timeDeltaMs
        } else {
            0L
        }
        
        val uploadSpeed = if (timeDeltaMs > 0) {
            (bytesSentDelta * 1000) / timeDeltaMs
        } else {
            0L
        }
        
        // Calculate connection duration
        val connectionDuration = currentTime - connectionStartTime
        
        // Update statistics
        _statistics.value = ConnectionStatistics(
            bytesReceived = totalBytesReceived,
            bytesSent = totalBytesSent,
            downloadSpeed = downloadSpeed,
            uploadSpeed = uploadSpeed,
            connectionDuration = connectionDuration,
            lastHandshakeTime = lastHandshakeTime,
            latency = latency
        )
        
        // Update tracking variables for next calculation
        lastBytesReceived = totalBytesReceived
        lastBytesSent = totalBytesSent
        lastUpdateTime = currentTime
    }
    
    /**
     * Gets the current statistics snapshot.
     * 
     * @return Current statistics or null if not monitoring
     */
    fun getCurrentStatistics(): ConnectionStatistics? {
        return _statistics.value
    }
}
