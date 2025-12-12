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
     * - VLESS URI format (starts with vless://)
     * 
     * @param configText The configuration text to import
     * @param profileName Optional name for the profile (not used for VLESS)
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
                        vlessConfig = parsed
                    )
                }
            }
            
            // Unknown format
            else -> {
                Result.failure(
                    ConfigParseException(
                        "Unable to detect configuration format. " +
                        "Expected VLESS URI format (starting with vless://)"
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
    val vlessConfig: ParsedVlessConfig
) {
    init {
        require(protocol == Protocol.VLESS) {
            "Only VLESS protocol is supported"
        }
    }
}
