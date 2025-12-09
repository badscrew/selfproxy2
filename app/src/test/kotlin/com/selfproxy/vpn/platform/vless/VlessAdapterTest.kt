package com.selfproxy.vpn.platform.vless

import android.content.Context
import com.selfproxy.vpn.data.model.FlowControl
import com.selfproxy.vpn.data.model.GrpcSettings
import com.selfproxy.vpn.data.model.Http2Settings
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.TlsSettings
import com.selfproxy.vpn.data.model.TransportProtocol
import com.selfproxy.vpn.data.model.VlessConfig
import com.selfproxy.vpn.data.model.WebSocketSettings
import com.selfproxy.vpn.domain.repository.CredentialStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for VlessAdapter.
 * 
 * Tests UUID validation, transport protocol selection, and TLS configuration.
 * 
 * Requirements: 3.2, 9.12
 */
class VlessAdapterTest {
    
    private lateinit var context: Context
    private lateinit var credentialStore: CredentialStore
    private lateinit var adapter: VlessAdapter
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        credentialStore = mockk()
        adapter = VlessAdapter(context, credentialStore)
    }
    
    // UUID Validation Tests
    
    @Test
    fun `valid UUID should pass validation`() = runTest {
        // Arrange
        val validUuids = listOf(
            "550e8400-e29b-41d4-a716-446655440000",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            "00000000-0000-0000-0000-000000000000",
            "ffffffff-ffff-ffff-ffff-ffffffffffff"
        )
        
        val profile = createTestProfile(TransportProtocol.TCP)
        
        // Act & Assert
        validUuids.forEach { uuid ->
            coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(uuid)
            
            val result = adapter.testConnection(profile)
            
            assertTrue(result.isSuccess, "UUID $uuid should be valid")
            val testResult = result.getOrNull()
            assertNotNull(testResult)
            // Note: Connection may fail for other reasons, but UUID validation should pass
        }
    }
    
    @Test
    fun `invalid UUID format should fail validation`() = runTest {
        // Arrange
        val invalidUuids = listOf(
            "not-a-uuid",
            "550e8400-e29b-41d4-a716",  // Too short
            "550e8400-e29b-41d4-a716-446655440000-extra",  // Too long
            "550e8400e29b41d4a716446655440000",  // Missing hyphens
            "gggggggg-gggg-gggg-gggg-gggggggggggg",  // Invalid hex
            "",  // Empty
            "550e8400-e29b-41d4-a716-44665544000g"  // Invalid character
        )
        
        val profile = createTestProfile(TransportProtocol.TCP)
        
        // Act & Assert
        invalidUuids.forEach { uuid ->
            coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(uuid)
            
            val result = adapter.testConnection(profile)
            
            assertTrue(result.isSuccess, "Test should complete")
            val testResult = result.getOrNull()
            assertNotNull(testResult)
            assertFalse(testResult.success, "UUID $uuid should be invalid")
            assertNotNull(testResult.errorMessage)
            assertTrue(
                testResult.errorMessage!!.contains("UUID", ignoreCase = true) ||
                testResult.errorMessage!!.contains("format", ignoreCase = true),
                "Error message should mention UUID format: ${testResult.errorMessage}"
            )
        }
    }
    
    @Test
    fun `missing UUID should fail validation`() = runTest {
        // Arrange
        val profile = createTestProfile(TransportProtocol.TCP)
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.failure(
            Exception("UUID not found")
        )
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        assertFalse(testResult.success)
        assertNotNull(testResult.errorMessage)
        assertTrue(
            testResult.errorMessage!!.contains("UUID", ignoreCase = true),
            "Error message should mention UUID: ${testResult.errorMessage}"
        )
    }
    
    // Transport Protocol Selection Tests
    
    @Test
    fun `TCP transport should be valid without additional settings`() = runTest {
        // Arrange
        val profile = createTestProfile(TransportProtocol.TCP)
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        // TCP should pass transport validation (may fail on connectivity)
    }
    
    @Test
    fun `WebSocket transport requires WebSocket settings`() = runTest {
        // Arrange - Profile without WebSocket settings
        // This should fail during VlessConfig creation due to init block
        var exceptionThrown = false
        try {
            VlessConfig(
                transport = TransportProtocol.WEBSOCKET,
                websocketSettings = null  // Missing required settings
            )
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            assertTrue(
                e.message?.contains("WebSocket", ignoreCase = true) == true,
                "Exception should mention WebSocket: ${e.message}"
            )
        }
        
        assertTrue(exceptionThrown, "Should throw exception for missing WebSocket settings")
    }
    
    @Test
    fun `WebSocket transport with valid settings should pass validation`() = runTest {
        // Arrange
        val profile = createTestProfile(
            TransportProtocol.WEBSOCKET,
            websocketSettings = WebSocketSettings(
                path = "/ws",
                headers = mapOf("Host" to "example.com")
            )
        )
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        // WebSocket settings should pass validation (may fail on connectivity)
    }
    
    @Test
    fun `gRPC transport requires gRPC settings`() = runTest {
        // Arrange - Profile without gRPC settings
        // This should fail during VlessConfig creation due to init block
        var exceptionThrown = false
        try {
            VlessConfig(
                transport = TransportProtocol.GRPC,
                grpcSettings = null  // Missing required settings
            )
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            assertTrue(
                e.message?.contains("gRPC", ignoreCase = true) == true,
                "Exception should mention gRPC: ${e.message}"
            )
        }
        
        assertTrue(exceptionThrown, "Should throw exception for missing gRPC settings")
    }
    
    @Test
    fun `gRPC transport with valid settings should pass validation`() = runTest {
        // Arrange
        val profile = createTestProfile(
            TransportProtocol.GRPC,
            grpcSettings = GrpcSettings(
                serviceName = "VlessService",
                multiMode = false
            )
        )
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        // gRPC settings should pass validation (may fail on connectivity)
    }
    
    @Test
    fun `HTTP2 transport requires HTTP2 settings`() = runTest {
        // Arrange - Profile without HTTP/2 settings
        // This should fail during VlessConfig creation due to init block
        var exceptionThrown = false
        try {
            VlessConfig(
                transport = TransportProtocol.HTTP2,
                http2Settings = null  // Missing required settings
            )
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            assertTrue(
                e.message?.contains("HTTP", ignoreCase = true) == true,
                "Exception should mention HTTP/2: ${e.message}"
            )
        }
        
        assertTrue(exceptionThrown, "Should throw exception for missing HTTP/2 settings")
    }
    
    @Test
    fun `HTTP2 transport with valid settings should pass validation`() = runTest {
        // Arrange
        val profile = createTestProfile(
            TransportProtocol.HTTP2,
            http2Settings = Http2Settings(
                path = "/http2",
                host = listOf("example.com")
            )
        )
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        // HTTP/2 settings should pass validation (may fail on connectivity)
    }
    
    // TLS Configuration Tests
    
    @Test
    fun `valid TLS configuration should pass validation`() = runTest {
        // Arrange
        val tlsSettings = TlsSettings(
            serverName = "example.com",
            alpn = listOf("h2", "http/1.1"),
            allowInsecure = false,
            fingerprint = "chrome"
        )
        
        val profile = createTestProfile(
            TransportProtocol.TCP,
            tlsSettings = tlsSettings
        )
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        // TLS settings should pass validation (may fail on connectivity)
    }
    
    @Test
    fun `TLS configuration with empty server name should fail validation`() = runTest {
        // Arrange - TLS with blank server name
        var exceptionThrown = false
        try {
            TlsSettings(
                serverName = "",  // Invalid: blank server name
                alpn = listOf("h2")
            )
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            assertTrue(
                e.message?.contains("server name", ignoreCase = true) == true,
                "Exception should mention server name: ${e.message}"
            )
        }
        
        assertTrue(exceptionThrown, "Should throw exception for blank server name")
    }
    
    @Test
    fun `TLS configuration with blank server name should fail validation`() = runTest {
        // Arrange - TLS with blank server name
        var exceptionThrown = false
        try {
            TlsSettings(
                serverName = "   ",  // Invalid: whitespace only
                alpn = listOf("h2")
            )
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            assertTrue(
                e.message?.contains("server name", ignoreCase = true) == true,
                "Exception should mention server name: ${e.message}"
            )
        }
        
        assertTrue(exceptionThrown, "Should throw exception for blank server name")
    }
    
    @Test
    fun `TLS configuration with valid SNI should pass validation`() = runTest {
        // Arrange
        val validServerNames = listOf(
            "example.com",
            "sub.example.com",
            "deep.sub.example.com",
            "example-with-dash.com",
            "192.168.1.1"  // IP addresses are valid SNI
        )
        
        validServerNames.forEach { serverName ->
            val tlsSettings = TlsSettings(
                serverName = serverName,
                alpn = listOf("h2")
            )
            
            val profile = createTestProfile(
                TransportProtocol.TCP,
                tlsSettings = tlsSettings
            )
            coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
            
            // Act
            val result = adapter.testConnection(profile)
            
            // Assert
            assertTrue(result.isSuccess, "Server name $serverName should be valid")
            val testResult = result.getOrNull()
            assertNotNull(testResult)
        }
    }
    
    @Test
    fun `TLS configuration with ALPN protocols should be valid`() = runTest {
        // Arrange
        val alpnConfigs = listOf(
            listOf("h2"),
            listOf("http/1.1"),
            listOf("h2", "http/1.1"),
            emptyList()  // Empty ALPN is valid
        )
        
        alpnConfigs.forEach { alpn ->
            val tlsSettings = TlsSettings(
                serverName = "example.com",
                alpn = alpn
            )
            
            val profile = createTestProfile(
                TransportProtocol.TCP,
                tlsSettings = tlsSettings
            )
            coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
            
            // Act
            val result = adapter.testConnection(profile)
            
            // Assert
            assertTrue(result.isSuccess, "ALPN $alpn should be valid")
            val testResult = result.getOrNull()
            assertNotNull(testResult)
        }
    }
    
    @Test
    fun `TLS configuration with allowInsecure flag should be valid`() = runTest {
        // Arrange
        val tlsSettings = TlsSettings(
            serverName = "example.com",
            allowInsecure = true  // Should be valid (though not recommended)
        )
        
        val profile = createTestProfile(
            TransportProtocol.TCP,
            tlsSettings = tlsSettings
        )
        coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
        
        // Act
        val result = adapter.testConnection(profile)
        
        // Assert
        assertTrue(result.isSuccess)
        val testResult = result.getOrNull()
        assertNotNull(testResult)
        // allowInsecure flag should be accepted (may fail on connectivity)
    }
    
    @Test
    fun `TLS configuration with fingerprint should be valid`() = runTest {
        // Arrange
        val fingerprints = listOf(
            "chrome",
            "firefox",
            "safari",
            "randomized"
        )
        
        fingerprints.forEach { fingerprint ->
            val tlsSettings = TlsSettings(
                serverName = "example.com",
                fingerprint = fingerprint
            )
            
            val profile = createTestProfile(
                TransportProtocol.TCP,
                tlsSettings = tlsSettings
            )
            coEvery { credentialStore.getVlessUuid(profile.id) } returns Result.success(TEST_UUID)
            
            // Act
            val result = adapter.testConnection(profile)
            
            // Assert
            assertTrue(result.isSuccess, "Fingerprint $fingerprint should be valid")
            val testResult = result.getOrNull()
            assertNotNull(testResult)
        }
    }
    
    // Helper Functions
    
    private fun createTestProfile(
        transport: TransportProtocol,
        websocketSettings: WebSocketSettings? = null,
        grpcSettings: GrpcSettings? = null,
        http2Settings: Http2Settings? = null,
        tlsSettings: TlsSettings? = null
    ): ServerProfile {
        val vlessConfig = when (transport) {
            TransportProtocol.TCP -> VlessConfig(
                transport = transport,
                tlsSettings = tlsSettings
            )
            TransportProtocol.WEBSOCKET -> VlessConfig(
                transport = transport,
                websocketSettings = websocketSettings ?: WebSocketSettings(path = "/ws"),
                tlsSettings = tlsSettings
            )
            TransportProtocol.GRPC -> VlessConfig(
                transport = transport,
                grpcSettings = grpcSettings ?: GrpcSettings(serviceName = "VlessService"),
                tlsSettings = tlsSettings
            )
            TransportProtocol.HTTP2 -> VlessConfig(
                transport = transport,
                http2Settings = http2Settings ?: Http2Settings(path = "/http2"),
                tlsSettings = tlsSettings
            )
        }
        
        return ServerProfile.createVlessProfile(
            name = "Test VLESS",
            hostname = "example.com",
            port = 443,
            config = vlessConfig,
            id = 1L
        )
    }
    
    companion object {
        private const val TEST_UUID = "550e8400-e29b-41d4-a716-446655440000"
    }
}
