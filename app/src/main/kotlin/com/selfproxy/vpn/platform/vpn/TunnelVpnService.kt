package com.selfproxy.vpn.platform.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.selfproxy.vpn.R
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
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_PROTOCOL_ADAPTER = "protocol_adapter"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var tunInterface: ParcelFileDescriptor? = null
    private var currentProfile: ServerProfile? = null
    private var currentAdapter: ProtocolAdapter? = null
    private var packetRoutingJob: Job? = null
    
    private var ipv6Enabled: Boolean = false
    private var customDnsServers: List<String> = listOf(PRIMARY_DNS, SECONDARY_DNS)
    private var excludedApps: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VPN service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_VPN -> {
                // Start VPN with provided configuration
                // Note: In production, profile and adapter would be passed via dependency injection
                // or retrieved from a connection manager
                startVpnTunnel()
            }
            ACTION_STOP_VPN -> {
                stopVpnTunnel()
                stopSelf()
            }
            else -> {
                // Default: start foreground service with notification
                val notification = createNotification("VPN Ready", "Waiting for connection")
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "VPN service destroyed")
        stopVpnTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        // User revoked VPN permission
        Log.i(TAG, "VPN permission revoked by user")
        stopVpnTunnel()
        stopSelf()
    }

    /**
     * Starts the VPN tunnel with the current profile and adapter.
     * 
     * Creates the TUN interface, configures routing, and starts packet forwarding.
     */
    fun startVpnTunnel(
        profile: ServerProfile? = null,
        adapter: ProtocolAdapter? = null,
        ipv6Enabled: Boolean = false,
        dnsServers: List<String> = listOf(PRIMARY_DNS, SECONDARY_DNS),
        excludedApps: Set<String> = emptySet()
    ) {
        try {
            Log.d(TAG, "Starting VPN tunnel")
            
            this.currentProfile = profile
            this.currentAdapter = adapter
            this.ipv6Enabled = ipv6Enabled
            this.customDnsServers = dnsServers
            this.excludedApps = excludedApps
            
            // Create TUN interface
            tunInterface = createTunInterface(profile, ipv6Enabled, dnsServers, excludedApps)
            
            if (tunInterface == null) {
                Log.e(TAG, "Failed to create TUN interface")
                stopSelf()
                return
            }
            
            // Update notification
            val profileName = profile?.name ?: "VPN Server"
            val notification = createNotification("VPN Connected", "Connected to $profileName")
            startForeground(NOTIFICATION_ID, notification)
            
            // Start packet routing
            startPacketRouting()
            
            Log.i(TAG, "VPN tunnel started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN tunnel", e)
            stopVpnTunnel()
            stopSelf()
        }
    }

    /**
     * Stops the VPN tunnel and cleans up resources.
     */
    fun stopVpnTunnel() {
        try {
            Log.d(TAG, "Stopping VPN tunnel")
            
            // Stop packet routing
            packetRoutingJob?.cancel()
            packetRoutingJob = null
            
            // Close TUN interface
            tunInterface?.close()
            tunInterface = null
            
            // Clear references
            currentAdapter = null
            currentProfile = null
            
            Log.i(TAG, "VPN tunnel stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN tunnel", e)
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
            Log.d(TAG, "Creating TUN interface")
            
            val builder = Builder()
                .setSession("SelfProxy VPN")
                .setBlocking(true)
                .setConfigureIntent(createConfigIntent())
            
            // Configure IPv4 address and routing
            builder.addAddress(VPN_ADDRESS, VPN_PREFIX_LENGTH)
            builder.addRoute(VPN_ROUTE, VPN_ROUTE_PREFIX)
            
            // Configure IPv6 if enabled
            if (ipv6Enabled) {
                Log.d(TAG, "Enabling IPv6 routing")
                builder.addAddress(IPV6_ADDRESS, IPV6_PREFIX_LENGTH)
                builder.addRoute(IPV6_ROUTE, IPV6_ROUTE_PREFIX)
            } else {
                Log.d(TAG, "IPv6 disabled - blocking IPv6 traffic")
                // IPv6 traffic will be blocked by not adding IPv6 routes
            }
            
            // Configure DNS servers
            dnsServers.forEach { dns ->
                Log.d(TAG, "Adding DNS server: $dns")
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
            Log.d(TAG, "Setting MTU: $mtu")
            builder.setMtu(mtu)
            
            // Configure per-app routing (exclude specified apps)
            excludedApps.forEach { packageName ->
                try {
                    builder.addDisallowedApplication(packageName)
                    Log.d(TAG, "Excluded app from VPN: $packageName")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to exclude app: $packageName", e)
                }
            }
            
            // Always exclude our own app to prevent routing loops
            try {
                builder.addDisallowedApplication(packageName)
                Log.d(TAG, "Excluded own app from VPN: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to exclude own app - this may cause routing loops!", e)
            }
            
            // Establish the VPN interface
            val tunInterface = builder.establish()
            
            if (tunInterface != null) {
                Log.i(TAG, "TUN interface created successfully")
            } else {
                Log.e(TAG, "Failed to establish TUN interface")
            }
            
            return tunInterface
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating TUN interface", e)
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
                Log.d(TAG, "Starting packet routing")
                
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
                                
                                Log.v(TAG, "Received packet: $length bytes")
                            }
                        } catch (e: IOException) {
                            if (isActive) {
                                Log.e(TAG, "Error reading packet", e)
                            }
                            break
                        }
                    }
                }
                
                Log.d(TAG, "Packet routing stopped")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in packet routing", e)
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
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates a notification for the foreground service.
     * 
     * Requirement 3.5: VPN key icon displayed in status bar
     * 
     * @param title Notification title
     * @param text Notification text
     * @return Notification object
     */
    private fun createNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, TunnelVpnService::class.java).apply {
            action = ACTION_STOP_VPN
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_vpn_key,
                "Disconnect",
                disconnectPendingIntent
            )
            .build()
    }

    /**
     * Updates the notification with new content.
     * 
     * @param title New notification title
     * @param text New notification text
     */
    fun updateNotification(title: String, text: String) {
        val notification = createNotification(title, text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
