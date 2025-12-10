package com.selfproxy.vpn.domain.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
import java.text.SimpleDateFormat
import java.util.*

/**
 * Collects diagnostic information for troubleshooting connection issues.
 * 
 * Requirements:
 * - 12.8: Diagnostic information collection
 * - 12.9: Verbose logging toggle
 * - 12.10: Log export with sanitization
 */
class DiagnosticCollector(
    private val context: Context
) {
    
    /**
     * Collects comprehensive diagnostic information.
     * 
     * @param connectionState Current connection state
     * @param protocol Protocol being used
     * @param error Error that occurred (if any)
     * @return Map of diagnostic information
     */
    fun collectDiagnostics(
        connectionState: ConnectionState,
        protocol: Protocol?,
        error: Throwable? = null
    ): Map<String, String> {
        return buildMap {
            // Timestamp
            put("timestamp", getCurrentTimestamp())
            
            // Device information
            put("device_manufacturer", Build.MANUFACTURER)
            put("device_model", Build.MODEL)
            put("android_version", Build.VERSION.RELEASE)
            put("sdk_version", Build.VERSION.SDK_INT.toString())
            
            // App information
            put("app_version", getAppVersion())
            
            // Connection state
            put("connection_state", connectionState.javaClass.simpleName)
            if (protocol != null) {
                put("protocol", protocol.name)
            }
            
            // Network information
            putAll(collectNetworkInfo())
            
            // Error information (if available)
            if (error != null) {
                put("error_type", error.javaClass.simpleName)
                put("error_message", error.message ?: "No message")
                error.cause?.let {
                    put("error_cause", it.javaClass.simpleName)
                    put("error_cause_message", it.message ?: "No message")
                }
            }
        }
    }
    
    /**
     * Collects network connectivity information.
     * 
     * @return Map of network diagnostic information
     */
    private fun collectNetworkInfo(): Map<String, String> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return mapOf("network_info" to "unavailable")
        
        return buildMap {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                put("network_status", "no_active_network")
                return@buildMap
            }
            
            put("network_status", "active")
            
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (capabilities != null) {
                put("network_wifi", capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI).toString())
                put("network_cellular", capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR).toString())
                put("network_vpn", capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN).toString())
                put("network_validated", capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).toString())
                put("network_internet", capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).toString())
                
                // Link properties
                val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                if (linkProperties != null) {
                    put("network_interface", linkProperties.interfaceName ?: "unknown")
                    // MTU is only available on API 29+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put("network_mtu", linkProperties.mtu.toString())
                    } else {
                        put("network_mtu", "unknown")
                    }
                }
            }
        }
    }
    
    /**
     * Generates a full diagnostic report.
     * 
     * @param connectionState Current connection state
     * @param protocol Protocol being used
     * @param error Error that occurred (if any)
     * @param includeSystemLogs Whether to include system logs
     * @return Formatted diagnostic report
     */
    fun generateReport(
        connectionState: ConnectionState,
        protocol: Protocol?,
        error: Throwable? = null,
        includeSystemLogs: Boolean = false
    ): String {
        val diagnostics = collectDiagnostics(connectionState, protocol, error)
        
        return buildString {
            appendLine("=== VPN Diagnostic Report ===")
            appendLine()
            
            appendLine("Generated: ${getCurrentTimestamp()}")
            appendLine()
            
            appendLine("--- Device Information ---")
            appendLine("Manufacturer: ${diagnostics["device_manufacturer"]}")
            appendLine("Model: ${diagnostics["device_model"]}")
            appendLine("Android Version: ${diagnostics["android_version"]} (SDK ${diagnostics["sdk_version"]})")
            appendLine("App Version: ${diagnostics["app_version"]}")
            appendLine()
            
            appendLine("--- Connection Information ---")
            appendLine("State: ${diagnostics["connection_state"]}")
            if (protocol != null) {
                appendLine("Protocol: ${diagnostics["protocol"]}")
            }
            appendLine()
            
            appendLine("--- Network Information ---")
            appendLine("Status: ${diagnostics["network_status"]}")
            appendLine("WiFi: ${diagnostics["network_wifi"]}")
            appendLine("Cellular: ${diagnostics["network_cellular"]}")
            appendLine("VPN Active: ${diagnostics["network_vpn"]}")
            appendLine("Validated: ${diagnostics["network_validated"]}")
            appendLine("Internet: ${diagnostics["network_internet"]}")
            diagnostics["network_interface"]?.let {
                appendLine("Interface: $it")
            }
            diagnostics["network_mtu"]?.let {
                appendLine("MTU: $it")
            }
            appendLine()
            
            if (error != null) {
                appendLine("--- Error Information ---")
                appendLine("Type: ${diagnostics["error_type"]}")
                appendLine("Message: ${diagnostics["error_message"]}")
                diagnostics["error_cause"]?.let {
                    appendLine("Cause: $it")
                    appendLine("Cause Message: ${diagnostics["error_cause_message"]}")
                }
                appendLine()
                
                // Stack trace (sanitized)
                appendLine("Stack Trace:")
                error.stackTrace.take(10).forEach { element ->
                    appendLine("  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
                }
                appendLine()
            }
            
            if (includeSystemLogs) {
                appendLine("--- System Logs ---")
                appendLine("(System logs would be included here if verbose logging is enabled)")
                appendLine()
            }
            
            appendLine("=== End of Report ===")
        }
    }
    
    /**
     * Exports diagnostic report to a file.
     * 
     * Requirement 12.10: Log export functionality
     * 
     * @param report The diagnostic report to export
     * @return File path where report was saved, or null if failed
     */
    fun exportReport(report: String): String? {
        return try {
            val fileName = "vpn_diagnostics_${System.currentTimeMillis()}.txt"
            val file = context.getExternalFilesDir(null)?.resolve(fileName)
            file?.writeText(report)
            file?.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the current timestamp in a readable format.
     */
    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    /**
     * Gets the app version from package info.
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
            "${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
