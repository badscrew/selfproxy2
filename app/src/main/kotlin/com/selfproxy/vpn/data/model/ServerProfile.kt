package com.selfproxy.vpn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selfproxy.vpn.domain.model.Protocol
import kotlinx.serialization.Serializable

/**
 * Server profile entity stored in the database.
 */
@Entity(tableName = "server_profiles")
@Serializable
data class ServerProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val protocol: Protocol,
    val hostname: String,
    val port: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    
    // WireGuard-specific (stored as JSON)
    val wireGuardConfigJson: String? = null,
    
    // VLESS-specific (stored as JSON)
    val vlessConfigJson: String? = null
)
