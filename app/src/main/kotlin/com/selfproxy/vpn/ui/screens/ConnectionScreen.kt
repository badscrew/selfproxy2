package com.selfproxy.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.adapter.ConnectionTestResult
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.ui.theme.*
import java.text.DecimalFormat
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main connection screen showing VPN status and controls.
 * 
 * Requirements: 3.4, 3.5, 7.1, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    connectionState: ConnectionState,
    currentProfile: ServerProfile?,
    statistics: ConnectionStatistics?,
    testResult: ConnectionTestResult?,
    isTesting: Boolean,
    onConnect: (Long) -> Unit,
    onDisconnect: () -> Unit,
    onTestConnection: (Long) -> Unit,
    onResetStatistics: () -> Unit,
    onClearTestResult: () -> Unit,
    onSelectProfile: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    // VPN permission handling
    showPermissionRationale: Boolean = false,
    onDismissPermissionRationale: () -> Unit = {},
    onProceedWithPermission: () -> Unit = {},
    vpnPermissionDenied: Boolean = false,
    onDismissPermissionDenied: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VPN Connection") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            ConnectionStatusCard(
                connectionState = connectionState,
                currentProfile = currentProfile
            )
            
            // Connect/Disconnect Button
            ConnectionButton(
                connectionState = connectionState,
                currentProfile = currentProfile,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onSelectProfile = onSelectProfile
            )
            
            // Statistics Card (only when connected)
            if (connectionState is ConnectionState.Connected && statistics != null) {
                StatisticsCard(
                    statistics = statistics,
                    protocol = currentProfile?.protocol,
                    onReset = onResetStatistics
                )
            }
            
            // Connection Test Section (only when connected)
            if (connectionState is ConnectionState.Connected && currentProfile != null) {
                ConnectionTestSection(
                    profileId = currentProfile.id,
                    testResult = testResult,
                    isTesting = isTesting,
                    onTest = onTestConnection,
                    onClearResult = onClearTestResult
                )
            }
            
            // Error Message (if in error state)
            if (connectionState is ConnectionState.Error) {
                ErrorCard(error = connectionState.error)
            }
        }
        
        // VPN Permission Rationale Dialog
        if (showPermissionRationale) {
            VpnPermissionRationaleDialog(
                onDismiss = onDismissPermissionRationale,
                onProceed = onProceedWithPermission
            )
        }
        
        // VPN Permission Denied Dialog
        if (vpnPermissionDenied) {
            VpnPermissionDeniedDialog(
                onDismiss = onDismissPermissionDenied
            )
        }
    }
}

/**
 * Card showing current connection status.
 * 
 * Requirement 7.1: Display connection status
 */
@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    currentProfile: ServerProfile?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is ConnectionState.Connected -> ConnectedGreen.copy(alpha = 0.1f)
                is ConnectionState.Connecting, is ConnectionState.Reconnecting -> ConnectingOrange.copy(alpha = 0.1f)
                is ConnectionState.Error -> ErrorRed.copy(alpha = 0.1f)
                else -> DisconnectedGray.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Icon
            Icon(
                imageVector = when (connectionState) {
                    is ConnectionState.Connected -> Icons.Default.CheckCircle
                    is ConnectionState.Connecting, is ConnectionState.Reconnecting -> Icons.Default.Refresh
                    is ConnectionState.Error -> Icons.Default.Error
                    else -> Icons.Default.VpnKey
                },
                contentDescription = "Connection Status",
                modifier = Modifier.size(64.dp),
                tint = when (connectionState) {
                    is ConnectionState.Connected -> ConnectedGreen
                    is ConnectionState.Connecting, is ConnectionState.Reconnecting -> ConnectingOrange
                    is ConnectionState.Error -> ErrorRed
                    else -> DisconnectedGray
                }
            )
            
            // Status Text
            Text(
                text = when (connectionState) {
                    is ConnectionState.Connected -> "Connected"
                    is ConnectionState.Connecting -> "Connecting..."
                    is ConnectionState.Reconnecting -> "Reconnecting..."
                    is ConnectionState.Error -> "Connection Failed"
                    else -> "Disconnected"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = when (connectionState) {
                    is ConnectionState.Connected -> ConnectedGreen
                    is ConnectionState.Connecting, is ConnectionState.Reconnecting -> ConnectingOrange
                    is ConnectionState.Error -> ErrorRed
                    else -> DisconnectedGray
                }
            )
            
            // Profile Info
            if (currentProfile != null) {
                Text(
                    text = currentProfile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${currentProfile.hostname}:${currentProfile.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Protocol Badge
                ProtocolBadge(protocol = currentProfile.protocol)
            }
        }
    }
}

/**
 * Protocol badge showing the active protocol.
 * 
 * Requirement 7.6: Display active protocol type
 */
@Composable
private fun ProtocolBadge(protocol: Protocol) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = protocol.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Connect/Disconnect button.
 * 
 * Requirement 3.5: Connect/disconnect button
 */
@Composable
private fun ConnectionButton(
    connectionState: ConnectionState,
    currentProfile: ServerProfile?,
    onConnect: (Long) -> Unit,
    onDisconnect: () -> Unit,
    onSelectProfile: () -> Unit
) {
    when (connectionState) {
        is ConnectionState.Connected -> {
            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnect", style = MaterialTheme.typography.titleMedium)
            }
        }
        is ConnectionState.Connecting, is ConnectionState.Reconnecting -> {
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = false
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (connectionState is ConnectionState.Reconnecting) "Reconnecting..." else "Connecting...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        else -> {
            if (currentProfile != null) {
                Button(
                    onClick = { onConnect(currentProfile.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConnectedGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                OutlinedButton(
                    onClick = onSelectProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Profile", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/**
 * Statistics card showing real-time connection data.
 * 
 * Requirements: 7.3, 7.4, 7.5, 7.7, 7.8
 */
@Composable
private fun StatisticsCard(
    statistics: ConnectionStatistics,
    protocol: Protocol?,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Statistics"
                    )
                }
            }
            
            Divider()
            
            // Connection Duration
            StatisticRow(
                label = "Duration",
                value = formatDuration(statistics.connectionDuration)
            )
            
            // Download Speed
            StatisticRow(
                label = "Download Speed",
                value = formatSpeed(statistics.downloadSpeed),
                icon = Icons.Default.ArrowDownward
            )
            
            // Upload Speed
            StatisticRow(
                label = "Upload Speed",
                value = formatSpeed(statistics.uploadSpeed),
                icon = Icons.Default.ArrowUpward
            )
            
            // Total Downloaded
            StatisticRow(
                label = "Downloaded",
                value = formatBytes(statistics.bytesReceived)
            )
            
            // Total Uploaded
            StatisticRow(
                label = "Uploaded",
                value = formatBytes(statistics.bytesSent)
            )
            
            // Protocol-specific info
            when (protocol) {
                Protocol.WIREGUARD -> {
                    if (statistics.lastHandshakeTime != null) {
                        Divider()
                        StatisticRow(
                            label = "Last Handshake",
                            value = formatTimeSince(statistics.lastHandshakeTime)
                        )
                    }
                }
                Protocol.VLESS -> {
                    if (statistics.latency != null) {
                        Divider()
                        StatisticRow(
                            label = "Latency",
                            value = "${statistics.latency} ms"
                        )
                    }
                }
                null -> {}
            }
        }
    }
}

/**
 * Single statistic row.
 */
@Composable
private fun StatisticRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Connection test section.
 * 
 * Requirement 8.1-8.10: Connection testing
 */
@Composable
private fun ConnectionTestSection(
    profileId: Long,
    testResult: ConnectionTestResult?,
    isTesting: Boolean,
    onTest: (Long) -> Unit,
    onClearResult: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connection Test",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { onTest(profileId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }
            
            // Test Result
            AnimatedVisibility(
                visible = testResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                testResult?.let { result ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = if (result.success) {
                            ConnectedGreen.copy(alpha = 0.1f)
                        } else {
                            ErrorRed.copy(alpha = 0.1f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (result.success) ConnectedGreen else ErrorRed
                                    )
                                    Text(
                                        text = if (result.success) "Test Passed" else "Test Failed",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.success) ConnectedGreen else ErrorRed
                                    )
                                }
                                if (result.success && result.latencyMs != null) {
                                    Text(
                                        text = "Latency: ${result.latencyMs} ms",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                if (!result.success && result.errorMessage != null) {
                                    Text(
                                        text = result.errorMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ErrorRed,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            IconButton(onClick = onClearResult) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Result"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Error card showing connection errors.
 * 
 * Requirement 3.6: Display specific error messages
 */
@Composable
private fun ErrorCard(error: Throwable) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connection Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
                Text(
                    text = error.message ?: "Unknown error occurred",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// Formatting utilities

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${DecimalFormat("#.##").format(bytesPerSecond / 1024.0)} KB/s"
        else -> "${DecimalFormat("#.##").format(bytesPerSecond / (1024.0 * 1024.0))} MB/s"
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${DecimalFormat("#.##").format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${DecimalFormat("#.##").format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

private fun formatDuration(milliseconds: Long): String {
    val duration = milliseconds.milliseconds
    val hours = duration.inWholeHours
    val minutes = (duration.inWholeMinutes % 60)
    val seconds = (duration.inWholeSeconds % 60)
    
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatTimeSince(timestamp: Long): String {
    val elapsed = System.currentTimeMillis() - timestamp
    val seconds = elapsed / 1000
    
    return when {
        seconds < 60 -> "$seconds seconds ago"
        seconds < 3600 -> "${seconds / 60} minutes ago"
        else -> "${seconds / 3600} hours ago"
    }
}

/**
 * Dialog explaining why VPN permission is needed.
 * 
 * Requirement 3.4: Show permission rationale to user
 */
@Composable
private fun VpnPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "VPN Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "SelfProxy needs VPN permission to route your device's internet traffic through your own VPN server.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "This permission allows the app to:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Create a secure VPN tunnel to your server",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Route all traffic through the encrypted tunnel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Protect your privacy and data",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Your data is never collected or shared. You maintain full control over your VPN server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onProceed) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog shown when VPN permission is denied.
 * 
 * Requirement 3.4: Handle permission denial
 */
@Composable
private fun VpnPermissionDeniedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = ErrorRed
            )
        },
        title = {
            Text(
                text = "Permission Denied",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "VPN permission is required to establish a secure connection to your server.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Without this permission, the app cannot create a VPN tunnel or route your traffic.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "To use SelfProxy, please grant VPN permission when prompted.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
