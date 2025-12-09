package com.selfproxy.vpn.domain.util

import android.util.Base64
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.io.ByteArrayInputStream
import java.util.UUID

/**
 * Security validation utilities for VPN credentials and configurations.
 * 
 * Provides validation for:
 * - WireGuard key formats (base64-encoded 32-byte keys)
 * - VLESS UUID format (RFC 4122)
 * - TLS certificate validation
 */
object SecurityValidator {
    
    /**
     * Validates a WireGuard key (private key, public key, or preshared key).
     * 
     * WireGuard keys must be:
     * - Base64-encoded
     * - Exactly 32 bytes when decoded
     * - Valid Curve25519 keys
     * 
     * @param key The base64-encoded key string
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateWireGuardKey(key: String): Result<Unit> {
        if (key.isBlank()) {
            return Result.failure(IllegalArgumentException("WireGuard key cannot be blank"))
        }
        
        // Remove whitespace
        val cleanKey = key.trim()
        
        // Check if it's valid base64
        val decodedBytes = try {
            Base64.decode(cleanKey, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            return Result.failure(
                IllegalArgumentException("WireGuard key must be valid base64-encoded: ${e.message}")
            )
        }
        
        // Check if it's exactly 32 bytes (256 bits for Curve25519)
        if (decodedBytes.size != 32) {
            return Result.failure(
                IllegalArgumentException(
                    "WireGuard key must be exactly 32 bytes when decoded (got ${decodedBytes.size} bytes)"
                )
            )
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Validates a WireGuard private key.
     * 
     * @param privateKey The base64-encoded private key
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateWireGuardPrivateKey(privateKey: String): Result<Unit> {
        return validateWireGuardKey(privateKey).mapCatching {
            // Additional validation: private key should not be all zeros
            val decoded = Base64.decode(privateKey.trim(), Base64.NO_WRAP)
            if (decoded.all { it == 0.toByte() }) {
                throw IllegalArgumentException("WireGuard private key cannot be all zeros")
            }
        }
    }
    
    /**
     * Validates a WireGuard public key.
     * 
     * @param publicKey The base64-encoded public key
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateWireGuardPublicKey(publicKey: String): Result<Unit> {
        return validateWireGuardKey(publicKey).mapCatching {
            // Additional validation: public key should not be all zeros
            val decoded = Base64.decode(publicKey.trim(), Base64.NO_WRAP)
            if (decoded.all { it == 0.toByte() }) {
                throw IllegalArgumentException("WireGuard public key cannot be all zeros")
            }
        }
    }
    
    /**
     * Validates a WireGuard preshared key.
     * 
     * @param presharedKey The base64-encoded preshared key
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateWireGuardPresharedKey(presharedKey: String): Result<Unit> {
        return validateWireGuardKey(presharedKey)
    }
    
    /**
     * Validates a VLESS UUID.
     * 
     * VLESS UUIDs must conform to RFC 4122 format:
     * - 8-4-4-4-12 hexadecimal format
     * - Example: "550e8400-e29b-41d4-a716-446655440000"
     * 
     * @param uuid The UUID string
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateVlessUuid(uuid: String): Result<Unit> {
        if (uuid.isBlank()) {
            return Result.failure(IllegalArgumentException("VLESS UUID cannot be blank"))
        }
        
        // Remove whitespace
        val cleanUuid = uuid.trim()
        
        // Try to parse as UUID (validates RFC 4122 format)
        return try {
            UUID.fromString(cleanUuid)
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.failure(
                IllegalArgumentException(
                    "VLESS UUID must be in RFC 4122 format (8-4-4-4-12 hexadecimal): ${e.message}"
                )
            )
        }
    }
    
    /**
     * Validates a TLS/SSL certificate.
     * 
     * Checks:
     * - Certificate can be parsed
     * - Certificate is not expired
     * - Certificate is currently valid (not before date has passed)
     * 
     * @param certificatePem The PEM-encoded certificate string
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateTlsCertificate(certificatePem: String): Result<Unit> {
        if (certificatePem.isBlank()) {
            return Result.failure(IllegalArgumentException("Certificate cannot be blank"))
        }
        
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(
                ByteArrayInputStream(certificatePem.toByteArray())
            ) as X509Certificate
            
            // Check if certificate is currently valid
            certificate.checkValidity()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("Invalid TLS certificate: ${e.message}", e)
            )
        }
    }
    
    /**
     * Validates a TLS/SSL certificate against a specific hostname.
     * 
     * Checks:
     * - Certificate is valid (not expired)
     * - Certificate subject or SAN matches the hostname
     * 
     * @param certificatePem The PEM-encoded certificate string
     * @param hostname The hostname to validate against
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateTlsCertificateForHostname(
        certificatePem: String,
        hostname: String
    ): Result<Unit> {
        return validateTlsCertificate(certificatePem).mapCatching {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(
                ByteArrayInputStream(certificatePem.toByteArray())
            ) as X509Certificate
            
            // Get subject alternative names
            val subjectAltNames = certificate.subjectAlternativeNames
            val dnsNames = subjectAltNames?.filter { it[0] == 2 }?.map { it[1] as String } ?: emptyList()
            
            // Get common name from subject
            val subjectDN = certificate.subjectX500Principal.name
            val cnPattern = "CN=([^,]+)".toRegex()
            val commonName = cnPattern.find(subjectDN)?.groupValues?.get(1)
            
            // Check if hostname matches any DNS name or common name
            val matches = dnsNames.any { matchesHostname(it, hostname) } ||
                         (commonName != null && matchesHostname(commonName, hostname))
            
            if (!matches) {
                throw IllegalArgumentException(
                    "Certificate does not match hostname '$hostname'. " +
                    "Certificate is valid for: ${dnsNames.joinToString(", ")}" +
                    (commonName?.let { " (CN: $it)" } ?: "")
                )
            }
        }
    }
    
    /**
     * Checks if a certificate DNS name matches a hostname.
     * Supports wildcard certificates (*.example.com).
     */
    private fun matchesHostname(certName: String, hostname: String): Boolean {
        if (certName == hostname) return true
        
        // Handle wildcard certificates
        if (certName.startsWith("*.")) {
            val domain = certName.substring(2)
            return hostname.endsWith(".$domain") || hostname == domain
        }
        
        return false
    }
    
    /**
     * Validates a WireGuard endpoint format.
     * 
     * Endpoint must be in format: "hostname:port" or "ip:port"
     * 
     * @param endpoint The endpoint string
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateWireGuardEndpoint(endpoint: String): Result<Unit> {
        if (endpoint.isBlank()) {
            return Result.failure(IllegalArgumentException("Endpoint cannot be blank"))
        }
        
        val parts = endpoint.split(":")
        if (parts.size != 2) {
            return Result.failure(
                IllegalArgumentException("Endpoint must be in format 'hostname:port' or 'ip:port'")
            )
        }
        
        val (host, portStr) = parts
        
        if (host.isBlank()) {
            return Result.failure(IllegalArgumentException("Hostname/IP cannot be blank"))
        }
        
        val port = portStr.toIntOrNull()
        if (port == null || port !in 1..65535) {
            return Result.failure(
                IllegalArgumentException("Port must be a number between 1 and 65535")
            )
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Validates allowed IPs format for WireGuard.
     * 
     * Each allowed IP must be in CIDR notation: "ip/prefix"
     * 
     * @param allowedIPs List of allowed IP ranges
     * @return Result.success if valid, Result.failure with error message if invalid
     */
    fun validateAllowedIPs(allowedIPs: List<String>): Result<Unit> {
        if (allowedIPs.isEmpty()) {
            return Result.failure(IllegalArgumentException("Allowed IPs cannot be empty"))
        }
        
        for (allowedIP in allowedIPs) {
            val parts = allowedIP.split("/")
            if (parts.size != 2) {
                return Result.failure(
                    IllegalArgumentException("Allowed IP must be in CIDR notation: '$allowedIP'")
                )
            }
            
            val prefix = parts[1].toIntOrNull()
            if (prefix == null) {
                return Result.failure(
                    IllegalArgumentException("Invalid prefix in allowed IP: '$allowedIP'")
                )
            }
            
            // Check if it's IPv4 or IPv6
            val isIPv6 = parts[0].contains(":")
            val maxPrefix = if (isIPv6) 128 else 32
            
            if (prefix !in 0..maxPrefix) {
                return Result.failure(
                    IllegalArgumentException(
                        "Prefix must be between 0 and $maxPrefix for ${if (isIPv6) "IPv6" else "IPv4"}: '$allowedIP'"
                    )
                )
            }
        }
        
        return Result.success(Unit)
    }
}
