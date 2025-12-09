package com.selfproxy.vpn.data.config

import com.selfproxy.vpn.domain.model.Protocol

/**
 * Unified configuration importer with automatic protocol detection.
 * 
 * Detects the configuration format and delegates to the appropriate parser.
 */
object ConfigurationImporter {
    
    /**
     * Imports a configuration string and automatically detects the protocol.
     * 
     * Supports:
     * - WireGuard INI format (starts with [Interface] or [Peer])
     * - VLESS URI format (starts with vless://)
     * 
     * @param configText The configuration text to import
     * @param profileName Optional name for the profile (used for WireGuard)
     * @return Result containing ImportedConfig on success or error on failure
     */
    fun import(configText: String, profileName: String? = null): Result<ImportedConfig> {
        val trimmedText = configText.trim()
        
        return when {
            // VLESS URI format
            trimmedText.startsWith("vless://", ignoreCase = true) -> {
                VlessUriParser.parse(trimmedText).map { parsed ->
                    ImportedConfig(
                        protocol = Protocol.VLESS,
                        wireGuardConfig = null,
                        vlessConfig = parsed
                    )
                }
            }
            
            // WireGuard INI format
            trimmedText.contains("[Interface]", ignoreCase = true) ||
            trimmedText.contains("[Peer]", ignoreCase = true) -> {
                WireGuardConfigParser.parse(trimmedText, profileName).map { parsed ->
                    ImportedConfig(
                        protocol = Protocol.WIREGUARD,
                        wireGuardConfig = parsed,
                        vlessConfig = null
                    )
                }
            }
            
            // Unknown format
            else -> {
                Result.failure(
                    ConfigParseException(
                        "Unable to detect configuration format. " +
                        "Expected WireGuard INI format (with [Interface] and [Peer] sections) " +
                        "or VLESS URI format (starting with vless://)"
                    )
                )
            }
        }
    }
    
    /**
     * Detects the protocol type from a configuration string without fully parsing it.
     * 
     * @param configText The configuration text
     * @return The detected protocol or null if unable to detect
     */
    fun detectProtocol(configText: String): Protocol? {
        val trimmedText = configText.trim()
        
        return when {
            trimmedText.startsWith("vless://", ignoreCase = true) -> Protocol.VLESS
            trimmedText.contains("[Interface]", ignoreCase = true) ||
            trimmedText.contains("[Peer]", ignoreCase = true) -> Protocol.WIREGUARD
            else -> null
        }
    }
}

/**
 * Result of importing a configuration.
 * Contains the detected protocol and the parsed configuration data.
 */
data class ImportedConfig(
    val protocol: Protocol,
    val wireGuardConfig: ParsedWireGuardConfig?,
    val vlessConfig: ParsedVlessConfig?
) {
    init {
        when (protocol) {
            Protocol.WIREGUARD -> require(wireGuardConfig != null) {
                "WireGuard configuration required for WireGuard protocol"
            }
            Protocol.VLESS -> require(vlessConfig != null) {
                "VLESS configuration required for VLESS protocol"
            }
        }
    }
}
