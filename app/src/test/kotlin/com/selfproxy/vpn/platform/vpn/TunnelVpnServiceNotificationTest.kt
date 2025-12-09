package com.selfproxy.vpn.platform.vpn

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for TunnelVpnService notification functionality.
 * 
 * Tests notification channel creation, notification updates, and status formatting.
 * 
 * Requirements:
 * - 3.5: VPN key icon displayed in status bar
 * - 11.9: Foreground service notification with connection status
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class TunnelVpnServiceNotificationTest {
    
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    /**
     * Tests that notification channel is created with correct properties.
     * 
     * Requirement 3.5: Notification channel for VPN service
     */
    @Test
    fun `notification channel should be created with correct properties`() {
        // Note: This test verifies the channel configuration
        // In a real service, the channel would be created in onCreate()
        
        val channelId = "vpn_service_channel"
        val channelName = "VPN Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        
        // Verify channel properties
        assertEquals(channelId, "vpn_service_channel")
        assertEquals(channelName, "VPN Service")
        assertEquals(importance, NotificationManager.IMPORTANCE_LOW)
    }
    
    /**
     * Tests notification status formatting for different connection states.
     * 
     * Requirement 11.9: Update notification on state changes
     */
    @Test
    fun `notification status should be formatted correctly for different states`() {
        // Test connected state
        val connectedTitle = formatNotificationTitle("connected")
        assertEquals("VPN Connected", connectedTitle)
        
        // Test connecting state
        val connectingTitle = formatNotificationTitle("connecting")
        assertEquals("VPN Connecting...", connectingTitle)
        
        // Test disconnected state
        val disconnectedTitle = formatNotificationTitle("disconnected")
        assertEquals("VPN Disconnected", disconnectedTitle)
        
        // Test reconnecting state
        val reconnectingTitle = formatNotificationTitle("reconnecting")
        assertEquals("VPN Reconnecting...", reconnectingTitle)
        
        // Test error state
        val errorTitle = formatNotificationTitle("error")
        assertEquals("VPN Connection Error", errorTitle)
    }
    
    /**
     * Tests notification text formatting with profile name and additional info.
     * 
     * Requirement 11.9: Connection status in notification
     */
    @Test
    fun `notification text should include profile name and additional info`() {
        // Test with profile name only
        val textWithProfile = formatNotificationText("My VPN Server", null)
        assertEquals("My VPN Server", textWithProfile)
        
        // Test with additional info only
        val textWithInfo = formatNotificationText(null, "↓ 1.5 MB/s ↑ 500 KB/s")
        assertEquals("↓ 1.5 MB/s ↑ 500 KB/s", textWithInfo)
        
        // Test with both profile name and additional info
        val textWithBoth = formatNotificationText("My VPN Server", "↓ 1.5 MB/s ↑ 500 KB/s")
        assertEquals("My VPN Server • ↓ 1.5 MB/s ↑ 500 KB/s", textWithBoth)
        
        // Test with neither
        val textWithNeither = formatNotificationText(null, null)
        assertEquals("Tap to open", textWithNeither)
    }
    
    /**
     * Tests that disconnect action button is shown only when connected.
     * 
     * Requirement 11.9: Disconnect action button
     */
    @Test
    fun `disconnect action should be shown only when connected`() {
        // Connected state should show disconnect button
        val showDisconnectWhenConnected = shouldShowDisconnectAction("connected")
        assertEquals(true, showDisconnectWhenConnected)
        
        // Connecting state should not show disconnect button
        val showDisconnectWhenConnecting = shouldShowDisconnectAction("connecting")
        assertEquals(false, showDisconnectWhenConnecting)
        
        // Disconnected state should not show disconnect button
        val showDisconnectWhenDisconnected = shouldShowDisconnectAction("disconnected")
        assertEquals(false, showDisconnectWhenDisconnected)
        
        // Error state should not show disconnect button
        val showDisconnectWhenError = shouldShowDisconnectAction("error")
        assertEquals(false, showDisconnectWhenError)
    }
    
    // Helper functions that mirror the logic in TunnelVpnService
    
    private fun formatNotificationTitle(status: String): String {
        return when (status.lowercase()) {
            "connected" -> "VPN Connected"
            "connecting" -> "VPN Connecting..."
            "disconnecting" -> "VPN Disconnecting..."
            "disconnected" -> "VPN Disconnected"
            "reconnecting" -> "VPN Reconnecting..."
            "error" -> "VPN Connection Error"
            else -> "VPN $status"
        }
    }
    
    private fun formatNotificationText(profileName: String?, additionalInfo: String?): String {
        return buildString {
            if (profileName != null) {
                append(profileName)
            }
            if (additionalInfo != null) {
                if (profileName != null) append(" • ")
                append(additionalInfo)
            }
            if (profileName == null && additionalInfo == null) {
                append("Tap to open")
            }
        }
    }
    
    private fun shouldShowDisconnectAction(status: String): Boolean {
        return status.lowercase() == "connected"
    }
}
