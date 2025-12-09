package com.selfproxy.vpn.domain.model

import android.net.Network

/**
 * Represents the state of network connectivity.
 * 
 * Used by NetworkMonitor to report network changes.
 */
sealed class NetworkState {
    /**
     * Network is available and has internet capability.
     * 
     * @param network The available network
     */
    data class Available(val network: Network) : NetworkState()
    
    /**
     * Network was lost.
     */
    object Lost : NetworkState()
    
    /**
     * Network capabilities changed (e.g., WiFi ↔ Mobile data).
     * 
     * @param isWifi True if the network is WiFi
     * @param isCellular True if the network is cellular/mobile data
     */
    data class Changed(
        val isWifi: Boolean,
        val isCellular: Boolean
    ) : NetworkState()
    
    /**
     * No network is currently available.
     */
    object Unavailable : NetworkState()
}
