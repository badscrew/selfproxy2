package com.selfproxy.vpn.ui.screens

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
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.ui.components.ProtocolRecommendationsDialog

/**
 * Welcome screen shown to first-time users.
 * 
 * Provides:
 * - Introduction to the app
 * - Protocol selection guidance
 * - Quick setup options for WireGuard and VLESS
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onCreateProfile: (Protocol) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProtocolRecommendations by remember { mutableStateOf(false) }
    
    if (showProtocolRecommendations) {
        ProtocolRecommendationsDialog(
            onDismiss = { showProtocolRecommendations = false },
            onProtocolSelected = { protocol ->
                showProtocolRecommendations = false
                onCreateProfile(protocol)
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to SelfProxy") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // App icon/logo placeholder
            Icon(
                Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Welcome message
            Text(
                text = "Your Privacy, Your Server",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Route your internet traffic through your own VPN server using modern, efficient protocols.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Features
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureItem(
                    icon = Icons.Default.Speed,
                    title = "High Performance",
                    description = "Fast, efficient protocols optimized for mobile"
                )
                
                FeatureItem(
                    icon = Icons.Default.Security,
                    title = "Complete Privacy",
                    description = "No data collection, all credentials encrypted"
                )
                
                FeatureItem(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Friendly",
                    description = "Optimized for all-day protection"
                )
                
                FeatureItem(
                    icon = Icons.Default.Settings,
                    title = "Full Control",
                    description = "Your server, your rules, your privacy"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Protocol selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose Your Protocol",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Select the VPN protocol that best fits your needs:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // WireGuard quick setup (recommended)
                    Button(
                        onClick = { onCreateProfile(Protocol.WIREGUARD) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "WireGuard (Recommended)",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fast, simple, battery-efficient",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                    
                    // VLESS quick setup
                    OutlinedButton(
                        onClick = { onCreateProfile(Protocol.VLESS) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "VLESS (Advanced)",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "For restrictive networks",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                    
                    // Help me choose button
                    TextButton(
                        onClick = { showProtocolRecommendations = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Help me choose the right protocol")
                    }
                }
            }
            
            // Skip button
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
