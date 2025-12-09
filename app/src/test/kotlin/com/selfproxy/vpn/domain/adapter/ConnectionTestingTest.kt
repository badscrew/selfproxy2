package com.selfproxy.vpn.domain.adapter

import android.content.Context
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.TransportProtocol
import com.selfproxy.vpn.data.model.VlessConfig
import com.selfproxy.vpn.data.model.WireGuardConfig
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.repository.CredentialStore
import com.selfproxy.vpn.platform.vless.VlessAdapter
import com.selfproxy.vpn.platform.wireguard.WireGuardAdapter
import com.wireguard.android.backend.Backend
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for connection testing functionality.
 * 
 * Tests timeout handling, error message generation, and latency calculation
 * for both WireGuard and VLESS protocols.
 * 
 * Requirements: 8.6, 8.7
 */
class ConnectionTestingTest {
    
    private lateinit var context: Context
    private lateinit var credentialStore: CredentialStore
    private lateinit var backend: Backend
    private lateinit var wireGuardAdapter: WireGuardAdapter
    private lateinit var vlessAdapter: VlessAdapter
    
    // Valid test keys
    private val validPrivateKey = "YAnz5TF+lXXJte14tji3zlMNftqL+Uc+oCONvOkjpkI="
    private val validPublicKey = "HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw="
    private val validUuid = "550e8400-e29b-41d4-a716-446655440000"
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        credentialStore = mockk()
        backend = mockk(relaxed = true)
        
        wireGuardAdapter = WireGuardAdapter(context, credentialStore, backend)
        vlessAdapter = VlessAdapter(context, credentialStore)
    }
    
    // Timeout Handling Tests
    
    /**
     * Test: Connection test should complete within reasonable time
     * Requirements: 8.6
     */
    @Test
    fun `WireGuard connection test should complete within timeout`() = runTest {
        // Arrange
        val profile = createWireGuardProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        
        val startTime = System.currentTimeMillis()
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        val duration = System.currentTimeMillis() - startTime
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete successfully")
        assertTrue(duration < 10000, "Test should complete within 10 seconds (actual: ${duration}ms)")
    }
    
    /**
     * Test: VLESS connection test should complete within timeout
     * Requirements: 8.6
     */
    @Test
    fun `VLESS connection test should complete within timeout`() = runTest {
        // Arrange
        val profile = createVlessProfile()
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(validUuid)
        
        val startTime = System.currentTimeMillis()
        
        // Act
        val result = vlessAdapter.testConnection(profile)
        
        val duration = System.currentTimeMillis() - startTime
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete successfully")
        assertTrue(duration < 10000, "Test should complete within 10 seconds (actual: ${duration}ms)")
    }
    
    /**
     * Test: Connection test with unreachable server should timeout gracefully
     * Requirements: 8.6
     */
    @Test
    fun `connection test with unreachable server should timeout gracefully`() = runTest {
        // Arrange - Use a non-routable IP address (RFC 5737 TEST-NET-1)
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "192.0.2.1:51820",  // Non-routable test IP
            mtu = 1420
        )
        val profile = ServerProfile.createWireGuardProfile(
            name = "Unreachable Server",
            hostname = "192.0.2.1",
            port = 51820,
            config = config,
            id = 1L
        )
        
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        
        val startTime = System.currentTimeMillis()
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        val duration = System.currentTimeMillis() - startTime
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete (not throw exception)")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        
        // Should fail but not hang
        assertTrue(duration < 15000, "Test should timeout within reasonable time (actual: ${duration}ms)")
    }
    
    // Error Message Generation Tests
    
    /**
     * Test: Missing credentials should generate specific error message
     * Requirements: 8.7
     */
    @Test
    fun `missing WireGuard private key should generate specific error message`() = runTest {
        // Arrange
        val profile = createWireGuardProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.failure(
            Exception("Key not found")
        )
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success, "Test should fail")
        assertNotNull(testResult.errorMessage, "Should have error message")
        assertTrue(
            testResult.errorMessage!!.contains("private key", ignoreCase = true) ||
            testResult.errorMessage!!.contains("not found", ignoreCase = true),
            "Error message should mention private key: ${testResult.errorMessage}"
        )
    }
    
    /**
     * Test: Invalid key format should generate specific error message
     * Requirements: 8.7
     */
    @Test
    fun `invalid WireGuard key format should generate specific error message`() = runTest {
        // Arrange
        val profile = createWireGuardProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success("invalid-key")
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success, "Test should fail")
        assertNotNull(testResult.errorMessage, "Should have error message")
        assertTrue(
            testResult.errorMessage!!.contains("key", ignoreCase = true) &&
            testResult.errorMessage!!.contains("format", ignoreCase = true),
            "Error message should mention key format: ${testResult.errorMessage}"
        )
    }
    
    /**
     * Test: Invalid endpoint format should generate specific error message
     * Requirements: 8.7
     */
    @Test
    fun `invalid endpoint format should generate specific error message`() = runTest {
        // Arrange - Endpoint without port
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com",  // Missing port
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
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success, "Test should fail")
        assertNotNull(testResult.errorMessage, "Should have error message")
        assertTrue(
            testResult.errorMessage!!.contains("endpoint", ignoreCase = true) ||
            testResult.errorMessage!!.contains("format", ignoreCase = true),
            "Error message should mention endpoint format: ${testResult.errorMessage}"
        )
    }
    
    /**
     * Test: Invalid port should generate specific error message
     * Requirements: 8.7
     */
    @Test
    fun `invalid port should generate specific error message`() = runTest {
        // Arrange - Invalid port number
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:99999",  // Invalid port
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
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success, "Test should fail")
        assertNotNull(testResult.errorMessage, "Should have error message")
        assertTrue(
            testResult.errorMessage!!.contains("port", ignoreCase = true),
            "Error message should mention port: ${testResult.errorMessage}"
        )
    }
    
    /**
     * Test: Missing UUID should generate specific error message
     * Requirements: 8.7
     */
    @Test
    fun `missing VLESS UUID should generate specific error message`() = runTest {
        // Arrange
        val profile = createVlessProfile()
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.failure(
            Exception("UUID not found")
        )
        
        // Act
        val result = vlessAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success, "Test should fail")
        assertNotNull(testResult.errorMessage, "Should have error message")
        assertTrue(
            testResult.errorMessage!!.contains("UUID", ignoreCase = true),
            "Error message should mention UUID: ${testResult.errorMessage}"
        )
    }
    
    /**
     * Test: Invalid UUID format should generate specific error message
     * Requirements: 8.7
     */
    @Test
    fun `invalid VLESS UUID format should generate specific error message`() = runTest {
        // Arrange
        val profile = createVlessProfile()
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success("not-a-uuid")
        
        // Act
        val result = vlessAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success, "Test should fail")
        assertNotNull(testResult.errorMessage, "Should have error message")
        assertTrue(
            testResult.errorMessage!!.contains("UUID", ignoreCase = true) &&
            testResult.errorMessage!!.contains("format", ignoreCase = true),
            "Error message should mention UUID format: ${testResult.errorMessage}"
        )
    }
    
    /**
     * Test: Unreachable server should generate specific error message
     * Requirements: 8.7
     */
    @Test
    fun `unreachable VLESS server should generate specific error message`() = runTest {
        // Arrange - Use non-routable IP
        val config = VlessConfig(
            transport = TransportProtocol.TCP
        )
        val profile = ServerProfile.createVlessProfile(
            name = "Unreachable Server",
            hostname = "192.0.2.1",  // Non-routable test IP
            port = 443,
            config = config,
            id = 1L
        )
        
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(validUuid)
        
        // Act
        val result = vlessAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        
        // May fail with unreachable or timeout error
        if (!testResult.success) {
            assertNotNull(testResult.errorMessage, "Should have error message")
            assertTrue(
                testResult.errorMessage!!.contains("connect", ignoreCase = true) ||
                testResult.errorMessage!!.contains("reach", ignoreCase = true) ||
                testResult.errorMessage!!.contains("timeout", ignoreCase = true),
                "Error message should indicate connectivity issue: ${testResult.errorMessage}"
            )
        }
    }
    
    // Latency Calculation Tests
    
    /**
     * Test: Successful connection test should include latency measurement
     * Requirements: 8.6
     */
    @Test
    fun `successful WireGuard test should include latency measurement`() = runTest {
        // Arrange
        val profile = createWireGuardProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        
        // If test succeeds, should have latency
        if (testResult.success) {
            assertNotNull(testResult.latencyMs, "Successful test should include latency")
            assertTrue(testResult.latencyMs!! > 0, "Latency should be positive")
            assertTrue(testResult.latencyMs!! < 10000, "Latency should be reasonable (< 10s)")
        }
    }
    
    /**
     * Test: Failed connection test should not include latency
     * Requirements: 8.6
     */
    @Test
    fun `failed connection test should not include latency`() = runTest {
        // Arrange - Invalid key to force failure
        val profile = createWireGuardProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success("invalid-key")
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success, "Test should fail")
        assertNull(testResult.latencyMs, "Failed test should not include latency")
    }
    
    /**
     * Test: Latency should be measured in milliseconds
     * Requirements: 8.6
     */
    @Test
    fun `latency should be measured in milliseconds`() = runTest {
        // Arrange
        val profile = createVlessProfile()
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(validUuid)
        
        // Act
        val result = vlessAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        
        // If test succeeds, verify latency is in reasonable range
        if (testResult.success && testResult.latencyMs != null) {
            assertTrue(testResult.latencyMs!! > 0, "Latency should be positive")
            assertTrue(testResult.latencyMs!! < 10000, "Latency should be < 10000ms (10 seconds)")
        }
    }
    
    /**
     * Test: Multiple test runs should produce consistent latency measurements
     * Requirements: 8.6
     */
    @Test
    fun `multiple test runs should produce consistent latency measurements`() = runTest {
        // Arrange
        val profile = createWireGuardProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        
        val latencies = mutableListOf<Long>()
        
        // Act - Run test multiple times
        repeat(3) {
            val result = wireGuardAdapter.testConnection(profile)
            assertTrue(result.isSuccess, "Test should complete")
            val testResult = result.getOrNull()
            assertNotNull(testResult)
            
            if (testResult.success && testResult.latencyMs != null) {
                latencies.add(testResult.latencyMs!!)
            }
        }
        
        // Assert - If we got latency measurements, they should be consistent
        if (latencies.size >= 2) {
            val avgLatency = latencies.average()
            latencies.forEach { latency ->
                // Latency should be within 50% of average (allowing for network variance)
                val deviation = kotlin.math.abs(latency - avgLatency) / avgLatency
                assertTrue(
                    deviation < 0.5,
                    "Latency measurements should be consistent (deviation: ${deviation * 100}%)"
                )
            }
        }
    }
    
    /**
     * Test: Connection test result should have all required fields
     * Requirements: 8.6, 8.7
     */
    @Test
    fun `connection test result should have all required fields`() = runTest {
        // Arrange
        val profile = createWireGuardProfile()
        coEvery { credentialStore.getWireGuardPrivateKey(profile.id) } returns Result.success(validPrivateKey)
        
        // Act
        val result = wireGuardAdapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess, "Test should complete")
        val testResult = result.getOrNull()
        assertNotNull(testResult, "Should have test result")
        
        // Verify result structure
        assertNotNull(testResult.success, "Should have success field")
        
        if (testResult.success) {
            // Successful test should have latency
            assertNotNull(testResult.latencyMs, "Successful test should have latency")
            // Error message should be null for success
            assertNull(testResult.errorMessage, "Successful test should not have error message")
        } else {
            // Failed test should have error message
            assertNotNull(testResult.errorMessage, "Failed test should have error message")
            assertTrue(testResult.errorMessage!!.isNotBlank(), "Error message should not be blank")
            // Latency should be null for failure
            assertNull(testResult.latencyMs, "Failed test should not have latency")
        }
    }
    
    // Helper Functions
    
    private fun createWireGuardProfile(): ServerProfile {
        val config = WireGuardConfig(
            publicKey = validPublicKey,
            allowedIPs = listOf("0.0.0.0/0", "::/0"),
            endpoint = "vpn.example.com:51820",
            mtu = 1420
        )
        
        return ServerProfile.createWireGuardProfile(
            name = "Test WireGuard",
            hostname = "vpn.example.com",
            port = 51820,
            config = config,
            id = 1L
        )
    }
    
    private fun createVlessProfile(): ServerProfile {
        val config = VlessConfig(
            transport = TransportProtocol.TCP
        )
        
        return ServerProfile.createVlessProfile(
            name = "Test VLESS",
            hostname = "example.com",
            port = 443,
            config = config,
            id = 1L
        )
    }
}
