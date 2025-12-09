package com.selfproxy.vpn.domain.util

import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for SecurityValidator.
 * 
 * Tests validation of:
 * - WireGuard keys (private, public, preshared)
 * - VLESS UUIDs
 * - TLS certificates
 * - WireGuard endpoints
 * - Allowed IPs
 */
class SecurityValidatorTest {
    
    @Before
    fun setup() {
        // Mock Android Base64 for unit tests
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            val input = firstArg<String>()
            // Simple base64 decoder for testing
            java.util.Base64.getDecoder().decode(input)
        }
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    // WireGuard Key Validation Tests
    
    @Test
    fun `validateWireGuardKey should accept valid 32-byte base64 key`() {
        // Valid 32-byte key encoded in base64 (44 characters)
        val validKey = "YAnz5TF+lXXJte14tji3Zlx3TGmHRcGgOoEdS4ss5uU="
        
        val result = SecurityValidator.validateWireGuardKey(validKey)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateWireGuardKey should reject blank key`() {
        val result = SecurityValidator.validateWireGuardKey("")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be blank") == true)
    }
    
    @Test
    fun `validateWireGuardKey should reject invalid base64`() {
        val invalidKey = "not-valid-base64!!!"
        
        val result = SecurityValidator.validateWireGuardKey(invalidKey)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("valid base64") == true)
    }
    
    @Test
    fun `validateWireGuardKey should reject wrong length key`() {
        // Valid base64 but wrong length (16 bytes instead of 32)
        val shortKey = "YAnz5TF+lXXJte14tji3Zg=="
        
        val result = SecurityValidator.validateWireGuardKey(shortKey)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("32 bytes") == true)
    }
    
    @Test
    fun `validateWireGuardPrivateKey should reject all-zero key`() {
        // 32 bytes of zeros
        val zeroKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        
        val result = SecurityValidator.validateWireGuardPrivateKey(zeroKey)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("all zeros") == true)
    }
    
    @Test
    fun `validateWireGuardPublicKey should reject all-zero key`() {
        // 32 bytes of zeros
        val zeroKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        
        val result = SecurityValidator.validateWireGuardPublicKey(zeroKey)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("all zeros") == true)
    }
    
    // VLESS UUID Validation Tests
    
    @Test
    fun `validateVlessUuid should accept valid RFC 4122 UUID`() {
        val validUuid = "550e8400-e29b-41d4-a716-446655440000"
        
        val result = SecurityValidator.validateVlessUuid(validUuid)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateVlessUuid should accept UUID with uppercase letters`() {
        val validUuid = "550E8400-E29B-41D4-A716-446655440000"
        
        val result = SecurityValidator.validateVlessUuid(validUuid)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateVlessUuid should reject blank UUID`() {
        val result = SecurityValidator.validateVlessUuid("")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be blank") == true)
    }
    
    @Test
    fun `validateVlessUuid should reject invalid format`() {
        val invalidUuid = "not-a-valid-uuid"
        
        val result = SecurityValidator.validateVlessUuid(invalidUuid)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("RFC 4122") == true)
    }
    
    @Test
    fun `validateVlessUuid should reject UUID without dashes`() {
        val invalidUuid = "550e8400e29b41d4a716446655440000"
        
        val result = SecurityValidator.validateVlessUuid(invalidUuid)
        
        assertTrue(result.isFailure)
    }
    
    
    // WireGuard Endpoint Validation Tests
    
    @Test
    fun `validateWireGuardEndpoint should accept valid hostname and port`() {
        val validEndpoint = "vpn.example.com:51820"
        
        val result = SecurityValidator.validateWireGuardEndpoint(validEndpoint)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateWireGuardEndpoint should accept valid IP and port`() {
        val validEndpoint = "192.168.1.1:51820"
        
        val result = SecurityValidator.validateWireGuardEndpoint(validEndpoint)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateWireGuardEndpoint should reject blank endpoint`() {
        val result = SecurityValidator.validateWireGuardEndpoint("")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be blank") == true)
    }
    
    @Test
    fun `validateWireGuardEndpoint should reject endpoint without port`() {
        val invalidEndpoint = "vpn.example.com"
        
        val result = SecurityValidator.validateWireGuardEndpoint(invalidEndpoint)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("hostname:port") == true)
    }
    
    @Test
    fun `validateWireGuardEndpoint should reject invalid port`() {
        val invalidEndpoint = "vpn.example.com:99999"
        
        val result = SecurityValidator.validateWireGuardEndpoint(invalidEndpoint)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("between 1 and 65535") == true)
    }
    
    @Test
    fun `validateWireGuardEndpoint should reject non-numeric port`() {
        val invalidEndpoint = "vpn.example.com:abc"
        
        val result = SecurityValidator.validateWireGuardEndpoint(invalidEndpoint)
        
        assertTrue(result.isFailure)
    }
    
    // Allowed IPs Validation Tests
    
    @Test
    fun `validateAllowedIPs should accept valid IPv4 CIDR`() {
        val validIPs = listOf("0.0.0.0/0", "192.168.1.0/24")
        
        val result = SecurityValidator.validateAllowedIPs(validIPs)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateAllowedIPs should accept valid IPv6 CIDR`() {
        val validIPs = listOf("::/0", "2001:db8::/32")
        
        val result = SecurityValidator.validateAllowedIPs(validIPs)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateAllowedIPs should reject empty list`() {
        val result = SecurityValidator.validateAllowedIPs(emptyList())
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be empty") == true)
    }
    
    @Test
    fun `validateAllowedIPs should reject IP without CIDR notation`() {
        val invalidIPs = listOf("192.168.1.0")
        
        val result = SecurityValidator.validateAllowedIPs(invalidIPs)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("CIDR notation") == true)
    }
    
    @Test
    fun `validateAllowedIPs should reject invalid IPv4 prefix`() {
        val invalidIPs = listOf("192.168.1.0/33")
        
        val result = SecurityValidator.validateAllowedIPs(invalidIPs)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("between 0 and 32") == true)
    }
    
    @Test
    fun `validateAllowedIPs should reject invalid IPv6 prefix`() {
        val invalidIPs = listOf("::/129")
        
        val result = SecurityValidator.validateAllowedIPs(invalidIPs)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("between 0 and 128") == true)
    }
    
    @Test
    fun `validateAllowedIPs should reject non-numeric prefix`() {
        val invalidIPs = listOf("192.168.1.0/abc")
        
        val result = SecurityValidator.validateAllowedIPs(invalidIPs)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid prefix") == true)
    }
}
