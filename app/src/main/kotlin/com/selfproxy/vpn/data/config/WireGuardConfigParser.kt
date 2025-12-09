package com.selfproxy.vpn.data.config

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.WireGuardConfig

/**
 * Parser for WireGuard configuration files (INI format).
 * 
 * Parses standard WireGuard configuration files and creates ServerProfile objects.
 * Supports both client and server configuration formats.
 */
object WireGuardConfigParser {
    
    /**
     * Parses a WireGuard configuration string in INI format.
     * 
     * Expected format:
     * ```
     * [Interface]
     * PrivateKey = <base64-private-key>
     * Address = <ip-address>
     * DNS = <dns-server>
     * MTU = <mtu-value>
     * 
     * [Peer]
     * PublicKey = <base64-public-key>
     * PresharedKey = <base64-preshared-key> (optional)
     * Endpoint = <hostname:port>
     * AllowedIPs = <ip-ranges>
     * PersistentKeepalive = <seconds>
     * ```
     * 
     * @param configText The WireGuard configuration text
     * @param profileName Optional name for the profile (defaults to hostname from endpoint)
     * @return Result containing ParsedWireGuardConfig on success or error on failure
     */
    fun parse(configText: String, profileName: String? = null): Result<ParsedWireGuardConfig> {
        return try {
            val lines = configText.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
            
            var currentSection = ""
            val interfaceData = mutableMapOf<String, String>()
            val peerData = mutableMapOf<String, String>()
            
            for (line in lines) {
                when {
                    line.startsWith("[") && line.endsWith("]") -> {
                        currentSection = line.substring(1, line.length - 1).lowercase()
                    }
                    line.contains("=") -> {
                        val (key, value) = line.split("=", limit = 2)
                            .map { it.trim() }
                        
                        when (currentSection) {
                            "interface" -> interfaceData[key.lowercase()] = value
                            "peer" -> peerData[key.lowercase()] = value
                        }
                    }
                }
            }
            
            // Validate required fields
            val privateKey = interfaceData["privatekey"]
                ?: return Result.failure(ConfigParseException("Missing PrivateKey in [Interface] section"))
            
            val publicKey = peerData["publickey"]
                ?: return Result.failure(ConfigParseException("Missing PublicKey in [Peer] section"))
            
            val endpoint = peerData["endpoint"]
                ?: return Result.failure(ConfigParseException("Missing Endpoint in [Peer] section"))
            
            // Parse endpoint (hostname:port)
            val endpointParts = endpoint.split(":")
            if (endpointParts.size != 2) {
                return Result.failure(ConfigParseException("Invalid Endpoint format. Expected hostname:port"))
            }
            
            val hostname = endpointParts[0]
            val port = endpointParts[1].toIntOrNull()
                ?: return Result.failure(ConfigParseException("Invalid port in Endpoint"))
            
            if (port !in 1..65535) {
                return Result.failure(ConfigParseException("Port must be between 1 and 65535"))
            }
            
            // Parse optional fields
            val allowedIPs = peerData["allowedips"]
                ?.split(",")
                ?.map { it.trim() }
                ?: listOf("0.0.0.0/0", "::/0")
            
            val persistentKeepalive = peerData["persistentkeepalive"]
                ?.toIntOrNull()
            
            val mtu = interfaceData["mtu"]
                ?.toIntOrNull()
                ?: 1420
            
            val presharedKey = peerData["presharedkey"]
            
            // Validate keys format (base64, 44 characters for 32-byte keys)
            if (!isValidBase64Key(privateKey)) {
                return Result.failure(ConfigParseException("Invalid PrivateKey format. Expected base64-encoded 32-byte key"))
            }
            
            if (!isValidBase64Key(publicKey)) {
                return Result.failure(ConfigParseException("Invalid PublicKey format. Expected base64-encoded 32-byte key"))
            }
            
            if (presharedKey != null && !isValidBase64Key(presharedKey)) {
                return Result.failure(ConfigParseException("Invalid PresharedKey format. Expected base64-encoded 32-byte key"))
            }
            
            val config = WireGuardConfig(
                publicKey = publicKey,
                allowedIPs = allowedIPs,
                persistentKeepalive = persistentKeepalive,
                endpoint = endpoint,
                mtu = mtu
            )
            
            val name = profileName ?: hostname
            
            Result.success(
                ParsedWireGuardConfig(
                    name = name,
                    hostname = hostname,
                    port = port,
                    config = config,
                    privateKey = privateKey,
                    presharedKey = presharedKey
                )
            )
        } catch (e: Exception) {
            Result.failure(ConfigParseException("Failed to parse WireGuard configuration: ${e.message}", e))
        }
    }
    
    /**
     * Validates if a string is a valid base64-encoded 32-byte key.
     * WireGuard keys are 32 bytes, which encode to 44 base64 characters (with padding).
     */
    private fun isValidBase64Key(key: String): Boolean {
        // WireGuard keys are 32 bytes = 44 base64 characters (including padding)
        if (key.length != 44) return false
        
        // Check if it's valid base64
        val base64Regex = "^[A-Za-z0-9+/]+=*$".toRegex()
        return base64Regex.matches(key)
    }
}

/**
 * Result of parsing a WireGuard configuration.
 * Contains all data needed to create a ServerProfile and store credentials.
 */
data class ParsedWireGuardConfig(
    val name: String,
    val hostname: String,
    val port: Int,
    val config: WireGuardConfig,
    val privateKey: String,
    val presharedKey: String?
)

/**
 * Exception thrown when configuration parsing fails.
 */
class ConfigParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
