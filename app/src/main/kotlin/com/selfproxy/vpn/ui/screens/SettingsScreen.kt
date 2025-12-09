package com.selfproxy.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selfproxy.vpn.data.model.AppSettings
import com.selfproxy.vpn.data.model.FlowControl
import com.selfproxy.vpn.data.model.TransportProtocol

/**
 * Settings screen for configuring application preferences.
 * 
 * Allows users to configure DNS, IPv6, MTU, connection settings,
 * protocol-specific settings, battery optimization, and logging options.
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 11.2, 11.5, 11.6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    validationErrors: List<String>,
    saveSuccess: Boolean,
    batteryOptimizationExempted: Boolean = false,
    batteryState: com.selfproxy.vpn.domain.manager.BatteryState = com.selfproxy.vpn.domain.manager.BatteryState(),
    batteryOptimizationMessage: String = "",
    onUpdateSettings: (AppSettings) -> Unit,
    onSaveSettings: () -> Unit,
    onResetToDefaults: () -> Unit,
    onClearSaveSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAppRouting: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit = {}
) {
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            kotlinx.coroutines.delay(2000)
            onClearSaveSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showResetDialog = true }) {
                        Text("Reset")
                    }
                    TextButton(onClick = onSaveSettings) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show validation errors
            if (validationErrors.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Validation Errors:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        validationErrors.forEach { error ->
                            Text(
                                "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Show success message
            if (saveSuccess) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "Settings saved successfully!",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // DNS Settings Section
            SettingsSection(title = "DNS Configuration") {
                SwitchSetting(
                    title = "Custom DNS Servers",
                    description = "Use custom DNS servers instead of defaults",
                    checked = settings.customDnsEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings(settings.copy(customDnsEnabled = enabled))
                    }
                )
                
                if (settings.customDnsEnabled) {
                    TextFieldSetting(
                        title = "Primary DNS Server",
                        value = settings.primaryDnsServer,
                        onValueChange = { value ->
                            onUpdateSettings(settings.copy(primaryDnsServer = value))
                        },
                        placeholder = "8.8.8.8"
                    )
                    
                    TextFieldSetting(
                        title = "Secondary DNS Server",
                        value = settings.secondaryDnsServer,
                        onValueChange = { value ->
                            onUpdateSettings(settings.copy(secondaryDnsServer = value))
                        },
                        placeholder = "8.8.4.4"
                    )
                }
            }
            
            // IPv6 Settings Section
            SettingsSection(title = "IPv6 Configuration") {
                SwitchSetting(
                    title = "Enable IPv6",
                    description = "Route IPv6 traffic through tunnel (disable to prevent leaks)",
                    checked = settings.ipv6Enabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings(settings.copy(ipv6Enabled = enabled))
                    }
                )
            }
            
            // MTU Settings Section
            SettingsSection(title = "MTU Configuration") {
                NumberFieldSetting(
                    title = "Default MTU",
                    description = "Maximum Transmission Unit (1280-1500 bytes)",
                    value = settings.mtu,
                    onValueChange = { value ->
                        onUpdateSettings(settings.copy(mtu = value))
                    },
                    range = 1280..1500
                )
            }
            
            // App Routing Section
            SettingsSection(title = "App Routing") {
                Button(
                    onClick = onOpenAppRouting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configure Per-App Routing")
                }
                Text(
                    text = "Choose which apps use the VPN tunnel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Connection Settings Section
            SettingsSection(title = "Connection Settings") {
                NumberFieldSetting(
                    title = "Connection Timeout",
                    description = "Timeout for connection attempts (5-120 seconds)",
                    value = settings.connectionTimeout,
                    onValueChange = { value ->
                        onUpdateSettings(settings.copy(connectionTimeout = value))
                    },
                    range = 5..120
                )
                
                NumberFieldSetting(
                    title = "Keep-Alive Interval",
                    description = "Interval for keep-alive packets (0-300 seconds)",
                    value = settings.keepAliveInterval,
                    onValueChange = { value ->
                        onUpdateSettings(settings.copy(keepAliveInterval = value))
                    },
                    range = 0..300
                )
                
                SwitchSetting(
                    title = "Auto-Reconnect",
                    description = "Automatically reconnect on connection drop",
                    checked = settings.autoReconnectEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings(settings.copy(autoReconnectEnabled = enabled))
                    }
                )
                
                if (settings.autoReconnectEnabled) {
                    NumberFieldSetting(
                        title = "Max Reconnection Attempts",
                        description = "Maximum number of reconnection attempts (1-20)",
                        value = settings.reconnectionMaxAttempts,
                        onValueChange = { value ->
                            onUpdateSettings(settings.copy(reconnectionMaxAttempts = value))
                        },
                        range = 1..20
                    )
                }
            }
            
            // WireGuard Settings Section
            SettingsSection(title = "WireGuard Settings") {
                NumberFieldSetting(
                    title = "Persistent Keepalive",
                    description = "Keepalive interval for NAT traversal (0 to disable, 1-65535 seconds)",
                    value = settings.wireGuardPersistentKeepalive,
                    onValueChange = { value ->
                        onUpdateSettings(settings.copy(wireGuardPersistentKeepalive = value))
                    },
                    range = 0..65535
                )
                
                NumberFieldSetting(
                    title = "WireGuard MTU",
                    description = "MTU for WireGuard connections (1280-1500 bytes)",
                    value = settings.wireGuardMtu,
                    onValueChange = { value ->
                        onUpdateSettings(settings.copy(wireGuardMtu = value))
                    },
                    range = 1280..1500
                )
            }
            
            // VLESS Settings Section
            SettingsSection(title = "VLESS Settings") {
                DropdownSetting(
                    title = "Default Transport Protocol",
                    description = "Default transport for new VLESS profiles",
                    value = settings.vlessDefaultTransport,
                    options = TransportProtocol.values().toList(),
                    onValueChange = { value ->
                        onUpdateSettings(settings.copy(vlessDefaultTransport = value))
                    },
                    displayName = { it.name }
                )
                
                DropdownSetting(
                    title = "Default Flow Control",
                    description = "Default flow control for new VLESS profiles",
                    value = settings.vlessDefaultFlowControl,
                    options = FlowControl.values().toList(),
                    onValueChange = { value ->
                        onUpdateSettings(settings.copy(vlessDefaultFlowControl = value))
                    },
                    displayName = { it.name }
                )
            }
            
            // Battery Optimization Section
            SettingsSection(title = "Battery Optimization") {
                // Battery optimization status
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (batteryOptimizationExempted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (batteryOptimizationExempted) {
                                "✓ Battery Optimization Disabled"
                            } else {
                                "⚠ Battery Optimization Enabled"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (batteryOptimizationExempted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        
                        Text(
                            text = batteryOptimizationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (batteryOptimizationExempted) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        
                        if (!batteryOptimizationExempted) {
                            Button(
                                onClick = onRequestBatteryOptimization,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Disable Battery Optimization")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Battery state information
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Battery Status",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Battery Level:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${batteryState.level}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    batteryState.isCriticalBattery -> MaterialTheme.colorScheme.error
                                    batteryState.isLowBattery -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Charging:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (batteryState.isCharging) "Yes" else "No",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Battery Saver:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (batteryState.isBatterySaverMode) "Active" else "Inactive",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (batteryState.isBatterySaverMode) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        
                        if (batteryState.isDozeMode) {
                            Text(
                                text = "⚠ Device is in Doze mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        
                        if (batteryState.isCriticalBattery) {
                            Text(
                                text = "⚠ Critical battery level - VPN may disconnect to save power",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Battery optimization settings help maintain VPN connection during doze mode. " +
                            "Keep-alive intervals are automatically adjusted based on battery level and power save mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Logging Settings Section
            SettingsSection(title = "Logging Options") {
                SwitchSetting(
                    title = "Verbose Logging",
                    description = "Enable detailed logging for debugging",
                    checked = settings.verboseLoggingEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings(settings.copy(verboseLoggingEnabled = enabled))
                    }
                )
                
                SwitchSetting(
                    title = "Log Export",
                    description = "Enable log export functionality",
                    checked = settings.logExportEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings(settings.copy(logExportEnabled = enabled))
                    }
                )
            }
        }
    }
    
    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Are you sure you want to reset all settings to defaults?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun TextFieldSetting(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun NumberFieldSetting(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { intValue ->
                    if (intValue in range) {
                        onValueChange(intValue)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSetting(
    title: String,
    description: String,
    value: T,
    options: List<T>,
    onValueChange: (T) -> Unit,
    displayName: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = displayName(value),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(displayName(option)) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
