package com.selfproxy.vpn.platform.vless.protocol

import android.util.Log
import com.selfproxy.vpn.platform.vless.transport.Transport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.UUID

/**
 * VLESS connection implementation.
 * 
 * Manages the complete VLESS connection lifecycle:
 * 1. Transport connection (TCP/TLS/Reality)
 * 2. VLESS handshake
 * 3. Data transfer
 * 4. Connection teardown
 * 
 * Usage:
 * ```
 * val connection = VlessConnection(uuid, transport)
 * connection.connect("example.com", 443).getOrThrow()
 * connection.send(data)
 * val received = connection.receive()
 * connection.close()
 * ```
 */
class VlessConnection(
    private val uuid: UUID,
    private val transport: Transport
) : Closeable {
    
    private var isHandshakeComplete = false
    
    companion object {
        private const val TAG = "VlessConnection"
        private const val BUFFER_SIZE = 8192
    }
    
    /**
     * Connects to the VLESS server and performs handshake.
     * 
     * @param targetAddress Target address to proxy to (usually the actual destination)
     * @param targetPort Target port to proxy to
     * @return Result indicating success or failure
     */
    suspend fun connect(targetAddress: String, targetPort: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting VLESS to $targetAddress:$targetPort")
            
            // Step 1: Establish transport connection
            val transportResult = transport.connect(targetAddress, targetPort)
            if (transportResult.isFailure) {
                return@withContext Result.failure(
                    VlessProtocolException(
                        "Transport connection failed",
                        transportResult.exceptionOrNull()
                    )
                )
            }
            
            Log.d(TAG, "Transport connected, performing VLESS handshake...")
            
            // Step 2: Build and send handshake request
            val handshakeRequest = VlessProtocol.buildHandshakeRequest(
                uuid = uuid,
                command = VlessProtocol.CMD_TCP,
                targetAddress = targetAddress,
                targetPort = targetPort
            )
            
            Log.d(TAG, "Sending handshake (${handshakeRequest.size} bytes)")
            val sendResult = transport.send(handshakeRequest)
            if (sendResult.isFailure) {
                return@withContext Result.failure(
                    VlessProtocolException(
                        "Failed to send handshake",
                        sendResult.exceptionOrNull()
                    )
                )
            }
            
            // Step 3: Receive and parse handshake response
            val responseBuffer = ByteArray(256)
            val receiveResult = transport.receive(responseBuffer)
            if (receiveResult.isFailure) {
                return@withContext Result.failure(
                    VlessProtocolException(
                        "Failed to receive handshake response",
                        receiveResult.exceptionOrNull()
                    )
                )
            }
            
            val bytesRead = receiveResult.getOrThrow()
            val responseData = responseBuffer.copyOf(bytesRead)
            
            Log.d(TAG, "Received handshake response ($bytesRead bytes)")
            
            // Step 4: Parse response
            val response = VlessProtocol.parseHandshakeResponse(responseData)
            Log.d(TAG, "Handshake successful (version: ${response.version})")
            
            isHandshakeComplete = true
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "VLESS connection failed", e)
            close()
            Result.failure(
                if (e is VlessProtocolException) e
                else VlessProtocolException("Connection failed: ${e.message}", e)
            )
        }
    }
    
    /**
     * Sends data through the VLESS tunnel.
     * 
     * @param data Data to send
     * @return Result indicating success or failure
     */
    suspend fun send(data: ByteArray): Result<Unit> {
        if (!isHandshakeComplete) {
            return Result.failure(VlessProtocolException("Handshake not complete"))
        }
        
        // Encode data (for basic VLESS, this is a no-op)
        val encodedData = VlessProtocol.encodeData(data)
        
        return transport.send(encodedData)
    }
    
    /**
     * Receives data from the VLESS tunnel.
     * 
     * @param buffer Buffer to read data into
     * @return Result containing number of bytes read
     */
    suspend fun receive(buffer: ByteArray = ByteArray(BUFFER_SIZE)): Result<ByteArray> {
        if (!isHandshakeComplete) {
            return Result.failure(VlessProtocolException("Handshake not complete"))
        }
        
        val receiveResult = transport.receive(buffer)
        if (receiveResult.isFailure) {
            return Result.failure(receiveResult.exceptionOrNull()!!)
        }
        
        val bytesRead = receiveResult.getOrThrow()
        val receivedData = buffer.copyOf(bytesRead)
        
        // Decode data (for basic VLESS, this is a no-op)
        val decodedData = VlessProtocol.decodeData(receivedData)
        
        return Result.success(decodedData)
    }
    
    /**
     * Checks if the connection is active.
     */
    fun isConnected(): Boolean {
        return isHandshakeComplete && transport.isConnected()
    }
    
    /**
     * Closes the VLESS connection.
     */
    override fun close() {
        try {
            transport.close()
            isHandshakeComplete = false
            Log.d(TAG, "VLESS connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VLESS connection", e)
        }
    }
}
