package com.selfproxy.vpn.domain.repository

/**
 * Interface for secure credential storage.
 * 
 * Provides encryption and storage for sensitive credentials:
 * - VLESS UUIDs
 * 
 * All credentials are encrypted using Android Keystore before storage.
 * Credentials are automatically deleted when their associated profile is deleted.
 */
interface CredentialStore {
    
    /**
     * Stores a VLESS UUID for a profile.
     * 
     * @param profileId The profile ID to associate with this UUID
     * @param uuid The VLESS UUID (RFC 4122 format)
     * @return Result.success if stored successfully, Result.failure on error
     */
    suspend fun storeVlessUuid(
        profileId: Long,
        uuid: String
    ): Result<Unit>
    
    /**
     * Retrieves a VLESS UUID for a profile.
     * 
     * @param profileId The profile ID
     * @return Result.success with the UUID, Result.failure if not found or on error
     */
    suspend fun getVlessUuid(profileId: Long): Result<String>
    
    /**
     * Deletes all credentials associated with a profile.
     * 
     * This should be called when a profile is deleted to ensure
     * no orphaned credentials remain in storage.
     * 
     * @param profileId The profile ID
     * @return Result.success if deleted successfully, Result.failure on error
     */
    suspend fun deleteCredentials(profileId: Long): Result<Unit>
    
    /**
     * Checks if credentials exist for a profile.
     * 
     * @param profileId The profile ID
     * @return true if any credentials exist for this profile
     */
    suspend fun hasCredentials(profileId: Long): Boolean
}
