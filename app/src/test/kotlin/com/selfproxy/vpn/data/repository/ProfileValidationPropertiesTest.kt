package com.selfproxy.vpn.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.selfproxy.vpn.data.database.AppDatabase
import com.selfproxy.vpn.data.model.*
import com.selfproxy.vpn.domain.model.Protocol
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property-based tests for profile validation.
 * 
 * These tests validate that incomplete or invalid profiles are properly rejected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileValidationPropertiesTest {
    
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
     * Feature: selfproxy, Property 3: Profile Validation Rejects Incomplete Data
     * Validates: Requirements 1.4
     * 
     * For any profile with missing required fields, the validation should fail 
     * and prevent profile creation.
     */
    @Test
    fun `property 3 - profiles with blank names should be rejected`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile(),
            Arb.blankString()
        ) { profile, blankName ->
            // Clear database before each iteration
            database.clearAllTables()
            
            // Attempting to create profile with blank name should throw exception
            // (either in constructor or in validation)
            try {
                val invalidProfile = profile.copy(name = blankName)
                
                // If constructor didn't throw, validation should fail
                val validationResult = repository.validateProfile(invalidProfile)
                assertFalse(validationResult.isSuccess, 
                    "Profile with blank name should fail validation")
                
                // Creation should fail
                val createResult = repository.createProfile(invalidProfile)
                assertFalse(createResult.isSuccess, 
                    "Profile with blank name should not be created")
            } catch (e: IllegalArgumentException) {
                // Expected - constructor validation caught it
                assertTrue(true, "Profile with blank name correctly rejected by constructor")
            }
        }
    }
    
    @Test
    fun `property 3 - profiles with blank hostnames should be rejected`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile(),
            Arb.blankString()
        ) { profile, blankHostname ->
            // Clear database before each iteration
            database.clearAllTables()
            
            try {
                val invalidProfile = profile.copy(hostname = blankHostname)
                
                // If constructor didn't throw, validation should fail
                val validationResult = repository.validateProfile(invalidProfile)
                assertFalse(validationResult.isSuccess, 
                    "Profile with blank hostname should fail validation")
                
                val createResult = repository.createProfile(invalidProfile)
                assertFalse(createResult.isSuccess, 
                    "Profile with blank hostname should not be created")
            } catch (e: IllegalArgumentException) {
                // Expected - constructor validation caught it
                assertTrue(true, "Profile with blank hostname correctly rejected")
            }
        }
    }
    
    @Test
    fun `property 3 - profiles with invalid ports should be rejected`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile(),
            Arb.invalidPort()
        ) { profile, invalidPort ->
            // Clear database before each iteration
            database.clearAllTables()
            
            try {
                val invalidProfile = profile.copy(port = invalidPort)
                
                // If constructor didn't throw, validation should fail
                val validationResult = repository.validateProfile(invalidProfile)
                assertFalse(validationResult.isSuccess, 
                    "Profile with invalid port should fail validation")
                
                val createResult = repository.createProfile(invalidProfile)
                assertFalse(createResult.isSuccess, 
                    "Profile with invalid port should not be created")
            } catch (e: IllegalArgumentException) {
                // Expected - constructor validation caught it
                assertTrue(true, "Profile with invalid port correctly rejected")
            }
        }
    }
    
    @Test
    fun `property 3 - WireGuard profiles without WireGuard config should be rejected`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile()
        ) { profile ->
            // Clear database before each iteration
            database.clearAllTables()
            
            try {
                val invalidProfile = profile.copy(wireGuardConfigJson = null)
                
                // If constructor didn't throw, validation should fail
                val validationResult = repository.validateProfile(invalidProfile)
                assertFalse(validationResult.isSuccess, 
                    "WireGuard profile without config should fail validation")
                
                val createResult = repository.createProfile(invalidProfile)
                assertFalse(createResult.isSuccess, 
                    "WireGuard profile without config should not be created")
            } catch (e: IllegalArgumentException) {
                // Expected - constructor validation caught it
                assertTrue(true, "WireGuard profile without config correctly rejected")
            }
        }
    }
    
    @Test
    fun `property 3 - VLESS profiles without VLESS config should be rejected`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validVlessProfile()
        ) { profile ->
            // Clear database before each iteration
            database.clearAllTables()
            
            try {
                val invalidProfile = profile.copy(vlessConfigJson = null)
                
                // If constructor didn't throw, validation should fail
                val validationResult = repository.validateProfile(invalidProfile)
                assertFalse(validationResult.isSuccess, 
                    "VLESS profile without config should fail validation")
                
                val createResult = repository.createProfile(invalidProfile)
                assertFalse(createResult.isSuccess, 
                    "VLESS profile without config should not be created")
            } catch (e: IllegalArgumentException) {
                // Expected - constructor validation caught it
                assertTrue(true, "VLESS profile without config correctly rejected")
            }
        }
    }
    
    @Test
    fun `property 3 - profiles with mismatched protocol and config should be rejected`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile()
        ) { wireGuardProfile ->
            // Clear database before each iteration
            database.clearAllTables()
            
            try {
                val invalidProfile = wireGuardProfile.copy(protocol = Protocol.VLESS)
                
                // If constructor didn't throw, validation should fail
                val validationResult = repository.validateProfile(invalidProfile)
                assertFalse(validationResult.isSuccess, 
                    "Profile with mismatched protocol and config should fail validation")
                
                val createResult = repository.createProfile(invalidProfile)
                assertFalse(createResult.isSuccess, 
                    "Profile with mismatched protocol and config should not be created")
            } catch (e: IllegalArgumentException) {
                // Expected - constructor validation caught it
                assertTrue(true, "Profile with mismatched protocol correctly rejected")
            }
        }
    }
    
    @Test
    fun `property 3 - valid profiles should pass validation`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile()
        ) { profile ->
            // Clear database before each iteration
            database.clearAllTables()
            
            // Validation should succeed
            val validationResult = repository.validateProfile(profile)
            assertTrue(validationResult.isSuccess, 
                "Valid profile should pass validation")
            
            // Creation should succeed
            val createResult = repository.createProfile(profile)
            assertTrue(createResult.isSuccess, 
                "Valid profile should be created successfully")
        }
    }
}

/**
 * Generates blank strings (empty or whitespace only).
 */
fun Arb.Companion.blankString(): Arb<String> = arbitrary {
    val type = Arb.int(0..2).bind()
    when (type) {
        0 -> ""
        1 -> " "
        else -> "   "
    }
}

/**
 * Generates invalid port numbers (outside 1-65535 range).
 */
fun Arb.Companion.invalidPort(): Arb<Int> = arbitrary {
    val useNegative = Arb.boolean().bind()
    if (useNegative) {
        Arb.int(Int.MIN_VALUE..-1).bind()
    } else {
        Arb.int(65536..Int.MAX_VALUE).bind()
    }
}
