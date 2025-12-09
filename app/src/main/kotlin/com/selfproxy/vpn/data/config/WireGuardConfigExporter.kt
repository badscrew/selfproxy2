package com.selfproxy.vpn.data.config

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.WireGuardConfig

/**
 * Exporter for WireGuard configuration files (INI format).
 * 
 * Generates standard WireGuard configuration files from ServerProfile objects.
 */
object WireGuardConfigExporter {
    
    /**
     * Exports a WireGuard profile to INI format configuration string.
     * 
     * @param profile The ServerProfile with WireGuard configuration
     * @param privateKey The private key for the client
     * @param presharedKey Optional preshared key
     * @param clientAddress The client's VPN IP address (e.g., "10.0.0.2/24")
     * @param dnsServers List of DNS servers to use (defaults to Google DNS)
     * @return The WireGuard configuration as an INI-formatted string
     */
    fun export(
        profile: ServerProfile,
        privateKey: String,
        presharedKey: String? = null,
        clientAddress: String = "10.0.0.2/24",
        dnsServers: List<String> = listOf("8.8.8.8", "8.8.4.4")
    ): String {
        require(profile.protocol == com.selfproxy.vpn.domain.model.Protocol.WIREGUARD) {
            "Profile must be a WireGuard profile"
        }
        
        val config = profile.getWireGuardConfig()
        
        return buildString {
            // [Interface] section
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = $clientAddress")
            if (dnsServers.isNotEmpty()) {
                appendLine("DNS = ${dnsServers.joinToString(", ")}")
            }
            if (config.mtu != 1420) {
                appendLine("MTU = ${config.mtu}")
            }
            appendLine()
            
            // [Peer] section
            appendLine("[Peer]")
            appendLine("PublicKey = ${config.publicKey}")
            if (presharedKey != null) {
                appendLine("PresharedKey = $presharedKey")
            }
            appendLine("Endpoint = ${config.endpoint}")
            appendLine("AllowedIPs = ${config.allowedIPs.joinToString(", ")}")
            if (config.persistentKeepalive != null && config.persistentKeepalive > 0) {
                appendLine("PersistentKeepalive = ${config.persistentKeepalive}")
            }
        }
    }
    
    /**
     * Exports a WireGuard profile to a QR code-compatible format.
     * This is the same as the INI format but optimized for QR code encoding.
     * 
     * @param profile The ServerProfile with WireGuard configuration
     * @param privateKey The private key for the client
     * @param presharedKey Optional preshared key
     * @param clientAddress The client's VPN IP address
     * @param dnsServers List of DNS servers to use
     * @return The WireGuard configuration string suitable for QR code encoding
     */
    fun exportForQrCode(
        profile: ServerProfile,
        privateKey: String,
        presharedKey: String? = null,
        clientAddress: String = "10.0.0.2/24",
        dnsServers: List<String> = listOf("8.8.8.8", "8.8.4.4")
    ): String {
        // QR code format is the same as INI format
        return export(profile, privateKey, presharedKey, clientAddress, dnsServers)
    }
}
