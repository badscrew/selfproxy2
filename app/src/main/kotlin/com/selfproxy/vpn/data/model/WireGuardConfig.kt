package com.selfproxy.vpn.data.model

import kotlinx.serialization.Serializable

/**
 * WireGuard protocol configuration.
 * 
 * Contains all WireGuard-specific settings for a server profile.
 * Credentials (private key, preshared key) are stored separately in the CredentialStore.
 */
@Serializable
data class WireGuardConfig(
    /**
     * Server's public key (base64-encoded 32-byte Curve25519 key).
     */
    val publicKey: String,
    
    /**
     * IP ranges that should be routed through the WireGuard tunnel.
     * Default: ["0.0.0.0/0", "::/0"] for full tunnel routing.
     */
    val allowedIPs: List<String> = listOf("0.0.0.0/0", "::/0"),
    
    /**
     * Interval for sending keepalive packets through NAT (in seconds).
     * 0 to disable, 1-65535 to enable.
     * Recommended: 25 seconds for NAT traversal, 0 if not needed.
     */
    val persistentKeepalive: Int? = null,
    
    /**
     * Server endpoint (hostname:port).
     * Example: "vpn.example.com:51820"
     */
    val endpoint: String,
    
    /**
     * MTU (Maximum Transmission Unit) for the WireGuard interface.
     * Default: 1420 (standard for WireGuard)
     * Range: 1280-1500 bytes
     */
    val mtu: Int = 1420
) {
    init {
        require(publicKey.isNotBlank()) { "Public key cannot be blank" }
        require(allowedIPs.isNotEmpty()) { "Allowed IPs cannot be empty" }
        require(persistentKeepalive == null || persistentKeepalive in 0..65535) {
            "Persistent keepalive must be between 0 and 65535 seconds"
        }
        require(endpoint.isNotBlank()) { "Endpoint cannot be blank" }
        require(mtu in 1280..1500) { "MTU must be between 1280 and 1500 bytes" }
    }
}
