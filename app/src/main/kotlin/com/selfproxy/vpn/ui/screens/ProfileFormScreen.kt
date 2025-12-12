package com.selfproxy.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selfproxy.vpn.data.model.*
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.ui.components.ProtocolInfoCard
import com.selfproxy.vpn.ui.components.ProtocolRecommendationsDialog

/**
 * Profile form screen for creating or editing VPN profiles.
 * 
 * Supports both WireGuard and VLESS protocols with protocol-specific configuration forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormScreen(
    profile: ServerProfile? = null,
    initialProtocol: Protocol? = null,
    onSave: (ServerProfile, String?) -> Unit, // Added presharedKey parameter
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = profile != null
    
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var selectedProtocol by remember { mutableStateOf(profile?.protocol ?: initialProtocol ?: Protocol.WIREGUARD) }
    var hostname by remember { mutableStateOf(profile?.hostname ?: "") }
    var port by remember { mutableStateOf(profile?.port?.toString() ?: "51820") }
    
    // WireGuard fields
    var wgPublicKey by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().publicKey } ?: "") }
    var wgPresharedKey by remember { mutableStateOf("") } // Will be loaded from credential store
    var wgEndpoint by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().endpoint } ?: "") }
    var wgAllowedIPs by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().allowedIPs.joinToString(", ") } ?: "0.0.0.0/0, ::/0") }
    var wgPersistentKeepalive by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().persistentKeepalive?.toString() } ?: "") }
    var wgMtu by remember { mutableStateOf(profile?.wireGuardConfigJson?.let { profile.getWireGuardConfig().mtu.toString() } ?: "1420") }
    
    // VLESS fields
    var vlessUuid by remember { mutableStateOf("") } // Will be loaded from credential store for editing
    var vlessTransport by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().transport } ?: TransportProtocol.TCP) }
    var vlessFlowControl by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().flowControl } ?: FlowControl.NONE) }
    
    // Security type: none, tls, or reality
    var vlessSecurityType by remember { 
        mutableStateOf(
            when {
                profile?.vlessConfigJson?.let { profile.getVlessConfig().realitySettings != null } == true -> "reality"
                profile?.vlessConfigJson?.let { profile.getVlessConfig().tlsSettings != null } == true -> "tls"
                else -> "none"
            }
        )
    }
    
    // TLS settings
    var vlessTlsServerName by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().tlsSettings?.serverName } ?: "") }
    var vlessTlsAllowInsecure by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().tlsSettings?.allowInsecure } ?: false) }
    var vlessTlsFingerprint by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().tlsSettings?.fingerprint } ?: "") }
    
    // Reality settings
    var vlessRealityServerName by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().realitySettings?.serverName } ?: "") }
    var vlessRealityPublicKey by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().realitySettings?.publicKey } ?: "") }
    var vlessRealityShortId by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().realitySettings?.shortId } ?: "") }
    var vlessRealitySpiderX by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().realitySettings?.spiderX } ?: "") }
    var vlessRealityFingerprint by remember { mutableStateOf(profile?.vlessConfigJson?.let { profile.getVlessConfig().realitySettings?.fingerprint } ?: "chrome") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showProtocolRecommendations by remember { mutableStateOf(false) }
    
    // Show protocol recommendations dialog for new profiles
    if (showProtocolRecommendations) {
        ProtocolRecommendationsDialog(
            onDismiss = { showProtocolRecommendations = false },
            onProtocolSelected = { protocol ->
                selectedProtocol = protocol
                showProtocolRecommendations = false
            }
        )
    }
    
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
                    onProtocolSelected = { selectedProtocol = it },
                    onShowRecommendations = { showProtocolRecommendations = true }
                )
            } else {
                // Show protocol info for existing profiles
                ProtocolInfoCard(protocol = selectedProtocol)
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
                        presharedKey = wgPresharedKey,
                        onPresharedKeyChange = { wgPresharedKey = it },
                        endpoint = wgEndpoint,
                        onEndpointChange = { wgEndpoint = it },
                        allowedIPs = wgAllowedIPs,
                        onAllowedIPsChange = { wgAllowedIPs = it },
                        persistentKeepalive = wgPersistentKeepalive,
                        onPersistentKeepaliveChange = { wgPersistentKeepalive = it },
                        mtu = wgMtu,
                        onMtuChange = { wgMtu = it },
                        isEditing = isEditing
                    )
                }
                Protocol.VLESS -> {
                    VlessConfigForm(
                        uuid = vlessUuid,
                        onUuidChange = { vlessUuid = it },
                        transport = vlessTransport,
                        onTransportChange = { vlessTransport = it },
                        flowControl = vlessFlowControl,
                        onFlowControlChange = { vlessFlowControl = it },
                        securityType = vlessSecurityType,
                        onSecurityTypeChange = { vlessSecurityType = it },
                        tlsServerName = vlessTlsServerName,
                        onTlsServerNameChange = { vlessTlsServerName = it },
                        tlsAllowInsecure = vlessTlsAllowInsecure,
                        onTlsAllowInsecureChange = { vlessTlsAllowInsecure = it },
                        tlsFingerprint = vlessTlsFingerprint,
                        onTlsFingerprintChange = { vlessTlsFingerprint = it },
                        realityServerName = vlessRealityServerName,
                        onRealityServerNameChange = { vlessRealityServerName = it },
                        realityPublicKey = vlessRealityPublicKey,
                        onRealityPublicKeyChange = { vlessRealityPublicKey = it },
                        realityShortId = vlessRealityShortId,
                        onRealityShortIdChange = { vlessRealityShortId = it },
                        realitySpiderX = vlessRealitySpiderX,
                        onRealitySpiderXChange = { vlessRealitySpiderX = it },
                        realityFingerprint = vlessRealityFingerprint,
                        onRealityFingerprintChange = { vlessRealityFingerprint = it },
                        isEditing = isEditing
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
                                // Validate UUID
                                if (vlessUuid.isBlank()) {
                                    errorMessage = "UUID is required for VLESS"
                                    return@Button
                                }
                                
                                // Build TLS settings if selected
                                val tlsSettings = if (vlessSecurityType == "tls") {
                                    if (vlessTlsServerName.isBlank()) {
                                        errorMessage = "TLS Server Name (SNI) is required when TLS is enabled"
                                        return@Button
                                    }
                                    TlsSettings(
                                        serverName = vlessTlsServerName,
                                        allowInsecure = vlessTlsAllowInsecure,
                                        fingerprint = vlessTlsFingerprint.takeIf { it.isNotBlank() }
                                    )
                                } else null
                                
                                // Build Reality settings if selected
                                val realitySettings = if (vlessSecurityType == "reality") {
                                    if (vlessRealityServerName.isBlank()) {
                                        errorMessage = "Reality Server Name (SNI) is required"
                                        return@Button
                                    }
                                    if (vlessRealityPublicKey.isBlank()) {
                                        errorMessage = "Reality Public Key is required"
                                        return@Button
                                    }
                                    if (vlessRealityShortId.isBlank()) {
                                        errorMessage = "Reality Short ID is required"
                                        return@Button
                                    }
                                    RealitySettings(
                                        serverName = vlessRealityServerName,
                                        publicKey = vlessRealityPublicKey,
                                        shortId = vlessRealityShortId,
                                        spiderX = vlessRealitySpiderX.takeIf { it.isNotBlank() },
                                        fingerprint = vlessRealityFingerprint.takeIf { it.isNotBlank() }
                                    )
                                } else null
                                
                                val config = VlessConfig(
                                    transport = vlessTransport,
                                    flowControl = vlessFlowControl,
                                    tlsSettings = tlsSettings,
                                    realitySettings = realitySettings
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
                        
                        // Pass credential (preshared key for WireGuard, UUID for VLESS)
                        val credentialToSave = when (selectedProtocol) {
                            Protocol.WIREGUARD -> if (wgPresharedKey.isNotBlank()) wgPresharedKey else null
                            Protocol.VLESS -> if (vlessUuid.isNotBlank()) vlessUuid else null
                        }
                        
                        onSave(newProfile, credentialToSave)
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
    onShowRecommendations: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Protocol",
                style = MaterialTheme.typography.titleMedium
            )
            
            TextButton(
                onClick = onShowRecommendations,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = "Protocol recommendations",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Help me choose", style = MaterialTheme.typography.labelMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
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
                Protocol.WIREGUARD -> "⭐ Recommended: Fast, efficient, and battery-friendly. Best for most users."
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
    presharedKey: String,
    onPresharedKeyChange: (String) -> Unit,
    endpoint: String,
    onEndpointChange: (String) -> Unit,
    allowedIPs: String,
    onAllowedIPsChange: (String) -> Unit,
    persistentKeepalive: String,
    onPersistentKeepaliveChange: (String) -> Unit,
    mtu: String,
    onMtuChange: (String) -> Unit,
    isEditing: Boolean = false,
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
        
        // Client Key Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Client Keys",
                    style = MaterialTheme.typography.titleSmall
                )
                if (isEditing) {
                    Text(
                        text = "Your client keys were generated when this profile was created. The private key is stored securely on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "A new key pair will be generated automatically when you save this profile. You'll need to provide your public key to the server administrator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = publicKey,
            onValueChange = onPublicKeyChange,
            label = { Text("Server Public Key") },
            supportingText = { Text("Enter the public key provided by your WireGuard server administrator") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = presharedKey,
            onValueChange = onPresharedKeyChange,
            label = { Text("Preshared Key (Optional)") },
            supportingText = { Text("Enter the preshared key if your server requires one for additional security") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Leave empty if not required") }
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
    uuid: String,
    onUuidChange: (String) -> Unit,
    transport: TransportProtocol,
    onTransportChange: (TransportProtocol) -> Unit,
    flowControl: FlowControl,
    onFlowControlChange: (FlowControl) -> Unit,
    securityType: String,
    onSecurityTypeChange: (String) -> Unit,
    tlsServerName: String,
    onTlsServerNameChange: (String) -> Unit,
    tlsAllowInsecure: Boolean,
    onTlsAllowInsecureChange: (Boolean) -> Unit,
    tlsFingerprint: String,
    onTlsFingerprintChange: (String) -> Unit,
    realityServerName: String,
    onRealityServerNameChange: (String) -> Unit,
    realityPublicKey: String,
    onRealityPublicKeyChange: (String) -> Unit,
    realityShortId: String,
    onRealityShortIdChange: (String) -> Unit,
    realitySpiderX: String,
    onRealitySpiderXChange: (String) -> Unit,
    realityFingerprint: String,
    onRealityFingerprintChange: (String) -> Unit,
    isEditing: Boolean = false,
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
        
        // UUID field
        OutlinedTextField(
            value = uuid,
            onValueChange = onUuidChange,
            label = { Text("UUID") },
            supportingText = { Text("Your VLESS client UUID (provided by server administrator)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("a1b2c3d4-e5f6-7890-abcd-ef1234567890") }
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
        
        // Security type selector
        Text(
            text = "Security",
            style = MaterialTheme.typography.titleSmall
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = securityType == "none",
                onClick = { onSecurityTypeChange("none") },
                label = { Text("None") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = securityType == "tls",
                onClick = { onSecurityTypeChange("tls") },
                label = { Text("TLS") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = securityType == "reality",
                onClick = { onSecurityTypeChange("reality") },
                label = { Text("Reality") },
                modifier = Modifier.weight(1f)
            )
        }
        
        // TLS settings
        if (securityType == "tls") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TLS Settings",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    OutlinedTextField(
                        value = tlsServerName,
                        onValueChange = onTlsServerNameChange,
                        label = { Text("Server Name (SNI) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("example.com") }
                    )
                    
                    OutlinedTextField(
                        value = tlsFingerprint,
                        onValueChange = onTlsFingerprintChange,
                        label = { Text("Fingerprint (Optional)") },
                        supportingText = { Text("e.g., chrome, firefox, safari") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("chrome") }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Insecure", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = tlsAllowInsecure,
                            onCheckedChange = onTlsAllowInsecureChange
                        )
                    }
                }
            }
        }
        
        // Reality settings
        if (securityType == "reality") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Reality Settings",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Text(
                        text = "Reality protocol provides advanced obfuscation for restrictive networks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = realityServerName,
                        onValueChange = onRealityServerNameChange,
                        label = { Text("Server Name (SNI) *") },
                        supportingText = { Text("Domain to mimic (e.g., www.microsoft.com)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("www.microsoft.com") }
                    )
                    
                    OutlinedTextField(
                        value = realityPublicKey,
                        onValueChange = onRealityPublicKeyChange,
                        label = { Text("Public Key *") },
                        supportingText = { Text("Reality public key from server") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Base64 encoded public key") }
                    )
                    
                    OutlinedTextField(
                        value = realityShortId,
                        onValueChange = onRealityShortIdChange,
                        label = { Text("Short ID *") },
                        supportingText = { Text("Reality short ID from server") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("0123456789abcdef") }
                    )
                    
                    OutlinedTextField(
                        value = realityFingerprint,
                        onValueChange = onRealityFingerprintChange,
                        label = { Text("Fingerprint") },
                        supportingText = { Text("TLS fingerprint (default: chrome)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("chrome") }
                    )
                    
                    OutlinedTextField(
                        value = realitySpiderX,
                        onValueChange = onRealitySpiderXChange,
                        label = { Text("Spider X (Optional)") },
                        supportingText = { Text("Advanced parameter, leave empty if unsure") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Leave empty") }
                    )
                }
            }
        }
    }
}
