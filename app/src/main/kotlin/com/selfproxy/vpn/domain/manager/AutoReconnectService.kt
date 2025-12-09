package com.selfproxy.vpn.domain.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.pow

/**
 * Service responsible for automatic reconnection when VPN connection drops.
 * 
 * Features:
 * - Connection drop detection
 * - Exponential backoff (1s to 60s)
 * - Network change monitoring
 * - Reconnection attempt counting
 * - User notification after failures
 * - Manual disconnect handling
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.8, 6.9, 6.10
 */
class AutoReconnectService(
    private val context: Context,
    private val connectionManager: ConnectionManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private var isEnabled = false
    private var currentProfileId: Long? = null
    private var reconnectJob: Job? = null
    private var monitoringJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    private var attemptCount = 0
    private var isManualDisconnect = false
    
    private val _reconnectState = MutableStateFlow<ReconnectState>(ReconnectState.Idle)
    val reconnectState: StateFlow<ReconnectState> = _reconnectState.asStateFlow()
    
    /**
     * Enables auto-reconnection for the specified profile.
     * 
     * Requirement 6.2: Auto-reconnect service attempts to re-establish connection
     * 
     * @param profileId The profile ID to reconnect to
     */
    fun enable(profileId: Long) {
        isEnabled = true
        currentProfileId = profileId
        attemptCount = 0
        isManualDisconnect = false
        
        // Start monitoring connection state (only if not already monitoring)
        if (monitoringJob == null) {
            monitoringJob = monitorConnectionState()
        }
        
        // Start monitoring network changes
        startNetworkMonitoring()
    }
    
    /**
     * Disables auto-reconnection.
     * 
     * Requirement 6.10: Manual disconnect disables auto-reconnect
     */
    fun disable() {
        isEnabled = false
        isManualDisconnect = true
        currentProfileId = null
        attemptCount = 0
        
        // Cancel any ongoing reconnection attempts
        reconnectJob?.cancel()
        reconnectJob = null
        
        // Cancel monitoring
        monitoringJob?.cancel()
        monitoringJob = null
        
        // Stop network monitoring
        stopNetworkMonitoring()
        
        _reconnectState.value = ReconnectState.Idle
    }
    
    /**
     * Monitors the connection state and triggers reconnection on drops.
     * 
     * Requirement 6.1: Detect disconnection within 10 seconds
     */
    private fun monitorConnectionState(): Job {
        return scope.launch {
            connectionManager.connectionState.collect { state ->
                if (!isEnabled || isManualDisconnect) return@collect
                
                when (state) {
                    is com.selfproxy.vpn.domain.model.ConnectionState.Error -> {
                        // Connection dropped unexpectedly
                        handleConnectionDrop()
                    }
                    is com.selfproxy.vpn.domain.model.ConnectionState.Disconnected -> {
                        // Only reconnect if we were previously connected and it wasn't manual
                        if (_reconnectState.value is ReconnectState.Reconnecting) {
                            handleConnectionDrop()
                        }
                    }
                    is com.selfproxy.vpn.domain.model.ConnectionState.Connected -> {
                        // Connection restored, reset attempt counter
                        if (_reconnectState.value is ReconnectState.Reconnecting) {
                            attemptCount = 0
                            _reconnectState.value = ReconnectState.Connected
                        }
                    }
                    else -> {
                        // Ignore other states
                    }
                }
            }
        }
    }
    
    /**
     * Handles connection drop by initiating reconnection with exponential backoff.
     * 
     * Requirement 6.2: Attempt to re-establish connection
     * Requirement 6.3: Use exponential backoff (1s to 60s)
     * Requirement 6.4: Notify user after 5 failed attempts
     */
    private fun handleConnectionDrop() {
        if (!isEnabled || isManualDisconnect) return
        
        val profileId = currentProfileId ?: return
        
        // Cancel any existing reconnection job
        reconnectJob?.cancel()
        
        reconnectJob = scope.launch {
            while (isEnabled && !isManualDisconnect) {
                attemptCount++
                
                // Calculate backoff delay using exponential backoff
                val delaySeconds = calculateBackoffDelay(attemptCount)
                
                _reconnectState.value = ReconnectState.Reconnecting(
                    attemptCount = attemptCount,
                    nextAttemptIn = delaySeconds
                )
                
                // Notify user after 5 failed attempts
                if (attemptCount >= 5) {
                    _reconnectState.value = ReconnectState.FailedMultipleTimes(
                        attemptCount = attemptCount,
                        nextAttemptIn = delaySeconds
                    )
                }
                
                // Wait before attempting reconnection
                delay(delaySeconds * 1000L)
                
                // Attempt reconnection
                val result = connectionManager.connect(profileId)
                
                if (result.isSuccess) {
                    // Reconnection successful
                    attemptCount = 0
                    _reconnectState.value = ReconnectState.Connected
                    break
                } else {
                    // Reconnection failed, continue loop
                    continue
                }
            }
        }
    }
    
    /**
     * Calculates the backoff delay using exponential backoff.
     * 
     * Requirement 6.3: Exponential backoff starting at 1 second up to maximum of 60 seconds
     * 
     * Formula: min(60, 2^(attempt-1))
     * - Attempt 1: 1 second
     * - Attempt 2: 2 seconds
     * - Attempt 3: 4 seconds
     * - Attempt 4: 8 seconds
     * - Attempt 5: 16 seconds
     * - Attempt 6: 32 seconds
     * - Attempt 7+: 60 seconds (capped)
     * 
     * @param attempt The current attempt number (1-based)
     * @return The delay in seconds
     */
    fun calculateBackoffDelay(attempt: Int): Int {
        if (attempt <= 0) return 1
        
        val exponentialDelay = 2.0.pow(attempt - 1).toInt()
        return min(exponentialDelay, 60)
    }
    
    /**
     * Starts monitoring network changes.
     * 
     * Requirement 6.5: Re-establish tunnel on network change (WiFi ↔ Mobile)
     */
    private fun startNetworkMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network became available
                handleNetworkChange()
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                // Network type changed (e.g., WiFi to Mobile)
                handleNetworkChange()
            }
            
            override fun onLost(network: Network) {
                // Network lost - will be handled by connection drop detection
            }
        }
        
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }
    
    /**
     * Stops monitoring network changes.
     */
    private fun stopNetworkMonitoring() {
        networkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Callback may already be unregistered
            }
        }
        networkCallback = null
    }
    
    /**
     * Handles network changes by triggering reconnection.
     * 
     * Requirement 6.5: Re-establish VPN tunnel on network change
     * Requirement 6.6: Restore TUN interface and resume traffic routing
     */
    private fun handleNetworkChange() {
        if (!isEnabled || isManualDisconnect) return
        
        val profileId = currentProfileId ?: return
        
        // Check if we're currently connected
        val currentState = connectionManager.connectionState.value
        if (currentState is com.selfproxy.vpn.domain.model.ConnectionState.Connected) {
            // Network changed while connected, trigger reconnection
            scope.launch {
                // Disconnect current connection
                connectionManager.disconnect()
                
                // Small delay to allow network to stabilize
                delay(1000)
                
                // Attempt reconnection
                attemptCount = 0 // Reset counter for network changes
                val result = connectionManager.connect(profileId)
                
                if (result.isSuccess) {
                    _reconnectState.value = ReconnectState.Connected
                } else {
                    // Failed to reconnect, start exponential backoff
                    handleConnectionDrop()
                }
            }
        }
    }
    
    /**
     * Gets the current reconnection attempt count.
     * 
     * @return The number of reconnection attempts
     */
    fun getAttemptCount(): Int = attemptCount
    
    /**
     * Checks if auto-reconnect is currently enabled.
     * 
     * @return True if enabled, false otherwise
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Checks if the last disconnect was manual.
     * 
     * @return True if manual disconnect, false otherwise
     */
    fun isManualDisconnect(): Boolean = isManualDisconnect
}

/**
 * Represents the state of the auto-reconnect service.
 */
sealed class ReconnectState {
    object Idle : ReconnectState()
    object Connected : ReconnectState()
    data class Reconnecting(
        val attemptCount: Int,
        val nextAttemptIn: Int
    ) : ReconnectState()
    data class FailedMultipleTimes(
        val attemptCount: Int,
        val nextAttemptIn: Int
    ) : ReconnectState()
}
