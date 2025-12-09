package com.selfproxy.vpn.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.ui.screens.ProfileFormScreen
import com.selfproxy.vpn.ui.screens.ProfileListScreen
import com.selfproxy.vpn.ui.theme.SelfProxyTheme
import com.selfproxy.vpn.ui.viewmodel.ProfileViewModel
import com.selfproxy.vpn.ui.viewmodel.ConnectionViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity for the SelfProxy VPN application.
 * Entry point for the UI.
 * 
 * Handles VPN permission requests as required by Android's VpnService API.
 * 
 * Requirements: 3.4 - VPN permission handling
 */
class MainActivity : ComponentActivity() {
    
    // ViewModel for connection management
    private val connectionViewModel: ConnectionViewModel by viewModel()
    
    // Activity result launcher for VPN permission
    private lateinit var vpnPermissionLauncher: ActivityResultLauncher<Intent>
    
    // Activity result launcher for battery optimization
    private lateinit var batteryOptimizationLauncher: ActivityResultLauncher<Intent>
    
    // ViewModel for settings management
    private val settingsViewModel: com.selfproxy.vpn.ui.viewmodel.SettingsViewModel by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register VPN permission launcher
        vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Permission granted - proceed with connection
                connectionViewModel.onVpnPermissionGranted()
            } else {
                // Permission denied - notify user
                connectionViewModel.onVpnPermissionDenied()
            }
        }
        
        // Register battery optimization launcher
        batteryOptimizationLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            // Result doesn't matter - user has seen the dialog
            // Refresh status when user returns
            settingsViewModel.refreshBatteryOptimizationStatus()
        }
        
        // Set the permission launcher in the view model
        connectionViewModel.setVpnPermissionLauncher { intent ->
            vpnPermissionLauncher.launch(intent)
        }
        
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
    
    /**
     * Requests VPN permission from the user.
     * 
     * This method checks if VPN permission is already granted.
     * If not, it launches the system VPN permission dialog.
     * 
     * Requirement 3.4: Request VPN permission using VpnService.prepare()
     * 
     * @return true if permission is already granted, false if permission dialog was shown
     */
    fun requestVpnPermission(): Boolean {
        val intent = VpnService.prepare(this)
        return if (intent != null) {
            // Permission not granted - show permission dialog
            vpnPermissionLauncher.launch(intent)
            false
        } else {
            // Permission already granted
            true
        }
    }
    
    /**
     * Requests battery optimization exemption from the user.
     * 
     * Launches the system settings to allow the user to disable battery optimization
     * for this app, which helps maintain VPN connection during doze mode.
     * 
     * Requirement 11.2: Request battery optimization exemption
     */
    fun requestBatteryOptimizationExemption(intent: Intent?) {
        intent?.let {
            batteryOptimizationLauncher.launch(it)
        }
    }
}

@Composable
fun ProfileManagementApp(
    profileViewModel: ProfileViewModel = koinViewModel(),
    connectionViewModel: com.selfproxy.vpn.ui.viewmodel.ConnectionViewModel = koinViewModel(),
    settingsViewModel: com.selfproxy.vpn.ui.viewmodel.SettingsViewModel = koinViewModel(),
    appRoutingViewModel: com.selfproxy.vpn.ui.viewmodel.AppRoutingViewModel = koinViewModel()
) {
    val profiles by profileViewModel.filteredProfiles.collectAsState()
    val connectionState by connectionViewModel.connectionState.collectAsState()
    val currentProfile by connectionViewModel.currentProfile.collectAsState()
    val statistics by connectionViewModel.statistics.collectAsState()
    val testResult by connectionViewModel.testResult.collectAsState()
    val isTesting by connectionViewModel.isTesting.collectAsState()
    
    // VPN permission state
    val vpnPermissionState by connectionViewModel.vpnPermissionState.collectAsState()
    val showPermissionRationale by connectionViewModel.showPermissionRationale.collectAsState()
    
    val settings by settingsViewModel.settings.collectAsState()
    val validationErrors by settingsViewModel.validationErrors.collectAsState()
    val saveSuccess by settingsViewModel.saveSuccess.collectAsState()
    val batteryOptimizationExempted by settingsViewModel.batteryOptimizationExempted.collectAsState()
    val batteryState by settingsViewModel.batteryState.collectAsState()
    
    val filteredApps by appRoutingViewModel.filteredApps.collectAsState()
    val routingConfig by appRoutingViewModel.config.collectAsState()
    val searchQuery by appRoutingViewModel.searchQuery.collectAsState()
    val includeSystemApps by appRoutingViewModel.includeSystemApps.collectAsState()
    val isLoadingApps by appRoutingViewModel.isLoading.collectAsState()
    val routingSaveSuccess by appRoutingViewModel.saveSuccess.collectAsState()
    
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Connection) }
    var profileToEdit by remember { mutableStateOf<ServerProfile?>(null) }
    
    val context = LocalContext.current
    
    // Handle VPN permission state changes
    LaunchedEffect(vpnPermissionState) {
        when (vpnPermissionState) {
            is com.selfproxy.vpn.ui.viewmodel.VpnPermissionState.Requesting -> {
                // Check if permission is already granted
                val intent = android.net.VpnService.prepare(context)
                if (intent == null) {
                    // Permission already granted
                    connectionViewModel.onVpnPermissionGranted()
                } else {
                    // Show rationale before requesting permission
                    connectionViewModel.requestVpnPermissionWithRationale()
                }
            }
            else -> {
                // Other states are handled by dialogs
            }
        }
    }
    
    when (currentScreen) {
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
                },
                // VPN permission handling
                showPermissionRationale = showPermissionRationale,
                onDismissPermissionRationale = {
                    connectionViewModel.dismissPermissionRationale()
                    connectionViewModel.clearVpnPermissionState()
                },
                onProceedWithPermission = {
                    connectionViewModel.proceedWithPermissionRequest()
                    // Launch the VPN permission request
                    val intent = android.net.VpnService.prepare(context)
                    if (intent != null) {
                        (context as? MainActivity)?.requestVpnPermission()
                    } else {
                        // Permission already granted
                        connectionViewModel.onVpnPermissionGranted()
                    }
                },
                vpnPermissionDenied = vpnPermissionState is com.selfproxy.vpn.ui.viewmodel.VpnPermissionState.Denied,
                onDismissPermissionDenied = {
                    connectionViewModel.clearVpnPermissionState()
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
                batteryOptimizationExempted = batteryOptimizationExempted,
                batteryState = batteryState,
                batteryOptimizationMessage = settingsViewModel.getBatteryOptimizationMessage(),
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
                },
                onOpenAppRouting = {
                    appRoutingViewModel.loadConfig(null)
                    currentScreen = Screen.AppRouting
                },
                onRequestBatteryOptimization = {
                    // Request battery optimization exemption
                    val batteryOptManager = settingsViewModel.getBatteryOptimizationManager()
                    val intent = batteryOptManager?.createBatteryOptimizationExemptionIntent()
                    (context as? MainActivity)?.requestBatteryOptimizationExemption(intent)
                }
            )
        }
        is Screen.AppRouting -> {
            com.selfproxy.vpn.ui.screens.AppRoutingScreen(
                apps = filteredApps,
                config = routingConfig,
                searchQuery = searchQuery,
                includeSystemApps = includeSystemApps,
                isLoading = isLoadingApps,
                saveSuccess = routingSaveSuccess,
                onSearchQueryChange = { query ->
                    appRoutingViewModel.updateSearchQuery(query)
                },
                onToggleRoutingMode = {
                    appRoutingViewModel.toggleRoutingMode()
                },
                onToggleAppSelection = { app ->
                    appRoutingViewModel.toggleAppSelection(app)
                },
                onSelectAll = {
                    appRoutingViewModel.selectAll()
                },
                onDeselectAll = {
                    appRoutingViewModel.deselectAll()
                },
                onToggleSystemApps = {
                    appRoutingViewModel.toggleIncludeSystemApps()
                },
                onSave = {
                    appRoutingViewModel.saveConfig()
                },
                onClearSaveSuccess = {
                    appRoutingViewModel.clearSaveSuccess()
                },
                onNavigateBack = {
                    currentScreen = Screen.Settings
                },
                getAppIcon = { packageName ->
                    appRoutingViewModel.getAppIcon(packageName)
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
    object AppRouting : Screen()
}
