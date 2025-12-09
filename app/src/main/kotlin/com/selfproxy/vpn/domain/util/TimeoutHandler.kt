package com.selfproxy.vpn.domain.util

import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.model.VpnError
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Utility for handling connection timeouts.
 * 
 * Requirements:
 * - 3.8: Timeout handling with duration
 * - 12.3: Timeout messages with suggestions
 */
object TimeoutHandler {
    
    /**
     * Default timeout durations for different protocols.
     * 
     * Requirement 3.8: Protocol-specific timeouts
     */
    object Timeouts {
        val WIREGUARD_CONNECTION = 30.seconds
        val WIREGUARD_HANDSHAKE = 5.seconds
        val VLESS_CONNECTION = 45.seconds
        val VLESS_TLS_HANDSHAKE = 10.seconds
        val CONNECTION_TEST = 10.seconds
    }
    
    /**
     * Executes a block with a timeout, converting timeout exceptions to VpnError.
     * 
     * @param timeout The timeout duration
     * @param protocol The protocol being used
     * @param stage The connection stage (for error messages)
     * @param block The block to execute
     * @return Result containing the block's result or a timeout error
     */
    suspend fun <T> withConnectionTimeout(
        timeout: Duration,
        protocol: Protocol,
        stage: String = "connection",
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = withTimeout(timeout) {
                block()
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            val error = VpnError.Timeout.ConnectionTimeout(
                protocol = protocol,
                durationSeconds = timeout.inWholeSeconds.toInt(),
                stage = stage
            )
            Result.failure(Exception(error.message, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Executes a WireGuard handshake with timeout.
     * 
     * @param block The handshake block to execute
     * @return Result containing the block's result or a handshake timeout error
     */
    suspend fun <T> withHandshakeTimeout(
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = withTimeout(Timeouts.WIREGUARD_HANDSHAKE) {
                block()
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            val error = VpnError.Timeout.HandshakeTimeout(
                durationSeconds = Timeouts.WIREGUARD_HANDSHAKE.inWholeSeconds.toInt()
            )
            Result.failure(Exception(error.message, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Executes a connection test with timeout.
     * 
     * Requirement 8.6: Test completion within 10 seconds
     * 
     * @param protocol The protocol being tested
     * @param block The test block to execute
     * @return Result containing the block's result or a timeout error
     */
    suspend fun <T> withTestTimeout(
        protocol: Protocol,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = withTimeout(Timeouts.CONNECTION_TEST) {
                block()
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            val error = VpnError.Timeout.ConnectionTimeout(
                protocol = protocol,
                durationSeconds = Timeouts.CONNECTION_TEST.inWholeSeconds.toInt(),
                stage = "connection test"
            )
            Result.failure(Exception(error.message, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets the recommended timeout for a protocol and operation.
     * 
     * @param protocol The protocol
     * @param operation The operation type
     * @return The recommended timeout duration
     */
    fun getRecommendedTimeout(protocol: Protocol, operation: String): Duration {
        return when (operation.lowercase()) {
            "handshake" -> when (protocol) {
                Protocol.WIREGUARD -> Timeouts.WIREGUARD_HANDSHAKE
                Protocol.VLESS -> Timeouts.VLESS_TLS_HANDSHAKE
            }
            "connection" -> when (protocol) {
                Protocol.WIREGUARD -> Timeouts.WIREGUARD_CONNECTION
                Protocol.VLESS -> Timeouts.VLESS_CONNECTION
            }
            "test" -> Timeouts.CONNECTION_TEST
            else -> when (protocol) {
                Protocol.WIREGUARD -> Timeouts.WIREGUARD_CONNECTION
                Protocol.VLESS -> Timeouts.VLESS_CONNECTION
            }
        }
    }
}
