package com.selfproxy.vpn.domain.model

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException

/**
 * Sealed class representing all possible VPN errors.
 * 
 * Provides structured error information with user-friendly messages
 * and diagnostic details for troubleshooting.
 * 
 * Requirements:
 * - 3.6: Specific error messages indicating failure reason
 * - 3.8: Timeout handling with duration and suggestions
 * - 12.1-12.7: Protocol-specific error handling
 */
sealed class VpnError {
    abstract val message: String
    abstract val diagnosticInfo: Map<String, String>
    abstract val suggestedAction: String
    
    /**
     * Authentication errors - invalid credentials or keys.
     * 
     * Requirement 12.1: Authentication failure messages
     */
    sealed class Authentication : VpnError() {
        data class WireGuardInvalidKeys(
            val keyType: String = "private or public key"
        ) : Authentication() {
            override val message = "Authentication failed: Invalid WireGuard $keyType"
            override val diagnosticInfo = mapOf(
                "error_type" to "authentication",
                "protocol" to "WireGuard",
                "key_type" to keyType
            )
            override val suggestedAction = "Verify your private key and server's public key are correct. Keys must be valid base64-encoded 32-byte values."
        }
        
        data class VlessInvalidUuid(
            val uuid: String? = null
        ) : Authentication() {
            override val message = "Authentication failed: Invalid VLESS UUID"
            override val diagnosticInfo = mapOf(
                "error_type" to "authentication",
                "protocol" to "VLESS",
                "uuid_provided" to (uuid != null).toString()
            )
            override val suggestedAction = "Verify your UUID is correct and follows RFC 4122 format (e.g., 550e8400-e29b-41d4-a716-446655440000)."
        }
        
        data class ServerRejection(
            val protocol: Protocol,
            val reason: String? = null
        ) : Authentication() {
            override val message = "Server rejected connection: ${reason ?: "Authentication failed"}"
            override val diagnosticInfo = mapOf(
                "error_type" to "server_rejection",
                "protocol" to protocol.name,
                "reason" to (reason ?: "unknown")
            )
            override val suggestedAction = "Check that your credentials match the server configuration. Contact your server administrator if the issue persists."
        }
    }
    
    /**
     * Network connectivity errors.
     * 
     * Requirement 12.2: Server unreachable messages
     */
    sealed class Network : VpnError() {
        data class ServerUnreachable(
            val hostname: String,
            val port: Int,
            val protocol: Protocol
        ) : Network() {
            override val message = "Cannot reach server: $hostname:$port"
            override val diagnosticInfo = mapOf(
                "error_type" to "network_unreachable",
                "hostname" to hostname,
                "port" to port.toString(),
                "protocol" to protocol.name
            )
            override val suggestedAction = "Check your internet connection and verify the server address is correct. Ensure the server is running and accessible."
        }
        
        data class DnsResolutionFailed(
            val hostname: String
        ) : Network() {
            override val message = "Cannot resolve hostname: $hostname"
            override val diagnosticInfo = mapOf(
                "error_type" to "dns_resolution",
                "hostname" to hostname
            )
            override val suggestedAction = "Check that the hostname is spelled correctly. Try using an IP address instead if DNS is unavailable."
        }
        
        data class NoInternet(
            val networkType: String? = null
        ) : Network() {
            override val message = "No internet connection available"
            override val diagnosticInfo = mapOf(
                "error_type" to "no_internet",
                "network_type" to (networkType ?: "unknown")
            )
            override val suggestedAction = "Connect to WiFi or enable mobile data, then try again."
        }
    }
    
    /**
     * Timeout errors with duration information.
     * 
     * Requirement 12.3: Timeout messages with duration
     */
    sealed class Timeout : VpnError() {
        data class ConnectionTimeout(
            val protocol: Protocol,
            val durationSeconds: Int,
            val stage: String = "connection"
        ) : Timeout() {
            override val message = "Connection timeout after ${durationSeconds}s during $stage. Check firewall settings."
            override val diagnosticInfo = mapOf(
                "error_type" to "timeout",
                "protocol" to protocol.name,
                "duration_seconds" to durationSeconds.toString(),
                "stage" to stage
            )
            override val suggestedAction = when (protocol) {
                Protocol.WIREGUARD -> "Check firewall settings and ensure UDP port is open. WireGuard requires UDP connectivity."
                Protocol.VLESS -> "Check firewall settings and server configuration. Verify the transport protocol is correctly configured."
            }
        }
        
        data class HandshakeTimeout(
            val durationSeconds: Int
        ) : Timeout() {
            override val message = "WireGuard handshake timeout after ${durationSeconds}s"
            override val diagnosticInfo = mapOf(
                "error_type" to "handshake_timeout",
                "protocol" to "WireGuard",
                "duration_seconds" to durationSeconds.toString()
            )
            override val suggestedAction = "Verify keys are correct, server is running, and firewall allows UDP traffic. Check server logs for handshake attempts."
        }
    }
    
    /**
     * WireGuard-specific errors.
     * 
     * Requirements 12.4, 12.5: WireGuard error handling
     */
    sealed class WireGuard : VpnError() {
        data class HandshakeFailed(
            val reason: String? = null
        ) : WireGuard() {
            override val message = "WireGuard handshake failed${reason?.let { ": $it" } ?: ""}"
            override val diagnosticInfo = mapOf(
                "error_type" to "handshake_failed",
                "protocol" to "WireGuard",
                "reason" to (reason ?: "unknown")
            )
            override val suggestedAction = "Check that keys match on both client and server. Verify endpoint address and port are correct."
        }
        
        data class KeyMismatch(
            val keyType: String
        ) : WireGuard() {
            override val message = "WireGuard key mismatch: $keyType"
            override val diagnosticInfo = mapOf(
                "error_type" to "key_mismatch",
                "protocol" to "WireGuard",
                "key_type" to keyType
            )
            override val suggestedAction = "Regenerate keys or verify you're using the correct key pair. Public key on client must match server configuration."
        }
        
        data class InvalidEndpoint(
            val endpoint: String
        ) : WireGuard() {
            override val message = "Invalid WireGuard endpoint: $endpoint"
            override val diagnosticInfo = mapOf(
                "error_type" to "invalid_endpoint",
                "protocol" to "WireGuard",
                "endpoint" to endpoint
            )
            override val suggestedAction = "Endpoint must be in format 'hostname:port' or 'ip:port'. Verify the format is correct."
        }
        
        data class InvalidAllowedIPs(
            val allowedIPs: String
        ) : WireGuard() {
            override val message = "Invalid AllowedIPs format: $allowedIPs"
            override val diagnosticInfo = mapOf(
                "error_type" to "invalid_allowed_ips",
                "protocol" to "WireGuard",
                "allowed_ips" to allowedIPs
            )
            override val suggestedAction = "AllowedIPs must be in CIDR format (e.g., 0.0.0.0/0, 10.0.0.0/24). Check your configuration."
        }
    }
    
    /**
     * VLESS-specific errors.
     * 
     * Requirements 12.6, 12.7: VLESS error handling
     */
    sealed class Vless : VpnError() {
        data class TlsError(
            val reason: String
        ) : Vless() {
            override val message = "TLS error: $reason"
            override val diagnosticInfo = mapOf(
                "error_type" to "tls_error",
                "protocol" to "VLESS",
                "reason" to reason
            )
            override val suggestedAction = "Verify server certificate is valid and TLS settings are correct. Check that server supports TLS 1.3."
        }
        
        data class CertificateValidationFailed(
            val hostname: String,
            val reason: String? = null
        ) : Vless() {
            override val message = "Certificate validation failed for $hostname${reason?.let { ": $it" } ?: ""}"
            override val diagnosticInfo = mapOf(
                "error_type" to "certificate_validation",
                "protocol" to "VLESS",
                "hostname" to hostname,
                "reason" to (reason ?: "unknown")
            )
            override val suggestedAction = "Ensure server certificate is valid and matches the hostname. Check certificate expiration date."
        }
        
        data class TransportProtocolError(
            val transport: String,
            val reason: String? = null
        ) : Vless() {
            override val message = "Transport protocol error ($transport)${reason?.let { ": $it" } ?: ""}"
            override val diagnosticInfo = mapOf(
                "error_type" to "transport_error",
                "protocol" to "VLESS",
                "transport" to transport,
                "reason" to (reason ?: "unknown")
            )
            override val suggestedAction = "Try a different transport protocol (TCP, WebSocket, gRPC, or HTTP/2). Verify server supports the selected transport."
        }
        
        data class RealityConfigError(
            val reason: String
        ) : Vless() {
            override val message = "Reality configuration error: $reason"
            override val diagnosticInfo = mapOf(
                "error_type" to "reality_config",
                "protocol" to "VLESS",
                "reason" to reason
            )
            override val suggestedAction = "Verify Reality settings (server name, public key, short ID) match server configuration."
        }
    }
    
    /**
     * Configuration errors.
     */
    sealed class Configuration : VpnError() {
        data class InvalidProfile(
            val reason: String
        ) : Configuration() {
            override val message = "Invalid profile configuration: $reason"
            override val diagnosticInfo = mapOf(
                "error_type" to "invalid_profile",
                "reason" to reason
            )
            override val suggestedAction = "Review and correct your profile settings. Ensure all required fields are filled correctly."
        }
        
        data class MissingRequiredField(
            val fieldName: String,
            val protocol: Protocol
        ) : Configuration() {
            override val message = "Missing required field: $fieldName"
            override val diagnosticInfo = mapOf(
                "error_type" to "missing_field",
                "protocol" to protocol.name,
                "field" to fieldName
            )
            override val suggestedAction = "Fill in the $fieldName field in your profile configuration."
        }
    }
    
    /**
     * Permission errors.
     */
    sealed class Permission : VpnError() {
        object VpnPermissionDenied : Permission() {
            override val message = "VPN permission denied"
            override val diagnosticInfo = mapOf(
                "error_type" to "permission_denied",
                "permission" to "VPN"
            )
            override val suggestedAction = "Grant VPN permission to use this app. Go to Settings and allow VPN access."
        }
        
        object BatteryOptimizationNeeded : Permission() {
            override val message = "Battery optimization may affect connection stability"
            override val diagnosticInfo = mapOf(
                "error_type" to "battery_optimization",
                "severity" to "warning"
            )
            override val suggestedAction = "Disable battery optimization for this app to ensure stable VPN connections."
        }
    }
    
    /**
     * Generic errors with diagnostic information.
     */
    data class Generic(
        override val message: String,
        val cause: Throwable? = null,
        val protocol: Protocol? = null,
        override val diagnosticInfo: Map<String, String> = emptyMap()
    ) : VpnError() {
        override val suggestedAction = "Check your configuration and try again. If the problem persists, check logs for more details."
    }
}

/**
 * Extension function to convert exceptions to VpnError.
 * 
 * Analyzes exception type and message to create appropriate VpnError.
 */
fun Throwable.toVpnError(protocol: Protocol? = null): VpnError {
    return when (this) {
        is SocketTimeoutException -> {
            VpnError.Timeout.ConnectionTimeout(
                protocol = protocol ?: Protocol.WIREGUARD,
                durationSeconds = 30,
                stage = message ?: "connection"
            )
        }
        
        is UnknownHostException -> {
            VpnError.Network.DnsResolutionFailed(
                hostname = message ?: "unknown"
            )
        }
        
        is IOException -> {
            when {
                message?.contains("unreachable", ignoreCase = true) == true -> {
                    VpnError.Network.ServerUnreachable(
                        hostname = "server",
                        port = 0,
                        protocol = protocol ?: Protocol.WIREGUARD
                    )
                }
                message?.contains("network", ignoreCase = true) == true -> {
                    VpnError.Network.NoInternet()
                }
                else -> VpnError.Generic(
                    message = message ?: "Network error",
                    cause = this,
                    protocol = protocol
                )
            }
        }
        
        is SSLException, is CertificateException -> {
            VpnError.Vless.TlsError(
                reason = message ?: "SSL/TLS error"
            )
        }
        
        else -> {
            // Try to parse message for specific error types
            val msg = message ?: "Unknown error"
            when {
                msg.contains("authentication", ignoreCase = true) -> {
                    when (protocol) {
                        Protocol.WIREGUARD -> VpnError.Authentication.WireGuardInvalidKeys()
                        Protocol.VLESS -> VpnError.Authentication.VlessInvalidUuid()
                        null -> VpnError.Generic(msg, this, protocol)
                    }
                }
                
                msg.contains("handshake", ignoreCase = true) -> {
                    VpnError.WireGuard.HandshakeFailed(msg)
                }
                
                msg.contains("certificate", ignoreCase = true) -> {
                    VpnError.Vless.CertificateValidationFailed(
                        hostname = "server",
                        reason = msg
                    )
                }
                
                msg.contains("timeout", ignoreCase = true) -> {
                    VpnError.Timeout.ConnectionTimeout(
                        protocol = protocol ?: Protocol.WIREGUARD,
                        durationSeconds = 30
                    )
                }
                
                else -> VpnError.Generic(msg, this, protocol)
            }
        }
    }
}

/**
 * Extension function to get full diagnostic report.
 * 
 * Requirement 12.8: Diagnostic information collection
 */
fun VpnError.getDiagnosticReport(): String {
    return buildString {
        appendLine("Error: $message")
        appendLine()
        appendLine("Suggested Action:")
        appendLine(suggestedAction)
        appendLine()
        appendLine("Diagnostic Information:")
        diagnosticInfo.forEach { (key, value) ->
            appendLine("  $key: $value")
        }
        
        // Add timestamp
        appendLine("  timestamp: ${System.currentTimeMillis()}")
    }
}
