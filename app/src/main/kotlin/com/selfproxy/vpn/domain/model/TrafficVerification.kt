package com.selfproxy.vpn.domain.model

/**
 * Represents the result of traffic verification tests.
 * 
 * Requirements: 8.9, 8.10
 */
data class TrafficVerificationResult(
    val currentIp: String?,
    val vpnServerIp: String?,
    val isUsingVpn: Boolean,
    val dnsServers: List<String>,
    val isDnsLeaking: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String? = null
) {
    /**
     * Checks if the verification was successful.
     */
    val isSuccess: Boolean
        get() = error == null && currentIp != null
    
    /**
     * Checks if all tests passed (using VPN and no DNS leak).
     */
    val allTestsPassed: Boolean
        get() = isSuccess && isUsingVpn && !isDnsLeaking
}

/**
 * Represents the state of traffic verification.
 */
sealed class VerificationState {
    object Idle : VerificationState()
    object Verifying : VerificationState()
    data class Completed(val result: TrafficVerificationResult) : VerificationState()
    data class Error(val message: String) : VerificationState()
}
