package com.selfproxy.vpn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * App routing configuration entity.
 * 
 * Stores per-app routing settings for VPN connections.
 * 
 * Requirements:
 * - 5.1: Display list of installed applications
 * - 5.2: Exclude apps from VPN tunnel
 * - 5.3: Include apps in VPN tunnel
 * - 5.4: Persist app routing configuration
 * - 5.8: Support "Route All Apps" or "Route Selected Apps Only" modes
 */
@Entity(tableName = "app_routing_config")
@Serializable
data class AppRoutingConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Profile ID this routing configuration belongs to.
     * If null, this is the global default configuration.
     */
    val profileId: Long? = null,
    
    /**
     * Routing mode: true for "Route All Apps", false for "Route Selected Apps Only".
     */
    val routeAllApps: Boolean = true,
    
    /**
     * Set of package names to exclude from VPN (when routeAllApps is true).
     * Or set of package names to include in VPN (when routeAllApps is false).
     */
    val packageNames: Set<String> = emptySet(),
    
    /**
     * Timestamp when the configuration was last updated.
     */
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Returns the set of package names that should be excluded from the VPN.
     * 
     * When routeAllApps is true: returns packageNames (excluded apps)
     * When routeAllApps is false: returns all apps except packageNames (included apps)
     */
    fun getExcludedPackages(allInstalledPackages: Set<String>): Set<String> {
        return if (routeAllApps) {
            // Route all apps mode: packageNames contains excluded apps
            packageNames
        } else {
            // Route selected apps only mode: exclude all apps except those in packageNames
            allInstalledPackages - packageNames
        }
    }
    
    /**
     * Returns the set of package names that should be included in the VPN.
     */
    fun getIncludedPackages(allInstalledPackages: Set<String>): Set<String> {
        return if (routeAllApps) {
            // Route all apps mode: all apps except excluded ones
            allInstalledPackages - packageNames
        } else {
            // Route selected apps only mode: only apps in packageNames
            packageNames
        }
    }
}

/**
 * Represents an installed application with metadata.
 */
@Serializable
data class InstalledApp(
    /**
     * Package name (e.g., "com.android.chrome").
     */
    val packageName: String,
    
    /**
     * User-friendly app name (e.g., "Chrome").
     */
    val appName: String,
    
    /**
     * Whether this app is currently selected for routing.
     */
    val isSelected: Boolean = false,
    
    /**
     * Whether this app is the VPN app itself (should always be excluded).
     */
    val isSelfApp: Boolean = false
)
