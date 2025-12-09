package com.selfproxy.vpn.domain.manager

import android.content.Context
import android.os.Build
import android.os.PowerManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BatteryOptimizationManager.
 * 
 * Tests battery optimization detection, keep-alive interval calculation,
 * and battery state management.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.5, 11.7
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class BatteryOptimizationManagerTest {
    
    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var batteryOptimizationManager: BatteryOptimizationManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "com.selfproxy.vpn"
        
        batteryOptimizationManager = BatteryOptimizationManager(context)
    }
    
    @Test
    fun `getRecommendedKeepAliveInterval returns normal interval for normal battery`() {
        // Arrange
        val batteryLevel = 80
        val isNatTraversal = true
        
        // Act
        val interval = batteryOptimizationManager.getRecommendedKeepAliveInterval(
            batteryLevel,
            isNatTraversal
        )
        
        // Assert
        assertEquals(BatteryOptimizationManager.KEEPALIVE_NORMAL, interval)
    }
    
    @Test
    fun `getRecommendedKeepAliveInterval returns extended interval for battery saver mode`() {
        // Arrange
        val batteryLevel = 50
        val isNatTraversal = true
        every { powerManager.isPowerSaveMode } returns true
        
        // Act
        val interval = batteryOptimizationManager.getRecommendedKeepAliveInterval(
            batteryLevel,
            isNatTraversal
        )
        
        // Assert
        assertEquals(BatteryOptimizationManager.KEEPALIVE_BATTERY_SAVER, interval)
    }
    
    @Test
    fun `getRecommendedKeepAliveInterval returns critical interval for low battery`() {
        // Arrange
        val batteryLevel = 5
        val isNatTraversal = true
        
        // Act
        val interval = batteryOptimizationManager.getRecommendedKeepAliveInterval(
            batteryLevel,
            isNatTraversal
        )
        
        // Assert
        assertEquals(BatteryOptimizationManager.KEEPALIVE_CRITICAL_BATTERY, interval)
    }
    
    @Test
    fun `getRecommendedKeepAliveInterval returns disabled when NAT traversal not needed`() {
        // Arrange
        val batteryLevel = 80
        val isNatTraversal = false
        
        // Act
        val interval = batteryOptimizationManager.getRecommendedKeepAliveInterval(
            batteryLevel,
            isNatTraversal
        )
        
        // Assert
        assertEquals(BatteryOptimizationManager.KEEPALIVE_DISABLED, interval)
    }
    
    @Test
    fun `updateBatteryState updates battery state correctly`() {
        // Arrange
        val batteryLevel = 15
        val isCharging = false
        every { powerManager.isPowerSaveMode } returns false
        every { powerManager.isDeviceIdleMode } returns false
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns true
        
        // Act
        batteryOptimizationManager.updateBatteryState(batteryLevel, isCharging)
        
        // Assert
        val state = batteryOptimizationManager.batteryState.value
        assertEquals(batteryLevel, state.level)
        assertEquals(isCharging, state.isCharging)
        assertTrue(state.isLowBattery)
        assertFalse(state.isCriticalBattery)
    }
    
    @Test
    fun `updateBatteryState marks critical battery correctly`() {
        // Arrange
        val batteryLevel = 8
        val isCharging = false
        every { powerManager.isPowerSaveMode } returns false
        every { powerManager.isDeviceIdleMode } returns false
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns true
        
        // Act
        batteryOptimizationManager.updateBatteryState(batteryLevel, isCharging)
        
        // Assert
        val state = batteryOptimizationManager.batteryState.value
        assertTrue(state.isCriticalBattery)
        assertTrue(state.isLowBattery)
    }
    
    @Test
    fun `shouldPromptForBatteryOptimization returns true when not exempted`() {
        // Arrange
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns false
        
        // Act
        val shouldPrompt = batteryOptimizationManager.shouldPromptForBatteryOptimization()
        
        // Assert
        assertTrue(shouldPrompt)
    }
    
    @Test
    fun `shouldPromptForBatteryOptimization returns false when exempted`() {
        // Arrange
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns true
        
        // Act
        val shouldPrompt = batteryOptimizationManager.shouldPromptForBatteryOptimization()
        
        // Assert
        assertFalse(shouldPrompt)
    }
    
    @Test
    fun `getBatteryOptimizationMessage returns correct message when exempted`() {
        // Arrange
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns true
        
        // Act
        val message = batteryOptimizationManager.getBatteryOptimizationMessage()
        
        // Assert
        assertTrue(message.contains("disabled"))
        assertTrue(message.contains("doze mode"))
    }
    
    @Test
    fun `getBatteryOptimizationMessage returns correct message when not exempted`() {
        // Arrange
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns false
        
        // Act
        val message = batteryOptimizationManager.getBatteryOptimizationMessage()
        
        // Assert
        assertTrue(message.contains("enabled"))
        assertTrue(message.contains("may disconnect"))
    }
    
    @Test
    fun `createBatteryOptimizationExemptionIntent returns valid intent`() {
        // Act
        val intent = batteryOptimizationManager.createBatteryOptimizationExemptionIntent()
        
        // Assert
        assertNotNull(intent)
        assertEquals(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            intent?.action
        )
    }
}
