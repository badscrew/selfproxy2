package com.selfproxy.vpn.data.database

import androidx.room.*
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.model.Protocol
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ServerProfile entities.
 * 
 * Provides database operations for managing VPN server profiles.
 */
@Dao
interface ProfileDao {
    
    /**
     * Inserts a new profile into the database.
     * @return The ID of the inserted profile
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ServerProfile): Long
    
    /**
     * Updates an existing profile in the database.
     * @return The number of profiles updated (should be 1)
     */
    @Update
    suspend fun update(profile: ServerProfile): Int
    
    /**
     * Deletes a profile from the database.
     * @return The number of profiles deleted (should be 1)
     */
    @Delete
    suspend fun delete(profile: ServerProfile): Int
    
    /**
     * Deletes a profile by its ID.
     * @return The number of profiles deleted (should be 1)
     */
    @Query("DELETE FROM server_profiles WHERE id = :profileId")
    suspend fun deleteById(profileId: Long): Int
    
    /**
     * Retrieves a profile by its ID.
     * @return The profile, or null if not found
     */
    @Query("SELECT * FROM server_profiles WHERE id = :profileId")
    suspend fun getById(profileId: Long): ServerProfile?
    
    /**
     * Retrieves all profiles from the database.
     * @return List of all profiles, ordered by last used (most recent first), then by name
     */
    @Query("SELECT * FROM server_profiles ORDER BY lastUsed DESC, name ASC")
    suspend fun getAll(): List<ServerProfile>
    
    /**
     * Observes all profiles from the database.
     * @return Flow of all profiles, ordered by last used (most recent first), then by name
     */
    @Query("SELECT * FROM server_profiles ORDER BY lastUsed DESC, name ASC")
    fun observeAll(): Flow<List<ServerProfile>>
    
    /**
     * Retrieves all profiles for a specific protocol.
     * @return List of profiles for the specified protocol
     */
    @Query("SELECT * FROM server_profiles WHERE protocol = :protocol ORDER BY lastUsed DESC, name ASC")
    suspend fun getByProtocol(protocol: Protocol): List<ServerProfile>
    
    /**
     * Observes all profiles for a specific protocol.
     * @return Flow of profiles for the specified protocol
     */
    @Query("SELECT * FROM server_profiles WHERE protocol = :protocol ORDER BY lastUsed DESC, name ASC")
    fun observeByProtocol(protocol: Protocol): Flow<List<ServerProfile>>
    
    /**
     * Updates the last used timestamp for a profile.
     * @param profileId The ID of the profile to update
     * @param timestamp The timestamp to set (defaults to current time)
     */
    @Query("UPDATE server_profiles SET lastUsed = :timestamp WHERE id = :profileId")
    suspend fun updateLastUsed(profileId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Counts the total number of profiles.
     * @return The total number of profiles
     */
    @Query("SELECT COUNT(*) FROM server_profiles")
    suspend fun count(): Int
    
    /**
     * Counts the number of profiles for a specific protocol.
     * @return The number of profiles for the specified protocol
     */
    @Query("SELECT COUNT(*) FROM server_profiles WHERE protocol = :protocol")
    suspend fun countByProtocol(protocol: Protocol): Int
    
    /**
     * Searches profiles by name (case-insensitive).
     * @param query The search query
     * @return List of profiles matching the query
     */
    @Query("SELECT * FROM server_profiles WHERE name LIKE '%' || :query || '%' ORDER BY lastUsed DESC, name ASC")
    suspend fun searchByName(query: String): List<ServerProfile>
    
    /**
     * Deletes all profiles from the database.
     * @return The number of profiles deleted
     */
    @Query("DELETE FROM server_profiles")
    suspend fun deleteAll(): Int
}
