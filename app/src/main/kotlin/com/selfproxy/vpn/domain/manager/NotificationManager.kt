package com.selfproxy.vpn.domain.manager

import android.content.Context
import android.content.Intent
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.util.SanitizedLogger
import com.selfproxy.vpn.platform.vpn.TunnelVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages VPN service notifications based on connection state.
 * 
 * Observes connection state and statistics to update the notification
 * with relevant information.
 * 
 * Requirements:
 * - 3.5: VPN key icon displayed in status bar
 * - 11.9: Foreground service notification with connection status
 */
class NotificationManager(
    private val context: Context,
    private val connectionManager: ConnectionManager,
    private val trafficMonitor: TrafficMonitor
) {
    companion object {
        private const val TAG = "NotificationManager"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Starts observing connection state and updating notifications.
     */
    fun startObserving() {
        SanitizedLogger.d(TAG, "Starting notification observation")
        
        // Observe connection state changes
        scope.launch {
            connectionManager.connectionState.collect { state ->
                updateNotificationForState(state)
            }
        }
        
        // Observe statistics for connected state
        scope.launch {
            trafficMonitor.statistics.collect { stats ->
                if (connectionManager.connectionState.value is ConnectionState.Connected && stats != null) {
                    updateNotificationWithStatistics(stats)
                }
            }
        }
    }
    
    /**
     * Updates the notification based on connection state.
     * 
     * @param state The current connection state
     */
    private fun updateNotificationForState(state: ConnectionState) {
        try {
            when (state) {
                is ConnectionState.Disconnected -> {
                    // Service should be stopped when disconnected
                    SanitizedLogger.d(TAG, "Connection disconnected - notification will be removed")
                }
                
                is ConnectionState.Connecting -> {
                    sendNotificationUpdate("Connecting", null, null)
                }
                
                is ConnectionState.Connected -> {
                    val profileName = state.connection.serverAddress
                    sendNotificationUpdate("Connected", profileName, null)
                }
                
                is ConnectionState.Reconnecting -> {
                    sendNotificationUpdate("Reconnecting", null, null)
                }
                
                is ConnectionState.Error -> {
                    val errorMessage = state.error.message?.take(50) ?: "Connection error"
                    sendNotificationUpdate("Error", null, errorMessage)
                }
            }
        } catch (e: Exception) {
            SanitizedLogger.e(TAG, "Error updating notification for state", e)
        }
    }
    
    /**
     * Updates the notification with connection statistics.
     * 
     * @param stats The current connection statistics
     */
    private fun updateNotificationWithStatistics(stats: ConnectionStatistics) {
        try {
            val state = connectionManager.connectionState.value
            if (state is ConnectionState.Connected) {
                val profileName = state.connection.serverAddress
                val additionalInfo = formatStatistics(stats)
                sendNotificationUpdate("Connected", profileName, additionalInfo)
            }
        } catch (e: Exception) {
            SanitizedLogger.e(TAG, "Error updating notification with statistics", e)
        }
    }
    
    /**
     * Formats statistics for display in notification.
     * 
     * @param stats The connection statistics
     * @return Formatted string with key statistics
     */
    private fun formatStatistics(stats: ConnectionStatistics): String {
        val downloadSpeed = formatBytes(stats.downloadSpeed) + "/s"
        val uploadSpeed = formatBytes(stats.uploadSpeed) + "/s"
        val totalData = formatBytes(stats.bytesReceived + stats.bytesSent)
        
        return "↓ $downloadSpeed ↑ $uploadSpeed • $totalData"
    }
    
    /**
     * Formats bytes into human-readable format.
     * 
     * @param bytes Number of bytes
     * @return Formatted string (e.g., "1.5 MB", "500 KB")
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Sends a notification update to the VPN service.
     * 
     * @param status The connection status
     * @param profileName Optional profile name
     * @param additionalInfo Optional additional information
     */
    private fun sendNotificationUpdate(
        status: String,
        profileName: String?,
        additionalInfo: String?
    ) {
        try {
            val intent = Intent(context, TunnelVpnService::class.java).apply {
                action = "UPDATE_NOTIFICATION"
                putExtra("status", status)
                putExtra("profile_name", profileName)
                putExtra("additional_info", additionalInfo)
            }
            context.startService(intent)
        } catch (e: Exception) {
            SanitizedLogger.e(TAG, "Error sending notification update", e)
        }
    }
}
