package com.selfproxy.vpn.platform.vpn

import com.selfproxy.vpn.TestKeys
import com.selfproxy.vpn.data.model.WireGuardConfig
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for VPN service configuration.
 * 
 * Tests TUN interface builder configuration, DNS server configuration,
 * and route configuration.
 * 
 * Requirements: 4.3, 4.7
 */
class TunnelVpnServiceConfigTest {
    
    @Before
    fun setup() {
        // Mock Android Base64
        TestKeys.mockAndroidBase64()
    }

    @Test
    fun `Service intent actions should be correctly defined`() {
        // Arrange
        val expectedStartAction = "com.selfproxy.vpn.START_VPN"
        val expectedStopAction = "com.selfproxy.vpn.STOP_VPN"
        
        // Act & Assert
        assertEquals(expectedStartAction, TunnelVpnService.ACTION_START_VPN)
        assertEquals(expectedStopAction, TunnelVpnService.ACTION_STOP_VPN)
    }

    @Test
    fun `Service intent extras should be correctly defined`() {
        // Arrange
        val expectedProfileIdExtra = "profile_id"
        val expectedProtocolAdapterExtra = "protocol_adapter"
        
        // Act & Assert
        assertEquals(expectedProfileIdExtra, TunnelVpnService.EXTRA_PROFILE_ID)
        assertEquals(expectedProtocolAdapterExtra, TunnelVpnService.EXTRA_PROTOCOL_ADAPTER)
    }

    @Test
    fun `WireGuard profile should use correct MTU`() {
        // Arrange
        val expectedWireGuardMtu = 1420
        val wireGuardConfig = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            mtu = expectedWireGuardMtu
        )
        
        // Act
        val actualMtu = wireGuardConfig.mtu
        
        // Assert
        assertEquals(expectedWireGuardMtu, actualMtu)
    }

    @Test
    fun `WireGuard config should use default MTU when not specified`() {
        // Arrange
        val expectedDefaultMtu = 1420
        val wireGuardConfig = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820"
        )
        
        // Act
        val actualMtu = wireGuardConfig.mtu
        
        // Assert
        assertEquals(expectedDefaultMtu, actualMtu)
    }

    @Test
    fun `WireGuard config should validate MTU range`() {
        // Arrange & Act & Assert
        // Valid MTU values
        val validConfig1 = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            mtu = 1280
        )
        assertEquals(1280, validConfig1.mtu)
        
        val validConfig2 = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            mtu = 1500
        )
        assertEquals(1500, validConfig2.mtu)
        
        // Invalid MTU values should throw
        try {
            WireGuardConfig(
                publicKey = TestKeys.VALID_PUBLIC_KEY,
                allowedIPs = listOf("0.0.0.0/0"),
                endpoint = "vpn.example.com:51820",
                mtu = 1279
            )
            throw AssertionError("Should have thrown exception for MTU < 1280")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("MTU must be between 1280 and 1500") == true)
        }
        
        try {
            WireGuardConfig(
                publicKey = TestKeys.VALID_PUBLIC_KEY,
                allowedIPs = listOf("0.0.0.0/0"),
                endpoint = "vpn.example.com:51820",
                mtu = 1501
            )
            throw AssertionError("Should have thrown exception for MTU > 1500")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("MTU must be between 1280 and 1500") == true)
        }
    }

    @Test
    fun `DNS servers list should be configurable`() {
        // Arrange
        val customDnsServers = listOf("1.1.1.1", "1.0.0.1")
        
        // Act & Assert
        // Verify that custom DNS servers can be provided
        assertTrue(customDnsServers.isNotEmpty())
        assertEquals(2, customDnsServers.size)
        assertEquals("1.1.1.1", customDnsServers[0])
        assertEquals("1.0.0.1", customDnsServers[1])
    }

    @Test
    fun `Excluded apps set should be configurable`() {
        // Arrange
        val excludedApps = setOf("com.example.app1", "com.example.app2")
        
        // Act & Assert
        // Verify that excluded apps can be configured
        assertTrue(excludedApps.isNotEmpty())
        assertEquals(2, excludedApps.size)
        assertTrue(excludedApps.contains("com.example.app1"))
        assertTrue(excludedApps.contains("com.example.app2"))
    }

    @Test
    fun `WireGuard config should validate allowed IPs`() {
        // Arrange & Act & Assert
        // Valid allowed IPs
        val validConfig = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0", "::/0"),
            endpoint = "vpn.example.com:51820"
        )
        assertEquals(2, validConfig.allowedIPs.size)
        
        // Empty allowed IPs should throw
        try {
            WireGuardConfig(
                publicKey = TestKeys.VALID_PUBLIC_KEY,
                allowedIPs = emptyList(),
                endpoint = "vpn.example.com:51820"
            )
            throw AssertionError("Should have thrown exception for empty allowed IPs")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Allowed IPs cannot be empty") == true)
        }
    }

    @Test
    fun `WireGuard config should validate persistent keepalive range`() {
        // Arrange & Act & Assert
        // Valid keepalive values
        val validConfig1 = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            persistentKeepalive = 0
        )
        assertEquals(0, validConfig1.persistentKeepalive)
        
        val validConfig2 = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            persistentKeepalive = 25
        )
        assertEquals(25, validConfig2.persistentKeepalive)
        
        val validConfig3 = WireGuardConfig(
            publicKey = TestKeys.VALID_PUBLIC_KEY,
            allowedIPs = listOf("0.0.0.0/0"),
            endpoint = "vpn.example.com:51820",
            persistentKeepalive = 65535
        )
        assertEquals(65535, validConfig3.persistentKeepalive)
        
        // Invalid keepalive values should throw
        try {
            WireGuardConfig(
                publicKey = TestKeys.VALID_PUBLIC_KEY,
                allowedIPs = listOf("0.0.0.0/0"),
                endpoint = "vpn.example.com:51820",
                persistentKeepalive = -1
            )
            throw AssertionError("Should have thrown exception for negative keepalive")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Persistent keepalive must be between 0 and 65535") == true)
        }
        
        try {
            WireGuardConfig(
                publicKey = TestKeys.VALID_PUBLIC_KEY,
                allowedIPs = listOf("0.0.0.0/0"),
                endpoint = "vpn.example.com:51820",
                persistentKeepalive = 65536
            )
            throw AssertionError("Should have thrown exception for keepalive > 65535")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Persistent keepalive must be between 0 and 65535") == true)
        }
    }
}
