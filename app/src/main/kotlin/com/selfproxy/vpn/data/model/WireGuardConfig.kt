package com.selfproxy.vpn.data.model

import kotlinx.serialization.Serializable

/**
 * WireGuard protocol configuration.
 * Legacy - WireGuard not fully implemented in this app.
 */
@Serializable
data class WireGuardConfig(
    val publicKey: String,
    val endpoint: String,
    val allowedIPs: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int? = null,
    val mtu: Int = 1420
)
