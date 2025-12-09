package com.selfproxy.vpn.data.repository

import com.selfproxy.vpn.data.database.ProfileDao
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of ProfileRepository using Room database.
 * 
 * Provides CRUD operations with validation and error handling.
 */
class ProfileRepositoryImpl(
    private val profileDao: ProfileDao
) : ProfileRepository {
    
    override suspend fun createProfile(profile: ServerProfile): Result<Long> {
        return try {
            // Validate profile before insertion
            validateProfile(profile).getOrThrow()
            
            // Insert profile
            val id = profileDao.insert(profile)
            Result.success(id)
        } catch (e: IllegalArgumentException) {
            Result.failure(ProfileValidationException(e.message ?: "Invalid profile", e))
        } catch (e: Exception) {
            Result.failure(ProfileCreationException("Failed to create profile: ${e.message}", e))
        }
    }
    
    override suspend fun getProfile(id: Long): ServerProfile? {
        return try {
            profileDao.getById(id)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getAllProfiles(): List<ServerProfile> {
        return try {
            profileDao.getAll()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun observeAllProfiles(): Flow<List<ServerProfile>> {
        return profileDao.observeAll()
    }
    
    override suspend fun getProfilesByProtocol(protocol: Protocol): List<ServerProfile> {
        return try {
            profileDao.getByProtocol(protocol)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun observeProfilesByProtocol(protocol: Protocol): Flow<List<ServerProfile>> {
        return profileDao.observeByProtocol(protocol)
    }
    
    override suspend fun updateProfile(profile: ServerProfile): Result<Unit> {
        return try {
            // Validate profile before update
            validateProfile(profile).getOrThrow()
            
            // Check if profile exists
            if (profile.id == 0L) {
                return Result.failure(ProfileValidationException("Profile ID must be set for update"))
            }
            
            val existingProfile = profileDao.getById(profile.id)
            if (existingProfile == null) {
                return Result.failure(ProfileNotFoundException("Profile with ID ${profile.id} not found"))
            }
            
            // Update profile
            val rowsUpdated = profileDao.update(profile)
            if (rowsUpdated > 0) {
                Result.success(Unit)
            } else {
                Result.failure(ProfileUpdateException("Failed to update profile"))
            }
        } catch (e: IllegalArgumentException) {
            Result.failure(ProfileValidationException(e.message ?: "Invalid profile", e))
        } catch (e: Exception) {
            Result.failure(ProfileUpdateException("Failed to update profile: ${e.message}", e))
        }
    }
    
    override suspend fun deleteProfile(id: Long): Result<Unit> {
        return try {
            val rowsDeleted = profileDao.deleteById(id)
            if (rowsDeleted > 0) {
                Result.success(Unit)
            } else {
                Result.failure(ProfileNotFoundException("Profile with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ProfileDeletionException("Failed to delete profile: ${e.message}", e))
        }
    }
    
    override suspend fun updateLastUsed(id: Long, timestamp: Long) {
        try {
            profileDao.updateLastUsed(id, timestamp)
        } catch (e: Exception) {
            // Log error but don't throw - this is a non-critical operation
        }
    }
    
    override suspend fun searchProfiles(query: String): List<ServerProfile> {
        return try {
            profileDao.searchByName(query)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getProfileCount(): Int {
        return try {
            profileDao.count()
        } catch (e: Exception) {
            0
        }
    }
    
    override suspend fun getProfileCountByProtocol(protocol: Protocol): Int {
        return try {
            profileDao.countByProtocol(protocol)
        } catch (e: Exception) {
            0
        }
    }
    
    override fun validateProfile(profile: ServerProfile): Result<Unit> {
        return try {
            // Basic validation
            require(profile.name.isNotBlank()) { "Profile name cannot be blank" }
            require(profile.hostname.isNotBlank()) { "Hostname cannot be blank" }
            require(profile.port in 1..65535) { "Port must be between 1 and 65535" }
            
            // Protocol-specific validation
            when (profile.protocol) {
                Protocol.WIREGUARD -> {
                    require(profile.wireGuardConfigJson != null) {
                        "WireGuard configuration required for WireGuard protocol"
                    }
                    require(profile.vlessConfigJson == null) {
                        "VLESS configuration should not be present for WireGuard protocol"
                    }
                    
                    // Validate WireGuard config can be parsed
                    try {
                        profile.getWireGuardConfig()
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid WireGuard configuration: ${e.message}", e)
                    }
                }
                Protocol.VLESS -> {
                    require(profile.vlessConfigJson != null) {
                        "VLESS configuration required for VLESS protocol"
                    }
                    require(profile.wireGuardConfigJson == null) {
                        "WireGuard configuration should not be present for VLESS protocol"
                    }
                    
                    // Validate VLESS config can be parsed
                    try {
                        profile.getVlessConfig()
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid VLESS configuration: ${e.message}", e)
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.failure(ProfileValidationException(e.message ?: "Invalid profile", e))
        } catch (e: Exception) {
            Result.failure(ProfileValidationException("Profile validation failed: ${e.message}", e))
        }
    }
    
    override suspend fun importConfiguration(configText: String): Result<ServerProfile> {
        return try {
            // Use ConfigurationImporter to parse the configuration
            val importResult = com.selfproxy.vpn.data.config.ConfigurationImporter.import(configText)
            
            importResult.fold(
                onSuccess = { importedConfig ->
                    // Create a profile from the imported configuration
                    val profile = when (importedConfig.protocol) {
                        Protocol.WIREGUARD -> {
                            val wgConfig = importedConfig.wireGuardConfig!!
                            ServerProfile(
                                id = 0,
                                name = wgConfig.name,
                                protocol = Protocol.WIREGUARD,
                                hostname = wgConfig.hostname,
                                port = wgConfig.port,
                                wireGuardConfigJson = kotlinx.serialization.json.Json.encodeToString(
                                    com.selfproxy.vpn.data.model.WireGuardConfig.serializer(),
                                    wgConfig.config
                                ),
                                vlessConfigJson = null,
                                createdAt = System.currentTimeMillis(),
                                lastUsed = null
                            )
                        }
                        Protocol.VLESS -> {
                            val vlessConfig = importedConfig.vlessConfig!!
                            ServerProfile(
                                id = 0,
                                name = vlessConfig.name,
                                protocol = Protocol.VLESS,
                                hostname = vlessConfig.hostname,
                                port = vlessConfig.port,
                                wireGuardConfigJson = null,
                                vlessConfigJson = kotlinx.serialization.json.Json.encodeToString(
                                    com.selfproxy.vpn.data.model.VlessConfig.serializer(),
                                    vlessConfig.config
                                ),
                                createdAt = System.currentTimeMillis(),
                                lastUsed = null
                            )
                        }
                    }
                    
                    // Create the profile in the database
                    createProfile(profile).fold(
                        onSuccess = { id ->
                            Result.success(profile.copy(id = id))
                        },
                        onFailure = { error ->
                            Result.failure(error)
                        }
                    )
                },
                onFailure = { error ->
                    Result.failure(ConfigurationImportException(
                        "Failed to import configuration: ${error.message}",
                        error
                    ))
                }
            )
        } catch (e: Exception) {
            Result.failure(ConfigurationImportException(
                "Failed to import configuration: ${e.message}",
                e
            ))
        }
    }
}

/**
 * Exception thrown when profile validation fails.
 */
class ProfileValidationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when profile creation fails.
 */
class ProfileCreationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when profile update fails.
 */
class ProfileUpdateException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when profile deletion fails.
 */
class ProfileDeletionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a profile is not found.
 */
class ProfileNotFoundException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when configuration import fails.
 */
class ConfigurationImportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
