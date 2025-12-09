package com.selfproxy.vpn.data.model

import com.selfproxy.vpn.TestKeys
import com.selfproxy.vpn.domain.model.Protocol
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Property-based tests for WireGuard profile data models.
 * 
 * These tests validate universal properties that should hold across all valid inputs.
 */
class WireGuardProfilePropertiesTest {
    
    @Before
    fun setup() {
        TestKeys.mockAndroidBase64()
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Feature: selfproxy, Property 1: Valid WireGuard Profile Acceptance
     * Validates: Requirements 1.2
     * 
     * For any valid WireGuard configuration (hostname, port, valid Private_Key, 
     * valid Public_Key, valid Allowed_IPs), the system should accept and store 
     * the profile successfully.
     */
    @Test
    fun `property 1 - valid WireGuard profiles should be accepted and stored`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validWireGuardProfile()
        ) { profile ->
            // The profile should be created successfully (no exceptions thrown)
            assertNotNull(profile)
            
            // Basic validations should pass
            assertEquals(Protocol.WIREGUARD, profile.protocol)
            assert(profile.name.isNotBlank()) { "Profile name should not be blank" }
            assert(profile.hostname.isNotBlank()) { "Hostname should not be blank" }
            assert(profile.port in 1..65535) { "Port should be in valid range" }
            assertNotNull(profile.wireGuardConfigJson) { "WireGuard config JSON should not be null" }
            
            // Should be able to retrieve the WireGuard config
            val config = profile.getWireGuardConfig()
            assertNotNull(config)
            assert(config.publicKey.isNotBlank()) { "Public key should not be blank" }
            assert(config.endpoint.isNotBlank()) { "Endpoint should not be blank" }
            assert(config.allowedIPs.isNotEmpty()) { "Allowed IPs should not be empty" }
            assert(config.mtu in 1280..1500) { "MTU should be in valid range" }
            
            // Persistent keepalive validation if present
            config.persistentKeepalive?.let { keepalive ->
                assert(keepalive in 0..65535) { "Persistent keepalive should be in valid range" }
            }
            
            // Should be able to serialize and deserialize the profile
            val serialized = json.encodeToString(profile)
            val deserialized = json.decodeFromString<ServerProfile>(serialized)
            assertEquals(profile.name, deserialized.name)
            assertEquals(profile.protocol, deserialized.protocol)
            assertEquals(profile.hostname, deserialized.hostname)
            assertEquals(profile.port, deserialized.port)
            
            // Deserialized config should match original
            val deserializedConfig = deserialized.getWireGuardConfig()
            assertEquals(config.publicKey, deserializedConfig.publicKey)
            assertEquals(config.allowedIPs, deserializedConfig.allowedIPs)
            assertEquals(config.persistentKeepalive, deserializedConfig.persistentKeepalive)
            assertEquals(config.endpoint, deserializedConfig.endpoint)
            assertEquals(config.mtu, deserializedConfig.mtu)
        }
    }
}

/**
 * Custom Arb generators for WireGuard profiles and configurations.
 */

/**
 * Generates valid WireGuard public keys (base64-encoded 32-byte keys).
 * For testing purposes, we generate actual valid base64-encoded 32-byte keys.
 */
fun Arb.Companion.wireGuardPublicKey(): Arb<String> = arbitrary {
    // Generate 32 random bytes
    val bytes = ByteArray(32) { byte().bind() }
    // Encode to base64 using Java's encoder (will be 44 characters with padding)
    java.util.Base64.getEncoder().encodeToString(bytes)
}

/**
 * Generates valid hostnames for testing.
 */
fun Arb.Companion.hostname(): Arb<String> = arbitrary {
    val tlds = listOf("com", "net", "org", "io", "dev")
    val labels = Arb.string(3..15, Codepoint.alphanumeric()).bind()
    val tld = tlds.random()
    "$labels.$tld"
}

/**
 * Generates valid port numbers (1-65535).
 */
fun Arb.Companion.validPort(): Arb<Int> = Arb.int(1..65535)

/**
 * Generates valid WireGuard endpoints (hostname:port format).
 */
fun Arb.Companion.wireGuardEndpoint(): Arb<String> = arbitrary {
    val host = Arb.hostname().bind()
    val port = Arb.validPort().bind()
    "$host:$port"
}

/**
 * Generates valid allowed IPs lists.
 * Can be full tunnel (0.0.0.0/0, ::/0) or specific subnets.
 */
fun Arb.Companion.allowedIPs(): Arb<List<String>> = arbitrary {
    val useFullTunnel = Arb.boolean().bind()
    if (useFullTunnel) {
        listOf("0.0.0.0/0", "::/0")
    } else {
        // Generate some specific subnets
        val count = Arb.int(1..5).bind()
        List(count) {
            val octet1 = Arb.int(1..255).bind()
            val octet2 = Arb.int(0..255).bind()
            val octet3 = Arb.int(0..255).bind()
            val octet4 = Arb.int(0..255).bind()
            val cidr = Arb.int(8..32).bind()
            "$octet1.$octet2.$octet3.$octet4/$cidr"
        }
    }
}

/**
 * Generates valid persistent keepalive values (0 to disable, or 1-65535 seconds).
 */
fun Arb.Companion.persistentKeepalive(): Arb<Int?> = arbitrary {
    val hasKeepalive = Arb.boolean().bind()
    if (hasKeepalive) {
        Arb.int(0..65535).bind()
    } else {
        null
    }
}

/**
 * Generates valid MTU values (1280-1500 bytes).
 */
fun Arb.Companion.validMtu(): Arb<Int> = Arb.int(1280..1500)

/**
 * Generates valid WireGuard configurations.
 */
fun Arb.Companion.validWireGuardConfig(): Arb<WireGuardConfig> = arbitrary {
    WireGuardConfig(
        publicKey = Arb.wireGuardPublicKey().bind(),
        allowedIPs = Arb.allowedIPs().bind(),
        persistentKeepalive = Arb.persistentKeepalive().bind(),
        endpoint = Arb.wireGuardEndpoint().bind(),
        mtu = Arb.validMtu().bind()
    )
}

/**
 * Generates valid profile names.
 */
fun Arb.Companion.profileName(): Arb<String> = arbitrary {
    val prefixes = listOf("Home", "Office", "Cloud", "VPN", "Server", "Backup")
    val suffixes = listOf("Primary", "Secondary", "Test", "Prod", "Dev")
    val prefix = prefixes.random()
    val suffix = suffixes.random()
    "$prefix $suffix ${Arb.int(1..999).bind()}"
}

/**
 * Generates valid WireGuard server profiles.
 */
fun Arb.Companion.validWireGuardProfile(): Arb<ServerProfile> = arbitrary {
    val config = Arb.validWireGuardConfig().bind()
    val hostname = Arb.hostname().bind()
    val port = Arb.validPort().bind()
    
    ServerProfile.createWireGuardProfile(
        name = Arb.profileName().bind(),
        hostname = hostname,
        port = port,
        config = config,
        id = Arb.long(0..Long.MAX_VALUE).bind(),
        createdAt = Arb.long(0..System.currentTimeMillis()).bind(),
        lastUsed = if (Arb.boolean().bind()) Arb.long(0..System.currentTimeMillis()).bind() else null
    )
}
