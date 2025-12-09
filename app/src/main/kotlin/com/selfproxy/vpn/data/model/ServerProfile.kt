package com.selfproxy.vpn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selfproxy.vpn.domain.model.Protocol
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Server profile entity stored in the database.
 * 
 * Represents a VPN server configuration that can use either WireGuard or VLESS protocol.
 * Protocol-specific configurations are stored as JSON strings in the database.
 * Sensitive credentials (private keys, UUIDs) are stored separately in the CredentialStore.
 */
@Entity(tableName = "server_profiles")
@Serializable
data class ServerProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * User-friendly name for the profile.
     */
    val name: String,
    
    /**
     * VPN protocol type (WireGuard or VLESS).
     */
    val protocol: Protocol,
    
    /**
     * Server hostname or IP address.
     */
    val hostname: String,
    
    /**
     * Server port number.
     */
    val port: Int,
    
    /**
     * Timestamp when the profile was created.
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when the profile was last used for connection.
     */
    val lastUsed: Long? = null,
    
    /**
     * WireGuard-specific configuration (stored as JSON).
     * Only populated when protocol is WIREGUARD.
     */
    val wireGuardConfigJson: String? = null,
    
    /**
     * VLESS-specific configuration (stored as JSON).
     * Only populated when protocol is VLESS.
     */
    val vlessConfigJson: String? = null
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
        require(hostname.isNotBlank()) { "Hostname cannot be blank" }
        require(port in 1..65535) { "Port must be between 1 and 65535" }
        
        // Validate that the correct config is present for the protocol
        when (protocol) {
            Protocol.WIREGUARD -> require(wireGuardConfigJson != null) {
                "WireGuard configuration required for WireGuard protocol"
            }
            Protocol.VLESS -> require(vlessConfigJson != null) {
                "VLESS configuration required for VLESS protocol"
            }
        }
    }
    
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        
        /**
         * Creates a ServerProfile with WireGuard configuration.
         */
        fun createWireGuardProfile(
            name: String,
            hostname: String,
            port: Int,
            config: WireGuardConfig,
            id: Long = 0,
            createdAt: Long = System.currentTimeMillis(),
            lastUsed: Long? = null
        ): ServerProfile {
            return ServerProfile(
                id = id,
                name = name,
                protocol = Protocol.WIREGUARD,
                hostname = hostname,
                port = port,
                createdAt = createdAt,
                lastUsed = lastUsed,
                wireGuardConfigJson = json.encodeToString(config),
                vlessConfigJson = null
            )
        }
        
        /**
         * Creates a ServerProfile with VLESS configuration.
         */
        fun createVlessProfile(
            name: String,
            hostname: String,
            port: Int,
            config: VlessConfig,
            id: Long = 0,
            createdAt: Long = System.currentTimeMillis(),
            lastUsed: Long? = null
        ): ServerProfile {
            return ServerProfile(
                id = id,
                name = name,
                protocol = Protocol.VLESS,
                hostname = hostname,
                port = port,
                createdAt = createdAt,
                lastUsed = lastUsed,
                wireGuardConfigJson = null,
                vlessConfigJson = json.encodeToString(config)
            )
        }
    }
    
    /**
     * Parses and returns the WireGuard configuration.
     * @throws IllegalStateException if protocol is not WIREGUARD
     */
    fun getWireGuardConfig(): WireGuardConfig {
        check(protocol == Protocol.WIREGUARD) { "Profile is not a WireGuard profile" }
        return json.decodeFromString(wireGuardConfigJson!!)
    }
    
    /**
     * Parses and returns the VLESS configuration.
     * @throws IllegalStateException if protocol is not VLESS
     */
    fun getVlessConfig(): VlessConfig {
        check(protocol == Protocol.VLESS) { "Profile is not a VLESS profile" }
        return json.decodeFromString(vlessConfigJson!!)
    }
}
