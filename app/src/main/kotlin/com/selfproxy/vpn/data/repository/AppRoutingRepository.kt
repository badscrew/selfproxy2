package com.selfproxy.vpn.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.selfproxy.vpn.data.database.AppRoutingDao
import com.selfproxy.vpn.data.model.AppRoutingConfig
import com.selfproxy.vpn.data.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing app routing configuration.
 * 
 * Provides operations for:
 * - Retrieving installed applications
 * - Saving and loading app routing configuration
 * - Determining which apps should be excluded from VPN
 * 
 * Requirements:
 * - 5.1: Display list of installed applications
 * - 5.2, 5.3: Exclude/include apps from VPN tunnel
 * - 5.4: Persist app routing configuration
 * - 5.5: Apply routing on VPN start
 * - 5.6: Support dynamic routing updates
 * - 5.7: Automatically exclude VPN app itself
 * - 5.8: Support "Route All Apps" or "Route Selected Apps Only" modes
 */
class AppRoutingRepository(
    private val context: Context,
    private val appRoutingDao: AppRoutingDao
) {
    
    private val packageManager: PackageManager = context.packageManager
    private val selfPackageName: String = context.packageName
    
    /**
     * Gets all installed applications on the device.
     * 
     * Requirement 5.1: Display list of installed applications with names and icons.
     * 
     * @param includeSystemApps Whether to include system apps
     * @return List of installed applications
     */
    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<InstalledApp> {
        return withContext(Dispatchers.IO) {
            try {
                val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                
                packages
                    .filter { appInfo ->
                        // Filter out system apps if requested
                        includeSystemApps || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    }
                    .map { appInfo ->
                        InstalledApp(
                            packageName = appInfo.packageName,
                            appName = appInfo.loadLabel(packageManager).toString(),
                            isSelected = false,
                            isSelfApp = appInfo.packageName == selfPackageName
                        )
                    }
                    .sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Gets the app icon for a package.
     * 
     * @param packageName The package name
     * @return The app icon, or null if not found
     */
    suspend fun getAppIcon(packageName: String): Drawable? {
        return withContext(Dispatchers.IO) {
            try {
                packageManager.getApplicationIcon(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
    
    /**
     * Saves app routing configuration for a profile.
     * 
     * Requirement 5.4: Persist app routing configuration.
     * 
     * @param config The configuration to save
     * @return Result indicating success or failure
     */
    suspend fun saveConfig(config: AppRoutingConfig): Result<Long> {
        return try {
            val id = appRoutingDao.insert(config.copy(lastUpdated = System.currentTimeMillis()))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates an existing app routing configuration.
     * 
     * Requirement 5.6: Support dynamic routing updates.
     * 
     * @param config The configuration to update
     * @return Result indicating success or failure
     */
    suspend fun updateConfig(config: AppRoutingConfig): Result<Unit> {
        return try {
            appRoutingDao.update(config.copy(lastUpdated = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets app routing configuration for a specific profile.
     * 
     * @param profileId The profile ID
     * @return The configuration, or null if not found
     */
    suspend fun getConfigForProfile(profileId: Long): AppRoutingConfig? {
        return try {
            appRoutingDao.getByProfileId(profileId)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Observes app routing configuration for a specific profile.
     * 
     * @param profileId The profile ID
     * @return Flow of configuration updates
     */
    fun observeConfigForProfile(profileId: Long): Flow<AppRoutingConfig?> {
        return appRoutingDao.observeByProfileId(profileId)
    }
    
    /**
     * Gets the global default app routing configuration.
     * 
     * @return The global configuration, or a default configuration if not found
     */
    suspend fun getGlobalConfig(): AppRoutingConfig {
        return try {
            appRoutingDao.getGlobalConfig() ?: AppRoutingConfig(
                profileId = null,
                routeAllApps = true,
                packageNames = emptySet()
            )
        } catch (e: Exception) {
            AppRoutingConfig(
                profileId = null,
                routeAllApps = true,
                packageNames = emptySet()
            )
        }
    }
    
    /**
     * Observes the global default app routing configuration.
     * 
     * @return Flow of global configuration updates
     */
    fun observeGlobalConfig(): Flow<AppRoutingConfig?> {
        return appRoutingDao.observeGlobalConfig()
    }
    
    /**
     * Gets the set of package names that should be excluded from the VPN.
     * 
     * Requirements:
     * - 5.2: Exclude apps from VPN tunnel
     * - 5.5: Apply routing on VPN start
     * - 5.7: Automatically exclude VPN app itself
     * 
     * @param profileId The profile ID, or null for global config
     * @return Set of package names to exclude (always includes self app)
     */
    suspend fun getExcludedPackages(profileId: Long? = null): Set<String> {
        return try {
            val config = if (profileId != null) {
                getConfigForProfile(profileId) ?: getGlobalConfig()
            } else {
                getGlobalConfig()
            }
            
            val allPackages = getInstalledApps(includeSystemApps = true)
                .map { it.packageName }
                .toSet()
            
            val excluded = config.getExcludedPackages(allPackages).toMutableSet()
            
            // Requirement 5.7: Always exclude self app to prevent routing loops
            excluded.add(selfPackageName)
            
            excluded
        } catch (e: Exception) {
            // On error, at minimum exclude self app
            setOf(selfPackageName)
        }
    }
    
    /**
     * Gets the set of package names that should be included in the VPN.
     * 
     * Requirement 5.3: Include apps in VPN tunnel.
     * 
     * @param profileId The profile ID, or null for global config
     * @return Set of package names to include (never includes self app)
     */
    suspend fun getIncludedPackages(profileId: Long? = null): Set<String> {
        return try {
            val config = if (profileId != null) {
                getConfigForProfile(profileId) ?: getGlobalConfig()
            } else {
                getGlobalConfig()
            }
            
            val allPackages = getInstalledApps(includeSystemApps = true)
                .map { it.packageName }
                .toSet()
            
            val included = config.getIncludedPackages(allPackages).toMutableSet()
            
            // Requirement 5.7: Never include self app
            included.remove(selfPackageName)
            
            included
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * Deletes app routing configuration for a specific profile.
     * 
     * @param profileId The profile ID
     * @return Result indicating success or failure
     */
    suspend fun deleteConfigForProfile(profileId: Long): Result<Unit> {
        return try {
            appRoutingDao.deleteByProfileId(profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Creates a default configuration for a profile.
     * 
     * @param profileId The profile ID
     * @return The default configuration
     */
    fun createDefaultConfig(profileId: Long): AppRoutingConfig {
        return AppRoutingConfig(
            profileId = profileId,
            routeAllApps = true,
            packageNames = emptySet()
        )
    }
}
