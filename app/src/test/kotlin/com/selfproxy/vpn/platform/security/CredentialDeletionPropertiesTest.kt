package com.selfproxy.vpn.platform.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Property-based tests for credential deletion.
 * 
 * Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
 * Validates: Requirements 2.5
 * 
 * Tests that all credentials are properly deleted when a profile is deleted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CredentialDeletionPropertiesTest {
    
    private lateinit var context: Context
    private lateinit var credentialStore: AndroidCredentialStore
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        credentialStore = AndroidCredentialStore(context)
    }
    
    @Test
    fun `deleting profile removes all WireGuard credentials`() = runTest {
        // Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
        // Validates: Requirements 2.5
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.wireGuardKey(),
            Arb.wireGuardKey()
        ) { profileId, privateKey, presharedKey ->
            // Store both WireGuard credentials
            credentialStore.storeWireGuardPrivateKey(profileId, privateKey)
            credentialStore.storeWireGuardPresharedKey(profileId, presharedKey)
            
            // Verify credentials exist
            credentialStore.hasCredentials(profileId) shouldBe true
            credentialStore.getWireGuardPrivateKey(profileId).isSuccess shouldBe true
            credentialStore.getWireGuardPresharedKey(profileId).isSuccess shouldBe true
            
            // Delete credentials
            val deleteResult = credentialStore.deleteCredentials(profileId)
            deleteResult.isSuccess shouldBe true
            
            // Verify credentials are gone
            credentialStore.hasCredentials(profileId) shouldBe false
            credentialStore.getWireGuardPrivateKey(profileId).isFailure shouldBe true
            credentialStore.getWireGuardPresharedKey(profileId).isFailure shouldBe true
        }
    }
    
    @Test
    fun `deleting profile removes VLESS credentials`() = runTest {
        // Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
        // Validates: Requirements 2.5
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.uuid()
        ) { profileId, uuid ->
            val uuidString = uuid.toString()
            
            // Store VLESS UUID
            credentialStore.storeVlessUuid(profileId, uuidString)
            
            // Verify credential exists
            credentialStore.hasCredentials(profileId) shouldBe true
            credentialStore.getVlessUuid(profileId).isSuccess shouldBe true
            
            // Delete credentials
            val deleteResult = credentialStore.deleteCredentials(profileId)
            deleteResult.isSuccess shouldBe true
            
            // Verify credential is gone
            credentialStore.hasCredentials(profileId) shouldBe false
            credentialStore.getVlessUuid(profileId).isFailure shouldBe true
        }
    }
    
    @Test
    fun `deleting profile removes all credential types`() = runTest {
        // Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
        // Validates: Requirements 2.5
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.wireGuardKey(),
            Arb.wireGuardKey(),
            Arb.uuid()
        ) { profileId, privateKey, presharedKey, uuid ->
            val uuidString = uuid.toString()
            
            // Store all credential types (simulating a profile with mixed credentials)
            credentialStore.storeWireGuardPrivateKey(profileId, privateKey)
            credentialStore.storeWireGuardPresharedKey(profileId, presharedKey)
            credentialStore.storeVlessUuid(profileId, uuidString)
            
            // Verify all credentials exist
            credentialStore.hasCredentials(profileId) shouldBe true
            credentialStore.getWireGuardPrivateKey(profileId).isSuccess shouldBe true
            credentialStore.getWireGuardPresharedKey(profileId).isSuccess shouldBe true
            credentialStore.getVlessUuid(profileId).isSuccess shouldBe true
            
            // Delete all credentials
            val deleteResult = credentialStore.deleteCredentials(profileId)
            deleteResult.isSuccess shouldBe true
            
            // Verify all credentials are gone
            credentialStore.hasCredentials(profileId) shouldBe false
            credentialStore.getWireGuardPrivateKey(profileId).isFailure shouldBe true
            credentialStore.getWireGuardPresharedKey(profileId).isFailure shouldBe true
            credentialStore.getVlessUuid(profileId).isFailure shouldBe true
        }
    }
    
    @Test
    fun `deleting one profile does not affect other profiles`() = runTest {
        // Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
        // Validates: Requirements 2.5
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.long(1L..1000000L),
            Arb.wireGuardKey(),
            Arb.wireGuardKey()
        ) { profileId1, profileId2Offset, privateKey1, privateKey2 ->
            // Ensure different profile IDs
            val profileId2 = profileId1 + profileId2Offset + 1
            
            // Store credentials for both profiles
            credentialStore.storeWireGuardPrivateKey(profileId1, privateKey1)
            credentialStore.storeWireGuardPrivateKey(profileId2, privateKey2)
            
            // Verify both exist
            credentialStore.hasCredentials(profileId1) shouldBe true
            credentialStore.hasCredentials(profileId2) shouldBe true
            
            // Delete first profile's credentials
            credentialStore.deleteCredentials(profileId1)
            
            // Verify first profile's credentials are gone
            credentialStore.hasCredentials(profileId1) shouldBe false
            credentialStore.getWireGuardPrivateKey(profileId1).isFailure shouldBe true
            
            // Verify second profile's credentials still exist
            credentialStore.hasCredentials(profileId2) shouldBe true
            val retrieved = credentialStore.getWireGuardPrivateKey(profileId2)
            retrieved.isSuccess shouldBe true
            retrieved.getOrThrow() shouldBe privateKey2
            
            // Cleanup
            credentialStore.deleteCredentials(profileId2)
        }
    }
    
    @Test
    fun `deleting non-existent credentials succeeds`() = runTest {
        // Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
        // Validates: Requirements 2.5
        checkAll(
            100,
            Arb.long(1L..1000000L)
        ) { profileId ->
            // Ensure no credentials exist
            credentialStore.deleteCredentials(profileId)
            
            // Verify no credentials
            credentialStore.hasCredentials(profileId) shouldBe false
            
            // Delete again (should succeed even though nothing to delete)
            val deleteResult = credentialStore.deleteCredentials(profileId)
            deleteResult.isSuccess shouldBe true
            
            // Still no credentials
            credentialStore.hasCredentials(profileId) shouldBe false
        }
    }
    
    @Test
    fun `hasCredentials returns false after deletion`() = runTest {
        // Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
        // Validates: Requirements 2.5
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.wireGuardKey()
        ) { profileId, privateKey ->
            // Store credential
            credentialStore.storeWireGuardPrivateKey(profileId, privateKey)
            
            // Verify hasCredentials returns true
            credentialStore.hasCredentials(profileId) shouldBe true
            
            // Delete credentials
            credentialStore.deleteCredentials(profileId)
            
            // Verify hasCredentials returns false
            credentialStore.hasCredentials(profileId) shouldBe false
        }
    }
    
    @Test
    fun `credentials can be re-added after deletion`() = runTest {
        // Feature: selfproxy, Property 9: Credential Deletion on Profile Deletion
        // Validates: Requirements 2.5
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.wireGuardKey(),
            Arb.wireGuardKey()
        ) { profileId, privateKey1, privateKey2 ->
            // Store first credential
            credentialStore.storeWireGuardPrivateKey(profileId, privateKey1)
            val retrieved1 = credentialStore.getWireGuardPrivateKey(profileId).getOrThrow()
            retrieved1 shouldBe privateKey1
            
            // Delete credentials
            credentialStore.deleteCredentials(profileId)
            credentialStore.hasCredentials(profileId) shouldBe false
            
            // Store new credential with same profile ID
            credentialStore.storeWireGuardPrivateKey(profileId, privateKey2)
            
            // Verify new credential is stored
            credentialStore.hasCredentials(profileId) shouldBe true
            val retrieved2 = credentialStore.getWireGuardPrivateKey(profileId).getOrThrow()
            retrieved2 shouldBe privateKey2
            
            // Cleanup
            credentialStore.deleteCredentials(profileId)
        }
    }
}
