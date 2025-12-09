package com.selfproxy.vpn.data.model

import com.selfproxy.vpn.domain.model.Protocol
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Property-based tests for VLESS profile data models.
 * 
 * These tests validate universal properties that should hold across all valid inputs.
 */
class VlessProfilePropertiesTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Feature: selfproxy, Property 2: Valid VLESS Profile Acceptance
     * Validates: Requirements 1.3
     * 
     * For any valid VLESS configuration (hostname, port, valid UUID, valid Flow_Control, 
     * valid Transport_Protocol, valid TLS_Settings), the system should accept and store 
     * the profile successfully.
     */
    @Test
    fun `property 2 - valid VLESS profiles should be accepted and stored`() = runTest {
        checkAll(
            iterations = 100,
            Arb.validVlessProfile()
        ) { profile ->
            // The profile should be created successfully (no exceptions thrown)
            assertNotNull(profile)
            
            // Basic validations should pass
            assertEquals(Protocol.VLESS, profile.protocol)
            assert(profile.name.isNotBlank()) { "Profile name should not be blank" }
            assert(profile.hostname.isNotBlank()) { "Hostname should not be blank" }
            assert(profile.port in 1..65535) { "Port should be in valid range" }
            assertNotNull(profile.vlessConfigJson) { "VLESS config JSON should not be null" }
            
            // Should be able to retrieve the VLESS config
            val config = profile.getVlessConfig()
            assertNotNull(config)
            
            // Validate transport protocol
            assert(config.transport in TransportProtocol.values()) { 
                "Transport protocol should be valid" 
            }
            
            // Validate flow control
            assert(config.flowControl in FlowControl.values()) { 
                "Flow control should be valid" 
            }
            
            // Validate transport-specific settings
            when (config.transport) {
                TransportProtocol.TCP -> {
                    // TCP doesn't require additional settings
                }
                TransportProtocol.WEBSOCKET -> {
                    val wsSettings = config.websocketSettings
                    assertNotNull(wsSettings) { 
                        "WebSocket settings required for WebSocket transport" 
                    }
                    assert(wsSettings.path.isNotBlank()) { 
                        "WebSocket path should not be blank" 
                    }
                }
                TransportProtocol.GRPC -> {
                    val grpcSettings = config.grpcSettings
                    assertNotNull(grpcSettings) { 
                        "gRPC settings required for gRPC transport" 
                    }
                    assert(grpcSettings.serviceName.isNotBlank()) { 
                        "gRPC service name should not be blank" 
                    }
                }
                TransportProtocol.HTTP2 -> {
                    val http2Settings = config.http2Settings
                    assertNotNull(http2Settings) { 
                        "HTTP/2 settings required for HTTP/2 transport" 
                    }
                    assert(http2Settings.path.isNotBlank()) { 
                        "HTTP/2 path should not be blank" 
                    }
                }
            }
            
            // Validate TLS settings if present
            config.tlsSettings?.let { tls ->
                assert(tls.serverName.isNotBlank()) { "TLS server name should not be blank" }
            }
            
            // Validate Reality settings if present
            config.realitySettings?.let { reality ->
                assert(reality.serverName.isNotBlank()) { "Reality server name should not be blank" }
                assert(reality.publicKey.isNotBlank()) { "Reality public key should not be blank" }
                assert(reality.shortId.isNotBlank()) { "Reality short ID should not be blank" }
            }
            
            // Should be able to serialize and deserialize the profile
            val serialized = json.encodeToString(profile)
            val deserialized = json.decodeFromString<ServerProfile>(serialized)
            assertEquals(profile.name, deserialized.name)
            assertEquals(profile.protocol, deserialized.protocol)
            assertEquals(profile.hostname, deserialized.hostname)
            assertEquals(profile.port, deserialized.port)
            
            // Deserialized config should match original
            val deserializedConfig = deserialized.getVlessConfig()
            assertEquals(config.transport, deserializedConfig.transport)
            assertEquals(config.flowControl, deserializedConfig.flowControl)
            assertEquals(config.tlsSettings?.serverName, deserializedConfig.tlsSettings?.serverName)
            assertEquals(config.realitySettings?.serverName, deserializedConfig.realitySettings?.serverName)
        }
    }
}

/**
 * Custom Arb generators for VLESS profiles and configurations.
 */

/**
 * Generates valid UUIDs (RFC 4122 compliant).
 * Format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
 */
fun Arb.Companion.validUuid(): Arb<String> = arbitrary {
    val hex = "0123456789abcdef"
    val uuid = buildString {
        // 8 hex digits
        repeat(8) { append(hex.random()) }
        append('-')
        // 4 hex digits
        repeat(4) { append(hex.random()) }
        append('-')
        // 4xxx (version 4)
        append('4')
        repeat(3) { append(hex.random()) }
        append('-')
        // yxxx (variant bits)
        append(listOf('8', '9', 'a', 'b').random())
        repeat(3) { append(hex.random()) }
        append('-')
        // 12 hex digits
        repeat(12) { append(hex.random()) }
    }
    uuid
}

/**
 * Generates valid TLS settings.
 */
fun Arb.Companion.tlsSettings(): Arb<TlsSettings?> = arbitrary {
    val hasTls = Arb.boolean().bind()
    if (hasTls) {
        TlsSettings(
            serverName = Arb.hostname().bind(),
            alpn = if (Arb.boolean().bind()) {
                listOf("h2", "http/1.1")
            } else {
                emptyList()
            },
            allowInsecure = Arb.boolean().bind(),
            fingerprint = if (Arb.boolean().bind()) {
                // Generate a fake fingerprint (SHA256 hex)
                buildString {
                    repeat(64) {
                        append("0123456789abcdef".random())
                    }
                }
            } else {
                null
            }
        )
    } else {
        null
    }
}

/**
 * Generates valid Reality settings.
 */
fun Arb.Companion.realitySettings(): Arb<RealitySettings?> = arbitrary {
    val hasReality = Arb.boolean().bind()
    if (hasReality) {
        RealitySettings(
            serverName = Arb.hostname().bind(),
            publicKey = buildString {
                // Generate a fake public key (base64-like)
                repeat(44) {
                    append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".random())
                }
            },
            shortId = buildString {
                // Generate a short ID (hex string)
                repeat(16) {
                    append("0123456789abcdef".random())
                }
            },
            spiderX = if (Arb.boolean().bind()) {
                "/" + Arb.string(5..20, Codepoint.alphanumeric()).bind()
            } else {
                null
            },
            fingerprint = if (Arb.boolean().bind()) {
                buildString {
                    repeat(64) {
                        append("0123456789abcdef".random())
                    }
                }
            } else {
                null
            }
        )
    } else {
        null
    }
}

/**
 * Generates valid WebSocket settings.
 */
fun Arb.Companion.websocketSettings(): Arb<WebSocketSettings> = arbitrary {
    val paths = listOf("/", "/ws", "/ray", "/v2ray", "/websocket")
    WebSocketSettings(
        path = paths.random(),
        headers = if (Arb.boolean().bind()) {
            mapOf(
                "Host" to Arb.hostname().bind(),
                "User-Agent" to "Mozilla/5.0"
            )
        } else {
            emptyMap()
        }
    )
}

/**
 * Generates valid gRPC settings.
 */
fun Arb.Companion.grpcSettings(): Arb<GrpcSettings> = arbitrary {
    val serviceNames = listOf("GunService", "TunService", "VlessService")
    GrpcSettings(
        serviceName = serviceNames.random(),
        multiMode = Arb.boolean().bind()
    )
}

/**
 * Generates valid HTTP/2 settings.
 */
fun Arb.Companion.http2Settings(): Arb<Http2Settings> = arbitrary {
    val paths = listOf("/", "/http2", "/h2", "/v2ray")
    Http2Settings(
        path = paths.random(),
        host = if (Arb.boolean().bind()) {
            List(Arb.int(1..3).bind()) {
                Arb.hostname().bind()
            }
        } else {
            emptyList()
        }
    )
}

/**
 * Generates valid VLESS configurations.
 */
fun Arb.Companion.validVlessConfig(): Arb<VlessConfig> = arbitrary {
    val transport = Arb.enum<TransportProtocol>().bind()
    val flowControl = Arb.enum<FlowControl>().bind()
    
    // Generate transport-specific settings
    val websocketSettings = if (transport == TransportProtocol.WEBSOCKET) {
        Arb.websocketSettings().bind()
    } else {
        null
    }
    
    val grpcSettings = if (transport == TransportProtocol.GRPC) {
        Arb.grpcSettings().bind()
    } else {
        null
    }
    
    val http2Settings = if (transport == TransportProtocol.HTTP2) {
        Arb.http2Settings().bind()
    } else {
        null
    }
    
    VlessConfig(
        flowControl = flowControl,
        transport = transport,
        tlsSettings = Arb.tlsSettings().bind(),
        realitySettings = Arb.realitySettings().bind(),
        websocketSettings = websocketSettings,
        grpcSettings = grpcSettings,
        http2Settings = http2Settings
    )
}

/**
 * Generates valid VLESS server profiles.
 */
fun Arb.Companion.validVlessProfile(): Arb<ServerProfile> = arbitrary {
    val config = Arb.validVlessConfig().bind()
    val hostname = Arb.hostname().bind()
    val port = Arb.validPort().bind()
    
    ServerProfile.createVlessProfile(
        name = Arb.profileName().bind(),
        hostname = hostname,
        port = port,
        config = config,
        id = Arb.long(0..Long.MAX_VALUE).bind(),
        createdAt = Arb.long(0..System.currentTimeMillis()).bind(),
        lastUsed = if (Arb.boolean().bind()) Arb.long(0..System.currentTimeMillis()).bind() else null
    )
}
