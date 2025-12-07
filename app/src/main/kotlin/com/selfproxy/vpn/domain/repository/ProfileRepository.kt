package com.selfproxy.vpn.domain.repository

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.model.Protocol

/**
 * Repository interface for managing server profiles.
 * Provides CRUD operations for profile management.
 */
interface ProfileRepository {
    suspend fun createProfile(profile: ServerProfile): Result<Long>
    suspend fun getProfile(id: Long): ServerProfile?
    suspend fun getAllProfiles(): List<ServerProfile>
    suspend fun updateProfile(profile: ServerProfile): Result<Unit>
    suspend fun deleteProfile(id: Long): Result<Unit>
    suspend fun getProfilesByProtocol(protocol: Protocol): List<ServerProfile>
}
