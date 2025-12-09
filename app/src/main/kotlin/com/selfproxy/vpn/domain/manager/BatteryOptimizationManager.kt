package com.selfproxy.vpn.domain.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.selfproxy.vpn.domain.util.SanitizedLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages battery optimization settings and power-related features.
 * 
 * Responsibilities:
 * - Request battery optimization exemption
 * - Monitor battery saver mode
 * - Detect doze mode
 * - Provide battery state information
 * 
 * Requirements:
 * - 11.1: Send keep-alive packets at configurable intervals
 * - 11.2: Request battery optimization exemption for doze mode
 * - 11.3: Use efficient polling intervals
 * - 11.4: Leverage WireGuard's efficient cryptography
 * - 11.5: Adjust keep-alive intervals in battery saver mode
 * - 11.7: Use persistent keepalive only when necessary
 */
class BatteryOptimizationManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "BatteryOptimizationManager"
        
        // Keep-alive intervals (in seconds)
        const val KEEPALIVE_NORMAL = 25 // Normal mode
        const val KEEPALIVE_BATTERY_SAVER = 60 // Battery saver mode
        const val KEEPALIVE_CRITICAL_BATTERY = 120 // Critical battery (<10%)
        const val KEEPALIVE_DISABLED = 0 // Disable keep-alive
        
        // Battery level thresholds
        const val CRITICAL_BATTERY_LEVEL = 10 // 10%
        const val LOW_BATTERY_LEVEL = 20 // 20%
    }
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()
    
    /**
     * Checks if the app is currently ignoring battery optimizations.
     * 
     * Requirement 11.2: Check battery optimization status
     * 
     * @return true if battery optimizations are ignored, false otherwise
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            // Battery optimizations don't exist before Android M
            true
        }
    }
    
    /**
     * Creates an intent to request battery optimization exemption.
     * 
     * This intent should be launched by an Activity to show the system dialog.
     * 
     * Requirement 11.2: Request battery optimization exemption
     * 
     * @return Intent to request battery optimization exemption, or null if not supported
     */
    fun createBatteryOptimizationExemptionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }
    
    /**
     * Checks if the device is currently in battery saver mode.
     * 
     * Requirement 11.5: Detect battery saver mode
     * 
     * @return true if battery saver mode is enabled, false otherwise
     */
    fun isBatterySaverMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
    
    /**
     * Checks if the device is currently in doze mode.
     * 
     * Requirement 11.2: Detect doze mode
     * 
     * @return true if device is in doze mode, false otherwise
     */
    fun isDeviceIdleMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isDeviceIdleMode
        } else {
            false
        }
    }
    
    /**
     * Gets the recommended keep-alive interval based on current battery state.
     * 
     * Requirements:
     * - 11.1: Configurable keep-alive intervals
     * - 11.5: Adjust intervals in battery saver mode
     * - 11.7: Use persistent keepalive only when necessary
     * 
     * @param batteryLevel Current battery level (0-100)
     * @param isNatTraversal Whether NAT traversal is needed (requires keep-alive)
     * @return Recommended keep-alive interval in seconds (0 to disable)
     */
    fun getRecommendedKeepAliveInterval(
        batteryLevel: Int,
        isNatTraversal: Boolean = true
    ): Int {
        // If NAT traversal is not needed, disable keep-alive for maximum battery savings
        if (!isNatTraversal) {
            SanitizedLogger.d(TAG, "NAT traversal not needed, disabling keep-alive")
            return KEEPALIVE_DISABLED
        }
        
        return when {
            // Critical battery: use longest interval or disable
            batteryLevel <= CRITICAL_BATTERY_LEVEL -> {
                SanitizedLogger.d(TAG, "Critical battery level: $batteryLevel%, using extended keep-alive")
                KEEPALIVE_CRITICAL_BATTERY
            }
            
            // Battery saver mode: use longer interval
            isBatterySaverMode() -> {
                SanitizedLogger.d(TAG, "Battery saver mode active, using extended keep-alive")
                KEEPALIVE_BATTERY_SAVER
            }
            
            // Normal mode: use standard interval
            else -> {
                SanitizedLogger.d(TAG, "Normal battery mode, using standard keep-alive")
                KEEPALIVE_NORMAL
            }
        }
    }
    
    /**
     * Updates the battery state.
     * 
     * Should be called when battery level or power save mode changes.
     * 
     * @param batteryLevel Current battery level (0-100)
     * @param isCharging Whether device is charging
     */
    fun updateBatteryState(batteryLevel: Int, isCharging: Boolean) {
        val batterySaverMode = isBatterySaverMode()
        val dozeMode = isDeviceIdleMode()
        val batteryOptimizationExempted = isIgnoringBatteryOptimizations()
        
        val newState = BatteryState(
            level = batteryLevel,
            isCharging = isCharging,
            isBatterySaverMode = batterySaverMode,
            isDozeMode = dozeMode,
            isBatteryOptimizationExempted = batteryOptimizationExempted,
            isCriticalBattery = batteryLevel <= CRITICAL_BATTERY_LEVEL,
            isLowBattery = batteryLevel <= LOW_BATTERY_LEVEL
        )
        
        _batteryState.value = newState
        
        SanitizedLogger.d(TAG, "Battery state updated: level=$batteryLevel%, " +
                "charging=$isCharging, batterySaver=$batterySaverMode, " +
                "doze=$dozeMode, exempted=$batteryOptimizationExempted")
    }
    
    /**
     * Checks if the user should be notified about battery optimization.
     * 
     * Returns true if:
     * - Battery optimization is not exempted
     * - Device supports battery optimization
     * - VPN is active or about to be activated
     * 
     * @return true if user should be prompted, false otherwise
     */
    fun shouldPromptForBatteryOptimization(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        
        return !isIgnoringBatteryOptimizations()
    }
    
    /**
     * Gets a user-friendly message about battery optimization status.
     * 
     * @return Message explaining battery optimization status and recommendations
     */
    fun getBatteryOptimizationMessage(): String {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                "Battery optimization not available on this Android version."
            }
            isIgnoringBatteryOptimizations() -> {
                "Battery optimization is disabled for this app. VPN will remain active in doze mode."
            }
            else -> {
                "Battery optimization is enabled. The VPN may disconnect when the device enters doze mode. " +
                        "Tap to disable battery optimization for reliable VPN connections."
            }
        }
    }
}

/**
 * Represents the current battery state of the device.
 * 
 * @property level Battery level (0-100)
 * @property isCharging Whether device is charging
 * @property isBatterySaverMode Whether battery saver mode is active
 * @property isDozeMode Whether device is in doze mode
 * @property isBatteryOptimizationExempted Whether app is exempted from battery optimization
 * @property isCriticalBattery Whether battery is critically low (<10%)
 * @property isLowBattery Whether battery is low (<20%)
 */
data class BatteryState(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val isBatterySaverMode: Boolean = false,
    val isDozeMode: Boolean = false,
    val isBatteryOptimizationExempted: Boolean = false,
    val isCriticalBattery: Boolean = false,
    val isLowBattery: Boolean = false
)
