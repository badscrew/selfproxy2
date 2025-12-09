package com.selfproxy.vpn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.selfproxy.vpn.domain.manager.ConnectionException
import com.selfproxy.vpn.domain.model.VpnError

/**
 * Error dialog component for displaying VPN errors with diagnostic information.
 * 
 * Requirements:
 * - 3.6: User-friendly error dialogs
 * - 12.8: Diagnostic information display
 */
@Composable
fun ErrorDialog(
    error: ConnectionException,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onViewDiagnostics: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Connection Error",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Error message
                Text(
                    text = error.message ?: "An unknown error occurred",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Suggested action
                val suggestedAction = error.getSuggestedAction()
                if (suggestedAction.isNotEmpty()) {
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Suggestion",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = suggestedAction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (onRetry != null) {
                Button(onClick = {
                    onDismiss()
                    onRetry()
                }) {
                    Text("Retry")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onViewDiagnostics != null) {
                    TextButton(onClick = {
                        onDismiss()
                        onViewDiagnostics()
                    }) {
                        Text("View Details")
                    }
                }
                
                if (onRetry != null) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}

/**
 * Diagnostic details dialog showing full error information.
 * 
 * Requirement 12.8: Diagnostic information collection
 */
@Composable
fun DiagnosticDialog(
    error: ConnectionException,
    onDismiss: () -> Unit,
    onExport: (() -> Unit)? = null
) {
    var showFullReport by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Diagnostic Information",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showFullReport) {
                    // Full diagnostic report
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = error.getDiagnosticReport(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    // Diagnostic info as key-value pairs
                    val diagnosticInfo = error.getDiagnosticInfo()
                    
                    if (diagnosticInfo.isEmpty()) {
                        Text(
                            text = "No diagnostic information available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        diagnosticInfo.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key.replace("_", " ").capitalize(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // Toggle button
                TextButton(
                    onClick = { showFullReport = !showFullReport },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showFullReport) "Show Summary" else "Show Full Report")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (onExport != null) {
                TextButton(onClick = {
                    onDismiss()
                    onExport()
                }) {
                    Text("Export Logs")
                }
            }
        }
    )
}

/**
 * Simple error snackbar for non-critical errors.
 */
@Composable
fun ErrorSnackbar(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Snackbar(
        action = {
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = {
                    onDismiss()
                    onAction()
                }) {
                    Text(actionLabel)
                }
            }
        },
        dismissAction = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    ) {
        Text(message)
    }
}

/**
 * Extension function to capitalize first letter of each word.
 */
private fun String.capitalize(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
