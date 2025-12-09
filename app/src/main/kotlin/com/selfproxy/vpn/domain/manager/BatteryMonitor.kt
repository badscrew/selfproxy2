package com.selfproxy.vpn.domain.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.selfproxy.vpn.domain.util.SanitizedLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors battery level and charging state changes.
 * 
 * Provides a Flow of battery state updates that can be observed by other components.
 * 
 * Requirements:
 * - 11.5: Monitor battery saver mode
 * - 11.6: Notify user when battery is critically low
 */
class BatteryMonitor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BatteryMonitor"
    }
    
    /**
     * Observes battery state changes.
     * 
     * Emits BatteryInfo whenever:
     * - Battery level changes
     * - Charging state changes
     * - Power save mode changes
     * 
     * @return Flow of battery information
     */
    fun observeBatteryState(): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val batteryInfo = extractBatteryInfo(intent)
                        SanitizedLogger.d(TAG, "Battery changed: level=${batteryInfo.level}%, " +
                                "charging=${batteryInfo.isCharging}")
                        trySend(batteryInfo)
                    }
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        // Battery saver mode changed, get current battery info
                        val currentBatteryInfo = getCurrentBatteryInfo()
                        SanitizedLogger.d(TAG, "Power save mode changed: ${currentBatteryInfo.isBatterySaverMode}")
                        trySend(currentBatteryInfo)
                    }
                }
            }
        }
        
        // Register receiver for battery changes
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        
        context.registerReceiver(receiver, filter)
        
        // Send initial battery state
        val initialBatteryInfo = getCurrentBatteryInfo()
        trySend(initialBatteryInfo)
        
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
    
    /**
     * Gets the current battery information.
     * 
     * @return Current battery information
     */
    fun getCurrentBatteryInfo(): BatteryInfo {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (batteryIntent != null) {
            extractBatteryInfo(batteryIntent)
        } else {
            BatteryInfo()
        }
    }
    
    /**
     * Extracts battery information from an intent.
     * 
     * @param intent Battery changed intent
     * @return Battery information
     */
    private fun extractBatteryInfo(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            100
        }
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        // Check battery saver mode
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isBatterySaverMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
        
        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            isBatterySaverMode = isBatterySaverMode
        )
    }
}

/**
 * Battery information snapshot.
 * 
 * @property level Battery level (0-100)
 * @property isCharging Whether device is charging
 * @property isBatterySaverMode Whether battery saver mode is active
 */
data class BatteryInfo(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val isBatterySaverMode: Boolean = false
)
