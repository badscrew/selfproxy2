package com.selfproxy.vpn.domain.repository

/**
 * Interface for secure credential storage.
 * 
 * Provides encryption and storage for sensitive credentials:
 * - WireGuard private keys and preshared keys
 * - VLESS UUIDs
 * 
 * All credentials are encrypted using Android Keystore before storage.
 * Credentials are automatically deleted when their associated profile is deleted.
 */
interface CredentialStore {
    
    /**
     * Stores a WireGuard private key for a profile.
     * 
     * @param profileId The profile ID to associate with this key
     * @param privateKey The WireGuard private key (base64-encoded 32-byte key)
     * @return Result.success if stored successfully, Result.failure on error
     */
    suspend fun storeWireGuardPrivateKey(
        profileId: Long,
        privateKey: String
    ): Result<Unit>
    
    /**
     * Retrieves a WireGuard private key for a profile.
     * 
     * @param profileId The profile ID
     * @return Result.success with the private key, Result.failure if not found or on error
     */
    suspend fun getWireGuardPrivateKey(profileId: Long): Result<String>
    
    /**
     * Stores a WireGuard preshared key for a profile.
     * 
     * @param profileId The profile ID to associate with this key
     * @param presharedKey The WireGuard preshared key (base64-encoded 32-byte key)
     * @return Result.success if stored successfully, Result.failure on error
     */
    suspend fun storeWireGuardPresharedKey(
        profileId: Long,
        presharedKey: String
    ): Result<Unit>
    
    /**
     * Retrieves a WireGuard preshared key for a profile.
     * 
     * @param profileId The profile ID
     * @return Result.success with the preshared key, Result.failure if not found or on error
     */
    suspend fun getWireGuardPresharedKey(profileId: Long): Result<String>
    
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
