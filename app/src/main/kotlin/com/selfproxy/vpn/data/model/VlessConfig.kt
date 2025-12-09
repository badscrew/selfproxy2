package com.selfproxy.vpn.data.model

import kotlinx.serialization.Serializable

/**
 * VLESS protocol configuration.
 * 
 * Contains all VLESS-specific settings for a server profile.
 * The UUID credential is stored separately in the CredentialStore.
 */
@Serializable
data class VlessConfig(
    /**
     * Flow control mode for VLESS.
     */
    val flowControl: FlowControl = FlowControl.NONE,
    
    /**
     * Underlying transport protocol for VLESS connection.
     */
    val transport: TransportProtocol,
    
    /**
     * TLS configuration (optional, but recommended).
     */
    val tlsSettings: TlsSettings? = null,
    
    /**
     * Reality protocol settings for advanced obfuscation (optional).
     */
    val realitySettings: RealitySettings? = null,
    
    /**
     * WebSocket-specific settings (only used when transport is WEBSOCKET).
     */
    val websocketSettings: WebSocketSettings? = null,
    
    /**
     * gRPC-specific settings (only used when transport is GRPC).
     */
    val grpcSettings: GrpcSettings? = null,
    
    /**
     * HTTP/2-specific settings (only used when transport is HTTP2).
     */
    val http2Settings: Http2Settings? = null
) {
    init {
        if (transport == TransportProtocol.WEBSOCKET) {
            require(websocketSettings != null) { "WebSocket settings required for WebSocket transport" }
        }
        if (transport == TransportProtocol.GRPC) {
            require(grpcSettings != null) { "gRPC settings required for gRPC transport" }
        }
        if (transport == TransportProtocol.HTTP2) {
            require(http2Settings != null) { "HTTP/2 settings required for HTTP/2 transport" }
        }
    }
}

/**
 * Flow control modes for VLESS protocol.
 */
@Serializable
enum class FlowControl {
    /**
     * No flow control (standard VLESS).
     */
    NONE,
    
    /**
     * XTLS-RPRX-Vision flow control for enhanced performance.
     */
    XTLS_RPRX_VISION
}

/**
 * Transport protocols supported by VLESS.
 */
@Serializable
enum class TransportProtocol {
    /**
     * Raw TCP transport (simplest, most compatible).
     */
    TCP,
    
    /**
     * WebSocket transport (good for HTTP-based networks).
     */
    WEBSOCKET,
    
    /**
     * gRPC transport (efficient multiplexing).
     */
    GRPC,
    
    /**
     * HTTP/2 transport (good balance of performance and compatibility).
     */
    HTTP2
}

/**
 * TLS configuration for VLESS connections.
 */
@Serializable
data class TlsSettings(
    /**
     * Server Name Indication (SNI) for TLS handshake.
     */
    val serverName: String,
    
    /**
     * Application-Layer Protocol Negotiation (ALPN) protocols.
     * Example: ["h2", "http/1.1"]
     */
    val alpn: List<String> = emptyList(),
    
    /**
     * Whether to allow insecure TLS connections (not recommended).
     */
    val allowInsecure: Boolean = false,
    
    /**
     * Fingerprint for certificate pinning (optional).
     */
    val fingerprint: String? = null
) {
    init {
        require(serverName.isNotBlank()) { "Server name cannot be blank" }
    }
}

/**
 * Reality protocol settings for advanced obfuscation.
 */
@Serializable
data class RealitySettings(
    /**
     * Server name for Reality protocol.
     */
    val serverName: String,
    
    /**
     * Public key for Reality protocol.
     */
    val publicKey: String,
    
    /**
     * Short ID for Reality protocol.
     */
    val shortId: String,
    
    /**
     * Spider X parameter for Reality protocol.
     */
    val spiderX: String? = null,
    
    /**
     * Fingerprint for Reality protocol.
     */
    val fingerprint: String? = null
) {
    init {
        require(serverName.isNotBlank()) { "Server name cannot be blank" }
        require(publicKey.isNotBlank()) { "Public key cannot be blank" }
        require(shortId.isNotBlank()) { "Short ID cannot be blank" }
    }
}

/**
 * WebSocket-specific transport settings.
 */
@Serializable
data class WebSocketSettings(
    /**
     * WebSocket path.
     * Example: "/ws" or "/ray"
     */
    val path: String = "/",
    
    /**
     * Custom HTTP headers for WebSocket connection.
     */
    val headers: Map<String, String> = emptyMap()
) {
    init {
        require(path.isNotBlank()) { "WebSocket path cannot be blank" }
    }
}

/**
 * gRPC-specific transport settings.
 */
@Serializable
data class GrpcSettings(
    /**
     * gRPC service name.
     */
    val serviceName: String,
    
    /**
     * Whether to use multi-mode gRPC.
     */
    val multiMode: Boolean = false
) {
    init {
        require(serviceName.isNotBlank()) { "Service name cannot be blank" }
    }
}

/**
 * HTTP/2-specific transport settings.
 */
@Serializable
data class Http2Settings(
    /**
     * HTTP/2 path.
     */
    val path: String = "/",
    
    /**
     * HTTP/2 host.
     */
    val host: List<String> = emptyList()
) {
    init {
        require(path.isNotBlank()) { "HTTP/2 path cannot be blank" }
    }
}
