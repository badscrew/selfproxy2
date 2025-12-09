package com.selfproxy.vpn.data.config

import com.selfproxy.vpn.TestKeys
import com.selfproxy.vpn.domain.model.Protocol
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Property-based tests for configuration import and protocol detection.
 * 
 * **Feature: selfproxy, Property 7: Configuration Import Protocol Detection**
 * **Validates: Requirements 1.10**
 */
class ConfigurationImportPropertiesTest {
    
    @Before
    fun setup() {
        TestKeys.mockAndroidBase64()
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    @Test
    fun `property test - WireGuard configuration should be detected as WireGuard protocol`() = runTest {
        // Feature: selfproxy, Property 7: Configuration Import Protocol Detection
        // Validates: Requirements 1.10
        checkAll(
            iterations = 100,
            Arb.wireGuardConfig()
        ) { configText ->
            // Act
            val detectedProtocol = ConfigurationImporter.detectProtocol(configText)
            val importResult = ConfigurationImporter.import(configText)
            
            // Assert
            detectedProtocol shouldBe Protocol.WIREGUARD
            importResult.isSuccess shouldBe true
            importResult.getOrNull()?.protocol shouldBe Protocol.WIREGUARD
            importResult.getOrNull()?.wireGuardConfig shouldNotBe null
            importResult.getOrNull()?.vlessConfig shouldBe null
        }
    }
    
    @Test
    fun `property test - VLESS URI should be detected as VLESS protocol`() = runTest {
        // Feature: selfproxy, Property 7: Configuration Import Protocol Detection
        // Validates: Requirements 1.10
        checkAll(
            iterations = 100,
            Arb.vlessUri()
        ) { uri ->
            // Act
            val detectedProtocol = ConfigurationImporter.detectProtocol(uri)
            val importResult = ConfigurationImporter.import(uri)
            
            // Assert
            detectedProtocol shouldBe Protocol.VLESS
            importResult.isSuccess shouldBe true
            importResult.getOrNull()?.protocol shouldBe Protocol.VLESS
            importResult.getOrNull()?.vlessConfig shouldNotBe null
            importResult.getOrNull()?.wireGuardConfig shouldBe null
        }
    }
    
    @Test
    fun `property test - invalid configuration should fail to detect protocol`() = runTest {
        // Feature: selfproxy, Property 7: Configuration Import Protocol Detection
        // Validates: Requirements 1.10
        checkAll(
            iterations = 100,
            Arb.invalidConfig()
        ) { configText ->
            // Act
            val detectedProtocol = ConfigurationImporter.detectProtocol(configText)
            val importResult = ConfigurationImporter.import(configText)
            
            // Assert
            detectedProtocol shouldBe null
            importResult.isFailure shouldBe true
        }
    }
    
    companion object {
        /**
         * Generates valid WireGuard configuration strings.
         */
        fun Arb.Companion.wireGuardConfig(): Arb<String> = arbitrary {
            val privateKey = base64Key().bind()
            val publicKey = base64Key().bind()
            val hostname = domain().bind()
            val port = int(1..65535).bind()
            val allowedIPs = list(ipRange(), 1..3).bind().joinToString(", ")
            val persistentKeepalive = int(0..300).bind()
            val mtu = int(1280..1500).bind()
            
            buildString {
                appendLine("[Interface]")
                appendLine("PrivateKey = $privateKey")
                appendLine("Address = 10.0.0.2/24")
                appendLine("DNS = 8.8.8.8")
                appendLine("MTU = $mtu")
                appendLine()
                appendLine("[Peer]")
                appendLine("PublicKey = $publicKey")
                appendLine("Endpoint = $hostname:$port")
                appendLine("AllowedIPs = $allowedIPs")
                if (persistentKeepalive > 0) {
                    appendLine("PersistentKeepalive = $persistentKeepalive")
                }
            }
        }
        
        /**
         * Generates valid VLESS URIs.
         */
        fun Arb.Companion.vlessUri(): Arb<String> = arbitrary {
            val uuid = uuid().bind()
            val hostname = domain().bind()
            val port = int(1..65535).bind()
            val name = string(5..20, Codepoint.alphanumeric()).bind()
            val type = element("tcp", "ws", "grpc", "h2").bind()
            val security = element("none", "tls").bind()
            
            buildString {
                append("vless://$uuid@$hostname:$port")
                append("?type=$type")
                append("&security=$security")
                
                if (security == "tls") {
                    append("&sni=$hostname")
                }
                
                if (type == "ws") {
                    append("&path=/ws")
                }
                
                if (type == "grpc") {
                    append("&serviceName=grpc")
                }
                
                append("#$name")
            }
        }
        
        /**
         * Generates invalid configuration strings that should not be detected.
         */
        fun Arb.Companion.invalidConfig(): Arb<String> = arbitrary {
            val type = int(0..4).bind()
            
            when (type) {
                0 -> string(10..100, Codepoint.alphanumeric()).bind() // Random text
                1 -> "http://${domain().bind()}" // HTTP URL
                2 -> "ssh://${domain().bind()}" // SSH URL
                3 -> "{\"key\": \"value\"}" // JSON
                else -> "This is just some random text without any structure"
            }
        }
        
        /**
         * Generates valid base64-encoded 32-byte keys (44 characters).
         */
        fun Arb.Companion.base64Key(): Arb<String> = arbitrary {
            // Generate 32 random bytes
            val bytes = ByteArray(32) { byte().bind() }
            // Encode to base64 using Java's encoder (will be 44 characters with padding)
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
        
        /**
         * Generates valid domain names.
         */
        fun Arb.Companion.domain(): Arb<String> = arbitrary {
            val parts = list(string(3..10, Codepoint.alphanumeric()), 2..3).bind()
            parts.joinToString(".") + ".com"
        }
        
        /**
         * Generates valid UUIDs.
         */
        fun Arb.Companion.uuid(): Arb<String> = arbitrary {
            val hex = "0123456789abcdef"
            buildString {
                repeat(8) { append(hex.random()) }
                append('-')
                repeat(4) { append(hex.random()) }
                append('-')
                append('4') // Version 4
                repeat(3) { append(hex.random()) }
                append('-')
                append(element('8', '9', 'a', 'b').bind()) // Variant
                repeat(3) { append(hex.random()) }
                append('-')
                repeat(12) { append(hex.random()) }
            }
        }
        
        /**
         * Generates valid IP ranges (CIDR notation).
         */
        fun Arb.Companion.ipRange(): Arb<String> = arbitrary {
            val useIPv6 = boolean().bind()
            
            if (useIPv6) {
                "::/0"
            } else {
                val octet1 = int(0..255).bind()
                val octet2 = int(0..255).bind()
                val octet3 = int(0..255).bind()
                val octet4 = int(0..255).bind()
                val cidr = int(0..32).bind()
                "$octet1.$octet2.$octet3.$octet4/$cidr"
            }
        }
    }
}
