package com.selfproxy.vpn.data.config

import android.net.Uri
import com.selfproxy.vpn.data.model.*
import com.selfproxy.vpn.domain.model.Protocol

/**
 * Exporter for VLESS URI format.
 * 
 * Generates VLESS URIs from ServerProfile objects.
 * Format: vless://UUID@hostname:port?parameters#name
 */
object VlessUriExporter {
    
    /**
     * Exports a VLESS profile to URI format.
     * 
     * @param profile The ServerProfile with VLESS configuration
     * @param uuid The UUID credential for authentication
     * @return The VLESS URI string
     */
    fun export(profile: ServerProfile, uuid: String): String {
        require(profile.protocol == Protocol.VLESS) {
            "Profile must be a VLESS profile"
        }
        
        val config = profile.getVlessConfig()
        
        val uriBuilder = Uri.Builder()
            .scheme("vless")
            .encodedAuthority("$uuid@${profile.hostname}:${profile.port}")
            .fragment(profile.name)
        
        // Add transport type
        val transportType = when (config.transport) {
            TransportProtocol.TCP -> "tcp"
            TransportProtocol.WEBSOCKET -> "ws"
            TransportProtocol.GRPC -> "grpc"
            TransportProtocol.HTTP2 -> "h2"
        }
        uriBuilder.appendQueryParameter("type", transportType)
        
        // Add flow control
        if (config.flowControl != FlowControl.NONE) {
            val flowValue = when (config.flowControl) {
                FlowControl.XTLS_RPRX_VISION -> "xtls-rprx-vision"
                else -> null
            }
            flowValue?.let { uriBuilder.appendQueryParameter("flow", it) }
        }
        
        // Add security settings
        when {
            config.tlsSettings != null -> {
                uriBuilder.appendQueryParameter("security", "tls")
                val tls = config.tlsSettings
                uriBuilder.appendQueryParameter("sni", tls.serverName)
                
                if (tls.alpn.isNotEmpty()) {
                    uriBuilder.appendQueryParameter("alpn", tls.alpn.joinToString(","))
                }
                
                if (tls.allowInsecure) {
                    uriBuilder.appendQueryParameter("allowInsecure", "1")
                }
                
                tls.fingerprint?.let {
                    uriBuilder.appendQueryParameter("fp", it)
                }
            }
            config.realitySettings != null -> {
                uriBuilder.appendQueryParameter("security", "reality")
                val reality = config.realitySettings
                uriBuilder.appendQueryParameter("sni", reality.serverName)
                uriBuilder.appendQueryParameter("pbk", reality.publicKey)
                uriBuilder.appendQueryParameter("sid", reality.shortId)
                
                reality.spiderX?.let {
                    uriBuilder.appendQueryParameter("spx", it)
                }
                
                reality.fingerprint?.let {
                    uriBuilder.appendQueryParameter("fp", it)
                }
            }
            else -> {
                uriBuilder.appendQueryParameter("security", "none")
            }
        }
        
        // Add transport-specific settings
        when (config.transport) {
            TransportProtocol.WEBSOCKET -> {
                config.websocketSettings?.let { ws ->
                    uriBuilder.appendQueryParameter("path", ws.path)
                    ws.headers["Host"]?.let { host ->
                        uriBuilder.appendQueryParameter("host", host)
                    }
                }
            }
            TransportProtocol.GRPC -> {
                config.grpcSettings?.let { grpc ->
                    uriBuilder.appendQueryParameter("serviceName", grpc.serviceName)
                    if (grpc.multiMode) {
                        uriBuilder.appendQueryParameter("mode", "multi")
                    }
                }
            }
            TransportProtocol.HTTP2 -> {
                config.http2Settings?.let { http2 ->
                    uriBuilder.appendQueryParameter("path", http2.path)
                    if (http2.host.isNotEmpty()) {
                        uriBuilder.appendQueryParameter("host", http2.host.joinToString(","))
                    }
                }
            }
            TransportProtocol.TCP -> {
                // No additional parameters for TCP
            }
        }
        
        return uriBuilder.build().toString()
    }
    
    /**
     * Exports a VLESS profile to JSON format compatible with Xray-core.
     * 
     * @param profile The ServerProfile with VLESS configuration
     * @param uuid The UUID credential for authentication
     * @return The JSON configuration string
     */
    fun exportToJson(profile: ServerProfile, uuid: String): String {
        require(profile.protocol == Protocol.VLESS) {
            "Profile must be a VLESS profile"
        }
        
        val config = profile.getVlessConfig()
        
        return buildString {
            appendLine("{")
            appendLine("  \"outbounds\": [")
            appendLine("    {")
            appendLine("      \"protocol\": \"vless\",")
            appendLine("      \"settings\": {")
            appendLine("        \"vnext\": [")
            appendLine("          {")
            appendLine("            \"address\": \"${profile.hostname}\",")
            appendLine("            \"port\": ${profile.port},")
            appendLine("            \"users\": [")
            appendLine("              {")
            appendLine("                \"id\": \"$uuid\",")
            appendLine("                \"encryption\": \"none\",")
            
            if (config.flowControl != FlowControl.NONE) {
                val flow = when (config.flowControl) {
                    FlowControl.XTLS_RPRX_VISION -> "xtls-rprx-vision"
                    else -> ""
                }
                appendLine("                \"flow\": \"$flow\",")
            }
            
            appendLine("                \"level\": 0")
            appendLine("              }")
            appendLine("            ]")
            appendLine("          }")
            appendLine("        ]")
            appendLine("      },")
            
            // Stream settings
            appendLine("      \"streamSettings\": {")
            appendLine("        \"network\": \"${getNetworkType(config.transport)}\",")
            
            // Security
            when {
                config.tlsSettings != null -> {
                    appendLine("        \"security\": \"tls\",")
                    appendLine("        \"tlsSettings\": {")
                    appendLine("          \"serverName\": \"${config.tlsSettings.serverName}\",")
                    appendLine("          \"allowInsecure\": ${config.tlsSettings.allowInsecure}")
                    if (config.tlsSettings.alpn.isNotEmpty()) {
                        appendLine("          ,\"alpn\": [${config.tlsSettings.alpn.joinToString(",") { "\"$it\"" }}]")
                    }
                    appendLine("        },")
                }
                config.realitySettings != null -> {
                    appendLine("        \"security\": \"reality\",")
                    appendLine("        \"realitySettings\": {")
                    appendLine("          \"serverName\": \"${config.realitySettings.serverName}\",")
                    appendLine("          \"publicKey\": \"${config.realitySettings.publicKey}\",")
                    appendLine("          \"shortId\": \"${config.realitySettings.shortId}\"")
                    config.realitySettings.spiderX?.let {
                        appendLine("          ,\"spiderX\": \"$it\"")
                    }
                    appendLine("        },")
                }
                else -> {
                    appendLine("        \"security\": \"none\",")
                }
            }
            
            // Transport settings
            when (config.transport) {
                TransportProtocol.WEBSOCKET -> {
                    config.websocketSettings?.let { ws ->
                        appendLine("        \"wsSettings\": {")
                        appendLine("          \"path\": \"${ws.path}\"")
                        if (ws.headers.isNotEmpty()) {
                            appendLine("          ,\"headers\": {")
                            ws.headers.entries.forEachIndexed { index, entry ->
                                val comma = if (index < ws.headers.size - 1) "," else ""
                                appendLine("            \"${entry.key}\": \"${entry.value}\"$comma")
                            }
                            appendLine("          }")
                        }
                        appendLine("        }")
                    }
                }
                TransportProtocol.GRPC -> {
                    config.grpcSettings?.let { grpc ->
                        appendLine("        \"grpcSettings\": {")
                        appendLine("          \"serviceName\": \"${grpc.serviceName}\",")
                        appendLine("          \"multiMode\": ${grpc.multiMode}")
                        appendLine("        }")
                    }
                }
                TransportProtocol.HTTP2 -> {
                    config.http2Settings?.let { http2 ->
                        appendLine("        \"httpSettings\": {")
                        appendLine("          \"path\": \"${http2.path}\"")
                        if (http2.host.isNotEmpty()) {
                            appendLine("          ,\"host\": [${http2.host.joinToString(",") { "\"$it\"" }}]")
                        }
                        appendLine("        }")
                    }
                }
                TransportProtocol.TCP -> {
                    // No additional settings for TCP
                }
            }
            
            appendLine("      }")
            appendLine("    }")
            appendLine("  ]")
            appendLine("}")
        }
    }
    
    private fun getNetworkType(transport: TransportProtocol): String {
        return when (transport) {
            TransportProtocol.TCP -> "tcp"
            TransportProtocol.WEBSOCKET -> "ws"
            TransportProtocol.GRPC -> "grpc"
            TransportProtocol.HTTP2 -> "http"
        }
    }
}
