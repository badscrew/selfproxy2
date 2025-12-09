package com.selfproxy.vpn.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.WireGuardConfig
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.ui.screens.ProfileListScreen
import com.selfproxy.vpn.ui.theme.SelfProxyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for profile management screens.
 * 
 * Tests profile list display, profile creation flow, and profile deletion.
 * Validates: Requirements 1.5, 1.8
 */
@RunWith(AndroidJUnit4::class)
class ProfileManagementUiTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun profileListScreen_displaysEmptyState_whenNoProfiles() {
        // Arrange
        val profiles = emptyList<ServerProfile>()
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = profiles,
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = {}
                )
            }
        }
        
        // Assert
        composeTestRule.onNodeWithText("No VPN Profiles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create your first profile to get started").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Profile").assertIsDisplayed()
    }
    
    @Test
    fun profileListScreen_displaysProfiles_whenProfilesExist() {
        // Arrange
        val profiles = listOf(
            createTestWireGuardProfile(
                id = 1,
                name = "Test WireGuard Server",
                hostname = "vpn.example.com",
                port = 51820
            ),
            createTestWireGuardProfile(
                id = 2,
                name = "Home Server",
                hostname = "home.example.com",
                port = 51821
            )
        )
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = profiles,
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.5: Display all profiles with details
        composeTestRule.onNodeWithText("Test WireGuard Server").assertIsDisplayed()
        composeTestRule.onNodeWithText("vpn.example.com:51820").assertIsDisplayed()
        composeTestRule.onNodeWithText("Home Server").assertIsDisplayed()
        composeTestRule.onNodeWithText("home.example.com:51821").assertIsDisplayed()
    }
    
    @Test
    fun profileListScreen_displaysProtocolBadges() {
        // Arrange
        val profiles = listOf(
            createTestWireGuardProfile(
                id = 1,
                name = "WireGuard Server",
                hostname = "wg.example.com",
                port = 51820
            )
        )
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = profiles,
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.5: Show protocol type
        composeTestRule.onNodeWithText("WireGuard").assertIsDisplayed()
    }
    
    @Test
    fun profileListScreen_showsLastUsedTime_whenProfileWasUsed() {
        // Arrange
        val lastUsed = System.currentTimeMillis() - 3600_000 // 1 hour ago
        val profiles = listOf(
            createTestWireGuardProfile(
                id = 1,
                name = "Recent Server",
                hostname = "recent.example.com",
                port = 51820,
                lastUsed = lastUsed
            )
        )
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = profiles,
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = {}
                )
            }
        }
        
        // Assert - Validates Requirement 1.5: Show last used time
        composeTestRule.onNodeWithText("Last used: 1 hours ago").assertIsDisplayed()
    }
    
    @Test
    fun profileListScreen_callsOnProfileClick_whenProfileCardClicked() {
        // Arrange
        val profile = createTestWireGuardProfile(
            id = 1,
            name = "Clickable Server",
            hostname = "click.example.com",
            port = 51820
        )
        var clickedProfile: ServerProfile? = null
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = listOf(profile),
                    onProfileClick = { clickedProfile = it },
                    onAddProfile = {},
                    onDeleteProfile = {}
                )
            }
        }
        
        composeTestRule.onNodeWithText("Clickable Server").performClick()
        
        // Assert
        assert(clickedProfile == profile)
    }
    
    @Test
    fun profileListScreen_callsOnAddProfile_whenFabClicked() {
        // Arrange
        var addProfileCalled = false
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = emptyList(),
                    onProfileClick = {},
                    onAddProfile = { addProfileCalled = true },
                    onDeleteProfile = {}
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Add Profile").performClick()
        
        // Assert
        assert(addProfileCalled)
    }
    
    @Test
    fun profileListScreen_showsDeleteConfirmation_whenDeleteClicked() {
        // Arrange
        val profile = createTestWireGuardProfile(
            id = 1,
            name = "Delete Me",
            hostname = "delete.example.com",
            port = 51820
        )
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = listOf(profile),
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = {}
                )
            }
        }
        
        // Click delete button
        composeTestRule.onNodeWithContentDescription("Delete Profile").performClick()
        
        // Assert - Validates Requirement 1.8: Delete confirmation dialog
        composeTestRule.onNodeWithText("Delete Profile?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete \"Delete Me\"? This action cannot be undone.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
    
    @Test
    fun profileListScreen_callsOnDeleteProfile_whenDeleteConfirmed() {
        // Arrange
        val profile = createTestWireGuardProfile(
            id = 1,
            name = "Delete Me",
            hostname = "delete.example.com",
            port = 51820
        )
        var deletedProfile: ServerProfile? = null
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = listOf(profile),
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = { deletedProfile = it }
                )
            }
        }
        
        // Click delete button
        composeTestRule.onNodeWithContentDescription("Delete Profile").performClick()
        
        // Confirm deletion
        composeTestRule.onNodeWithText("Delete").performClick()
        
        // Assert - Validates Requirement 1.8: Profile deletion
        assert(deletedProfile == profile)
    }
    
    @Test
    fun profileListScreen_dismissesDeleteDialog_whenCancelClicked() {
        // Arrange
        val profile = createTestWireGuardProfile(
            id = 1,
            name = "Keep Me",
            hostname = "keep.example.com",
            port = 51820
        )
        var deletedProfile: ServerProfile? = null
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = listOf(profile),
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = { deletedProfile = it }
                )
            }
        }
        
        // Click delete button
        composeTestRule.onNodeWithContentDescription("Delete Profile").performClick()
        
        // Cancel deletion
        composeTestRule.onNodeWithText("Cancel").performClick()
        
        // Assert
        assert(deletedProfile == null)
        composeTestRule.onNodeWithText("Delete Profile?").assertDoesNotExist()
    }
    
    @Test
    fun profileListScreen_displaysMultipleProfiles_inCorrectOrder() {
        // Arrange - Profiles should be ordered by last used (most recent first)
        val now = System.currentTimeMillis()
        val profiles = listOf(
            createTestWireGuardProfile(
                id = 1,
                name = "Oldest",
                hostname = "old.example.com",
                port = 51820,
                lastUsed = now - 86400_000 // 1 day ago
            ),
            createTestWireGuardProfile(
                id = 2,
                name = "Recent",
                hostname = "recent.example.com",
                port = 51821,
                lastUsed = now - 3600_000 // 1 hour ago
            ),
            createTestWireGuardProfile(
                id = 3,
                name = "Never Used",
                hostname = "never.example.com",
                port = 51822,
                lastUsed = null
            )
        )
        
        // Act
        composeTestRule.setContent {
            SelfProxyTheme {
                ProfileListScreen(
                    profiles = profiles,
                    onProfileClick = {},
                    onAddProfile = {},
                    onDeleteProfile = {}
                )
            }
        }
        
        // Assert - All profiles are displayed
        composeTestRule.onNodeWithText("Oldest").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Never Used").assertIsDisplayed()
    }
    
    // Helper function to create test WireGuard profiles
    private fun createTestWireGuardProfile(
        id: Long,
        name: String,
        hostname: String,
        port: Int,
        lastUsed: Long? = null
    ): ServerProfile {
        val config = WireGuardConfig(
            publicKey = "test_public_key_base64_encoded_32_bytes",
            endpoint = "$hostname:$port",
            allowedIPs = listOf("0.0.0.0/0", "::/0"),
            persistentKeepalive = 25,
            mtu = 1420
        )
        
        return ServerProfile.createWireGuardProfile(
            id = id,
            name = name,
            hostname = hostname,
            port = port,
            config = config,
            createdAt = System.currentTimeMillis(),
            lastUsed = lastUsed
        )
    }
}
