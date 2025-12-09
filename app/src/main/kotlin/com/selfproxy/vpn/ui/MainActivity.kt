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
    viewModel: ProfileViewModel = koinViewModel()
) {
    val profiles by viewModel.filteredProfiles.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.ProfileList) }
    var profileToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    
    when (val screen = currentScreen) {
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
                    viewModel.deleteProfile(profile.id)
                }
            )
        }
        is Screen.ProfileForm -> {
            ProfileFormScreen(
                profile = profileToEdit,
                onSave = { profile ->
                    if (profile.id == 0L) {
                        viewModel.createProfile(profile)
                    } else {
                        viewModel.updateProfile(profile)
                    }
                    currentScreen = Screen.ProfileList
                },
                onCancel = {
                    currentScreen = Screen.ProfileList
                }
            )
        }
    }
}

sealed class Screen {
    object ProfileList : Screen()
    object ProfileForm : Screen()
}
