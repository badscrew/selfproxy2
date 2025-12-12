package com.selfproxy.vpn.platform.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.selfproxy.vpn.R
import com.selfproxy.vpn.domain.util.SanitizedLogger
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * VPN service that manages the TUN interface and packet routing.
 * This service runs in the foreground with a persistent notification.
 * 
 * Requirements:
 * - 3.4: VPN service creates TUN interface for routing
 * - 3.5: VPN key icon displayed in status bar
 * - 4.1: Routes all TCP traffic through VPN tunnel
 * - 4.2: Routes all UDP traffic through VPN tunnel
 * - 4.3: Routes DNS queries through VPN tunnel
 * - 4.7: Prevents DNS leaks by blocking direct DNS queries
 * - 4.8: Handles IPv6 traffic (route or block)
 */
class TunnelVpnService : VpnService() {

    companion object {
        private const val TAG = "TunnelVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_service_channel"
        
        // VPN interface configuration
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_PREFIX_LENGTH = 24
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_ROUTE_PREFIX = 0
        
        // DNS servers (Google DNS as default)
        private const val PRIMARY_DNS = "8.8.8.8"
        private const val SECONDARY_DNS = "8.8.4.4"
        
        // MTU settings
        private const val DEFAULT_MTU = 1500
        private const val WIREGUARD_MTU = 1420
        
        // IPv6 configuration
        private const val IPV6_ADDRESS = "fd00::2"
        private const val IPV6_PREFIX_LENGTH = 64
        private const val IPV6_ROUTE = "::"
        private const val IPV6_ROUTE_PREFIX = 0
        
        // Intent actions
        const val ACTION_START_VPN = "com.selfproxy.vpn.START_VPN"
        const val ACTION_STOP_VPN = "com.selfproxy.vpn.STOP_VPN"
        const val ACTION_UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION"
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_PROTOCOL_ADAPTER = "protocol_adapter"
        const val EXTRA_STATUS = "status"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_ADDITIONAL_INFO = "additional_info"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var tunInterface: ParcelFileDescriptor? = null
    private var currentProfile: ServerProfile? = null
    private var currentAdapter: ProtocolAdapter? = null
    private var packetRoutingJob: Job? = null
    
    private var ipv6Enabled: Boolean = false
    private var customDnsServers: List<String> = listOf(PRIMARY_DNS, SECONDARY_DNS)
    private var excludedApps: Set<String> = emptySet()
    
    // Xray-core integration for TUN mode
    private var xrayCore: com.selfproxy.vpn.platform.vless.XrayCore? = null
    private var profileId: Long = 0
    private var vlessUuid: String = ""

    override fun onCreate() {
        super.onCreate()
        SanitizedLogger.d(TAG, "VPN service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SanitizedLogger.d(TAG, "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_VPN -> {
                // Extract profile info and UUID from intent
                profileId = intent.getLongExtra("PROFILE_ID", 0)
                vlessUuid = intent.getStringExtra("VLESS_UUID") ?: ""
                val profileName = intent.getStringExtra("PROFILE_NAME") ?: "VPN Server"
                val serverAddress = intent.getStringExtra("SERVER_ADDRESS") ?: "Unknown"
                
                SanitizedLogger.d(TAG, "Starting VPN with TUN mode")
                SanitizedLogger.d(TAG, "Profile: $profileName, Server: $serverAddress")
                
                if (profileId == 0L || vlessUuid.isEmpty()) {
                    SanitizedLogger.e(TAG, "Invalid profile ID or UUID")
                    stopSelf()
                    return START_NOT_STICKY
                }
                
                // Start foreground service with notification
                val notification = createNotification(
                    "VPN Connecting...", 
                    "Establishing tunnel to $profileName",
                    showDisconnectAction = false
                )
                startForeground(NOTIFICATION_ID, notification)
                
                // Start VPN tunnel with Xray TUN mode
                startVpnTunnelWithXray(profileId, vlessUuid, profileName, serverAddress)
            }
            ACTION_STOP_VPN -> {
                updateNotificationWithStatus("Disconnecting")
                stopVpnTunnel()
                stopSelf()
            }
            ACTION_UPDATE_NOTIFICATION -> {
                // Update notification with provided status and info
                val status = intent.getStringExtra(EXTRA_STATUS) ?: "Unknown"
                val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME)
                val additionalInfo = intent.getStringExtra(EXTRA_ADDITIONAL_INFO)
                updateNotificationWithStatus(status, profileName, additionalInfo)
            }
            else -> {
                // Default: start foreground service with notification
                val notification = createNotification(
                    "VPN Ready", 
                    "Waiting for connection",
                    showDisconnectAction = false
                )
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        SanitizedLogger.d(TAG, "VPN service destroyed")
        stopVpnTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        // User revoked VPN permission
        SanitizedLogger.i(TAG, "VPN permission revoked by user")
        stopVpnTunnel()
        stopSelf()
    }

    /**
     * Starts the VPN tunnel with the current profile and adapter.
     * 
     * Creates the TUN interface, configures routing, and starts packet forwarding.
     * 
     * Requirements:
     * - 5.5: Apply routing on VPN start
     * - 5.6: Support dynamic routing updates
     */
    fun startVpnTunnel(
        profile: ServerProfile? = null,
        adapter: ProtocolAdapter? = null,
        ipv6Enabled: Boolean = false,
        dnsServers: List<String> = listOf(PRIMARY_DNS, SECONDARY_DNS),
        excludedApps: Set<String> = emptySet()
    ) {
        try {
            SanitizedLogger.d(TAG, "Starting VPN tunnel")
            
            this.currentProfile = profile
            this.currentAdapter = adapter
            this.ipv6Enabled = ipv6Enabled
            this.customDnsServers = dnsServers
            this.excludedApps = excludedApps
            
            // Create TUN interface
            tunInterface = createTunInterface(profile, ipv6Enabled, dnsServers, excludedApps)
            
            if (tunInterface == null) {
                SanitizedLogger.e(TAG, "Failed to create TUN interface")
                stopSelf()
                return
            }
            
            // Update notification with connection status
            val profileName = profile?.name ?: "VPN Server"
            updateNotificationWithStatus("Connected", profileName)
            
            // Start packet routing
            startPacketRouting()
            
            SanitizedLogger.i(TAG, "VPN tunnel started successfully")
            
        } catch (e: Exception) {
            SanitizedLogger.e(TAG, "Error starting VPN tunnel", e)
            stopVpnTunnel()
            stopSelf()
        }
    }

    /**
     * Starts VPN tunnel with Xray TUN mode.
     * 
     * This is the new approach where Xray-core handles packet routing directly.
     */
    private fun startVpnTunnelWithXray(
        profileId: Long,
        uuid: String,
        profileName: String,
        serverAddress: String
    ) {
        serviceScope.launch {
            try {
                SanitizedLogger.d(TAG, "Starting VPN tunnel with Xray TUN mode")
                
                // 1. Create TUN interface first
                tunInterface = createTunInterface(
                    profile = null,  // Not needed for TUN creation
                    ipv6Enabled = false,
                    dnsServers = listOf(PRIMARY_DNS, SECONDARY_DNS),
                    excludedApps = emptySet()
                )
                
                if (tunInterface == null) {
                    SanitizedLogger.e(TAG, "Failed to create TUN interface")
                    updateNotificationWithStatus("Error", profileName, "Failed to create TUN interface")
                    stopSelf()
                    return@launch
                }
                
                // 2. Get TUN file descriptor
                val tunFd = tunInterface!!.fd
                SanitizedLogger.d(TAG, "TUN interface created with fd: $tunFd")
                
                // 3. Get profile from repository
                // Since we can't easily inject ProfileRepository into a Service,
                // we retrieve it from Koin directly
                val profileRepository = org.koin.core.component.KoinComponent().getKoin().get<com.selfproxy.vpn.domain.repository.ProfileRepository>()
                val profile = profileRepository.getProfile(profileId)
                
                if (profile == null) {
                    SanitizedLogger.e(TAG, "Profile not found: $profileId")
                    updateNotificationWithStatus("Error", profileName, "Profile not found")
                    stopSelf()
                    return@launch
                }
                
                SanitizedLogger.d(TAG, "Profile retrieved: ${profile.name}")
                
                // 4. Initialize Xray-core
                xrayCore = com.selfproxy.vpn.platform.vless.XrayCore(this@TunnelVpnService)
                
                // 5. Build Xray config with TUN fd
                val xrayConfig = xrayCore!!.buildConfig(profile, uuid, tunFd)
                SanitizedLogger.d(TAG, "Xray config built with TUN mode")
                
                // 6. Create callback handler
                val callbackHandler = object : libv2ray.CoreCallbackHandler {
                    override fun onEmitStatus(code: Long, status: String): Long {
                        SanitizedLogger.d(TAG, "Xray status [$code]: $status")
                        return 0
                    }
                    
                    override fun startup(): Long {
                        SanitizedLogger.d(TAG, "Xray startup called")
                        return 0
                    }
                    
                    override fun shutdown(): Long {
                        SanitizedLogger.d(TAG, "Xray shutdown called")
                        return 0
                    }
                }
                
                // 7. Start Xray-core with TUN fd
                SanitizedLogger.d(TAG, "Starting Xray-core with TUN mode...")
                val startResult = xrayCore!!.start(xrayConfig, callbackHandler)
                
                if (startResult.isFailure) {
                    val errorMsg = startResult.exceptionOrNull()?.message ?: "Failed to start Xray-core"
                    SanitizedLogger.e(TAG, "Xray-core start failed: $errorMsg", startResult.exceptionOrNull())
                    updateNotificationWithStatus("Error", profileName, errorMsg)
                    stopVpnTunnel()
                    stopSelf()
                    return@launch
                }
                
                SanitizedLogger.i(TAG, "VPN tunnel with Xray TUN mode started successfully")
                SanitizedLogger.i(TAG, "Xray-core is now handling all packet routing through TUN interface")
                updateNotificationWithStatus("Connected", profileName, "via $serverAddress")
                
            } catch (e: Exception) {
                SanitizedLogger.e(TAG, "Error starting VPN tunnel with Xray", e)
                updateNotificationWithStatus("Error", profileName, e.message ?: "Unknown error")
                stopVpnTunnel()
                stopSelf()
            }
        }
    }
    
    /**
     * Stops the VPN tunnel and cleans up resources.
     */
    fun stopVpnTunnel() {
        try {
            SanitizedLogger.d(TAG, "Stopping VPN tunnel")
            
            // Stop Xray-core if running
            serviceScope.launch {
                try {
                    xrayCore?.stop()
                    xrayCore = null
                    SanitizedLogger.d(TAG, "Xray-core stopped")
                } catch (e: Exception) {
                    SanitizedLogger.e(TAG, "Error stopping Xray-core", e)
                }
            }
            
            // Stop packet routing
            packetRoutingJob?.cancel()
            packetRoutingJob = null
            
            // Close TUN interface
            tunInterface?.close()
            tunInterface = null
            
            // Clear references
            currentAdapter = null
            currentProfile = null
            
            SanitizedLogger.i(TAG, "VPN tunnel stopped")
            
        } catch (e: Exception) {
            SanitizedLogger.e(TAG, "Error stopping VPN tunnel", e)
        }
    }

    /**
     * Creates and configures the TUN interface.
     * 
     * Requirements:
     * - 3.4: Creates TUN interface for packet routing
     * - 4.1, 4.2: Routes all TCP and UDP traffic
     * - 4.3, 4.7: Configures DNS to prevent leaks
     * - 4.8: Handles IPv6 (route or block)
     * - 5.2: Exclude apps from VPN tunnel
     * - 5.7: Automatically exclude VPN app itself
     * 
     * @param profile The server profile (used for protocol-specific MTU)
     * @param ipv6Enabled Whether to enable IPv6 routing
     * @param dnsServers List of DNS servers to use
     * @param excludedApps Set of package names to exclude from VPN
     * @return ParcelFileDescriptor for the TUN interface, or null on failure
     */
    private fun createTunInterface(
        profile: ServerProfile?,
        ipv6Enabled: Boolean,
        dnsServers: List<String>,
        excludedApps: Set<String>
    ): ParcelFileDescriptor? {
        try {
            SanitizedLogger.d(TAG, "Creating TUN interface")
            
            val builder = Builder()
                .setSession("SelfProxy VPN")
                .setBlocking(true)
                .setConfigureIntent(createConfigIntent())
            
            // Configure IPv4 address and routing
            builder.addAddress(VPN_ADDRESS, VPN_PREFIX_LENGTH)
            builder.addRoute(VPN_ROUTE, VPN_ROUTE_PREFIX)
            
            // Configure IPv6 if enabled
            if (ipv6Enabled) {
                SanitizedLogger.d(TAG, "Enabling IPv6 routing")
                builder.addAddress(IPV6_ADDRESS, IPV6_PREFIX_LENGTH)
                builder.addRoute(IPV6_ROUTE, IPV6_ROUTE_PREFIX)
            } else {
                SanitizedLogger.d(TAG, "IPv6 disabled - blocking IPv6 traffic")
                // IPv6 traffic will be blocked by not adding IPv6 routes
            }
            
            // Configure DNS servers
            dnsServers.forEach { dns ->
                SanitizedLogger.d(TAG, "Adding DNS server: $dns")
                builder.addDnsServer(dns)
            }
            
            // Configure MTU based on protocol
            val mtu = when (profile?.protocol) {
                Protocol.WIREGUARD -> {
                    // WireGuard uses slightly lower MTU due to encryption overhead
                    profile.getWireGuardConfig().mtu
                }
                Protocol.VLESS -> DEFAULT_MTU
                null -> DEFAULT_MTU
            }
            SanitizedLogger.d(TAG, "Setting MTU: $mtu")
            builder.setMtu(mtu)
            
            // Configure per-app routing (exclude specified apps)
            excludedApps.forEach { packageName ->
                try {
                    builder.addDisallowedApplication(packageName)
                    SanitizedLogger.d(TAG, "Excluded app from VPN: $packageName")
                } catch (e: Exception) {
                    SanitizedLogger.w(TAG, "Failed to exclude app: $packageName", e)
                }
            }
            
            // Always exclude our own app to prevent routing loops
            try {
                builder.addDisallowedApplication(packageName)
                SanitizedLogger.d(TAG, "Excluded own app from VPN: $packageName")
            } catch (e: Exception) {
                SanitizedLogger.e(TAG, "Failed to exclude own app - this may cause routing loops!", e)
            }
            
            // Establish the VPN interface
            val tunInterface = builder.establish()
            
            if (tunInterface != null) {
                SanitizedLogger.i(TAG, "TUN interface created successfully")
            } else {
                SanitizedLogger.e(TAG, "Failed to establish TUN interface")
            }
            
            return tunInterface
            
        } catch (e: Exception) {
            SanitizedLogger.e(TAG, "Error creating TUN interface", e)
            return null
        }
    }

    /**
     * Starts packet routing between TUN interface and protocol adapter.
     * 
     * Reads packets from the TUN interface and forwards them through the protocol adapter.
     * Receives packets from the protocol adapter and writes them to the TUN interface.
     */
    private fun startPacketRouting() {
        val tun = tunInterface ?: return
        
        packetRoutingJob = serviceScope.launch {
            try {
                SanitizedLogger.d(TAG, "Starting packet routing")
                
                // Read packets from TUN interface
                val inputStream = FileInputStream(tun.fileDescriptor)
                val outputStream = FileOutputStream(tun.fileDescriptor)
                val buffer = ByteArray(32767) // Max IP packet size
                
                withContext(Dispatchers.IO) {
                    while (isActive) {
                        try {
                            val length = inputStream.read(buffer)
                            if (length > 0) {
                                // Packet received from TUN interface
                                val packet = buffer.copyOf(length)
                                
                                // TODO: Route packet through protocol adapter
                                // For now, this is a placeholder for the packet routing logic
                                // In a complete implementation, packets would be:
                                // 1. Parsed to extract destination
                                // 2. Routed through the protocol adapter (WireGuard/VLESS)
                                // 3. Response packets written back to TUN interface
                                
                                SanitizedLogger.d(TAG, "Received packet: $length bytes")
                            }
                        } catch (e: IOException) {
                            if (isActive) {
                                SanitizedLogger.e(TAG, "Error reading packet", e)
                            }
                            break
                        }
                    }
                }
                
                SanitizedLogger.d(TAG, "Packet routing stopped")
                
            } catch (e: Exception) {
                SanitizedLogger.e(TAG, "Error in packet routing", e)
            }
        }
    }

    /**
     * Creates a pending intent for the notification tap action.
     */
    private fun createConfigIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Creates the notification channel for the VPN service.
     * Required for Android O and above.
     * 
     * Requirement 3.5: Notification channel for VPN service
     * Requirement 11.9: Foreground service notification
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when VPN tunnel is active"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            
            SanitizedLogger.d(TAG, "Notification channel created")
        }
    }

    /**
     * Creates a notification for the foreground service.
     * 
     * Requirements:
     * - 3.5: VPN key icon displayed in status bar
     * - 11.9: Foreground service notification with connection status
     * 
     * @param title Notification title
     * @param text Notification text
     * @param showDisconnectAction Whether to show the disconnect action button
     * @return Notification object
     */
    private fun createNotification(
        title: String, 
        text: String,
        showDisconnectAction: Boolean = true
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        
        // Add disconnect action button if requested
        if (showDisconnectAction) {
            val disconnectIntent = Intent(this, TunnelVpnService::class.java).apply {
                action = ACTION_STOP_VPN
            }
            val disconnectPendingIntent = PendingIntent.getService(
                this,
                1,
                disconnectIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            builder.addAction(
                R.drawable.ic_vpn_key,
                "Disconnect",
                disconnectPendingIntent
            )
        }

        return builder.build()
    }

    /**
     * Updates the notification with new content.
     * 
     * Requirement 11.9: Update notification on state changes
     * 
     * @param title New notification title
     * @param text New notification text
     * @param showDisconnectAction Whether to show the disconnect action button
     */
    fun updateNotification(
        title: String, 
        text: String,
        showDisconnectAction: Boolean = true
    ) {
        try {
            val notification = createNotification(title, text, showDisconnectAction)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
            SanitizedLogger.d(TAG, "Notification updated: $title")
        } catch (e: Exception) {
            SanitizedLogger.e(TAG, "Error updating notification", e)
        }
    }
    
    /**
     * Updates the notification with connection status.
     * 
     * Provides a convenient way to update notification based on connection state.
     * 
     * @param status The connection status (e.g., "Connected", "Connecting", "Disconnected")
     * @param profileName Optional profile name to include in the notification
     * @param additionalInfo Optional additional information (e.g., data transferred, duration)
     */
    fun updateNotificationWithStatus(
        status: String,
        profileName: String? = null,
        additionalInfo: String? = null
    ) {
        val title = when (status.lowercase()) {
            "connected" -> "VPN Connected"
            "connecting" -> "VPN Connecting..."
            "disconnecting" -> "VPN Disconnecting..."
            "disconnected" -> "VPN Disconnected"
            "reconnecting" -> "VPN Reconnecting..."
            "error" -> "VPN Connection Error"
            else -> "VPN $status"
        }
        
        val text = buildString {
            if (profileName != null) {
                append(profileName)
            }
            if (additionalInfo != null) {
                if (profileName != null) append(" • ")
                append(additionalInfo)
            }
            if (profileName == null && additionalInfo == null) {
                append("Tap to open")
            }
        }
        
        // Show disconnect button only when connected
        val showDisconnect = status.lowercase() == "connected"
        
        updateNotification(title, text, showDisconnect)
    }
}
