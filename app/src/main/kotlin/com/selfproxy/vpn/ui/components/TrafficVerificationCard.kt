package com.selfproxy.vpn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.selfproxy.vpn.domain.model.TrafficVerificationResult
import com.selfproxy.vpn.domain.model.VerificationState

/**
 * Card displaying traffic verification results.
 * 
 * Shows:
 * - Current external IP address
 * - VPN server IP address
 * - Whether traffic is routed through VPN
 * - DNS leak detection results
 * 
 * Requirements: 8.9, 8.10
 */
@Composable
fun TrafficVerificationCard(
    verificationState: VerificationState,
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Traffic Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (verificationState is VerificationState.Completed) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            }
            
            when (verificationState) {
                is VerificationState.Idle -> {
                    Text(
                        text = "Verify that your traffic is being routed through the VPN and check for DNS leaks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = onVerify,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Verification")
                    }
                }
                
                is VerificationState.Verifying -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Verifying traffic routing...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                is VerificationState.Completed -> {
                    VerificationResults(result = verificationState.result)
                }
                
                is VerificationState.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = verificationState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Button(
                        onClick = onVerify,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationResults(
    result: TrafficVerificationResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overall status
        OverallStatusBanner(result = result)
        
        Divider()
        
        // IP Address Information
        VerificationItem(
            icon = Icons.Default.Public,
            label = "Current IP Address",
            value = result.currentIp ?: "Unknown",
            isGood = result.currentIp != null
        )
        
        if (result.vpnServerIp != null) {
            VerificationItem(
                icon = Icons.Default.VpnKey,
                label = "VPN Server IP",
                value = result.vpnServerIp,
                isGood = true
            )
        }
        
        // VPN Status
        VerificationItem(
            icon = if (result.isUsingVpn) Icons.Default.CheckCircle else Icons.Default.Warning,
            label = "VPN Status",
            value = if (result.isUsingVpn) "Traffic routed through VPN" else "Not using VPN",
            isGood = result.isUsingVpn
        )
        
        // DNS Leak Status
        VerificationItem(
            icon = if (!result.isDnsLeaking) Icons.Default.CheckCircle else Icons.Default.Warning,
            label = "DNS Leak Test",
            value = if (!result.isDnsLeaking) "No DNS leak detected" else "DNS leak detected",
            isGood = !result.isDnsLeaking
        )
        
        // DNS Servers (if available)
        if (result.dnsServers.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "DNS Servers:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                result.dnsServers.forEach { dns ->
                    Text(
                        text = "  • $dns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallStatusBanner(
    result: TrafficVerificationResult,
    modifier: Modifier = Modifier
) {
    val (icon, text, color) = when {
        !result.isSuccess -> Triple(
            Icons.Default.Error,
            "Verification Failed",
            MaterialTheme.colorScheme.error
        )
        result.allTestsPassed -> Triple(
            Icons.Default.CheckCircle,
            "All Tests Passed",
            MaterialTheme.colorScheme.primary
        )
        else -> Triple(
            Icons.Default.Warning,
            "Issues Detected",
            MaterialTheme.colorScheme.error
        )
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun VerificationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
