package com.selfproxy.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selfproxy.vpn.data.model.*
import com.selfproxy.vpn.domain.model.Protocol

/**
 * Profile form screen for creating or editing VPN profiles.
 * 
 * Supports both WireGuard and VLESS protocols with protocol-specific configuration forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormScreen(
    profile: ServerProfile? = null,
    onSave: (ServerProfile) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = profile != null
    
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var selectedProtocol by remember { mutableStateOf(profile?.protocol ?: Protocol.WIREGUARD) }
    var hostname by remember { mutableStateOf(profile?.hostname ?: "") }
    var port by remember { mutableStateOf(profile?.port?.toString() ?: "51820") }
    
    // WireGuard fields
    var wgPublicKey by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().publicKey } ?: "") }
    var wgEndpoint by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().endpoint } ?: "") }
    var wgAllowedIPs by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().allowedIPs.joinToString(", ") } ?: "0.0.0.0/0, ::/0") }
    var wgPersistentKeepalive by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().persistentKeepalive?.toString() } ?: "") }
    var wgMtu by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().mtu.toString() } ?: "1420") }
    
    // VLESS fields
    var vlessTransport by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().transport } ?: TransportProtocol.TCP) }
    var vlessFlowControl by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().flowControl } ?: FlowControl.NONE) }
    var vlessTlsEnabled by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().tlsSettings != null } ?: false) }
    var vlessTlsServerName by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().tlsSettings?.serverName } ?: "") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Profile" else "New Profile") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Basic fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Protocol selection (only for new profiles)
            if (!isEditing) {
                ProtocolSelector(
                    selectedProtocol = selectedProtocol,
                    onProtocolSelected = { selectedProtocol = it }
                )
            }
            
            OutlinedTextField(
                value = hostname,
                onValueChange = { hostname = it },
                label = { Text("Server Hostname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            // Protocol-specific configuration
            when (selectedProtocol) {
                Protocol.WIREGUARD -> {
                    WireGuardConfigForm(
                        publicKey = wgPublicKey,
                        onPublicKeyChange = { wgPublicKey = it },
                        endpoint = wgEndpoint,
                        onEndpointChange = { wgEndpoint = it },
                        allowedIPs = wgAllowedIPs,
                        onAllowedIPsChange = { wgAllowedIPs = it },
                        persistentKeepalive = wgPersistentKeepalive,
                        onPersistentKeepaliveChange = { wgPersistentKeepalive = it },
                        mtu = wgMtu,
                        onMtuChange = { wgMtu = it }
                    )
                }
                Protocol.VLESS -> {
                    VlessConfigForm(
                        transport = vlessTransport,
                        onTransportChange = { vlessTransport = it },
                        flowControl = vlessFlowControl,
                        onFlowControlChange = { vlessFlowControl = it },
                        tlsEnabled = vlessTlsEnabled,
                        onTlsEnabledChange = { vlessTlsEnabled = it },
                        tlsServerName = vlessTlsServerName,
                        onTlsServerNameChange = { vlessTlsServerName = it }
                    )
                }
            }
            
            // Save button
            Button(
                onClick = {
                    try {
                        val portInt = port.toIntOrNull()
                        if (portInt == null || portInt !in 1..65535) {
                            errorMessage = "Port must be between 1 and 65535"
                            return@Button
                        }
                        
                        val newProfile = when (selectedProtocol) {
                            Protocol.WIREGUARD -> {
                                val config = WireGuardConfig(
                                    publicKey = wgPublicKey,
                                    endpoint = wgEndpoint,
                                    allowedIPs = wgAllowedIPs.split(",").map { it.trim() },
                                    persistentKeepalive = wgPersistentKeepalive.toIntOrNull(),
                                    mtu = wgMtu.toIntOrNull() ?: 1420
                                )
                                ServerProfile.createWireGuardProfile(
                                    name = name,
                                    hostname = hostname,
                                    port = portInt,
                                    config = config,
                                    id = profile?.id ?: 0,
                                    createdAt = profile?.createdAt ?: System.currentTimeMillis(),
                                    lastUsed = profile?.lastUsed
                                )
                            }
                            Protocol.VLESS -> {
                                val tlsSettings = if (vlessTlsEnabled && vlessTlsServerName.isNotBlank()) {
                                    TlsSettings(serverName = vlessTlsServerName)
                                } else null
                                
                                val config = VlessConfig(
                                    transport = vlessTransport,
                                    flowControl = vlessFlowControl,
                                    tlsSettings = tlsSettings
                                )
                                ServerProfile.createVlessProfile(
                                    name = name,
                                    hostname = hostname,
                                    port = portInt,
                                    config = config,
                                    id = profile?.id ?: 0,
                                    createdAt = profile?.createdAt ?: System.currentTimeMillis(),
                                    lastUsed = profile?.lastUsed
                                )
                            }
                        }
                        
                        onSave(newProfile)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Invalid configuration"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Save Changes" else "Create Profile")
            }
        }
    }
}

@Composable
private fun ProtocolSelector(
    selectedProtocol: Protocol,
    onProtocolSelected: (Protocol) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Protocol",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProtocolOption(
                protocol = Protocol.WIREGUARD,
                isSelected = selectedProtocol == Protocol.WIREGUARD,
                onClick = { onProtocolSelected(Protocol.WIREGUARD) },
                modifier = Modifier.weight(1f)
            )
            ProtocolOption(
                protocol = Protocol.VLESS,
                isSelected = selectedProtocol == Protocol.VLESS,
                onClick = { onProtocolSelected(Protocol.VLESS) },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Protocol description
        Text(
            text = when (selectedProtocol) {
                Protocol.WIREGUARD -> "Recommended: Fast, efficient, and battery-friendly. Best for most users."
                Protocol.VLESS -> "Advanced: For users requiring obfuscation in restrictive networks."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ProtocolOption(
    protocol: Protocol,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = when (protocol) {
                    Protocol.WIREGUARD -> "WireGuard"
                    Protocol.VLESS -> "VLESS"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun WireGuardConfigForm(
    publicKey: String,
    onPublicKeyChange: (String) -> Unit,
    endpoint: String,
    onEndpointChange: (String) -> Unit,
    allowedIPs: String,
    onAllowedIPsChange: (String) -> Unit,
    persistentKeepalive: String,
    onPersistentKeepaliveChange: (String) -> Unit,
    mtu: String,
    onMtuChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "WireGuard Configuration",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = publicKey,
            onValueChange = onPublicKeyChange,
            label = { Text("Server Public Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = endpoint,
            onValueChange = onEndpointChange,
            label = { Text("Endpoint (hostname:port)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("vpn.example.com:51820") }
        )
        
        OutlinedTextField(
            value = allowedIPs,
            onValueChange = onAllowedIPsChange,
            label = { Text("Allowed IPs") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0.0.0.0/0, ::/0") }
        )
        
        OutlinedTextField(
            value = persistentKeepalive,
            onValueChange = onPersistentKeepaliveChange,
            label = { Text("Persistent Keepalive (seconds, optional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("25") }
        )
        
        OutlinedTextField(
            value = mtu,
            onValueChange = onMtuChange,
            label = { Text("MTU") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}

@Composable
private fun VlessConfigForm(
    transport: TransportProtocol,
    onTransportChange: (TransportProtocol) -> Unit,
    flowControl: FlowControl,
    onFlowControlChange: (FlowControl) -> Unit,
    tlsEnabled: Boolean,
    onTlsEnabledChange: (Boolean) -> Unit,
    tlsServerName: String,
    onTlsServerNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VLESS Configuration",
            style = MaterialTheme.typography.titleMedium
        )
        
        // Transport protocol dropdown
        var transportExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = transportExpanded,
            onExpandedChange = { transportExpanded = it }
        ) {
            OutlinedTextField(
                value = transport.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Transport Protocol") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transportExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = transportExpanded,
                onDismissRequest = { transportExpanded = false }
            ) {
                TransportProtocol.entries.forEach { protocol ->
                    DropdownMenuItem(
                        text = { Text(protocol.name) },
                        onClick = {
                            onTransportChange(protocol)
                            transportExpanded = false
                        }
                    )
                }
            }
        }
        
        // Flow control dropdown
        var flowExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = flowExpanded,
            onExpandedChange = { flowExpanded = it }
        ) {
            OutlinedTextField(
                value = flowControl.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Flow Control") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flowExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = flowExpanded,
                onDismissRequest = { flowExpanded = false }
            ) {
                FlowControl.entries.forEach { flow ->
                    DropdownMenuItem(
                        text = { Text(flow.name) },
                        onClick = {
                            onFlowControlChange(flow)
                            flowExpanded = false
                        }
                    )
                }
            }
        }
        
        // TLS settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable TLS", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = tlsEnabled,
                onCheckedChange = onTlsEnabledChange
            )
        }
        
        if (tlsEnabled) {
            OutlinedTextField(
                value = tlsServerName,
                onValueChange = onTlsServerNameChange,
                label = { Text("TLS Server Name (SNI)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("example.com") }
            )
        }
    }
}
