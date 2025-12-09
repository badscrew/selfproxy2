package com.selfproxy.vpn.domain.repository

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.model.Protocol
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing VPN server profiles.
 * 
 * Provides CRUD operations and profile validation for both WireGuard and VLESS profiles.
 * All operations return Result types for consistent error handling.
 */
interface ProfileRepository {
    
    /**
     * Creates a new server profile.
     * 
     * Validates the profile before insertion and ensures all required fields are present.
     * 
     * @param profile The profile to create
     * @return Result containing the ID of the created profile, or an error
     */
    suspend fun createProfile(profile: ServerProfile): Result<Long>
    
    /**
     * Retrieves a profile by its ID.
     * 
     * @param id The profile ID
     * @return The profile, or null if not found
     */
    suspend fun getProfile(id: Long): ServerProfile?
    
    /**
     * Retrieves all profiles.
     * 
     * Profiles are ordered by last used (most recent first), then by name.
     * 
     * @return List of all profiles
     */
    suspend fun getAllProfiles(): List<ServerProfile>
    
    /**
     * Observes all profiles.
     * 
     * Emits a new list whenever profiles are added, updated, or deleted.
     * 
     * @return Flow of all profiles
     */
    fun observeAllProfiles(): Flow<List<ServerProfile>>
    
    /**
     * Retrieves all profiles for a specific protocol.
     * 
     * @param protocol The protocol to filter by
     * @return List of profiles for the specified protocol
     */
    suspend fun getProfilesByProtocol(protocol: Protocol): List<ServerProfile>
    
    /**
     * Observes all profiles for a specific protocol.
     * 
     * @param protocol The protocol to filter by
     * @return Flow of profiles for the specified protocol
     */
    fun observeProfilesByProtocol(protocol: Protocol): Flow<List<ServerProfile>>
    
    /**
     * Updates an existing profile.
     * 
     * Validates the profile before updating.
     * 
     * @param profile The profile to update (must have a valid ID)
     * @return Result indicating success or failure
     */
    suspend fun updateProfile(profile: ServerProfile): Result<Unit>
    
    /**
     * Deletes a profile by its ID.
     * 
     * @param id The ID of the profile to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteProfile(id: Long): Result<Unit>
    
    /**
     * Updates the last used timestamp for a profile.
     * 
     * Called when a profile is used to establish a connection.
     * 
     * @param id The profile ID
     * @param timestamp The timestamp to set (defaults to current time)
     */
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Searches profiles by name (case-insensitive).
     * 
     * @param query The search query
     * @return List of profiles matching the query
     */
    suspend fun searchProfiles(query: String): List<ServerProfile>
    
    /**
     * Counts the total number of profiles.
     * 
     * @return The total number of profiles
     */
    suspend fun getProfileCount(): Int
    
    /**
     * Counts the number of profiles for a specific protocol.
     * 
     * @param protocol The protocol to count
     * @return The number of profiles for the specified protocol
     */
    suspend fun getProfileCountByProtocol(protocol: Protocol): Int
    
    /**
     * Validates a profile before creation or update.
     * 
     * Checks that all required fields are present and valid for the selected protocol.
     * 
     * @param profile The profile to validate
     * @return Result indicating success or validation error
     */
    fun validateProfile(profile: ServerProfile): Result<Unit>
    
    /**
     * Imports a configuration from text (QR code, URI, or config file).
     * 
     * Automatically detects the protocol and creates a profile.
     * Supports:
     * - WireGuard INI format
     * - VLESS URI format (vless://)
     * 
     * @param configText The configuration text to import
     * @return Result containing the created profile, or an error
     */
    suspend fun importConfiguration(configText: String): Result<ServerProfile>
}
