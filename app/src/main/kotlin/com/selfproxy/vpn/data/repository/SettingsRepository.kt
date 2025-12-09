package com.selfproxy.vpn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.selfproxy.vpn.data.model.AppSettings
import com.selfproxy.vpn.data.model.FlowControl
import com.selfproxy.vpn.data.model.TransportProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing application settings.
 * 
 * Uses DataStore for persistent storage of user preferences.
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5
 */
class SettingsRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
    
    companion object {
        // DNS settings
        private val PRIMARY_DNS = stringPreferencesKey("primary_dns")
        private val SECONDARY_DNS = stringPreferencesKey("secondary_dns")
        private val CUSTOM_DNS_ENABLED = booleanPreferencesKey("custom_dns_enabled")
        
        // IPv6 settings
        private val IPV6_ENABLED = booleanPreferencesKey("ipv6_enabled")
        
        // MTU settings
        private val MTU = intPreferencesKey("mtu")
        
        // Connection settings
        private val CONNECTION_TIMEOUT = intPreferencesKey("connection_timeout")
        private val KEEPALIVE_INTERVAL = intPreferencesKey("keepalive_interval")
        private val AUTO_RECONNECT_ENABLED = booleanPreferencesKey("auto_reconnect_enabled")
        private val RECONNECTION_MAX_ATTEMPTS = intPreferencesKey("reconnection_max_attempts")
        
        // WireGuard settings
        private val WIREGUARD_PERSISTENT_KEEPALIVE = intPreferencesKey("wireguard_persistent_keepalive")
        private val WIREGUARD_MTU = intPreferencesKey("wireguard_mtu")
        
        // VLESS settings
        private val VLESS_DEFAULT_TRANSPORT = stringPreferencesKey("vless_default_transport")
        private val VLESS_DEFAULT_FLOW_CONTROL = stringPreferencesKey("vless_default_flow_control")
        
        // Logging settings
        private val VERBOSE_LOGGING_ENABLED = booleanPreferencesKey("verbose_logging_enabled")
        private val LOG_EXPORT_ENABLED = booleanPreferencesKey("log_export_enabled")
    }
    
    /**
     * Observes application settings.
     * 
     * @return Flow of current settings
     */
    fun observeSettings(): Flow<AppSettings> {
        return context.dataStore.data.map { preferences ->
            AppSettings(
                primaryDnsServer = preferences[PRIMARY_DNS] ?: "8.8.8.8",
                secondaryDnsServer = preferences[SECONDARY_DNS] ?: "8.8.4.4",
                customDnsEnabled = preferences[CUSTOM_DNS_ENABLED] ?: false,
                ipv6Enabled = preferences[IPV6_ENABLED] ?: true,
                mtu = preferences[MTU] ?: 1500,
                connectionTimeout = preferences[CONNECTION_TIMEOUT] ?: 30,
                keepAliveInterval = preferences[KEEPALIVE_INTERVAL] ?: 25,
                autoReconnectEnabled = preferences[AUTO_RECONNECT_ENABLED] ?: true,
                reconnectionMaxAttempts = preferences[RECONNECTION_MAX_ATTEMPTS] ?: 5,
                wireGuardPersistentKeepalive = preferences[WIREGUARD_PERSISTENT_KEEPALIVE] ?: 25,
                wireGuardMtu = preferences[WIREGUARD_MTU] ?: 1420,
                vlessDefaultTransport = TransportProtocol.valueOf(
                    preferences[VLESS_DEFAULT_TRANSPORT] ?: TransportProtocol.TCP.name
                ),
                vlessDefaultFlowControl = FlowControl.valueOf(
                    preferences[VLESS_DEFAULT_FLOW_CONTROL] ?: FlowControl.NONE.name
                ),
                verboseLoggingEnabled = preferences[VERBOSE_LOGGING_ENABLED] ?: false,
                logExportEnabled = preferences[LOG_EXPORT_ENABLED] ?: false
            )
        }
    }
    
    /**
     * Updates application settings.
     * 
     * @param settings The new settings to save
     */
    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PRIMARY_DNS] = settings.primaryDnsServer
            preferences[SECONDARY_DNS] = settings.secondaryDnsServer
            preferences[CUSTOM_DNS_ENABLED] = settings.customDnsEnabled
            preferences[IPV6_ENABLED] = settings.ipv6Enabled
            preferences[MTU] = settings.mtu
            preferences[CONNECTION_TIMEOUT] = settings.connectionTimeout
            preferences[KEEPALIVE_INTERVAL] = settings.keepAliveInterval
            preferences[AUTO_RECONNECT_ENABLED] = settings.autoReconnectEnabled
            preferences[RECONNECTION_MAX_ATTEMPTS] = settings.reconnectionMaxAttempts
            preferences[WIREGUARD_PERSISTENT_KEEPALIVE] = settings.wireGuardPersistentKeepalive
            preferences[WIREGUARD_MTU] = settings.wireGuardMtu
            preferences[VLESS_DEFAULT_TRANSPORT] = settings.vlessDefaultTransport.name
            preferences[VLESS_DEFAULT_FLOW_CONTROL] = settings.vlessDefaultFlowControl.name
            preferences[VERBOSE_LOGGING_ENABLED] = settings.verboseLoggingEnabled
            preferences[LOG_EXPORT_ENABLED] = settings.logExportEnabled
        }
    }
    
    /**
     * Resets settings to defaults.
     */
    suspend fun resetToDefaults() {
        updateSettings(AppSettings())
    }
}
