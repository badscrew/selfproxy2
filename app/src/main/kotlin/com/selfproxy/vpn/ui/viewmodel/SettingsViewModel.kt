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
 * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
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
    
    init {
        // Load settings on initialization
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { loadedSettings ->
                _settings.value = loadedSettings
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
}
