package com.selfproxy.vpn.domain.manager

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
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
     * Requirement 3.6: Connection error messaging
     * 
     * @param error The original error
     * @param protocol The protocol being used
     * @return A ConnectionException with a specific, actionable error message
     */
    private fun generateConnectionError(error: Throwable?, protocol: Protocol): ConnectionException {
        val message = error?.message ?: "Unknown error"
        
        // Check for common error patterns and generate specific messages
        return when {
            // Authentication errors
            message.contains("authentication", ignoreCase = true) ||
            message.contains("invalid key", ignoreCase = true) ||
            message.contains("invalid uuid", ignoreCase = true) -> {
                when (protocol) {
                    Protocol.WIREGUARD -> ConnectionException(
                        "Authentication failed: Invalid WireGuard keys. Please verify your private key and server's public key.",
                        error
                    )
                    Protocol.VLESS -> ConnectionException(
                        "Authentication failed: Invalid UUID. Please verify your VLESS UUID is correct.",
                        error
                    )
                }
            }
            
            // Network unreachable errors
            message.contains("unreachable", ignoreCase = true) ||
            message.contains("cannot resolve", ignoreCase = true) ||
            message.contains("network", ignoreCase = true) -> {
                ConnectionException(
                    "Server unreachable: Cannot connect to ${protocol.name} server. Check your internet connection and server address.",
                    error
                )
            }
            
            // Timeout errors
            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) -> {
                when (protocol) {
                    Protocol.WIREGUARD -> ConnectionException(
                        "Connection timeout: No response from WireGuard server. Check firewall settings and ensure UDP port is open.",
                        error
                    )
                    Protocol.VLESS -> ConnectionException(
                        "Connection timeout: No response from VLESS server. Check firewall settings and server configuration.",
                        error
                    )
                }
            }
            
            // Handshake errors (WireGuard-specific)
            message.contains("handshake", ignoreCase = true) -> {
                ConnectionException(
                    "Handshake failed: WireGuard handshake timeout. Verify keys are correct and server is running.",
                    error
                )
            }
            
            // Configuration errors
            message.contains("invalid configuration", ignoreCase = true) ||
            message.contains("invalid format", ignoreCase = true) -> {
                ConnectionException(
                    "Invalid configuration: ${message}. Please check your profile settings.",
                    error
                )
            }
            
            // TLS/Certificate errors (VLESS-specific)
            message.contains("certificate", ignoreCase = true) ||
            message.contains("tls", ignoreCase = true) -> {
                ConnectionException(
                    "TLS error: Certificate validation failed. Check server certificate and TLS settings.",
                    error
                )
            }
            
            // Transport protocol errors (VLESS-specific)
            message.contains("transport", ignoreCase = true) ||
            message.contains("websocket", ignoreCase = true) ||
            message.contains("grpc", ignoreCase = true) -> {
                ConnectionException(
                    "Transport protocol error: Failed to establish ${protocol.name} connection. Try a different transport protocol.",
                    error
                )
            }
            
            // Permission errors
            message.contains("permission", ignoreCase = true) -> {
                ConnectionException(
                    "Permission denied: VPN permission required. Please grant VPN permission in settings.",
                    error
                )
            }
            
            // Generic error with protocol context
            else -> {
                ConnectionException(
                    "Connection failed: ${message}. Protocol: ${protocol.name}",
                    error
                )
            }
        }
    }
}

/**
 * Exception thrown when connection operations fail.
 * 
 * Contains user-friendly error messages suitable for display in the UI.
 */
class ConnectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
