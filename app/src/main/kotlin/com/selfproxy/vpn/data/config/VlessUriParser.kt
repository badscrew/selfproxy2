package com.selfproxy.vpn.data.config

import com.selfproxy.vpn.data.model.*
import kotlinx.serialization.json.Json
import java.net.URLDecoder

/**
 * Parser for VLESS URI format.
 * 
 * Parses VLESS URIs in the format:
 * vless://UUID@hostname:port?parameters#name
 * 
 * Example:
 * vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@example.com:443?type=ws&path=/ws&security=tls&sni=example.com#MyServer
 */
object VlessUriParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Parses a VLESS URI string.
     * 
     * @param uriString The VLESS URI string
     * @return Result containing ParsedVlessConfig on success or error on failure
     */
    fun parse(uriString: String): Result<ParsedVlessConfig> {
        return try {
            // Validate scheme
            if (!uriString.startsWith("vless://")) {
                return Result.failure(ConfigParseException("Invalid VLESS URI. Must start with vless://"))
            }
            
            val uri = parseUri(uriString)
            
            // Extract UUID from userInfo
            val uuid = uri.userInfo
                ?: return Result.failure(ConfigParseException("Missing UUID in VLESS URI"))
            
            if (!isValidUuid(uuid)) {
                return Result.failure(ConfigParseException("Invalid UUID format. Expected RFC 4122 compliant UUID"))
            }
            
            // Extract hostname and port
            val hostname = uri.host
                ?: return Result.failure(ConfigParseException("Missing hostname in VLESS URI"))
            
            val port = uri.port
            if (port == -1 || port !in 1..65535) {
                return Result.failure(ConfigParseException("Invalid or missing port in VLESS URI"))
            }
            
            // Extract profile name from fragment
            val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: hostname
            
            // Parse query parameters
            val type = uri.queryParams["type"] ?: "tcp"
            val security = uri.queryParams["security"] ?: "none"
            val flow = uri.queryParams["flow"]
            
            // Parse transport protocol
            val transport = when (type.lowercase()) {
                "tcp" -> TransportProtocol.TCP
                "ws", "websocket" -> TransportProtocol.WEBSOCKET
                "grpc" -> TransportProtocol.GRPC
                "http", "h2" -> TransportProtocol.HTTP2
                else -> return Result.failure(ConfigParseException("Unsupported transport type: $type"))
            }
            
            // Parse flow control
            val flowControl = when (flow?.lowercase()) {
                null, "none", "" -> FlowControl.NONE
                "xtls-rprx-vision" -> FlowControl.XTLS_RPRX_VISION
                else -> return Result.failure(ConfigParseException("Unsupported flow control: $flow"))
            }
            
            // Parse TLS settings
            val tlsSettings = if (security == "tls") {
                val sni = uri.queryParams["sni"] ?: hostname
                val alpn = uri.queryParams["alpn"]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?: emptyList()
                val allowInsecure = uri.queryParams["allowInsecure"] == "1"
                val fingerprint = uri.queryParams["fp"]
                
                TlsSettings(
                    serverName = sni,
                    alpn = alpn,
                    allowInsecure = allowInsecure,
                    fingerprint = fingerprint
                )
            } else null
            
            // Parse Reality settings
            val realitySettings = if (security == "reality") {
                val sni = uri.queryParams["sni"]
                    ?: return Result.failure(ConfigParseException("Missing SNI for Reality protocol"))
                val publicKey = uri.queryParams["pbk"]
                    ?: return Result.failure(ConfigParseException("Missing public key for Reality protocol"))
                val shortId = uri.queryParams["sid"]
                    ?: return Result.failure(ConfigParseException("Missing short ID for Reality protocol"))
                val spiderX = uri.queryParams["spx"]
                val fingerprint = uri.queryParams["fp"]
                
                RealitySettings(
                    serverName = sni,
                    publicKey = publicKey,
                    shortId = shortId,
                    spiderX = spiderX,
                    fingerprint = fingerprint
                )
            } else null
            
            // Parse transport-specific settings
            val websocketSettings = if (transport == TransportProtocol.WEBSOCKET) {
                val path = uri.queryParams["path"] ?: "/"
                val host = uri.queryParams["host"]
                val headers = if (host != null) {
                    mapOf("Host" to host)
                } else {
                    emptyMap()
                }
                
                WebSocketSettings(
                    path = path,
                    headers = headers
                )
            } else null
            
            val grpcSettings = if (transport == TransportProtocol.GRPC) {
                val serviceName = uri.queryParams["serviceName"]
                    ?: return Result.failure(ConfigParseException("Missing serviceName for gRPC transport"))
                val multiMode = uri.queryParams["mode"] == "multi"
                
                GrpcSettings(
                    serviceName = serviceName,
                    multiMode = multiMode
                )
            } else null
            
            val http2Settings = if (transport == TransportProtocol.HTTP2) {
                val path = uri.queryParams["path"] ?: "/"
                val host = uri.queryParams["host"]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?: emptyList()
                
                Http2Settings(
                    path = path,
                    host = host
                )
            } else null
            
            // Ensure transport-specific settings are present
            val finalWebsocketSettings = if (transport == TransportProtocol.WEBSOCKET) {
                websocketSettings ?: WebSocketSettings(path = "/")
            } else null
            
            val finalGrpcSettings = if (transport == TransportProtocol.GRPC) {
                grpcSettings ?: return Result.failure(
                    ConfigParseException("Missing serviceName for gRPC transport")
                )
            } else null
            
            val finalHttp2Settings = if (transport == TransportProtocol.HTTP2) {
                http2Settings ?: Http2Settings(path = "/")
            } else null
            
            val config = VlessConfig(
                flowControl = flowControl,
                transport = transport,
                tlsSettings = tlsSettings,
                realitySettings = realitySettings,
                websocketSettings = finalWebsocketSettings,
                grpcSettings = finalGrpcSettings,
                http2Settings = finalHttp2Settings
            )
            
            Result.success(
                ParsedVlessConfig(
                    name = name,
                    hostname = hostname,
                    port = port,
                    uuid = uuid,
                    config = config
                )
            )
        } catch (e: Exception) {
            Result.failure(ConfigParseException("Failed to parse VLESS URI: ${e.message}", e))
        }
    }
    
    /**
     * Validates if a string is a valid UUID (RFC 4122).
     */
    private fun isValidUuid(uuid: String): Boolean {
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        return uuidRegex.matches(uuid)
    }
    
    /**
     * Simple URI parser that doesn't depend on Android APIs.
     * Parses URIs in the format: scheme://userInfo@host:port/path?query#fragment
     */
    private fun parseUri(uriString: String): ParsedUri {
        var remaining = uriString
        
        // Extract fragment
        val fragmentIndex = remaining.indexOf('#')
        val fragment = if (fragmentIndex != -1) {
            remaining.substring(fragmentIndex + 1).also {
                remaining = remaining.substring(0, fragmentIndex)
            }
        } else null
        
        // Extract query
        val queryIndex = remaining.indexOf('?')
        val queryString = if (queryIndex != -1) {
            remaining.substring(queryIndex + 1).also {
                remaining = remaining.substring(0, queryIndex)
            }
        } else null
        
        // Parse query parameters
        val queryParams = mutableMapOf<String, String>()
        queryString?.split('&')?.forEach { param ->
            val parts = param.split('=', limit = 2)
            if (parts.size == 2) {
                queryParams[URLDecoder.decode(parts[0], "UTF-8")] = 
                    URLDecoder.decode(parts[1], "UTF-8")
            }
        }
        
        // Extract scheme
        val schemeIndex = remaining.indexOf("://")
        if (schemeIndex == -1) {
            throw IllegalArgumentException("Invalid URI: missing scheme")
        }
        remaining = remaining.substring(schemeIndex + 3)
        
        // Extract userInfo
        val atIndex = remaining.indexOf('@')
        val userInfo = if (atIndex != -1) {
            remaining.substring(0, atIndex).also {
                remaining = remaining.substring(atIndex + 1)
            }
        } else null
        
        // Extract host and port
        val colonIndex = remaining.indexOf(':')
        val (host, port) = if (colonIndex != -1) {
            val h = remaining.substring(0, colonIndex)
            val p = remaining.substring(colonIndex + 1).toIntOrNull() ?: -1
            h to p
        } else {
            remaining to -1
        }
        
        return ParsedUri(
            userInfo = userInfo,
            host = if (host.isNotEmpty()) host else null,
            port = port,
            queryParams = queryParams,
            fragment = fragment
        )
    }
    
    private data class ParsedUri(
        val userInfo: String?,
        val host: String?,
        val port: Int,
        val queryParams: Map<String, String>,
        val fragment: String?
    )
}

/**
 * Result of parsing a VLESS URI.
 * Contains all data needed to create a ServerProfile and store credentials.
 */
data class ParsedVlessConfig(
    val name: String,
    val hostname: String,
    val port: Int,
    val uuid: String,
    val config: VlessConfig
)
