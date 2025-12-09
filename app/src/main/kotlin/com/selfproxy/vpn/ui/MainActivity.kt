package com.selfproxy.vpn.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.ui.screens.ProfileFormScreen
import com.selfproxy.vpn.ui.screens.ProfileListScreen
import com.selfproxy.vpn.ui.theme.SelfProxyTheme
import com.selfproxy.vpn.ui.viewmodel.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Main activity for the SelfProxy VPN application.
 * Entry point for the UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SelfProxyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProfileManagementApp()
                }
            }
        }
    }
}

@Composable
fun ProfileManagementApp(
    profileViewModel: ProfileViewModel = koinViewModel(),
    connectionViewModel: com.selfproxy.vpn.ui.viewmodel.ConnectionViewModel = koinViewModel(),
    settingsViewModel: com.selfproxy.vpn.ui.viewmodel.SettingsViewModel = koinViewModel()
) {
    val profiles by profileViewModel.filteredProfiles.collectAsState()
    val connectionState by connectionViewModel.connectionState.collectAsState()
    val currentProfile by connectionViewModel.currentProfile.collectAsState()
    val statistics by connectionViewModel.statistics.collectAsState()
    val testResult by connectionViewModel.testResult.collectAsState()
    val isTesting by connectionViewModel.isTesting.collectAsState()
    
    val settings by settingsViewModel.settings.collectAsState()
    val validationErrors by settingsViewModel.validationErrors.collectAsState()
    val saveSuccess by settingsViewModel.saveSuccess.collectAsState()
    
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Connection) }
    var profileToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    
    when (val screen = currentScreen) {
        is Screen.Connection -> {
            com.selfproxy.vpn.ui.screens.ConnectionScreen(
                connectionState = connectionState,
                currentProfile = currentProfile,
                statistics = statistics,
                testResult = testResult,
                isTesting = isTesting,
                onConnect = { profileId ->
                    connectionViewModel.connect(profileId)
                },
                onDisconnect = {
                    connectionViewModel.disconnect()
                },
                onTestConnection = { profileId ->
                    connectionViewModel.testConnection(profileId)
                },
                onResetStatistics = {
                    connectionViewModel.resetStatistics()
                },
                onClearTestResult = {
                    connectionViewModel.clearTestResult()
                },
                onSelectProfile = {
                    currentScreen = Screen.ProfileList
                },
                onOpenSettings = {
                    currentScreen = Screen.Settings
                }
            )
        }
        is Screen.ProfileList -> {
            ProfileListScreen(
                profiles = profiles,
                onProfileClick = { profile ->
                    profileToEdit = profile
                    currentScreen = Screen.ProfileForm
                },
                onAddProfile = {
                    profileToEdit = null
                    currentScreen = Screen.ProfileForm
                },
                onDeleteProfile = { profile ->
                    profileViewModel.deleteProfile(profile.id)
                },
                onNavigateToConnection = {
                    currentScreen = Screen.Connection
                }
            )
        }
        is Screen.ProfileForm -> {
            ProfileFormScreen(
                profile = profileToEdit,
                onSave = { profile ->
                    if (profile.id == 0L) {
                        profileViewModel.createProfile(profile)
                    } else {
                        profileViewModel.updateProfile(profile)
                    }
                    currentScreen = Screen.ProfileList
                },
                onCancel = {
                    currentScreen = Screen.ProfileList
                }
            )
        }
        is Screen.Settings -> {
            com.selfproxy.vpn.ui.screens.SettingsScreen(
                settings = settings,
                validationErrors = validationErrors,
                saveSuccess = saveSuccess,
                onUpdateSettings = { newSettings ->
                    settingsViewModel.updateSettings { newSettings }
                },
                onSaveSettings = {
                    settingsViewModel.saveSettings()
                },
                onResetToDefaults = {
                    settingsViewModel.resetToDefaults()
                },
                onClearSaveSuccess = {
                    settingsViewModel.clearSaveSuccess()
                },
                onNavigateBack = {
                    currentScreen = Screen.Connection
                }
            )
        }
    }
}

sealed class Screen {
    object Connection : Screen()
    object ProfileList : Screen()
    object ProfileForm : Screen()
    object Settings : Screen()
}
