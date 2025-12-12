package com.selfproxy.vpn.platform.vless.transport

import java.io.Closeable

/**
 * Transport layer interface for VLESS connections.
 * 
 * Supports various transport protocols:
 * - TCP (plain)
 * - TLS (encrypted TCP)
 * - Reality (TLS with fingerprinting)
 * - WebSocket (future)
 * - gRPC (future)
 * - HTTP/2 (future)
 */
interface Transport : Closeable {
    
    /**
     * Connects to the remote server.
     * 
     * @param host Server hostname or IP address
     * @param port Server port
     * @return Result indicating success or failure
     */
    suspend fun connect(host: String, port: Int): Result<Unit>
    
    /**
     * Sends data through the transport.
     * 
     * @param data Data to send
     * @return Result indicating success or failure
     */
    suspend fun send(data: ByteArray): Result<Unit>
    
    /**
     * Receives data from the transport.
     * 
     * @param buffer Buffer to read data into
     * @return Result containing number of bytes read, or error
     */
    suspend fun receive(buffer: ByteArray): Result<Int>
    
    /**
     * Checks if the transport is connected.
     * 
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean
    
    /**
     * Closes the transport connection.
     */
    override fun close()
}

/**
 * Exception thrown when transport operations fail.
 */
class TransportException(message: String, cause: Throwable? = null) : Exception(message, cause)
