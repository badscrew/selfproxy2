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
 * Represents a VPN server configuration using VLESS protocol.
 * Protocol-specific configurations are stored as JSON strings in the database.
 * Sensitive credentials (UUIDs) are stored separately in the CredentialStore.
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
     * VPN protocol type (VLESS only).
     */
    val protocol: Protocol = Protocol.VLESS,
    
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
     * VLESS-specific configuration (stored as JSON).
     */
    val vlessConfigJson: String
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
        require(hostname.isNotBlank()) { "Hostname cannot be blank" }
        require(port in 1..65535) { "Port must be between 1 and 65535" }
        require(protocol == Protocol.VLESS) { "Only VLESS protocol is supported" }
    }
    
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true
            encodeDefaults = true
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
                vlessConfigJson = json.encodeToString(config)
            )
        }
    }
    
    /**
     * Parses and returns the VLESS configuration.
     */
    fun getVlessConfig(): VlessConfig {
        return json.decodeFromString(vlessConfigJson)
    }
}
