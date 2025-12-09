package com.selfproxy.vpn.domain.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for AutoReconnectService.
 * 
 * Tests:
 * - Exponential backoff calculation
 * - Reconnection attempt counting
 * - Manual disconnect handling
 * 
 * Requirements: 6.3, 6.10
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoReconnectServiceTest {
    
    private lateinit var context: Context
    private lateinit var connectionManager: ConnectionManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var autoReconnectService: AutoReconnectService
    
    private val testDispatcher = StandardTestDispatcher()
    private val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock NetworkRequest.Builder
        mockkConstructor(NetworkRequest.Builder::class)
        every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns mockk(relaxed = true)
        every { anyConstructed<NetworkRequest.Builder>().build() } returns mockk(relaxed = true)
        
        // Mock dependencies
        context = mockk(relaxed = true)
        connectionManager = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        
        // Setup context to return connectivity manager
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        
        // Setup connection manager to return state flow
        every { connectionManager.connectionState } returns connectionStateFlow
        
        // Mock registerNetworkCallback to do nothing
        every { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>()) } returns Unit
        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } returns Unit
        
        // Create service
        autoReconnectService = AutoReconnectService(
            context = context,
            connectionManager = connectionManager,
            dispatcher = testDispatcher
        )
    }
    
    /**
     * Test exponential backoff calculation.
     * 
     * Requirement 6.3: Exponential backoff starting at 1 second up to maximum of 60 seconds
     * 
     * Expected delays:
     * - Attempt 1: 1 second
     * - Attempt 2: 2 seconds
     * - Attempt 3: 4 seconds
     * - Attempt 4: 8 seconds
     * - Attempt 5: 16 seconds
     * - Attempt 6: 32 seconds
     * - Attempt 7+: 60 seconds (capped)
     */
    @Test
    fun `exponential backoff calculation should follow correct pattern`() {
        // Test first attempt
        assertEquals(1, autoReconnectService.calculateBackoffDelay(1))
        
        // Test second attempt
        assertEquals(2, autoReconnectService.calculateBackoffDelay(2))
        
        // Test third attempt
        assertEquals(4, autoReconnectService.calculateBackoffDelay(3))
        
        // Test fourth attempt
        assertEquals(8, autoReconnectService.calculateBackoffDelay(4))
        
        // Test fifth attempt
        assertEquals(16, autoReconnectService.calculateBackoffDelay(5))
        
        // Test sixth attempt
        assertEquals(32, autoReconnectService.calculateBackoffDelay(6))
        
        // Test seventh attempt (should be capped at 60)
        assertEquals(60, autoReconnectService.calculateBackoffDelay(7))
        
        // Test eighth attempt (should still be capped at 60)
        assertEquals(60, autoReconnectService.calculateBackoffDelay(8))
        
        // Test large attempt number (should be capped at 60)
        assertEquals(60, autoReconnectService.calculateBackoffDelay(100))
    }
    
    /**
     * Test exponential backoff with edge cases.
     */
    @Test
    fun `exponential backoff should handle edge cases`() {
        // Test zero attempt (should return 1)
        assertEquals(1, autoReconnectService.calculateBackoffDelay(0))
        
        // Test negative attempt (should return 1)
        assertEquals(1, autoReconnectService.calculateBackoffDelay(-1))
        
        // Test negative large number (should return 1)
        assertEquals(1, autoReconnectService.calculateBackoffDelay(-100))
    }
    
    /**
     * Test that enabling sets the correct state.
     */
    @Test
    fun `enabling auto-reconnect should set correct state`() {
        val profileId = 1L
        
        // Enable auto-reconnect
        autoReconnectService.enable(profileId)
        
        // Verify state
        assertTrue(autoReconnectService.isEnabled())
        assertFalse(autoReconnectService.isManualDisconnect())
        assertEquals(0, autoReconnectService.getAttemptCount())
    }
    
    /**
     * Test manual disconnect handling.
     * 
     * Requirement 6.10: Manual disconnect disables auto-reconnect
     */
    @Test
    fun `manual disconnect should disable auto-reconnect`() = runTest {
        val profileId = 1L
        
        // Enable auto-reconnect
        autoReconnectService.enable(profileId)
        
        // Verify auto-reconnect is enabled
        assertTrue(autoReconnectService.isEnabled())
        assertFalse(autoReconnectService.isManualDisconnect())
        
        // Manually disable (simulating user disconnect)
        autoReconnectService.disable()
        
        // Verify auto-reconnect is disabled
        assertFalse(autoReconnectService.isEnabled())
        assertTrue(autoReconnectService.isManualDisconnect())
        
        // Verify attempt count is reset
        assertEquals(0, autoReconnectService.getAttemptCount())
    }
    
    /**
     * Test that disabling clears the state.
     */
    @Test
    fun `disabling auto-reconnect should clear state`() {
        val profileId = 1L
        
        // Enable first
        autoReconnectService.enable(profileId)
        assertTrue(autoReconnectService.isEnabled())
        
        // Disable
        autoReconnectService.disable()
        
        // Verify state is cleared
        assertFalse(autoReconnectService.isEnabled())
        assertTrue(autoReconnectService.isManualDisconnect())
        assertEquals(0, autoReconnectService.getAttemptCount())
    }
    
    /**
     * Test that enabling auto-reconnect resets manual disconnect flag.
     */
    @Test
    fun `enabling auto-reconnect should reset manual disconnect flag`() {
        val profileId = 1L
        
        // Disable first (simulating manual disconnect)
        autoReconnectService.disable()
        assertTrue(autoReconnectService.isManualDisconnect())
        
        // Enable again
        autoReconnectService.enable(profileId)
        
        // Manual disconnect flag should be reset
        assertFalse(autoReconnectService.isManualDisconnect())
        assertTrue(autoReconnectService.isEnabled())
    }
    
    /**
     * Test initial reconnect state.
     */
    @Test
    fun `initial reconnect state should be Idle`() {
        // Initial state should be Idle
        assertEquals(ReconnectState.Idle, autoReconnectService.reconnectState.value)
    }
}
