package com.selfproxy.vpn.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.selfproxy.vpn.data.model.AppRoutingConfig
import kotlinx.coroutines.flow.Flow

/**
 * DAO for app routing configuration.
 * 
 * Provides database operations for storing and retrieving per-app routing settings.
 */
@Dao
interface AppRoutingDao {
    
    /**
     * Inserts a new app routing configuration.
     * 
     * @param config The configuration to insert
     * @return The ID of the inserted configuration
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AppRoutingConfig): Long
    
    /**
     * Updates an existing app routing configuration.
     * 
     * @param config The configuration to update
     */
    @Update
    suspend fun update(config: AppRoutingConfig)
    
    /**
     * Gets the app routing configuration for a specific profile.
     * 
     * @param profileId The profile ID
     * @return The configuration, or null if not found
     */
    @Query("SELECT * FROM app_routing_config WHERE profileId = :profileId LIMIT 1")
    suspend fun getByProfileId(profileId: Long): AppRoutingConfig?
    
    /**
     * Observes the app routing configuration for a specific profile.
     * 
     * @param profileId The profile ID
     * @return Flow of configuration updates
     */
    @Query("SELECT * FROM app_routing_config WHERE profileId = :profileId LIMIT 1")
    fun observeByProfileId(profileId: Long): Flow<AppRoutingConfig?>
    
    /**
     * Gets the global default app routing configuration.
     * 
     * @return The global configuration, or null if not found
     */
    @Query("SELECT * FROM app_routing_config WHERE profileId IS NULL LIMIT 1")
    suspend fun getGlobalConfig(): AppRoutingConfig?
    
    /**
     * Observes the global default app routing configuration.
     * 
     * @return Flow of global configuration updates
     */
    @Query("SELECT * FROM app_routing_config WHERE profileId IS NULL LIMIT 1")
    fun observeGlobalConfig(): Flow<AppRoutingConfig?>
    
    /**
     * Deletes the app routing configuration for a specific profile.
     * 
     * @param profileId The profile ID
     */
    @Query("DELETE FROM app_routing_config WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Long)
    
    /**
     * Deletes all app routing configurations.
     */
    @Query("DELETE FROM app_routing_config")
    suspend fun deleteAll()
}
