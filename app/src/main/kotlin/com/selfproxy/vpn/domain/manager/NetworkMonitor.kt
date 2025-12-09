package com.selfproxy.vpn.domain.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.selfproxy.vpn.domain.model.NetworkState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors network connectivity changes.
 * 
 * Observes network state changes including:
 * - Network availability
 * - Network type changes (WiFi ↔ Mobile data)
 * - Network loss
 * 
 * This class is used by AutoReconnectService to trigger reconnection
 * when the network changes.
 * 
 * Requirement 6.5: Detect network changes and trigger reconnection
 * 
 * @param context Android context for accessing system services
 */
class NetworkMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Observes network state changes as a Flow.
     * 
     * Emits NetworkState events when:
     * - A network becomes available
     * - A network is lost
     * - Network capabilities change (WiFi ↔ Mobile)
     * 
     * The Flow automatically registers and unregisters the network callback
     * when collected/cancelled.
     * 
     * @return Flow of NetworkState events
     */
    fun observeNetworkChanges(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available(network))
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkState.Lost)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                trySend(NetworkState.Changed(isWifi, isCellular))
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
    
    /**
     * Checks if network is currently available.
     * 
     * @return True if a network with internet capability is available, false otherwise
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Gets the current network type.
     * 
     * @return NetworkType indicating the current network type, or null if no network
     */
    fun getCurrentNetworkType(): NetworkType? {
        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }
}

/**
 * Represents the type of network connection.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}
