package com.selfproxy.vpn.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.WireGuardConfig
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.ui.screens.ProfileFormScreen
import com.selfproxy.vpn.ui.theme.SelfProxyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for profile form screen.
 * 
 * Tests profile creation and editing flows.
 * Validates: Requirements 1.1, 1.2, 1.3, 1.7
 */
@RunWith(AndroidJUnit4::class)
class ProfileFormUiTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun profileFormScreen_showsNewProfileTitle_whenCreatingProfile() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert
        composeTestRule.onNodeWithText("New Profile").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_showsEditProfileTitle_whenEditingProfile() {
        // Arrange
        val profile = createTestWireGuardProfile()
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = profile,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert
        composeTestRule.onNodeWithText("Edit Profile").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_displaysBasicFields() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.1: Profile creation fields
        composeTestRule.onNodeWithText("Profile Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server Hostname").assertIsDisplayed()
        composeTestRule.onNodeWithText("Port").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_displaysProtocolSelector_whenCreatingProfile() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.1: Protocol selection
        composeTestRule.onNodeWithText("Protocol").assertIsDisplayed()
        composeTestRule.onNodeWithText("WireGuard").assertIsDisplayed()
        composeTestRule.onNodeWithText("VLESS").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_doesNotDisplayProtocolSelector_whenEditingProfile() {
        // Arrange
        val profile = createTestWireGuardProfile()
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = profile,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert - Protocol cannot be changed when editing
        composeTestRule.onNodeWithText("Protocol").assertDoesNotExist()
    }
    
    @Test
    fun profileFormScreen_displaysWireGuardFields_whenWireGuardSelected() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.2: WireGuard configuration fields
        composeTestRule.onNodeWithText("WireGuard Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server Public Key").assertIsDisplayed()
        composeTestRule.onNodeWithText("Endpoint (hostname:port)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Allowed IPs").assertIsDisplayed()
        composeTestRule.onNodeWithText("Persistent Keepalive (seconds, optional)").assertIsDisplayed()
        composeTestRule.onNodeWithText("MTU").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_displaysVlessFields_whenVlessSelected() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Click VLESS protocol option
        composeTestRule.onAllNodesWithText("VLESS")[0].performClick()
        
        // Assert - Validates Requirement 1.3: VLESS configuration fields
        composeTestRule.onNodeWithText("VLESS Configuration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transport Protocol").assertIsDisplayed()
        composeTestRule.onNodeWithText("Flow Control").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable TLS").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_showsTlsFields_whenTlsEnabled() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Click VLESS protocol option
        composeTestRule.onAllNodesWithText("VLESS")[0].performClick()
        
        // Enable TLS
        composeTestRule.onNode(hasText("Enable TLS").and(hasClickAction())).performClick()
        
        // Assert - Validates Requirement 1.3: TLS settings
        composeTestRule.onNodeWithText("TLS Server Name (SNI)").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_populatesFields_whenEditingProfile() {
        // Arrange
        val profile = createTestWireGuardProfile()
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = profile,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.7: Profile editing
        composeTestRule.onNodeWithText("Test Server").assertIsDisplayed()
        composeTestRule.onNodeWithText("vpn.example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("51820").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_callsOnSave_whenSaveButtonClicked() {
        // Arrange
        var savedProfile: ServerProfile? = null
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = { savedProfile = it },
                    onCancel = {}
                )
            }
        }
        
        // Fill in required fields
        composeTestRule.onNodeWithText("Profile Name").performTextInput("New Server")
        composeTestRule.onNodeWithText("Server Hostname").performTextInput("new.example.com")
        composeTestRule.onNodeWithText("Port").performTextClearance()
        composeTestRule.onNodeWithText("Port").performTextInput("51820")
        composeTestRule.onNodeWithText("Server Public Key").performTextInput("test_public_key")
        composeTestRule.onNodeWithText("Endpoint (hostname:port)").performTextInput("new.example.com:51820")
        
        // Click save
        composeTestRule.onNodeWithText("Create Profile").performClick()
        
        // Assert
        assert(savedProfile != null)
        assert(savedProfile?.name == "New Server")
        assert(savedProfile?.hostname == "new.example.com")
        assert(savedProfile?.port == 51820)
    }
    
    @Test
    fun profileFormScreen_callsOnCancel_whenBackButtonClicked() {
        // Arrange
        var cancelCalled = false
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = { cancelCalled = true }
                )
            }
        }
        
        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Assert
        assert(cancelCalled)
    }
    
    @Test
    fun profileFormScreen_showsError_whenPortIsInvalid() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Fill in fields with invalid port
        composeTestRule.onNodeWithText("Profile Name").performTextInput("Test")
        composeTestRule.onNodeWithText("Server Hostname").performTextInput("test.com")
        composeTestRule.onNodeWithText("Port").performTextClearance()
        composeTestRule.onNodeWithText("Port").performTextInput("99999")
        composeTestRule.onNodeWithText("Server Public Key").performTextInput("key")
        composeTestRule.onNodeWithText("Endpoint (hostname:port)").performTextInput("test.com:51820")
        
        // Click save
        composeTestRule.onNodeWithText("Create Profile").performClick()
        
        // Assert
        composeTestRule.onNodeWithText("Port must be between 1 and 65535").assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_showsProtocolDescription_forWireGuard() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.9: Protocol guidance
        composeTestRule.onNodeWithText("Recommended: Fast, efficient, and battery-friendly. Best for most users.")
            .assertIsDisplayed()
    }
    
    @Test
    fun profileFormScreen_showsProtocolDescription_forVless() {
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileFormScreen(
                    profile = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }
        
        // Click VLESS protocol option
        composeTestRule.onAllNodesWithText("VLESS")[0].performClick()
        
        // Assert - Validates Requirement 1.9: Protocol guidance
        composeTestRule.onNodeWithText("Advanced: For users requiring obfuscation in restrictive networks.")
            .assertIsDisplayed()
    }
    
    // Helper function to create test WireGuard profile
    private fun createTestWireGuardProfile(): ServerProfile {
        val config = WireGuardConfig(
            publicKey = "test_public_key_base64_encoded_32_bytes",
            endpoint = "vpn.example.com:51820",
            allowedIPs = listOf("0.0.0.0/0", "::/0"),
            persistentKeepalive = 25,
            mtu = 1420
        )
        
        return ServerProfile.createWireGuardProfile(
            id = 1,
            name = "Test Server",
            hostname = "vpn.example.com",
            port = 51820,
            config = config,
            createdAt = System.currentTimeMillis(),
            lastUsed = null
        )
    }
}
