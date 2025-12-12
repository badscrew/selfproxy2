package com.selfproxy.vpn.data.model

import kotlinx.serialization.Serializable

/**
 * Application settings data model.
 * 
 * Contains all user-configurable settings for the VPN application.
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5
 */
@Serializable
data class AppSettings(
    // General DNS settings (Requirement 14.2)
    val primaryDnsServer: String = "8.8.8.8",
    val secondaryDnsServer: String = "8.8.4.4",
    val customDnsEnabled: Boolean = false,
    
    // IPv6 settings (Requirement 14.3, 14.4)
    val ipv6Enabled: Boolean = true,
    
    // MTU settings (Requirement 14.5)
    val mtu: Int = 1500,
    
    // Connection settings (Requirement 14.1)
    val connectionTimeout: Int = 30, // seconds
    val keepAliveInterval: Int = 25, // seconds
    val autoReconnectEnabled: Boolean = true,
    val reconnectionMaxAttempts: Int = 5,
    
    // Connection test settings
    val connectionTestUrl: String = "http://www.google.com/generate_204",
    
    // WireGuard-specific settings (Requirement 14.6, 14.7, 14.8, 14.9, 14.10)
    val wireGuardPersistentKeepalive: Int = 25, // seconds, 0 to disable
    val wireGuardMtu: Int = 1420,
    
    // VLESS-specific settings (Requirement 14.11, 14.12, 14.13, 14.14, 14.15, 14.16)
    val vlessDefaultTransport: TransportProtocol = TransportProtocol.TCP,
    val vlessDefaultFlowControl: FlowControl = FlowControl.NONE,
    
    // Logging settings (Requirement 12.9, 12.10)
    val verboseLoggingEnabled: Boolean = false,
    val logExportEnabled: Boolean = false
)

/**
 * Settings validation result.
 */
data class SettingsValidation(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Validates application settings.
 */
fun AppSettings.validate(): SettingsValidation {
    val errors = mutableListOf<String>()
    
    // Validate DNS servers
    if (customDnsEnabled) {
        if (!isValidIpAddress(primaryDnsServer)) {
            errors.add("Primary DNS server is not a valid IP address")
        }
        if (!isValidIpAddress(secondaryDnsServer)) {
            errors.add("Secondary DNS server is not a valid IP address")
        }
    }
    
    // Validate MTU
    if (mtu < 1280 || mtu > 1500) {
        errors.add("MTU must be between 1280 and 1500")
    }
    
    // Validate WireGuard MTU
    if (wireGuardMtu < 1280 || wireGuardMtu > 1500) {
        errors.add("WireGuard MTU must be between 1280 and 1500")
    }
    
    // Validate connection timeout
    if (connectionTimeout < 5 || connectionTimeout > 120) {
        errors.add("Connection timeout must be between 5 and 120 seconds")
    }
    
    // Validate keep-alive interval
    if (keepAliveInterval < 0 || keepAliveInterval > 300) {
        errors.add("Keep-alive interval must be between 0 and 300 seconds")
    }
    
    // Validate WireGuard persistent keepalive
    if (wireGuardPersistentKeepalive < 0 || wireGuardPersistentKeepalive > 65535) {
        errors.add("WireGuard persistent keepalive must be between 0 and 65535 seconds")
    }
    
    // Validate reconnection attempts
    if (reconnectionMaxAttempts < 1 || reconnectionMaxAttempts > 20) {
        errors.add("Reconnection max attempts must be between 1 and 20")
    }
    
    // Validate connection test URL
    if (connectionTestUrl.isBlank()) {
        errors.add("Connection test URL cannot be blank")
    } else if (!connectionTestUrl.startsWith("http://") && !connectionTestUrl.startsWith("https://")) {
        errors.add("Connection test URL must start with http:// or https://")
    }
    
    return SettingsValidation(
        isValid = errors.isEmpty(),
        errors = errors
    )
}

/**
 * Simple IP address validation.
 */
private fun isValidIpAddress(ip: String): Boolean {
    val parts = ip.split(".")
    if (parts.size != 4) return false
    
    return parts.all { part ->
        part.toIntOrNull()?.let { it in 0..255 } ?: false
    }
}
