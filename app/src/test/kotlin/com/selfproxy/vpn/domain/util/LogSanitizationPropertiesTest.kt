package com.selfproxy.vpn.domain.util

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.*

/**
 * Property-based tests for log sanitization
 * 
 * Feature: selfproxy, Property 10: Log Sanitization
 * Validates: Requirements 2.6
 */
class LogSanitizationPropertiesTest {
    
    @Test
    fun `sanitized logs should not contain WireGuard private keys`() = runTest {
        // Feature: selfproxy, Property 10: Log Sanitization
        // Validates: Requirements 2.6
        checkAll(
            iterations = 100,
            Arb.wireGuardKey(),
            Arb.string(5..50)
        ) { key, context ->
            // Create a log message containing a WireGuard key
            val message = "Connection established with key: $key for $context"
            
            // Sanitize the message
            val sanitized = SanitizedLogger.sanitize(message)
            
            // The sanitized message should not contain the original key
            sanitized shouldNotContain key
            
            // The sanitized message should contain the redaction marker
            sanitized.contains("[REDACTED_KEY]") shouldBe true
        }
    }
    
    @Test
    fun `sanitized logs should not contain VLESS UUIDs`() = runTest {
        // Feature: selfproxy, Property 10: Log Sanitization
        // Validates: Requirements 2.6
        checkAll(
            iterations = 100,
            Arb.uuid(),
            Arb.string(5..50)
        ) { uuid, context ->
            // Create a log message containing a UUID
            val message = "VLESS connection with UUID: $uuid for $context"
            
            // Sanitize the message
            val sanitized = SanitizedLogger.sanitize(message)
            
            // The sanitized message should not contain the original UUID
            sanitized shouldNotContain uuid.toString()
            
            // The sanitized message should contain the redaction marker
            sanitized.contains("[REDACTED_UUID]") shouldBe true
        }
    }
    
    @Test
    fun `sanitized logs should not contain preshared keys`() = runTest {
        // Feature: selfproxy, Property 10: Log Sanitization
        // Validates: Requirements 2.6
        checkAll(
            iterations = 100,
            Arb.wireGuardKey(),
            Arb.string(5..50)
        ) { psk, context ->
            // Create a log message containing a preshared key
            val message = "Using preshared_key=$psk for enhanced security in $context"
            
            // Sanitize the message
            val sanitized = SanitizedLogger.sanitize(message)
            
            // The sanitized message should not contain the original key
            sanitized shouldNotContain psk
            
            // The sanitized message should contain the redaction marker
            sanitized.contains("[REDACTED") shouldBe true
        }
    }
    
    @Test
    fun `sanitized logs should not contain labeled secrets`() = runTest {
        // Feature: selfproxy, Property 10: Log Sanitization
        // Validates: Requirements 2.6
        checkAll(
            iterations = 100,
            Arb.secretLabel(),
            Arb.alphanumeric(20..40),
            Arb.string(5..50)
        ) { label, secret, context ->
            // Create a log message with labeled secret
            val message = "$context: $label: $secret"
            
            // Sanitize the message
            val sanitized = SanitizedLogger.sanitize(message)
            
            // The sanitized message should not contain the original secret
            sanitized shouldNotContain secret
            
            // The sanitized message should contain the label but not the secret
            sanitized.contains("[REDACTED_SECRET]") shouldBe true
        }
    }
    
    @Test
    fun `sanitized logs should preserve non-sensitive data`() = runTest {
        // Feature: selfproxy, Property 10: Log Sanitization
        // Validates: Requirements 2.6
        checkAll(
            iterations = 100,
            Arb.string(10..50, Codepoint.alphanumeric()),
            Arb.int(1..65535),
            Arb.domain()
        ) { username, port, hostname ->
            // Create a log message with non-sensitive data
            val message = "Connecting to $hostname:$port as $username"
            
            // Sanitize the message
            val sanitized = SanitizedLogger.sanitize(message)
            
            // Non-sensitive data should be preserved
            sanitized.contains(hostname) shouldBe true
            sanitized.contains(port.toString()) shouldBe true
            sanitized.contains(username) shouldBe true
        }
    }
    
    @Test
    fun `sanitized logs should handle multiple sensitive values`() = runTest {
        // Feature: selfproxy, Property 10: Log Sanitization
        // Validates: Requirements 2.6
        checkAll(
            iterations = 100,
            Arb.wireGuardKey(),
            Arb.wireGuardKey(),
            Arb.uuid()
        ) { privateKey, presharedKey, uuid ->
            // Create a log message with multiple sensitive values
            val message = """
                Profile configuration:
                privateKey: $privateKey
                presharedKey: $presharedKey
                uuid: $uuid
            """.trimIndent()
            
            // Sanitize the message
            val sanitized = SanitizedLogger.sanitize(message)
            
            // None of the sensitive values should be present
            sanitized shouldNotContain privateKey
            sanitized shouldNotContain presharedKey
            sanitized shouldNotContain uuid.toString()
            
            // Redaction markers should be present
            sanitized.contains("[REDACTED") shouldBe true
        }
    }
    
    // Custom generators
    
    companion object {
        /**
         * Generate a valid WireGuard key (base64 encoded 32-byte key)
         */
        fun Arb.Companion.wireGuardKey(): Arb<String> = arbitrary {
            val random = kotlin.random.Random.Default
            val bytes = ByteArray(32) { random.nextInt(256).toByte() }
            Base64.getEncoder().encodeToString(bytes)
        }
        
        /**
         * Generate a valid UUID
         */
        fun Arb.Companion.uuid(): Arb<UUID> = arbitrary {
            UUID.randomUUID()
        }
        
        /**
         * Generate secret labels
         */
        fun Arb.Companion.secretLabel(): Arb<String> = Arb.of(
            "privateKey",
            "private_key",
            "presharedKey",
            "preshared_key",
            "secretKey",
            "secret_key",
            "password",
            "token",
            "auth"
        )
        
        /**
         * Generate domain names
         */
        fun Arb.Companion.domain(): Arb<String> = arbitrary {
            val parts = Arb.list(
                Arb.string(3..10, Codepoint.alphanumeric()),
                2..4
            ).bind()
            parts.joinToString(".") + ".com"
        }
        
        /**
         * Generate alphanumeric strings
         */
        fun Arb.Companion.alphanumeric(range: IntRange): Arb<String> = 
            Arb.string(range, Codepoint.alphanumeric())
    }
}
