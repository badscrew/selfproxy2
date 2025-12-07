package com.selfproxy.vpn.domain.model

/**
 * Represents the current state of the VPN connection.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val connection: Connection) : ConnectionState()
    object Reconnecting : ConnectionState()
    data class Error(val error: Throwable) : ConnectionState()
}

/**
 * Represents an active VPN connection.
 */
data class Connection(
    val profileId: Long,
    val protocol: Protocol,
    val connectedAt: Long,
    val serverAddress: String
)
