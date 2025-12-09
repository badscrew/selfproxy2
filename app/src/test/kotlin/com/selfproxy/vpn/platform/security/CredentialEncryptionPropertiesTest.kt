package com.selfproxy.vpn.platform.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Property-based tests for credential encryption.
 * 
 * Feature: selfproxy, Property 8: Credential Encryption
 * Validates: Requirements 2.1, 2.3, 2.4
 * 
 * Tests that credentials are properly encrypted and can be decrypted back to original values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CredentialEncryptionPropertiesTest {
    
    private lateinit var context: Context
    private lateinit var credentialStore: AndroidCredentialStore
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        credentialStore = AndroidCredentialStore(context)
    }
    
    @Test
    fun `WireGuard private key encryption round-trip preserves data`() = runTest {
        // Feature: selfproxy, Property 8: Credential Encryption
        // Validates: Requirements 2.1, 2.3
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.wireGuardKey()
        ) { profileId, privateKey ->
            // Store the private key
            val storeResult = credentialStore.storeWireGuardPrivateKey(profileId, privateKey)
            storeResult.isSuccess shouldBe true
            
            // Retrieve the private key
            val retrieveResult = credentialStore.getWireGuardPrivateKey(profileId)
            retrieveResult.isSuccess shouldBe true
            
            // Verify it matches the original
            val retrieved = retrieveResult.getOrThrow()
            retrieved shouldBe privateKey
            
            // Cleanup
            credentialStore.deleteCredentials(profileId)
        }
    }
    
    @Test
    fun `WireGuard preshared key encryption round-trip preserves data`() = runTest {
        // Feature: selfproxy, Property 8: Credential Encryption
        // Validates: Requirements 2.1, 2.3
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.wireGuardKey()
        ) { profileId, presharedKey ->
            // Store the preshared key
            val storeResult = credentialStore.storeWireGuardPresharedKey(profileId, presharedKey)
            storeResult.isSuccess shouldBe true
            
            // Retrieve the preshared key
            val retrieveResult = credentialStore.getWireGuardPresharedKey(profileId)
            retrieveResult.isSuccess shouldBe true
            
            // Verify it matches the original
            val retrieved = retrieveResult.getOrThrow()
            retrieved shouldBe presharedKey
            
            // Cleanup
            credentialStore.deleteCredentials(profileId)
        }
    }
    
    @Test
    fun `VLESS UUID encryption round-trip preserves data`() = runTest {
        // Feature: selfproxy, Property 8: Credential Encryption
        // Validates: Requirements 2.4
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.uuid()
        ) { profileId, uuid ->
            val uuidString = uuid.toString()
            
            // Store the UUID
            val storeResult = credentialStore.storeVlessUuid(profileId, uuidString)
            storeResult.isSuccess shouldBe true
            
            // Retrieve the UUID
            val retrieveResult = credentialStore.getVlessUuid(profileId)
            retrieveResult.isSuccess shouldBe true
            
            // Verify it matches the original
            val retrieved = retrieveResult.getOrThrow()
            retrieved shouldBe uuidString
            
            // Cleanup
            credentialStore.deleteCredentials(profileId)
        }
    }
    

    @Test
    fun `invalid WireGuard key format is rejected`() = runTest {
        // Feature: selfproxy, Property 8: Credential Encryption
        // Validates: Requirements 2.1, 2.3
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.invalidWireGuardKey()
        ) { profileId, invalidKey ->
            // Attempt to store invalid key
            val result = credentialStore.storeWireGuardPrivateKey(profileId, invalidKey)
            
            // Should fail
            result.isFailure shouldBe true
        }
    }
    
    @Test
    fun `invalid VLESS UUID format is rejected`() = runTest {
        // Feature: selfproxy, Property 8: Credential Encryption
        // Validates: Requirements 2.4
        checkAll(
            100,
            Arb.long(1L..1000000L),
            Arb.invalidUuid()
        ) { profileId, invalidUuid ->
            // Attempt to store invalid UUID
            val result = credentialStore.storeVlessUuid(profileId, invalidUuid)
            
            // Should fail
            result.isFailure shouldBe true
        }
    }
    
    @Test
    fun `retrieving non-existent credential returns failure`() = runTest {
        // Feature: selfproxy, Property 8: Credential Encryption
        // Validates: Requirements 2.1, 2.3, 2.4
        checkAll(
            100,
            Arb.long(1L..1000000L)
        ) { profileId ->
            // Ensure no credentials exist
            credentialStore.deleteCredentials(profileId)
            
            // Attempt to retrieve non-existent credentials
            val privateKeyResult = credentialStore.getWireGuardPrivateKey(profileId)
            val presharedKeyResult = credentialStore.getWireGuardPresharedKey(profileId)
            val uuidResult = credentialStore.getVlessUuid(profileId)
            
            // All should fail
            privateKeyResult.isFailure shouldBe true
            presharedKeyResult.isFailure shouldBe true
            uuidResult.isFailure shouldBe true
        }
    }
}

/**
 * Generates valid WireGuard keys (base64-encoded 32-byte keys).
 */
fun Arb.Companion.wireGuardKey(): Arb<String> = arbitrary {
    // Generate 32 random bytes
    val bytes = ByteArray(32) { kotlin.random.Random.nextInt().toByte() }
    // Encode to base64
    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

/**
 * Generates invalid WireGuard keys for testing validation.
 */
fun Arb.Companion.invalidWireGuardKey(): Arb<String> = Arb.string(1..50)

/**
 * Generates invalid UUIDs for testing validation.
 */
fun Arb.Companion.invalidUuid(): Arb<String> = Arb.string(1..50)
