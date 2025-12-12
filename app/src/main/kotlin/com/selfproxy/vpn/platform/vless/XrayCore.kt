package com.selfproxy.vpn.platform.vless

import android.content.Context
import android.util.Log
import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.TransportProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libv2ray.Libv2ray
import libv2ray.CoreController
import libv2ray.CoreCallbackHandler

/**
 * Wrapper for AndroidLibXrayLite (Xray-core) library.
 * 
 * Provides a Kotlin-friendly interface to the Go-based Xray-core library.
 */
class XrayCore(
    private val context: Context
) {
    private var coreController: CoreController? = null
    private var isRunningFlag = false
    
    companion object {
        private const val TAG = "XrayCore"
        private const val SOCKS_PORT = 10808
    }
    
    /**
     * Starts Xray-core with the given configuration.
     */
    suspend fun start(
        config: String,
        callbackHandler: CoreCallbackHandler
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (isRunningFlag) {
                Log.w(TAG, "Xray-core is already running")
                return@withContext Result.success(SOCKS_PORT)
            }
            
            Log.d(TAG, "Initializing Xray-core...")
            Log.d(TAG, "Config: $config")
            
            // Initialize core environment
            val filesDir = context.filesDir.absolutePath
            
            // Try UDP without XUDP by passing empty cache path
            // XUDP is only needed for flow control optimization
            // Basic UDP should work without it
            val cachePath = ""
            Log.d(TAG, "Initializing Xray with empty cache (UDP without XUDP)")
            Libv2ray.initCoreEnv(filesDir, cachePath)
            
            // Create core controller
            val controller = Libv2ray.newCoreController(callbackHandler)
            
            // Start core with configuration
            Log.d(TAG, "Starting Xray-core...")
            controller.startLoop(config)
            
            coreController = controller
            isRunningFlag = true
            Log.d(TAG, "Xray-core started successfully on port $SOCKS_PORT")
            
            Result.success(SOCKS_PORT)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray-core", e)
            isRunningFlag = false
            Result.failure(XrayCoreException("Failed to start Xray-core: ${e.message}", e))
        }
    }
    
    /**
     * Stops Xray-core.
     */
    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isRunningFlag) {
                Log.w(TAG, "Xray-core is not running")
                return@withContext Result.success(Unit)
            }
            
            Log.d(TAG, "Stopping Xray-core...")
            coreController?.stopLoop()
            coreController = null
            isRunningFlag = false
            
            Log.d(TAG, "Xray-core stopped successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Xray-core", e)
            Result.failure(XrayCoreException("Failed to stop Xray-core: ${e.message}", e))
        }
    }
    
    /**
     * Checks if Xray-core is running.
     */
    fun isRunning(): Boolean = isRunningFlag
    
    /**
     * Gets the SOCKS5 proxy port.
     */
    fun getSocksPort(): Int = SOCKS_PORT
    
    /**
     * Builds Xray-core configuration JSON from profile.
     * 
     * @param tunFd Optional TUN file descriptor. If provided, uses TUN inbound.
     *              If null, uses SOCKS5 inbound.
     */
    fun buildConfig(profile: ServerProfile, uuid: String, tunFd: Int? = null): String {
        val vlessConfig = profile.getVlessConfig()
        
        return buildString {
            append("{")
            
            // Inbound: TUN or SOCKS5
            if (tunFd != null) {
                // TUN mode - Xray handles packet routing
                append("\"inbounds\":[{")
                append("\"protocol\":\"tun\",")
                append("\"tag\":\"tun-in\",")
                append("\"settings\":{")
                append("\"fd\":$tunFd,")
                append("\"mtu\":1500,")
                append("\"sniffing\":{")
                append("\"enabled\":true,")
                append("\"destOverride\":[\"http\",\"tls\"]")
                append("}")
                append("}")
                append("}],")
            } else {
                // SOCKS5 mode - for testing without VPN
                append("\"inbounds\":[{")
                append("\"port\":$SOCKS_PORT,")
                append("\"protocol\":\"socks\",")
                append("\"settings\":{")
                append("\"auth\":\"noauth\",")
                append("\"udp\":true")
                append("}")
                append("}],")
            }
            
            // Outbound: VLESS
            append("\"outbounds\":[{")
            append("\"protocol\":\"vless\",")
            append("\"settings\":{")
            append("\"vnext\":[{")
            append("\"address\":\"${profile.hostname}\",")
            append("\"port\":${profile.port},")
            append("\"users\":[{")
            append("\"id\":\"$uuid\",")
            append("\"encryption\":\"none\"")
            
            // IMPORTANT: Flow control (xtls-rprx-vision) triggers XUDP requirement
            // which has a bug in AndroidLibXrayLite v25.12.8
            // Omitting flow control to enable UDP without XUDP
            // This may reduce performance but allows UDP to work
            // TODO: Re-enable flow control when XUDP bug is fixed
            
            append("}]")
            append("}]")
            append("},")
            
            // Stream settings (transport)
            append("\"streamSettings\":{")
            append("\"network\":\"${vlessConfig.transport.name.lowercase()}\"")
            
            // Add transport-specific settings
            when (vlessConfig.transport) {
                TransportProtocol.TCP -> {
                    // TCP requires no additional settings
                }
                TransportProtocol.WEBSOCKET -> {
                    vlessConfig.websocketSettings?.let { ws ->
                        append(",\"wsSettings\":{")
                        append("\"path\":\"${ws.path}\"")
                        if (ws.headers.isNotEmpty()) {
                            append(",\"headers\":{")
                            append(ws.headers.entries.joinToString(",") { 
                                "\"${it.key}\":\"${it.value}\"" 
                            })
                            append("}")
                        }
                        append("}")
                    }
                }
                TransportProtocol.GRPC -> {
                    vlessConfig.grpcSettings?.let { grpc ->
                        append(",\"grpcSettings\":{")
                        append("\"serviceName\":\"${grpc.serviceName}\",")
                        append("\"multiMode\":${grpc.multiMode}")
                        append("}")
                    }
                }
                TransportProtocol.HTTP2 -> {
                    vlessConfig.http2Settings?.let { http2 ->
                        append(",\"httpSettings\":{")
                        append("\"path\":\"${http2.path}\"")
                        if (http2.host.isNotEmpty()) {
                            append(",\"host\":[")
                            append(http2.host.joinToString(",") { "\"$it\"" })
                            append("]")
                        }
                        append("}")
                    }
                }
            }
            
            // Add TLS settings
            vlessConfig.tlsSettings?.let { tls ->
                append(",\"security\":\"tls\",")
                append("\"tlsSettings\":{")
                append("\"serverName\":\"${tls.serverName}\"")
                if (tls.alpn.isNotEmpty()) {
                    append(",\"alpn\":[")
                    append(tls.alpn.joinToString(",") { "\"$it\"" })
                    append("]")
                }
                if (tls.allowInsecure) {
                    append(",\"allowInsecure\":true")
                }
                tls.fingerprint?.let { fp ->
                    append(",\"fingerprint\":\"$fp\"")
                }
                append("}")
            }
            
            // Add Reality settings
            vlessConfig.realitySettings?.let { reality ->
                append(",\"security\":\"reality\",")
                append("\"realitySettings\":{")
                append("\"serverName\":\"${reality.serverName}\",")
                append("\"publicKey\":\"${reality.publicKey}\",")
                append("\"shortId\":\"${reality.shortId}\"")
                reality.spiderX?.let { sx ->
                    append(",\"spiderX\":\"$sx\"")
                }
                reality.fingerprint?.let { fp ->
                    append(",\"fingerprint\":\"$fp\"")
                }
                append("}")
            }
            
            append("}")
            append("}]")
            append("}")
        }
    }
}

/**
 * Exception thrown by XrayCore.
 */
class XrayCoreException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
