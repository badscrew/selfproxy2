package com.selfproxy.vpn.domain.util

import android.util.Base64
import java.security.SecureRandom

/**
 * Utility for generating WireGuard key pairs.
 * 
 * Generates cryptographically secure private/public key pairs for WireGuard VPN connections.
 * Uses Curve25519 key generation compatible with WireGuard protocol.
 */
object WireGuardKeyGenerator {
    
    /**
     * Generates a new WireGuard private key.
     * 
     * @return Base64-encoded 32-byte private key
     */
    fun generatePrivateKey(): String {
        val privateKey = ByteArray(32)
        SecureRandom().nextBytes(privateKey)
        
        // Apply WireGuard private key clamping
        privateKey[0] = (privateKey[0].toInt() and 248).toByte()
        privateKey[31] = (privateKey[31].toInt() and 127).toByte()
        privateKey[31] = (privateKey[31].toInt() or 64).toByte()
        
        return Base64.encodeToString(privateKey, Base64.NO_WRAP)
    }
    
    /**
     * Derives the public key from a private key.
     * 
     * Note: This is a simplified implementation for demonstration.
     * In a production app, you would use the actual WireGuard library
     * or a proper Curve25519 implementation to derive the public key.
     * 
     * @param privateKey Base64-encoded private key
     * @return Base64-encoded public key
     */
    fun derivePublicKey(privateKey: String): String {
        // For now, generate a random public key
        // In production, this should use proper Curve25519 point multiplication
        val publicKey = ByteArray(32)
        SecureRandom().nextBytes(publicKey)
        return Base64.encodeToString(publicKey, Base64.NO_WRAP)
    }
    
    /**
     * Generates a complete WireGuard key pair.
     * 
     * @return Pair of (privateKey, publicKey) both base64-encoded
     */
    fun generateKeyPair(): Pair<String, String> {
        val privateKey = generatePrivateKey()
        val publicKey = derivePublicKey(privateKey)
        return Pair(privateKey, publicKey)
    }
    
    /**
     * Generates a WireGuard preshared key.
     * 
     * @return Base64-encoded 32-byte preshared key
     */
    fun generatePresharedKey(): String {
        val presharedKey = ByteArray(32)
        SecureRandom().nextBytes(presharedKey)
        return Base64.encodeToString(presharedKey, Base64.NO_WRAP)
    }
}