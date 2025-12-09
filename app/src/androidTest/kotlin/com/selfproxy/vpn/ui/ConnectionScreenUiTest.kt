package com.selfproxy.vpn.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.VlessConfig
import com.selfproxy.vpn.data.model.WireGuardConfig
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.adapter.ConnectionTestResult
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.FlowControl
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.model.TransportProtocol
import com.selfproxy.vpn.ui.screens.ConnectionScreen
import com.selfproxy.vpn.ui.theme.SelfProxyTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Connection Screen.
 * 
 * Tests connection button states, statistics display, and status updates.
 * 
 * Requirements: 7.1, 7.9
 */
class ConnectionScreenUiTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testWireGuardProfile = ServerProfile(
        id = 1L,
        name = "Test WireGuard",
        protocol = Protocol.WIREGUARD,
        hostname = "test.example.com",
        port = 51820,
        wireGuardConfig = WireGuardConfig(
            publicKey = "HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=",
            allowedIPs = listOf("0.0.0.0/0"),
            persistentKeepalive = 25,
            endpoint = "test.example.com:51820"
        ),
        vlessConfig = null,
        createdAt = System.currentTimeMillis(),
        lastUsed = null
    )
    
    private val testVlessProfile = ServerProfile(
        id = 2L,
        name = "Test VLESS",
        protocol = Protocol.VLESS,
        hostname = "vless.example.com",
        port = 443,
        wireGuardConfig = null,
        vlessConfig = VlessConfig(
            uuid = "test-uuid-1234",
            flowControl = FlowControl.XTLS_RPRX_VISION,
            transport = TransportProtocol.TCP,
            tlsSettings = null,
            realitySettings = null
        ),
        createdAt = System.currentTimeMillis(),
        lastUsed = null
    )
    
    private val testStatistics = ConnectionStatistics(
        bytesReceived = 1024 * 1024 * 50, // 50 MB
        bytesSent = 1024 * 1024 * 20, // 20 MB
        downloadSpeed = 1024 * 500, // 500 KB/s
        uploadSpeed = 1024 * 200, // 200 KB/s
        connectionDuration = 3600000, // 1 hour
        lastHandshakeTime = System.currentTimeMillis() - 30000, // 30 seconds ago
        latency = null
    )
    
    /**
     * Test that disconnected state shows correct UI elements.
     * 
     * Requirement 7.1: Display connection status
     */
    @Test
    fun disconnectedState_showsCorrectElements() {
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Disconnected,
                    currentProfile = null,
                    statistics = null,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify disconnected status is shown
        composeTestRule.onNodeWithText("Disconnected").assertIsDisplayed()
        
        // Verify select profile button is shown
        composeTestRule.onNodeWithText("Select Profile").assertIsDisplayed()
        
        // Verify statistics card is not shown
        composeTestRule.onNodeWithText("Statistics").assertDoesNotExist()
    }
    
    /**
     * Test that connecting state shows correct UI elements.
     * 
     * Requirement 7.1: Display connection status
     */
    @Test
    fun connectingState_showsCorrectElements() {
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connecting,
                    currentProfile = testWireGuardProfile,
                    statistics = null,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify connecting status is shown
        composeTestRule.onNodeWithText("Connecting...").assertIsDisplayed()
        
        // Verify profile info is shown
        composeTestRule.onNodeWithText("Test WireGuard").assertIsDisplayed()
        composeTestRule.onNodeWithText("test.example.com:51820").assertIsDisplayed()
        
        // Verify button is disabled during connection
        composeTestRule.onNodeWithText("Connecting...").assertIsNotEnabled()
    }
    
    /**
     * Test that connected state shows correct UI elements.
     * 
     * Requirement 7.1: Display connection status
     */
    @Test
    fun connectedState_showsCorrectElements() {
        val connection = Connection(
            profileId = 1L,
            protocol = Protocol.WIREGUARD,
            connectedAt = System.currentTimeMillis(),
            serverAddress = "test.example.com"
        )
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connected(connection),
                    currentProfile = testWireGuardProfile,
                    statistics = testStatistics,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify connected status is shown
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        
        // Verify disconnect button is shown
        composeTestRule.onNodeWithText("Disconnect").assertIsDisplayed()
        
        // Verify statistics card is shown
        composeTestRule.onNodeWithText("Statistics").assertIsDisplayed()
        
        // Verify connection test section is shown
        composeTestRule.onNodeWithText("Connection Test").assertIsDisplayed()
    }
    
    /**
     * Test that error state shows correct UI elements.
     * 
     * Requirement 7.1: Display connection status
     */
    @Test
    fun errorState_showsCorrectElements() {
        val error = Exception("Authentication failed: Invalid WireGuard keys")
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Error(error),
                    currentProfile = testWireGuardProfile,
                    statistics = null,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify error status is shown
        composeTestRule.onNodeWithText("Connection Failed").assertIsDisplayed()
        
        // Verify error message is shown
        composeTestRule.onNodeWithText("Connection Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Authentication failed: Invalid WireGuard keys").assertIsDisplayed()
        
        // Verify connect button is shown (to retry)
        composeTestRule.onNodeWithText("Connect").assertIsDisplayed()
    }
    
    /**
     * Test that statistics are displayed correctly.
     * 
     * Requirement 7.9: Statistics display
     */
    @Test
    fun connectedState_displaysStatistics() {
        val connection = Connection(
            profileId = 1L,
            protocol = Protocol.WIREGUARD,
            connectedAt = System.currentTimeMillis(),
            serverAddress = "test.example.com"
        )
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connected(connection),
                    currentProfile = testWireGuardProfile,
                    statistics = testStatistics,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify statistics labels are shown
        composeTestRule.onNodeWithText("Duration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download Speed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Upload Speed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloaded").assertIsDisplayed()
        composeTestRule.onNodeWithText("Uploaded").assertIsDisplayed()
        
        // Verify WireGuard-specific statistics
        composeTestRule.onNodeWithText("Last Handshake").assertIsDisplayed()
    }
    
    /**
     * Test that VLESS-specific statistics are displayed.
     * 
     * Requirement 7.9: Statistics display
     */
    @Test
    fun connectedState_displaysVlessStatistics() {
        val connection = Connection(
            profileId = 2L,
            protocol = Protocol.VLESS,
            connectedAt = System.currentTimeMillis(),
            serverAddress = "vless.example.com"
        )
        
        val vlessStatistics = testStatistics.copy(
            lastHandshakeTime = null,
            latency = 45L
        )
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connected(connection),
                    currentProfile = testVlessProfile,
                    statistics = vlessStatistics,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify VLESS-specific statistics
        composeTestRule.onNodeWithText("Latency").assertIsDisplayed()
        composeTestRule.onNodeWithText("45 ms").assertIsDisplayed()
    }
    
    /**
     * Test that connect button triggers connection.
     * 
     * Requirement 7.1: Connection button states
     */
    @Test
    fun connectButton_triggersConnection() {
        var connectCalled = false
        var connectedProfileId = 0L
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Disconnected,
                    currentProfile = testWireGuardProfile,
                    statistics = null,
                    testResult = null,
                    isTesting = false,
                    onConnect = { profileId ->
                        connectCalled = true
                        connectedProfileId = profileId
                    },
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Click connect button
        composeTestRule.onNodeWithText("Connect").performClick()
        
        // Verify callback was called with correct profile ID
        assert(connectCalled)
        assert(connectedProfileId == 1L)
    }
    
    /**
     * Test that disconnect button triggers disconnection.
     * 
     * Requirement 7.1: Connection button states
     */
    @Test
    fun disconnectButton_triggersDisconnection() {
        var disconnectCalled = false
        
        val connection = Connection(
            profileId = 1L,
            protocol = Protocol.WIREGUARD,
            connectedAt = System.currentTimeMillis(),
            serverAddress = "test.example.com"
        )
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connected(connection),
                    currentProfile = testWireGuardProfile,
                    statistics = testStatistics,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = { disconnectCalled = true },
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Click disconnect button
        composeTestRule.onNodeWithText("Disconnect").performClick()
        
        // Verify callback was called
        assert(disconnectCalled)
    }
    
    /**
     * Test that connection test button triggers test.
     * 
     * Requirement 7.1: Connection button states
     */
    @Test
    fun testConnectionButton_triggersTest() {
        var testCalled = false
        var testedProfileId = 0L
        
        val connection = Connection(
            profileId = 1L,
            protocol = Protocol.WIREGUARD,
            connectedAt = System.currentTimeMillis(),
            serverAddress = "test.example.com"
        )
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connected(connection),
                    currentProfile = testWireGuardProfile,
                    statistics = testStatistics,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = { profileId ->
                        testCalled = true
                        testedProfileId = profileId
                    },
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Click test connection button
        composeTestRule.onNodeWithText("Test Connection").performClick()
        
        // Verify callback was called with correct profile ID
        assert(testCalled)
        assert(testedProfileId == 1L)
    }
    
    /**
     * Test that test result is displayed correctly.
     * 
     * Requirement 7.9: Status updates
     */
    @Test
    fun testResult_displaysCorrectly() {
        val connection = Connection(
            profileId = 1L,
            protocol = Protocol.WIREGUARD,
            connectedAt = System.currentTimeMillis(),
            serverAddress = "test.example.com"
        )
        
        val successResult = ConnectionTestResult(
            success = true,
            latencyMs = 45L,
            errorMessage = null
        )
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connected(connection),
                    currentProfile = testWireGuardProfile,
                    statistics = testStatistics,
                    testResult = successResult,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify test result is shown
        composeTestRule.onNodeWithText("Test Passed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Latency: 45 ms").assertIsDisplayed()
    }
    
    /**
     * Test that failed test result is displayed correctly.
     * 
     * Requirement 7.9: Status updates
     */
    @Test
    fun failedTestResult_displaysCorrectly() {
        val connection = Connection(
            profileId = 1L,
            protocol = Protocol.WIREGUARD,
            connectedAt = System.currentTimeMillis(),
            serverAddress = "test.example.com"
        )
        
        val failedResult = ConnectionTestResult(
            success = false,
            latencyMs = null,
            errorMessage = "Connection timeout"
        )
        
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Connected(connection),
                    currentProfile = testWireGuardProfile,
                    statistics = testStatistics,
                    testResult = failedResult,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify failed test result is shown
        composeTestRule.onNodeWithText("Test Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection timeout").assertIsDisplayed()
    }
    
    /**
     * Test that protocol badge is displayed correctly.
     * 
     * Requirement 7.9: Status updates
     */
    @Test
    fun protocolBadge_displaysCorrectly() {
        composeTestRule.setContent {
            SelfProxyTheme {
                ConnectionScreen(
                    connectionState = ConnectionState.Disconnected,
                    currentProfile = testWireGuardProfile,
                    statistics = null,
                    testResult = null,
                    isTesting = false,
                    onConnect = {},
                    onDisconnect = {},
                    onTestConnection = {},
                    onResetStatistics = {},
                    onClearTestResult = {},
                    onSelectProfile = {}
                )
            }
        }
        
        // Verify protocol badge is shown
        composeTestRule.onNodeWithText("WIREGUARD").assertIsDisplayed()
    }
}
