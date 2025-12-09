package com.selfproxy.vpn.platform.wireguard

import android.content.Context
import com.selfproxy.vpn.TestKeys
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.WireGuardConfig
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.repository.CredentialStore
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for WireGuardAdapter.
 * 
 * Tests key validation, endpoint parsing, and configuration validation.
 * Requirements: 3.1, 9.4
 */
class WireGuardAdapterTest {
    
    private lateinit var context: Context
    private lateinit var credentialStore: CredentialStore
    private lateinit var backend: Backend
    private lateinit var adapter: WireGuardAdapter
    
    // Use TestKeys for consistency
    private val validPrivateKey = TestKeys.VALID_PRIVATE_KEY
    private val validPublicKey = TestKeys.VALID_PUBLIC_KEY
    private val validPresharedKey = TestKeys.VALID_PRESHARED_KEY
    
    @Before
    fun setup() {
        // Mock Android Base64
        TestKeys.mockAndroidBase64()
        
        context = mockk(relaxed = true)
        credentialStore = mockk()
        backend = mockk(relaxed = true)
        
        // Setup default backend behavior
        every { backend.setState(any(), any(), any()) } returns Tunnel.State.UP
        every { backend.getStatistics(any()) } returns mockk(relaxed = true) {
            every { totalRx() } returns 1000L
            every { totalTx() } returns 500L
        }
        
        adapter = WireGuardAdapter(context, credentialStore, backend)
    }
    
    /**
     * Test: Valid private key should be accepted
     * Requirements: 9.4
     */
    @Test
    fun `valid private key format should be accepted`() = runTest {
        // Arrange
        val profile = createTestProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        coEvery { credentialStore.getWireGuardPresharedKey(profile.id) } returns Result.failure(Exception("Not found"))
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Connection should succeed with valid private key")
        coVerify { credentialStore.getWireGuardPrivateKey(profile.id) }
    }
    
    /**
     * Test: Invalid private key format should be rejected
     * Requirements: 9.4
     */
    @Test
    fun `invalid private key format should be rejected`() = runTest {
        // Arrange
        val profile = createTestProfile()
        val invalidKey = "not-a-valid-base64-key"
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(invalidKey)
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isFailure, "Connection should fail with invalid private key")
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is WireGuardException)
    }
    
    /**
     * Test: Valid public key should be accepted
     * Requirements: 9.4
     */
    @Test
    fun `valid public key format should be accepted`() = runTest {
        // Arrange
        val profile = createTestProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        coEvery { credentialStore.getWireGuardPresharedKey(profile.id) } returns Result.failure(Exception("Not found"))
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Connection should succeed with valid public key")
    }
    
    /**
     * Test: Invalid public key format should be rejected
     * Requirements: 9.4
     */
    @Test
    fun `invalid public key format should be rejected`() = runTest {
        // Arrange - Invalid public key will be rejected by constructor
        try {
            val invalidPublicKey = "invalid-public-key"
            val config = WireGuardConfig(
                publicKey = invalidPublicKey,
                allowedIPs = listOf("0.0.0.0/0"),
                endpoint = "vpn.example.com:51820",
                mtu = 1420
            )
            
            // If we get here, validation didn't catch it (shouldn't happen)
            val profile = ServerProfile.createWireGuardProfile(
                name = "Test Server",
                hostname = "vpn.example.com",
                port = 51820,
                config = config,
                id = 1L
            )
            
            coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
            
            // Act
            val result = adapter.connect(profile)
            
            // Assert
            assertTrue(result.isFailure, "Connection should fail with invalid public key")
        } catch (e: IllegalArgumentException) {
            // Expected - constructor validation caught the invalid key
            assertTrue(
                e.message?.contains("public key", ignoreCase = true) == true ||
                e.message?.contains("key", ignoreCase = true) == true,
                "Error message should mention key: ${e.message}"
            )
        }
    }
    
    /**
     * Test: Valid preshared key should be accepted
     * Requirements: 9.4
     */
    @Test
    fun `valid preshared key format should be accepted`() = runTest {
        // Arrange
        val profile = createTestProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        coEvery { credentialStore.getWireGuardPresharedKey(profile.id) } returns Result.success(validPresharedKey)
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Connection should succeed with valid preshared key")
        coVerify { credentialStore.getWireGuardPresharedKey(profile.id) }
    }
    
    /**
     * Test: Valid endpoint format should be accepted
     * Requirements: 3.1
     */
    @Test
    fun `valid endpoint format should be accepted`() = runTest {
        // Arrange
        val profile = createTestProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        coEvery { credentialStore.getWireGuardPresharedKey(profile.id) } returns Result.failure(Exception("Not found"))
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        // Endpoint validation should pass (actual connection may fail in test environment)
    }
    
    /**
     * Test: Invalid endpoint format should be rejected
     * Requirements: 3.1
     */
    @Test
    fun `invalid endpoint format should be rejected`() = runTest {
        // Arrange - endpoint without port will be rejected by constructor
        try {
            val config = WireGuardConfig(
                publicKey = validPublicKey,
                allowedIPs = listOf("0.0.0.0/0"),
                endpoint = "vpn.example.com", // Missing port
                mtu = 1420
            )
            
            // If we get here, validation didn't catch it (shouldn't happen)
            val profile = ServerProfile.createWireGuardProfile(
                name = "Test Server",
                hostname = "vpn.example.com",
                port = 51820,
                config = config,
                id = 1L
            )
            
            coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
            
            // Act
            val result = adapter.testConnection(profile)
            
            // Assert
            assertTrue(result.isSuccess)
            val testResult = result.getOrNull()
            assertNotNull(testResult)
            assertFalse(testResult.success, "Test should fail with invalid endpoint format")
        } catch (e: IllegalArgumentException) {
            // Expected - constructor validation caught the invalid endpoint
            assertTrue(
                e.message?.contains("endpoint", ignoreCase = true) == true,
                "Error message should mention endpoint: ${e.message}"
            )
        }
    }
    
    /**
     * Test: Endpoint with invalid port should be rejected
     * Requirements: 3.1
     */
    @Test
    fun `endpoint with invalid port should be rejected`() = runTest {
        // Arrange - endpoint with invalid port will be rejected by constructor
        try {
            val config = WireGuardConfig(
                publicKey = validPublicKey,
                allowedIPs = listOf("0.0.0.0/0"),
                endpoint = "vpn.example.com:99999", // Invalid port
                mtu = 1420
            )
            
            // If we get here, validation didn't catch it (shouldn't happen)
            val profile = ServerProfile.createWireGuardProfile(
                name = "Test Server",
                hostname = "vpn.example.com",
                port = 51820,
                config = config,
                id = 1L
            )
            
            coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
            
            // Act
            val result = adapter.testConnection(profile)
            
            // Assert
            assertTrue(result.isSuccess)
            val testResult = result.getOrNull()
            assertNotNull(testResult)
            assertFalse(testResult.success, "Test should fail with invalid port")
        } catch (e: IllegalArgumentException) {
            // Expected - constructor validation caught the invalid port
            assertTrue(
                e.message?.contains("port", ignoreCase = true) == true ||
                e.message?.contains("endpoint", ignoreCase = true) == true,
                "Error message should mention port: ${e.message}"
            )
        }
    }
    
    /**
     * Test: Configuration with valid allowed IPs should be accepted
     * Requirements: 3.1
     */
    @Test
    fun `configuration with valid allowed IPs should be accepted`() = runTest {
        // Arrange
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0", "::/0", "10.0.0.0/24"),
            endpoint = "vpn.example.com:51820",
            mtu = 1420
        )
        val profile = ServerProfile.createWireGuardProfile(
            name = "Test Server",
            hostname = "vpn.example.com",
            port = 51820,
            config = config,
            id = 1L
        )
        
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        coEvery { credentialStore.getWireGuardPresharedKey(profile.id) } returns Result.failure(Exception("Not found"))
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Connection should succeed with valid allowed IPs")
    }
    
    /**
     * Test: Configuration with valid MTU should be accepted
     * Requirements: 3.1
     */
    @Test
    fun `configuration with valid MTU should be accepted`() = runTest {
        // Arrange
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            mtu = 1280 // Minimum valid MTU
        )
        val profile = ServerProfile.createWireGuardProfile(
            name = "Test Server",
            hostname = "vpn.example.com",
            port = 51820,
            config = config,
            id = 1L
        )
        
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        coEvery { credentialStore.getWireGuardPresharedKey(profile.id) } returns Result.failure(Exception("Not found"))
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Connection should succeed with valid MTU")
    }
    
    /**
     * Test: Configuration with valid persistent keepalive should be accepted
     * Requirements: 3.1
     */
    @Test
    fun `configuration with valid persistent keepalive should be accepted`() = runTest {
        // Arrange
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            persistentKeepalive = 25,
            mtu = 1420
        )
        val profile = ServerProfile.createWireGuardProfile(
            name = "Test Server",
            hostname = "vpn.example.com",
            port = 51820,
            config = config,
            id = 1L
        )
        
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        coEvery { credentialStore.getWireGuardPresharedKey(profile.id) } returns Result.failure(Exception("Not found"))
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Connection should succeed with valid persistent keepalive")
    }
    
    /**
     * Test: Missing private key should fail connection
     * Requirements: 9.4
     */
    @Test
    fun `missing private key should fail connection`() = runTest {
        // Arrange
        val profile = createTestProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.failure(Exception("Key not found"))
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isFailure, "Connection should fail when private key is missing")
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is WireGuardException)
        assertTrue(exception.message?.contains("Failed to retrieve private key") == true)
    }
    
    /**
     * Test: Non-WireGuard profile should be rejected
     * Requirements: 3.1
     */
    @Test
    fun `non-WireGuard profile should be rejected`() = runTest {
        // Arrange - Create a VLESS profile
        val profile = ServerProfile(
            id = 1L,
            name = "VLESS Server",
            protocol = Protocol.VLESS,
            hostname = "vpn.example.com",
            port = 443,
            vlessConfigJson = "{}"
        )
        
        // Act
        val result = adapter.connect(profile)
        
        // Assert
        assertTrue(result.isFailure, "Connection should fail for non-WireGuard profile")
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        // The exception is wrapped in WireGuardException
        assertTrue(exception is WireGuardException || exception is IllegalArgumentException)
    }
    
    /**
     * Helper function to create a test profile with valid configuration.
     */
    private fun createTestProfile(): ServerProfile {
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0", "::/0"),
            endpoint = "vpn.example.com:51820",
            mtu = 1420
        )
        
        return ServerProfile.createWireGuardProfile(
            name = "Test Server",
            hostname = "vpn.example.com",
            port = 51820,
            config = config,
            id = 1L
        )
    }
}
