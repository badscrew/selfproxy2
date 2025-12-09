package com.selfproxy.vpn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfproxy.vpn.data.model.AppSettings
import com.selfproxy.vpn.data.model.validate
import com.selfproxy.vpn.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the settings screen.
 * 
 * Manages application settings and user preferences.
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 11.2, 11.5
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val batteryOptimizationManager: com.selfproxy.vpn.domain.manager.BatteryOptimizationManager? = null,
    private val batteryMonitor: com.selfproxy.vpn.domain.manager.BatteryMonitor? = null
) : ViewModel() {
    
    // Current settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    // Validation errors
    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()
    
    // Save success state
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    // Battery optimization state
    private val _batteryOptimizationExempted = MutableStateFlow(false)
    val batteryOptimizationExempted: StateFlow<Boolean> = _batteryOptimizationExempted.asStateFlow()
    
    // Battery state
    private val _batteryState = MutableStateFlow(com.selfproxy.vpn.domain.manager.BatteryState())
    val batteryState: StateFlow<com.selfproxy.vpn.domain.manager.BatteryState> = _batteryState.asStateFlow()
    
    init {
        // Load settings on initialization
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { loadedSettings ->
                _settings.value = loadedSettings
            }
        }
        
        // Monitor battery optimization status
        batteryOptimizationManager?.let { manager ->
            _batteryOptimizationExempted.value = manager.isIgnoringBatteryOptimizations()
            
            // Observe battery state
            viewModelScope.launch {
                manager.batteryState.collect { state ->
                    _batteryState.value = state
                }
            }
        }
        
        // Monitor battery changes
        batteryMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.observeBatteryState().collect { batteryInfo ->
                    batteryOptimizationManager?.updateBatteryState(
                        batteryInfo.level,
                        batteryInfo.isCharging
                    )
                }
            }
        }
    }
    
    /**
     * Updates a setting value.
     * 
     * @param updater Function to update the settings
     */
    fun updateSettings(updater: (AppSettings) -> AppSettings) {
        _settings.value = updater(_settings.value)
        _saveSuccess.value = false
    }
    
    /**
     * Saves the current settings.
     * 
     * Validates settings before saving.
     */
    fun saveSettings() {
        viewModelScope.launch {
            val validation = _settings.value.validate()
            
            if (validation.isValid) {
                settingsRepository.updateSettings(_settings.value)
                _validationErrors.value = emptyList()
                _saveSuccess.value = true
            } else {
                _validationErrors.value = validation.errors
                _saveSuccess.value = false
            }
        }
    }
    
    /**
     * Resets settings to defaults.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            _validationErrors.value = emptyList()
            _saveSuccess.value = false
        }
    }
    
    /**
     * Clears the save success state.
     */
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
    
    /**
     * Clears validation errors.
     */
    fun clearValidationErrors() {
        _validationErrors.value = emptyList()
    }
    
    /**
     * Checks if battery optimization exemption should be requested.
     * 
     * Requirement 11.2: Request battery optimization exemption
     * 
     * @return true if exemption should be requested
     */
    fun shouldRequestBatteryOptimization(): Boolean {
        return batteryOptimizationManager?.shouldPromptForBatteryOptimization() ?: false
    }
    
    /**
     * Gets the battery optimization message for display.
     * 
     * @return User-friendly message about battery optimization
     */
    fun getBatteryOptimizationMessage(): String {
        return batteryOptimizationManager?.getBatteryOptimizationMessage()
            ?: "Battery optimization information not available"
    }
    
    /**
     * Refreshes the battery optimization status.
     */
    fun refreshBatteryOptimizationStatus() {
        batteryOptimizationManager?.let { manager ->
            _batteryOptimizationExempted.value = manager.isIgnoringBatteryOptimizations()
        }
    }
    
    /**
     * Gets the battery optimization manager for external use.
     * 
     * @return Battery optimization manager instance
     */
    fun getBatteryOptimizationManager(): com.selfproxy.vpn.domain.manager.BatteryOptimizationManager? {
        return batteryOptimizationManager
    }
}
