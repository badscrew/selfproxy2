package com.selfproxy.vpn.data.model

import com.selfproxy.vpn.domain.model.Protocol
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for data models.
 */
class DataModelsTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    @Test
    fun `WireGuardConfig validates public key`() {
        assertFailsWith<IllegalArgumentException> {
            WireGuardConfig(
                publicKey = "",
                endpoint = "vpn.example.com:51820"
            )
        }
    }
    
    @Test
    fun `WireGuardConfig validates persistent keepalive range`() {
        assertFailsWith<IllegalArgumentException> {
            WireGuardConfig(
                publicKey = "test-key",
                endpoint = "vpn.example.com:51820",
                persistentKeepalive = 70000 // Too large
            )
        }
    }
    
    @Test
    fun `WireGuardConfig validates MTU range`() {
        assertFailsWith<IllegalArgumentException> {
            WireGuardConfig(
                publicKey = "test-key",
                endpoint = "vpn.example.com:51820",
                mtu = 2000 // Too large
            )
        }
    }
    
    @Test
    fun `WireGuardConfig serialization works`() {
        val config = WireGuardConfig(
            publicKey = "test-public-key",
            allowedIPs = listOf("0.0.0.0/0", "::/0"),
            persistentKeepalive = 25,
            endpoint = "vpn.example.com:51820",
            mtu = 1420
        )
        
        val jsonString = json.encodeToString(config)
        val decoded = json.decodeFromString<WireGuardConfig>(jsonString)
        
        assertEquals(config, decoded)
    }
    
    @Test
    fun `VlessConfig requires WebSocket settings for WebSocket transport`() {
        assertFailsWith<IllegalArgumentException> {
            VlessConfig(
                transport = TransportProtocol.WEBSOCKET,
                websocketSettings = null // Missing required settings
            )
        }
    }
    
    @Test
    fun `VlessConfig requires gRPC settings for gRPC transport`() {
        assertFailsWith<IllegalArgumentException> {
            VlessConfig(
                transport = TransportProtocol.GRPC,
                grpcSettings = null // Missing required settings
            )
        }
    }
    
    @Test
    fun `VlessConfig serialization works`() {
        val config = VlessConfig(
            flowControl = FlowControl.XTLS_RPRX_VISION,
            transport = TransportProtocol.WEBSOCKET,
            tlsSettings = TlsSettings(
                serverName = "example.com",
                alpn = listOf("h2", "http/1.1")
            ),
            websocketSettings = WebSocketSettings(
                path = "/ws",
                headers = mapOf("Host" to "example.com")
            )
        )
        
        val jsonString = json.encodeToString(config)
        val decoded = json.decodeFromString<VlessConfig>(jsonString)
        
        assertEquals(config, decoded)
    }
    
    @Test
    fun `ServerProfile validates name`() {
        assertFailsWith<IllegalArgumentException> {
            ServerProfile(
                name = "",
                protocol = Protocol.WIREGUARD,
                hostname = "vpn.example.com",
                port = 51820,
                wireGuardConfigJson = "{}"
            )
        }
    }
    
    @Test
    fun `ServerProfile validates port range`() {
        assertFailsWith<IllegalArgumentException> {
            ServerProfile(
                name = "Test",
                protocol = Protocol.WIREGUARD,
                hostname = "vpn.example.com",
                port = 70000, // Too large
                wireGuardConfigJson = "{}"
            )
        }
    }
    
    @Test
    fun `ServerProfile validates WireGuard config presence`() {
        assertFailsWith<IllegalArgumentException> {
            ServerProfile(
                name = "Test",
                protocol = Protocol.WIREGUARD,
                hostname = "vpn.example.com",
                port = 51820,
                wireGuardConfigJson = null // Missing required config
            )
        }
    }
    
    @Test
    fun `ServerProfile validates VLESS config presence`() {
        assertFailsWith<IllegalArgumentException> {
            ServerProfile(
                name = "Test",
                protocol = Protocol.VLESS,
                hostname = "vpn.example.com",
                port = 443,
                vlessConfigJson = null // Missing required config
            )
        }
    }
    
    @Test
    fun `ServerProfile createWireGuardProfile works`() {
        val config = WireGuardConfig(
            publicKey = "test-public-key",
            endpoint = "vpn.example.com:51820"
        )
        
        val profile = ServerProfile.createWireGuardProfile(
            name = "My WireGuard Server",
            hostname = "vpn.example.com",
            port = 51820,
            config = config
        )
        
        assertEquals("My WireGuard Server", profile.name)
        assertEquals(Protocol.WIREGUARD, profile.protocol)
        assertEquals("vpn.example.com", profile.hostname)
        assertEquals(51820, profile.port)
        assertNotNull(profile.wireGuardConfigJson)
        
        val retrievedConfig = profile.getWireGuardConfig()
        assertEquals(config, retrievedConfig)
    }
    
    @Test
    fun `ServerProfile createVlessProfile works`() {
        val config = VlessConfig(
            transport = TransportProtocol.TCP
        )
        
        val profile = ServerProfile.createVlessProfile(
            name = "My VLESS Server",
            hostname = "vpn.example.com",
            port = 443,
            config = config
        )
        
        assertEquals("My VLESS Server", profile.name)
        assertEquals(Protocol.VLESS, profile.protocol)
        assertEquals("vpn.example.com", profile.hostname)
        assertEquals(443, profile.port)
        assertNotNull(profile.vlessConfigJson)
        
        val retrievedConfig = profile.getVlessConfig()
        assertEquals(config, retrievedConfig)
    }
    
    @Test
    fun `ServerProfile getWireGuardConfig throws for VLESS profile`() {
        val config = VlessConfig(transport = TransportProtocol.TCP)
        val profile = ServerProfile.createVlessProfile(
            name = "Test",
            hostname = "example.com",
            port = 443,
            config = config
        )
        
        assertFailsWith<IllegalStateException> {
            profile.getWireGuardConfig()
        }
    }
    
    @Test
    fun `ServerProfile getVlessConfig throws for WireGuard profile`() {
        val config = WireGuardConfig(
            publicKey = "test-key",
            endpoint = "example.com:51820"
        )
        val profile = ServerProfile.createWireGuardProfile(
            name = "Test",
            hostname = "example.com",
            port = 51820,
            config = config
        )
        
        assertFailsWith<IllegalStateException> {
            profile.getVlessConfig()
        }
    }
}
