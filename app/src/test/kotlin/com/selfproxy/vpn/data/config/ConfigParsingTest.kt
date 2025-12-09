package com.selfproxy.vpn.data.config

import com.selfproxy.vpn.TestKeys
import com.selfproxy.vpn.data.model.FlowControl
import com.selfproxy.vpn.data.model.TransportProtocol
import com.selfproxy.vpn.domain.model.Protocol
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for configuration parsing.
 * Tests WireGuard INI parsing, VLESS URI parsing, and invalid format handling.
 * 
 * **Validates: Requirements 16.10**
 */
class ConfigParsingTest {
    
    @Before
    fun setup() {
        TestKeys.mockAndroidBase64()
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    // WireGuard INI Parsing Tests
    
    @Test
    fun `parse valid WireGuard configuration`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            Address = 10.0.0.2/24
            DNS = 8.8.8.8
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0, ::/0
            PersistentKeepalive = 25
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.hostname shouldBe "vpn.example.com"
        parsed.port shouldBe 51820
        parsed.config.publicKey shouldBe "HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw="
        parsed.config.endpoint shouldBe "vpn.example.com:51820"
        parsed.config.allowedIPs shouldBe listOf("0.0.0.0/0", "::/0")
        parsed.config.persistentKeepalive shouldBe 25
        parsed.privateKey shouldBe "YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE="
    }
    
    @Test
    fun `parse WireGuard configuration with preshared key`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            Address = 10.0.0.2/24
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            PresharedKey = FpCyhws9cxwWoV4xELtfJvjJN+zQVRPISllRWgeopVE=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.presharedKey shouldBe "FpCyhws9cxwWoV4xELtfJvjJN+zQVRPISllRWgeopVE="
    }
    
    @Test
    fun `parse WireGuard configuration with custom MTU`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            Address = 10.0.0.2/24
            MTU = 1380
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.config.mtu shouldBe 1380
    }
    
    @Test
    fun `fail to parse WireGuard configuration with missing private key`() {
        val config = """
            [Interface]
            Address = 10.0.0.2/24
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "PrivateKey"
    }
    
    @Test
    fun `fail to parse WireGuard configuration with missing public key`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            Address = 10.0.0.2/24
            
            [Peer]
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "PublicKey"
    }
    
    @Test
    fun `fail to parse WireGuard configuration with missing endpoint`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            Address = 10.0.0.2/24
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "Endpoint"
    }
    
    @Test
    fun `fail to parse WireGuard configuration with invalid key format`() {
        val config = """
            [Interface]
            PrivateKey = invalid-key
            Address = 10.0.0.2/24
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "PrivateKey"
    }
    
    @Test
    fun `fail to parse WireGuard configuration with invalid endpoint format`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            Address = 10.0.0.2/24
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            Endpoint = invalid-endpoint
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = WireGuardConfigParser.parse(config)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "Endpoint"
    }
    
    // VLESS URI Parsing Tests
    
    @Test
    fun `parse valid VLESS URI with TCP transport`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=tcp&security=none#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.name shouldBe "MyServer"
        parsed.hostname shouldBe "example.com"
        parsed.port shouldBe 443
        parsed.uuid shouldBe "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        parsed.config.transport shouldBe TransportProtocol.TCP
        parsed.config.flowControl shouldBe FlowControl.NONE
        parsed.config.tlsSettings shouldBe null
    }
    
    @Test
    fun `parse valid VLESS URI with WebSocket transport`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=ws&path=/ws&security=tls&sni=example.com#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.config.transport shouldBe TransportProtocol.WEBSOCKET
        parsed.config.websocketSettings shouldNotBe null
        parsed.config.websocketSettings?.path shouldBe "/ws"
        parsed.config.tlsSettings shouldNotBe null
        parsed.config.tlsSettings?.serverName shouldBe "example.com"
    }
    
    @Test
    fun `parse valid VLESS URI with gRPC transport`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=grpc&serviceName=myservice&security=tls&sni=example.com#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.config.transport shouldBe TransportProtocol.GRPC
        parsed.config.grpcSettings shouldNotBe null
        parsed.config.grpcSettings?.serviceName shouldBe "myservice"
    }
    
    @Test
    fun `parse valid VLESS URI with HTTP2 transport`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=h2&path=/http&security=tls&sni=example.com#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.config.transport shouldBe TransportProtocol.HTTP2
        parsed.config.http2Settings shouldNotBe null
        parsed.config.http2Settings?.path shouldBe "/http"
    }
    
    @Test
    fun `parse VLESS URI with XTLS flow control`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=tcp&flow=xtls-rprx-vision&security=tls&sni=example.com#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isSuccess shouldBe true
        val parsed = result.getOrNull()!!
        parsed.config.flowControl shouldBe FlowControl.XTLS_RPRX_VISION
    }
    
    @Test
    fun `fail to parse VLESS URI with invalid scheme`() {
        val uri = "http://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443"
        
        val result = VlessUriParser.parse(uri)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "vless://"
    }
    
    @Test
    fun `fail to parse VLESS URI with invalid UUID`() {
        val uri = "vless://invalid-uuid@example.com:443?type=tcp&security=none#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "UUID"
    }
    
    @Test
    fun `fail to parse VLESS URI with missing hostname`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@:443?type=tcp&security=none#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "hostname"
    }
    
    @Test
    fun `fail to parse VLESS URI with missing port`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com?type=tcp&security=none#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "port"
    }
    
    @Test
    fun `fail to parse VLESS URI with unsupported transport`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=quic&security=none#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "transport"
    }
    
    @Test
    fun `fail to parse VLESS URI with gRPC transport missing serviceName`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=grpc&security=none#MyServer"
        
        val result = VlessUriParser.parse(uri)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "serviceName"
    }
    
    // Configuration Importer Tests
    
    @Test
    fun `detect WireGuard protocol from configuration`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val protocol = ConfigurationImporter.detectProtocol(config)
        
        protocol shouldBe Protocol.WIREGUARD
    }
    
    @Test
    fun `detect VLESS protocol from URI`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=tcp&security=none#MyServer"
        
        val protocol = ConfigurationImporter.detectProtocol(uri)
        
        protocol shouldBe Protocol.VLESS
    }
    
    @Test
    fun `fail to detect protocol from invalid configuration`() {
        val config = "This is just random text"
        
        val protocol = ConfigurationImporter.detectProtocol(config)
        
        protocol shouldBe null
    }
    
    @Test
    fun `import WireGuard configuration`() {
        val config = """
            [Interface]
            PrivateKey = YAnz5TF+lXXJte14tji3zlMNftqL1kmNVWmWjeiJJRE=
            Address = 10.0.0.2/24
            
            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        
        val result = ConfigurationImporter.import(config)
        
        result.isSuccess shouldBe true
        val imported = result.getOrNull()!!
        imported.protocol shouldBe Protocol.WIREGUARD
        imported.wireGuardConfig shouldNotBe null
        imported.vlessConfig shouldBe null
    }
    
    @Test
    fun `import VLESS URI`() {
        val uri = "vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=tcp&security=none#MyServer"
        
        val result = ConfigurationImporter.import(uri)
        
        result.isSuccess shouldBe true
        val imported = result.getOrNull()!!
        imported.protocol shouldBe Protocol.VLESS
        imported.vlessConfig shouldNotBe null
        imported.wireGuardConfig shouldBe null
    }
    
    @Test
    fun `fail to import invalid configuration`() {
        val config = "This is just random text"
        
        val result = ConfigurationImporter.import(config)
        
        result.isFailure shouldBe true
        result.exceptionOrNull()?.message shouldContain "detect"
    }
}
