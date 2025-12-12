package com.selfproxy.vpn.domain.manager

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.model.VpnError
import com.selfproxy.vpn.domain.model.getDiagnosticReport
import com.selfproxy.vpn.domain.model.toVpnError
import com.selfproxy.vpn.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages VPN connections across all protocols.
 * 
 * Responsibilities:
 * - Protocol adapter selection based on profile type
 * - Connection state management
 * - Error handling and error message generation
 * - Integration with auto-reconnect service
 * - Battery optimization integration
 * 
 * Requirements: 3.3, 3.6, 3.7, 11.1, 11.3, 11.5
 */
class ConnectionManager(
    private val wireGuardAdapter: ProtocolAdapter,
    private val vlessAdapter: ProtocolAdapter,
    private val profileRepository: ProfileRepository,
    private val batteryOptimizationManager: BatteryOptimizationManager? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private var currentAdapter: ProtocolAdapter? = null
    private var currentProfileId: Long? = null
    
    // Auto-reconnect service (will be set via setter injection to avoid circular dependency)
    private var autoReconnectService: AutoReconnectService? = null
    
    /**
     * Sets the auto-reconnect service.
     * 
     * Uses setter injection to avoid circular dependency.
     * 
     * @param service The auto-reconnect service
     */
    fun setAutoReconnectService(service: AutoReconnectService) {
        this.autoReconnectService = service
    }
    
    /**
     * Connects to a VPN server using the specified profile.
     * 
     * Automatically selects the appropriate protocol adapter based on the profile's protocol type.
     * Updates connection state throughout the connection process.
     * 
     * @param profileId The ID of the profile to connect to
     * @return Result indicating success or failure with specific error message
     */
    suspend fun connect(profileId: Long): Result<Unit> {
        try {
            // Get profile from repository
            val profile = profileRepository.getProfile(profileId)
                ?: return Result.failure(ConnectionException("Profile not found"))
            
            // Validate profile
            val validationResult = profileRepository.validateProfile(profile)
            if (validationResult.isFailure) {
                val error = ConnectionException(
                    "Invalid profile configuration: ${validationResult.exceptionOrNull()?.message}"
                )
                _connectionState.value = ConnectionState.Error(error)
                return Result.failure(error)
            }
            
            // Disconnect if already connected
            if (_connectionState.value is ConnectionState.Connected) {
                disconnect()
            }
            
            // Select appropriate adapter based on protocol
            val adapter = selectAdapter(profile.protocol)
            
            // Update state to connecting
            _connectionState.value = ConnectionState.Connecting
            
            // Attempt connection
            val connectionResult = adapter.connect(profile)
            
            if (connectionResult.isSuccess) {
                val connection = connectionResult.getOrThrow()
                currentAdapter = adapter
                currentProfileId = profileId
                
                // Update last used timestamp
                profileRepository.updateLastUsed(profileId)
                
                // Observe adapter state changes
                observeAdapterState(adapter)
                
                _connectionState.value = ConnectionState.Connected(connection)
                
                // Enable auto-reconnect for this profile
                autoReconnectService?.enable(profileId)
                
                return Result.success(Unit)
            } else {
                val error = connectionResult.exceptionOrNull()
                val connectionError = generateConnectionError(error, profile.protocol)
                _connectionState.value = ConnectionState.Error(connectionError)
                return Result.failure(connectionError)
            }
            
        } catch (e: Exception) {
            val error = ConnectionException("Unexpected error during connection: ${e.message}", e)
            _connectionState.value = ConnectionState.Error(error)
            return Result.failure(error)
        }
    }
    
    /**
     * Disconnects the current VPN connection.
     * 
     * Safely disconnects regardless of current state.
     * Disables auto-reconnect when manually disconnecting.
     */
    suspend fun disconnect() {
        try {
            // Disable auto-reconnect for manual disconnects
            autoReconnectService?.disable()
            
            currentAdapter?.disconnect()
            currentAdapter = null
            currentProfileId = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            // Always mark as disconnected even if disconnect fails
            autoReconnectService?.disable()
            currentAdapter = null
            currentProfileId = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }
    
    /**
     * Gets the currently connected profile ID.
     * 
     * @return The profile ID if connected, null otherwise
     */
    fun getCurrentProfileId(): Long? {
        return if (_connectionState.value is ConnectionState.Connected) {
            currentProfileId
        } else {
            null
        }
    }
    
    /**
     * Gets the current protocol adapter.
     * 
     * @return The active adapter if connected, null otherwise
     */
    fun getCurrentAdapter(): ProtocolAdapter? {
        return if (_connectionState.value is ConnectionState.Connected) {
            currentAdapter
        } else {
            null
        }
    }
    
    /**
     * Gets current connection statistics from the active adapter.
     * 
     * @return Current statistics or null if not connected
     */
    fun getCurrentStatistics(): com.selfproxy.vpn.domain.adapter.ConnectionStatistics? {
        return currentAdapter?.getStatistics()
    }
    
    /**
     * Gets the recommended keep-alive interval based on battery state.
     * 
     * Requirements:
     * - 11.1: Configurable keep-alive intervals
     * - 11.3: Efficient polling intervals
     * - 11.5: Adjust intervals in battery saver mode
     * 
     * @param batteryLevel Current battery level (0-100)
     * @param isNatTraversal Whether NAT traversal is needed
     * @return Recommended keep-alive interval in seconds
     */
    fun getRecommendedKeepAliveInterval(
        batteryLevel: Int,
        isNatTraversal: Boolean = true
    ): Int {
        return batteryOptimizationManager?.getRecommendedKeepAliveInterval(
            batteryLevel,
            isNatTraversal
        ) ?: BatteryOptimizationManager.KEEPALIVE_NORMAL
    }
    
    /**
     * Selects the appropriate protocol adapter based on the protocol type.
     * 
     * Requirement 3.3: Protocol adapter selection
     * 
     * @param protocol The protocol type
     * @return The corresponding protocol adapter
     */
    private fun selectAdapter(protocol: Protocol): ProtocolAdapter {
        return when (protocol) {
            Protocol.WIREGUARD -> wireGuardAdapter
            Protocol.VLESS -> vlessAdapter
        }
    }
    
    /**
     * Observes state changes from the protocol adapter.
     * 
     * Synchronizes adapter state with connection manager state.
     */
    private fun observeAdapterState(adapter: ProtocolAdapter) {
        scope.launch {
            adapter.observeConnectionState().collect { adapterState ->
                // Only update if this is still the current adapter
                if (adapter == currentAdapter) {
                    when (adapterState) {
                        is ConnectionState.Error -> {
                            _connectionState.value = adapterState
                        }
                        is ConnectionState.Disconnected -> {
                            if (_connectionState.value is ConnectionState.Connected) {
                                // Unexpected disconnection
                                val error = ConnectionException("Connection lost unexpectedly")
                                _connectionState.value = ConnectionState.Error(error)
                            }
                        }
                        else -> {
                            // Adapter handles other states
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Generates a user-friendly error message based on the error and protocol.
     * 
     * Requirements:
     * - 3.6: Connection error messaging
     * - 12.1-12.7: Protocol-specific error handling
     * 
     * @param error The original error
     * @param protocol The protocol being used
     * @return A ConnectionException with a specific, actionable error message
     */
    private fun generateConnectionError(error: Throwable?, protocol: Protocol): ConnectionException {
        if (error == null) {
            return ConnectionException("Unknown error occurred", null)
        }
        
        // Convert to VpnError for structured error handling
        val vpnError = error.toVpnError(protocol)
        
        // Create ConnectionException with VpnError information
        return ConnectionException(
            message = vpnError.message,
            cause = error,
            vpnError = vpnError
        )
    }
}

/**
 * Exception thrown when connection operations fail.
 * 
 * Contains user-friendly error messages suitable for display in the UI.
 * Includes structured VpnError for detailed error information and diagnostics.
 * 
 * Requirements:
 * - 3.6: Specific error messages
 * - 12.8: Diagnostic information collection
 */
class ConnectionException(
    message: String,
    cause: Throwable? = null,
    val vpnError: VpnError? = null
) : Exception(message, cause) {
    
    /**
     * Gets the suggested action for this error.
     */
    fun getSuggestedAction(): String {
        return vpnError?.suggestedAction ?: "Check your configuration and try again."
    }
    
    /**
     * Gets diagnostic information for troubleshooting.
     */
    fun getDiagnosticInfo(): Map<String, String> {
        return vpnError?.diagnosticInfo ?: emptyMap()
    }
    
    /**
     * Gets a full diagnostic report for export.
     */
    fun getDiagnosticReport(): String {
        return if (vpnError != null) {
            vpnError.getDiagnosticReport()
        } else {
            buildString {
                appendLine("Error: $message")
                appendLine()
                appendLine("Cause: ${cause?.message ?: "Unknown"}")
                appendLine()
                appendLine("Timestamp: ${System.currentTimeMillis()}")
            }
        }
    }
}
