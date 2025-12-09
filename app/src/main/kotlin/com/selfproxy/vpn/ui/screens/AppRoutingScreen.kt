package com.selfproxy.vpn.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.selfproxy.vpn.data.model.AppRoutingConfig
import com.selfproxy.vpn.data.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App Routing Screen.
 * 
 * Displays list of installed applications and allows users to configure
 * which apps should use the VPN tunnel.
 * 
 * Requirements:
 * - 5.1: Display list of installed applications with names and icons
 * - 5.2, 5.3: Exclude/include apps from VPN tunnel
 * - 5.8: Support "Route All Apps" or "Route Selected Apps Only" modes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoutingScreen(
    apps: List<InstalledApp>,
    config: AppRoutingConfig?,
    searchQuery: String,
    includeSystemApps: Boolean,
    isLoading: Boolean,
    saveSuccess: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleRoutingMode: () -> Unit,
    onToggleAppSelection: (InstalledApp) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onToggleSystemApps: () -> Unit,
    onSave: () -> Unit,
    onClearSaveSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    getAppIcon: suspend (String) -> Drawable?
) {
    // Show save success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            kotlinx.coroutines.delay(2000)
            onClearSaveSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Routing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Toggle system apps
                    IconButton(onClick = onToggleSystemApps) {
                        Icon(
                            if (includeSystemApps) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (includeSystemApps) "Hide system apps" else "Show system apps"
                        )
                    }
                    
                    // Save button
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        snackbarHost = {
            if (saveSuccess) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Configuration saved successfully")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Routing mode selector
            if (config != null) {
                RoutingModeCard(
                    routeAllApps = config.routeAllApps,
                    selectedCount = config.packageNames.size,
                    onToggleMode = onToggleRoutingMode,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select All")
                }
                
                OutlinedButton(
                    onClick = onDeselectAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Deselect All")
                }
            }
            
            Divider()
            
            // App list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No apps found" else "No apps match your search",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            onToggle = { onToggleAppSelection(app) },
                            getAppIcon = getAppIcon
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying the current routing mode and allowing mode toggle.
 */
@Composable
private fun RoutingModeCard(
    routeAllApps: Boolean,
    selectedCount: Int,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (routeAllApps) "Route All Apps" else "Route Selected Apps Only",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (routeAllApps) {
                            if (selectedCount == 0) {
                                "All apps will use the VPN"
                            } else {
                                "$selectedCount app${if (selectedCount == 1) "" else "s"} excluded from VPN"
                            }
                        } else {
                            if (selectedCount == 0) {
                                "No apps will use the VPN"
                            } else {
                                "$selectedCount app${if (selectedCount == 1) "" else "s"} will use the VPN"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Switch(
                    checked = routeAllApps,
                    onCheckedChange = { onToggleMode() }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (routeAllApps) {
                    "Uncheck apps to exclude them from the VPN tunnel"
                } else {
                    "Check apps to route them through the VPN tunnel"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Search bar for filtering apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search apps...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

/**
 * List item for an installed app.
 */
@Composable
private fun AppListItem(
    app: InstalledApp,
    onToggle: () -> Unit,
    getAppIcon: suspend (String) -> Drawable?
) {
    var icon by remember { mutableStateOf<Drawable?>(null) }
    
    LaunchedEffect(app.packageName) {
        icon = withContext(Dispatchers.IO) {
            getAppIcon(app.packageName)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !app.isSelfApp, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        if (icon != null) {
            Image(
                bitmap = icon!!.toBitmap(48, 48).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Icon(
                Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // App name and package
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = if (app.isSelfApp) {
                    "${app.packageName} (This app - always excluded)"
                } else {
                    app.packageName
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Checkbox
        Checkbox(
            checked = app.isSelected,
            onCheckedChange = if (app.isSelfApp) null else { _ -> onToggle() },
            enabled = !app.isSelfApp
        )
    }
}
