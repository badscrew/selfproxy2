package com.selfproxy.vpn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing VPN profiles.
 * 
 * Handles profile CRUD operations and exposes profile state to the UI.
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    
    /**
     * All profiles, ordered by last used (most recent first), then by name.
     */
    val profiles: StateFlow<List<ServerProfile>> = profileRepository
        .observeAllProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * UI state for profile operations.
     */
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    /**
     * Filter by protocol (null = show all).
     */
    private val _protocolFilter = MutableStateFlow<Protocol?>(null)
    val protocolFilter: StateFlow<Protocol?> = _protocolFilter.asStateFlow()
    
    /**
     * Filtered profiles based on protocol filter.
     */
    val filteredProfiles: StateFlow<List<ServerProfile>> = combine(
        profiles,
        protocolFilter
    ) { allProfiles, filter ->
        if (filter == null) {
            allProfiles
        } else {
            allProfiles.filter { it.protocol == filter }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * Creates a new profile.
     */
    fun createProfile(profile: ServerProfile) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            profileRepository.createProfile(profile)
                .onSuccess { id ->
                    _uiState.value = ProfileUiState.Success("Profile created successfully")
                }
                .onFailure { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Failed to create profile")
                }
        }
    }
    
    /**
     * Updates an existing profile.
     */
    fun updateProfile(profile: ServerProfile) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            profileRepository.updateProfile(profile)
                .onSuccess {
                    _uiState.value = ProfileUiState.Success("Profile updated successfully")
                }
                .onFailure { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Failed to update profile")
                }
        }
    }
    
    /**
     * Deletes a profile.
     */
    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            profileRepository.deleteProfile(profileId)
                .onSuccess {
                    _uiState.value = ProfileUiState.Success("Profile deleted successfully")
                }
                .onFailure { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Failed to delete profile")
                }
        }
    }
    
    /**
     * Gets a profile by ID.
     */
    suspend fun getProfile(profileId: Long): ServerProfile? {
        return profileRepository.getProfile(profileId)
    }
    
    /**
     * Sets the protocol filter.
     */
    fun setProtocolFilter(protocol: Protocol?) {
        _protocolFilter.value = protocol
    }
    
    /**
     * Clears the UI state.
     */
    fun clearUiState() {
        _uiState.value = ProfileUiState.Idle
    }
    
    /**
     * Searches profiles by name.
     */
    fun searchProfiles(query: String): Flow<List<ServerProfile>> = flow {
        if (query.isBlank()) {
            emit(profiles.value)
        } else {
            emit(profileRepository.searchProfiles(query))
        }
    }
}

/**
 * UI state for profile operations.
 */
sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val message: String) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
