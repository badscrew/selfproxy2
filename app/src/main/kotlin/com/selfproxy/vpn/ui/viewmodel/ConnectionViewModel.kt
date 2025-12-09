package com.selfproxy.vpn.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.adapter.ConnectionTestResult
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.manager.ConnectionException
import com.selfproxy.vpn.domain.manager.ConnectionManager
import com.selfproxy.vpn.domain.manager.TrafficMonitor
import com.selfproxy.vpn.domain.manager.TrafficVerificationService
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.TrafficVerificationResult
import com.selfproxy.vpn.domain.model.VerificationState
import com.selfproxy.vpn.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the connection screen.
 * 
 * Manages connection state, statistics, user actions, and VPN permission handling.
 * 
 * Requirements: 3.4, 3.5, 7.1, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9
 */
class ConnectionViewModel(
    private val connectionManager: ConnectionManager,
    private val trafficMonitor: TrafficMonitor,
    private val profileRepository: ProfileRepository,
    private val trafficVerificationService: TrafficVerificationService
) : ViewModel() {
    
    // Connection state from manager
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    
    // Statistics from traffic monitor
    val statistics: StateFlow<ConnectionStatistics?> = trafficMonitor.statistics
    
    // Current profile being used for connection
    private val _currentProfile = MutableStateFlow<ServerProfile?>(null)
    val currentProfile: StateFlow<ServerProfile?> = _currentProfile.asStateFlow()
    
    // Connection test state
    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()
    
    // Testing in progress flag
    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()
    
    // VPN permission state
    private val _vpnPermissionState = MutableStateFlow<VpnPermissionState>(VpnPermissionState.Unknown)
    val vpnPermissionState: StateFlow<VpnPermissionState> = _vpnPermissionState.asStateFlow()
    
    // Show permission rationale dialog
    private val _showPermissionRationale = MutableStateFlow(false)
    val showPermissionRationale: StateFlow<Boolean> = _showPermissionRationale.asStateFlow()
    
    // Pending profile ID for connection after permission is granted
    private var pendingConnectionProfileId: Long? = null
    
    // VPN permission launcher callback
    private var vpnPermissionLauncher: ((Intent) -> Unit)? = null
    
    // Current error state
    private val _currentError = MutableStateFlow<ConnectionException?>(null)
    val currentError: StateFlow<ConnectionException?> = _currentError.asStateFlow()
    
    // Traffic verification state
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()
    
    init {
        // Load current profile if connected
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        loadCurrentProfile(state.connection.profileId)
                        trafficMonitor.start()
                        _currentError.value = null
                    }
                    is ConnectionState.Disconnected -> {
                        _currentProfile.value = null
                        trafficMonitor.stop()
                        _currentError.value = null
                    }
                    is ConnectionState.Error -> {
                        // Extract ConnectionException if available
                        val error = state.error as? ConnectionException
                        _currentError.value = error ?: ConnectionException(
                            message = state.error.message ?: "Unknown error",
                            cause = state.error
                        )
                    }
                    else -> {
                        // Connecting or Reconnecting - clear error
                        _currentError.value = null
                    }
                }
            }
        }
    }
    
    /**
     * Sets the VPN permission launcher callback.
     * 
     * This is called by MainActivity to provide a way to launch the VPN permission dialog.
     * 
     * @param launcher Callback to launch the VPN permission intent
     */
    fun setVpnPermissionLauncher(launcher: (Intent) -> Unit) {
        this.vpnPermissionLauncher = launcher
    }
    
    /**
     * Connects to a VPN server using the specified profile.
     * 
     * This method first checks if VPN permission is granted.
     * If not, it requests permission and stores the profile ID for later connection.
     * 
     * Requirements:
     * - 3.4: Request VPN permission before connecting
     * - 3.5: One-tap connection
     * 
     * @param profileId The ID of the profile to connect to
     */
    fun connect(profileId: Long) {
        viewModelScope.launch {
            // Load profile first
            val profile = profileRepository.getProfile(profileId)
            if (profile == null) {
                _vpnPermissionState.value = VpnPermissionState.Error("Profile not found")
                return@launch
            }
            
            _currentProfile.value = profile
            
            // Check if VPN permission is needed
            // Note: The actual permission check happens in MainActivity
            // We set the state to requesting and store the profile ID
            pendingConnectionProfileId = profileId
            _vpnPermissionState.value = VpnPermissionState.Requesting
        }
    }
    
    /**
     * Requests VPN permission with rationale.
     * 
     * Shows a dialog explaining why VPN permission is needed before requesting it.
     * 
     * Requirement 3.4: Show permission rationale to user
     */
    fun requestVpnPermissionWithRationale() {
        _showPermissionRationale.value = true
    }
    
    /**
     * Dismisses the permission rationale dialog.
     */
    fun dismissPermissionRationale() {
        _showPermissionRationale.value = false
    }
    
    /**
     * Proceeds with VPN permission request after showing rationale.
     * 
     * This is called when the user accepts the rationale and wants to proceed.
     */
    fun proceedWithPermissionRequest() {
        _showPermissionRationale.value = false
        // The actual permission request will be triggered by MainActivity
        // when it observes the Requesting state
    }
    
    /**
     * Called when VPN permission is granted.
     * 
     * Proceeds with the pending connection if there is one.
     * 
     * Requirement 3.4: Handle permission grant
     */
    fun onVpnPermissionGranted() {
        _vpnPermissionState.value = VpnPermissionState.Granted
        
        // Proceed with pending connection
        val profileId = pendingConnectionProfileId
        if (profileId != null) {
            viewModelScope.launch {
                connectionManager.connect(profileId)
            }
            pendingConnectionProfileId = null
        }
    }
    
    /**
     * Called when VPN permission is denied.
     * 
     * Updates the state and clears any pending connection.
     * 
     * Requirement 3.4: Handle permission denial
     */
    fun onVpnPermissionDenied() {
        _vpnPermissionState.value = VpnPermissionState.Denied
        pendingConnectionProfileId = null
    }
    
    /**
     * Clears the VPN permission state.
     * 
     * This is called after the user has acknowledged the permission result.
     */
    fun clearVpnPermissionState() {
        _vpnPermissionState.value = VpnPermissionState.Unknown
    }
    
    /**
     * Disconnects the current VPN connection.
     * 
     * Requirement 3.5: Disconnect action
     */
    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
        }
    }
    
    /**
     * Tests the connection to a VPN server.
     * 
     * Requirement 8.1-8.10: Connection testing
     * 
     * @param profileId The ID of the profile to test
     */
    fun testConnection(profileId: Long) {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            
            try {
                val profile = profileRepository.getProfile(profileId)
                if (profile == null) {
                    _testResult.value = ConnectionTestResult(
                        success = false,
                        errorMessage = "Profile not found"
                    )
                    return@launch
                }
                
                // Get the appropriate adapter
                val adapter = connectionManager.getCurrentAdapter()
                if (adapter == null) {
                    _testResult.value = ConnectionTestResult(
                        success = false,
                        errorMessage = "No active connection to test"
                    )
                    return@launch
                }
                
                // Test connection
                val result = adapter.testConnection(profile)
                _testResult.value = result.getOrElse {
                    ConnectionTestResult(
                        success = false,
                        errorMessage = it.message ?: "Connection test failed"
                    )
                }
            } finally {
                _isTesting.value = false
            }
        }
    }
    
    /**
     * Resets the accumulated statistics.
     * 
     * Requirement 7.10: Statistics reset
     */
    fun resetStatistics() {
        trafficMonitor.reset()
    }
    
    /**
     * Clears the test result.
     */
    fun clearTestResult() {
        _testResult.value = null
    }
    
    /**
     * Clears the current error.
     * 
     * Called when the user dismisses the error dialog.
     */
    fun clearError() {
        _currentError.value = null
    }
    
    /**
     * Gets the diagnostic report for the current error.
     * 
     * Requirement 12.8: Diagnostic information export
     * 
     * @return Diagnostic report string, or null if no error
     */
    fun getDiagnosticReport(): String? {
        return _currentError.value?.getDiagnosticReport()
    }
    
    /**
     * Verifies that traffic is being routed through the VPN.
     * 
     * Checks:
     * - Current external IP address
     * - Compares with VPN server IP
     * - Detects DNS leaks
     * 
     * Requirements:
     * - 8.9: IP address verification
     * - 8.10: DNS leak detection
     */
    fun verifyTraffic() {
        viewModelScope.launch {
            _verificationState.value = VerificationState.Verifying
            
            try {
                // Get expected VPN server IP from current profile
                val expectedIp = _currentProfile.value?.hostname
                
                // Perform verification
                val result = trafficVerificationService.verifyTraffic(expectedIp)
                
                _verificationState.value = VerificationState.Completed(result)
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Error(
                    e.message ?: "Verification failed"
                )
            }
        }
    }
    
    /**
     * Clears the verification result.
     */
    fun clearVerificationResult() {
        _verificationState.value = VerificationState.Idle
    }
    
    /**
     * Loads the current profile from repository.
     */
    private suspend fun loadCurrentProfile(profileId: Long) {
        val profile = profileRepository.getProfile(profileId)
        _currentProfile.value = profile
    }
    
    override fun onCleared() {
        super.onCleared()
        trafficVerificationService.cleanup()
    }
}

/**
 * Represents the state of VPN permission.
 * 
 * Used to track permission requests and results.
 */
sealed class VpnPermissionState {
    /** Permission state is unknown (initial state) */
    object Unknown : VpnPermissionState()
    
    /** Permission is being requested */
    object Requesting : VpnPermissionState()
    
    /** Permission was granted */
    object Granted : VpnPermissionState()
    
    /** Permission was denied by the user */
    object Denied : VpnPermissionState()
    
    /** An error occurred during permission handling */
    data class Error(val message: String) : VpnPermissionState()
}
