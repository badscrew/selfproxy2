package com.selfproxy.vpn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.adapter.ConnectionTestResult
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.manager.ConnectionManager
import com.selfproxy.vpn.domain.manager.TrafficMonitor
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the connection screen.
 * 
 * Manages connection state, statistics, and user actions.
 * 
 * Requirements: 3.5, 7.1, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9
 */
class ConnectionViewModel(
    private val connectionManager: ConnectionManager,
    private val trafficMonitor: TrafficMonitor,
    private val profileRepository: ProfileRepository
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
    
    init {
        // Load current profile if connected
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    loadCurrentProfile(state.connection.profileId)
                    trafficMonitor.start()
                } else if (state is ConnectionState.Disconnected) {
                    _currentProfile.value = null
                    trafficMonitor.stop()
                }
            }
        }
    }
    
    /**
     * Connects to a VPN server using the specified profile.
     * 
     * Requirement 3.5: One-tap connection
     * 
     * @param profileId The ID of the profile to connect to
     */
    fun connect(profileId: Long) {
        viewModelScope.launch {
            // Load profile first
            val profile = profileRepository.getProfile(profileId)
            if (profile != null) {
                _currentProfile.value = profile
                connectionManager.connect(profileId)
            }
        }
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
     * Loads the current profile from repository.
     */
    private suspend fun loadCurrentProfile(profileId: Long) {
        val profile = profileRepository.getProfile(profileId)
        _currentProfile.value = profile
    }
}
