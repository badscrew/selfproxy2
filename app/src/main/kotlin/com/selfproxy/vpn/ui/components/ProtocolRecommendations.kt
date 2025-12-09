package com.selfproxy.vpn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.selfproxy.vpn.domain.model.Protocol

/**
 * Protocol recommendations dialog that helps users choose between WireGuard and VLESS.
 * 
 * Displays:
 * - Brief descriptions of each protocol
 * - WireGuard as recommended default
 * - VLESS for advanced obfuscation needs
 * - Advantages and disadvantages
 * - Documentation links
 */
@Composable
fun ProtocolRecommendationsDialog(
    onDismiss: () -> Unit,
    onProtocolSelected: (Protocol) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Your Protocol",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select the VPN protocol that best fits your needs:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // WireGuard recommendation
                ProtocolRecommendationCard(
                    protocol = Protocol.WIREGUARD,
                    isRecommended = true,
                    onSelect = {
                        onProtocolSelected(Protocol.WIREGUARD)
                        onDismiss()
                    }
                )
                
                // VLESS option
                ProtocolRecommendationCard(
                    protocol = Protocol.VLESS,
                    isRecommended = false,
                    onSelect = {
                        onProtocolSelected(Protocol.VLESS)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

/**
 * Card displaying protocol information with advantages, disadvantages, and documentation links.
 */
@Composable
private fun ProtocolRecommendationCard(
    protocol: Protocol,
    isRecommended: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with protocol name and recommended badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (protocol) {
                        Protocol.WIREGUARD -> "WireGuard"
                        Protocol.VLESS -> "VLESS"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecommended) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (isRecommended) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Recommended") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            
            // Description
            Text(
                text = when (protocol) {
                    Protocol.WIREGUARD -> "Modern, high-performance VPN protocol with minimal overhead and superior battery efficiency."
                    Protocol.VLESS -> "Advanced proxy protocol with obfuscation capabilities for restrictive network environments."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRecommended) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            // Use case
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isRecommended) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = when (protocol) {
                        Protocol.WIREGUARD -> "Best for: Most users seeking fast, reliable VPN connections"
                        Protocol.VLESS -> "Best for: Users requiring advanced obfuscation or operating in restrictive networks"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isRecommended) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Divider()
            
            // Advantages
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Advantages:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecommended) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                when (protocol) {
                    Protocol.WIREGUARD -> {
                        AdvantageItem("Exceptional speed and performance", isRecommended)
                        AdvantageItem("Excellent battery efficiency", isRecommended)
                        AdvantageItem("Simple configuration", isRecommended)
                        AdvantageItem("Modern cryptography (Curve25519, ChaCha20)", isRecommended)
                        AdvantageItem("Minimal attack surface", isRecommended)
                    }
                    Protocol.VLESS -> {
                        AdvantageItem("Strong obfuscation capabilities", isRecommended)
                        AdvantageItem("Multiple transport protocols (TCP, WebSocket, gRPC, HTTP/2)", isRecommended)
                        AdvantageItem("Reality protocol support", isRecommended)
                        AdvantageItem("TLS 1.3 encryption", isRecommended)
                        AdvantageItem("Effective in restrictive networks", isRecommended)
                    }
                }
            }
            
            // Disadvantages
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Considerations:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecommended) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                when (protocol) {
                    Protocol.WIREGUARD -> {
                        ConsiderationItem("May be blocked in some restrictive networks", isRecommended)
                        ConsiderationItem("Less obfuscation than VLESS", isRecommended)
                    }
                    Protocol.VLESS -> {
                        ConsiderationItem("More complex configuration", isRecommended)
                        ConsiderationItem("Slightly higher battery usage", isRecommended)
                        ConsiderationItem("Requires more technical knowledge", isRecommended)
                    }
                }
            }
            
            // Documentation link
            TextButton(
                onClick = {
                    val url = when (protocol) {
                        Protocol.WIREGUARD -> "https://www.wireguard.com/"
                        Protocol.VLESS -> "https://xtls.github.io/"
                    }
                    uriHandler.openUri(url)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Learn More")
            }
            
            // Select button
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecommended) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            ) {
                Text(
                    text = when (protocol) {
                        Protocol.WIREGUARD -> "Use WireGuard"
                        Protocol.VLESS -> "Use VLESS"
                    }
                )
            }
        }
    }
}

@Composable
private fun AdvantageItem(
    text: String,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isRecommended) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            }
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isRecommended) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ConsiderationItem(
    text: String,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isRecommended) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isRecommended) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            }
        )
    }
}

/**
 * Compact protocol info card for inline display in forms.
 */
@Composable
fun ProtocolInfoCard(
    protocol: Protocol,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (protocol) {
                    Protocol.WIREGUARD -> Icons.Default.Speed
                    Protocol.VLESS -> Icons.Default.Security
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (protocol) {
                        Protocol.WIREGUARD -> "WireGuard Protocol"
                        Protocol.VLESS -> "VLESS Protocol"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (protocol) {
                        Protocol.WIREGUARD -> "Fast, efficient, battery-friendly"
                        Protocol.VLESS -> "Advanced obfuscation capabilities"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
