package com.selfproxy.vpn.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.selfproxy.vpn.data.database.AppDatabase
import com.selfproxy.vpn.data.model.*
import com.selfproxy.vpn.domain.model.Protocol
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Property-based tests for ProfileRepository.
 * 
 * These tests validate universal properties that should hold across all valid inputs.
 * Uses Robolectric for Android-specific Room database testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileRepositoryPropertiesTest {
    
    private lateinit var database: AppDatabase
    private lateinit var repository: ProfileRepositoryImpl
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = ProfileRepositoryImpl(database.profileDao())
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    /**
     * Feature: selfproxy, Property 4: Profile Listing Completeness
     * Validates: Requirements 1.5
     * 
     * For any set of saved profiles, retrieving all profiles should return exactly 
     * the profiles that were saved with correct names, protocol types, server addresses, 
     * and last connection times.
     */
    @Test
    fun `property 4 - profile listing should return all saved profiles with correct data`() = runTest {
        checkAll(
            iterations = 100,
            Arb.profileList()
        ) { profiles ->
            // Clear database before each iteration
            database.clearAllTables()
            
            // Save all profiles (with id=0 for auto-generation)
            val savedIds = mutableListOf<Long>()
            profiles.forEach { profile ->
                val profileForCreation = profile.copy(id = 0)
                val result = repository.createProfile(profileForCreation)
                assertTrue(result.isSuccess, "Profile creation should succeed")
                savedIds.add(result.getOrThrow())
            }
            
            // Retrieve all profiles
            val retrievedProfiles = repository.getAllProfiles()
            
            // Should have same count
            assertEquals(profiles.size, retrievedProfiles.size, 
                "Retrieved profile count should match saved count")
            
            // Each saved profile should be in the retrieved list
            profiles.forEachIndexed { index, originalProfile ->
                val savedId = savedIds[index]
                val retrieved = retrievedProfiles.find { it.id == savedId }
                
                assertNotNull(retrieved, "Saved profile should be in retrieved list")
                assertEquals(originalProfile.name, retrieved.name, "Name should match")
                assertEquals(originalProfile.protocol, retrieved.protocol, "Protocol should match")
                assertEquals(originalProfile.hostname, retrieved.hostname, "Hostname should match")
                assertEquals(originalProfile.port, retrieved.port, "Port should match")
                
                // Verify protocol-specific config
                when (originalProfile.protocol) {
                    Protocol.WIREGUARD -> {
                        val originalConfig = originalProfile.getWireGuardConfig()
                        val retrievedConfig = retrieved.getWireGuardConfig()
                        assertEquals(originalConfig.publicKey, retrievedConfig.publicKey)
                        assertEquals(originalConfig.endpoint, retrievedConfig.endpoint)
                    }
                    Protocol.VLESS -> {
                        val originalConfig = originalProfile.getVlessConfig()
                        val retrievedConfig = retrieved.getVlessConfig()
                        assertEquals(originalConfig.transport, retrievedConfig.transport)
                        assertEquals(originalConfig.flowControl, retrievedConfig.flowControl)
                    }
                }
            }
        }
    }
    
    /**
     * Feature: selfproxy, Property 5: Profile Update Round-Trip
     * Validates: Requirements 1.7
     * 
     * For any saved profile, if it is modified and saved again, then retrieving it 
     * should return the profile with the updated values.
     */
    @Test
    fun `property 5 - profile update should persist all changes`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile(),
            Arb.profileName(),
            Arb.validPort()
        ) { originalProfile, newName, newPort ->
            // Clear database before each iteration
            database.clearAllTables()
            
            // Create original profile (with id=0 for auto-generation)
            val profileForCreation = originalProfile.copy(id = 0)
            val createResult = repository.createProfile(profileForCreation)
            assertTrue(createResult.isSuccess, "Profile creation should succeed")
            val profileId = createResult.getOrThrow()
            
            // Modify the profile
            val updatedProfile = profileForCreation.copy(
                id = profileId,
                name = newName,
                port = newPort
            )
            
            // Update the profile
            val updateResult = repository.updateProfile(updatedProfile)
            assertTrue(updateResult.isSuccess, "Profile update should succeed")
            
            // Retrieve the profile
            val retrieved = repository.getProfile(profileId)
            
            // Verify all changes persisted
            assertNotNull(retrieved, "Updated profile should be retrievable")
            assertEquals(newName, retrieved.name, "Name should be updated")
            assertEquals(newPort, retrieved.port, "Port should be updated")
            assertEquals(originalProfile.hostname, retrieved.hostname, "Hostname should remain unchanged")
            assertEquals(originalProfile.protocol, retrieved.protocol, "Protocol should remain unchanged")
            
            // Verify config remained intact
            val originalConfig = originalProfile.getWireGuardConfig()
            val retrievedConfig = retrieved.getWireGuardConfig()
            assertEquals(originalConfig.publicKey, retrievedConfig.publicKey, "Config should remain unchanged")
            assertEquals(originalConfig.endpoint, retrievedConfig.endpoint, "Config should remain unchanged")
        }
    }
    
    /**
     * Feature: selfproxy, Property 6: Profile Deletion Completeness
     * Validates: Requirements 1.8
     * 
     * For any saved profile, after deletion, the profile should not appear in the 
     * profile list and should not be retrievable by ID.
     */
    @Test
    fun `property 6 - deleted profiles should not be retrievable`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile()
        ) { profile ->
            // Clear database before each iteration
            database.clearAllTables()
            
            // Create profile (with id=0 for auto-generation)
            val profileForCreation = profile.copy(id = 0)
            val createResult = repository.createProfile(profileForCreation)
            assertTrue(createResult.isSuccess, "Profile creation should succeed")
            val profileId = createResult.getOrThrow()
            
            // Verify profile exists
            val beforeDeletion = repository.getProfile(profileId)
            assertNotNull(beforeDeletion, "Profile should exist before deletion")
            
            val allBeforeDeletion = repository.getAllProfiles()
            assertTrue(allBeforeDeletion.any { it.id == profileId }, 
                "Profile should be in list before deletion")
            
            // Delete profile
            val deleteResult = repository.deleteProfile(profileId)
            assertTrue(deleteResult.isSuccess, "Profile deletion should succeed")
            
            // Verify profile no longer exists
            val afterDeletion = repository.getProfile(profileId)
            assertNull(afterDeletion, "Profile should not exist after deletion")
            
            val allAfterDeletion = repository.getAllProfiles()
            assertTrue(allAfterDeletion.none { it.id == profileId }, 
                "Profile should not be in list after deletion")
        }
    }
}

/**
 * Generates a list of profiles with mixed protocols.
 */
fun Arb.Companion.profileList(): Arb<List<com.selfproxy.vpn.data.model.ServerProfile>> = arbitrary {
    val count = Arb.int(1..10).bind()
    List(count) {
        val useWireGuard = Arb.boolean().bind()
        if (useWireGuard) {
            Arb.validWireGuardProfile().bind()
        } else {
            Arb.validVlessProfile().bind()
        }
    }
}
