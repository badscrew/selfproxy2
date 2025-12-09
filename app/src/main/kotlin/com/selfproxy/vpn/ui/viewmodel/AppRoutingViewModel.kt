package com.selfproxy.vpn.ui.viewmodel

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfproxy.vpn.data.model.AppRoutingConfig
import com.selfproxy.vpn.data.model.InstalledApp
import com.selfproxy.vpn.data.repository.AppRoutingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the app routing screen.
 * 
 * Manages per-app routing configuration and installed app list.
 * 
 * Requirements:
 * - 5.1: Display list of installed applications
 * - 5.2, 5.3: Exclude/include apps from VPN tunnel
 * - 5.4: Persist app routing configuration
 * - 5.6: Support dynamic routing updates
 * - 5.8: Support "Route All Apps" or "Route Selected Apps Only" modes
 */
class AppRoutingViewModel(
    private val appRoutingRepository: AppRoutingRepository
) : ViewModel() {
    
    // Current profile ID (null for global config)
    private val _profileId = MutableStateFlow<Long?>(null)
    val profileId: StateFlow<Long?> = _profileId.asStateFlow()
    
    // All installed apps
    private val _allApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val allApps: StateFlow<List<InstalledApp>> = _allApps.asStateFlow()
    
    // Filtered apps (based on search query)
    private val _filteredApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val filteredApps: StateFlow<List<InstalledApp>> = _filteredApps.asStateFlow()
    
    // Current routing configuration
    private val _config = MutableStateFlow<AppRoutingConfig?>(null)
    val config: StateFlow<AppRoutingConfig?> = _config.asStateFlow()
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Include system apps
    private val _includeSystemApps = MutableStateFlow(false)
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps.asStateFlow()
    
    // Save success state
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    /**
     * Loads app routing configuration for a specific profile.
     * 
     * @param profileId The profile ID, or null for global config
     */
    fun loadConfig(profileId: Long? = null) {
        _profileId.value = profileId
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Load configuration
                val config = if (profileId != null) {
                    appRoutingRepository.getConfigForProfile(profileId)
                        ?: appRoutingRepository.createDefaultConfig(profileId)
                } else {
                    appRoutingRepository.getGlobalConfig()
                }
                _config.value = config
                
                // Load installed apps
                loadInstalledApps()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Loads the list of installed applications.
     * 
     * Requirement 5.1: Display list of installed applications.
     */
    private suspend fun loadInstalledApps() {
        val apps = appRoutingRepository.getInstalledApps(_includeSystemApps.value)
        val config = _config.value ?: return
        
        // Mark apps as selected based on current configuration
        val updatedApps = apps.map { app ->
            val isSelected = if (config.routeAllApps) {
                // Route all mode: selected means NOT excluded
                !config.packageNames.contains(app.packageName)
            } else {
                // Route selected mode: selected means included
                config.packageNames.contains(app.packageName)
            }
            
            app.copy(isSelected = isSelected)
        }
        
        _allApps.value = updatedApps
        applySearchFilter()
    }
    
    /**
     * Toggles whether to include system apps in the list.
     */
    fun toggleIncludeSystemApps() {
        viewModelScope.launch {
            _includeSystemApps.value = !_includeSystemApps.value
            loadInstalledApps()
        }
    }
    
    /**
     * Updates the search query and filters the app list.
     * 
     * @param query The search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applySearchFilter()
    }
    
    /**
     * Applies the search filter to the app list.
     */
    private fun applySearchFilter() {
        val query = _searchQuery.value.lowercase()
        _filteredApps.value = if (query.isEmpty()) {
            _allApps.value
        } else {
            _allApps.value.filter { app ->
                app.appName.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
            }
        }
    }
    
    /**
     * Toggles the routing mode between "Route All Apps" and "Route Selected Apps Only".
     * 
     * Requirement 5.8: Support "Route All Apps" or "Route Selected Apps Only" modes.
     */
    fun toggleRoutingMode() {
        val currentConfig = _config.value ?: return
        val newConfig = currentConfig.copy(
            routeAllApps = !currentConfig.routeAllApps,
            // When switching modes, clear the package list
            packageNames = emptySet()
        )
        _config.value = newConfig
        
        // Update app selection states
        viewModelScope.launch {
            loadInstalledApps()
        }
    }
    
    /**
     * Toggles the selection state of an app.
     * 
     * Requirements:
     * - 5.2: Exclude apps from VPN tunnel
     * - 5.3: Include apps in VPN tunnel
     * 
     * @param app The app to toggle
     */
    fun toggleAppSelection(app: InstalledApp) {
        // Don't allow toggling the VPN app itself
        if (app.isSelfApp) return
        
        val currentConfig = _config.value ?: return
        val packageNames = currentConfig.packageNames.toMutableSet()
        
        if (currentConfig.routeAllApps) {
            // Route all mode: packageNames contains excluded apps
            if (packageNames.contains(app.packageName)) {
                packageNames.remove(app.packageName)
            } else {
                packageNames.add(app.packageName)
            }
        } else {
            // Route selected mode: packageNames contains included apps
            if (packageNames.contains(app.packageName)) {
                packageNames.remove(app.packageName)
            } else {
                packageNames.add(app.packageName)
            }
        }
        
        _config.value = currentConfig.copy(packageNames = packageNames)
        
        // Update the app in the list
        _allApps.value = _allApps.value.map { 
            if (it.packageName == app.packageName) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
        applySearchFilter()
    }
    
    /**
     * Selects all apps.
     */
    fun selectAll() {
        val currentConfig = _config.value ?: return
        
        if (currentConfig.routeAllApps) {
            // Route all mode: clear excluded apps (select all)
            _config.value = currentConfig.copy(packageNames = emptySet())
        } else {
            // Route selected mode: add all apps to included list
            val allPackages = _allApps.value
                .filter { !it.isSelfApp }
                .map { it.packageName }
                .toSet()
            _config.value = currentConfig.copy(packageNames = allPackages)
        }
        
        // Update app selection states
        viewModelScope.launch {
            loadInstalledApps()
        }
    }
    
    /**
     * Deselects all apps.
     */
    fun deselectAll() {
        val currentConfig = _config.value ?: return
        
        if (currentConfig.routeAllApps) {
            // Route all mode: exclude all apps
            val allPackages = _allApps.value
                .filter { !it.isSelfApp }
                .map { it.packageName }
                .toSet()
            _config.value = currentConfig.copy(packageNames = allPackages)
        } else {
            // Route selected mode: clear included apps (deselect all)
            _config.value = currentConfig.copy(packageNames = emptySet())
        }
        
        // Update app selection states
        viewModelScope.launch {
            loadInstalledApps()
        }
    }
    
    /**
     * Saves the current configuration.
     * 
     * Requirement 5.4: Persist app routing configuration.
     */
    fun saveConfig() {
        viewModelScope.launch {
            val config = _config.value ?: return@launch
            
            val result = if (config.id == 0L) {
                appRoutingRepository.saveConfig(config)
            } else {
                appRoutingRepository.updateConfig(config)
            }
            
            if (result.isSuccess) {
                _saveSuccess.value = true
            }
        }
    }
    
    /**
     * Clears the save success state.
     */
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
    
    /**
     * Gets the app icon for a package.
     * 
     * @param packageName The package name
     * @return The app icon, or null if not found
     */
    suspend fun getAppIcon(packageName: String): Drawable? {
        return appRoutingRepository.getAppIcon(packageName)
    }
}
