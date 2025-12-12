package com.selfproxy.vpn.platform.vless.transport

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Plain TCP transport implementation.
 * 
 * Provides basic TCP socket connectivity without encryption.
 * Should be wrapped with TLS for production use.
 */
class TcpTransport(
    private val connectTimeout: Int = 10000,
    private val readTimeout: Int = 30000
) : Transport {
    
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    companion object {
        private const val TAG = "TcpTransport"
    }
    
    override suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to $host:$port")
            
            val newSocket = Socket()
            newSocket.soTimeout = readTimeout
            newSocket.tcpNoDelay = true // Disable Nagle's algorithm for lower latency
            newSocket.keepAlive = true
            
            newSocket.connect(InetSocketAddress(host, port), connectTimeout)
            
            socket = newSocket
            inputStream = newSocket.getInputStream()
            outputStream = newSocket.getOutputStream()
            
            Log.d(TAG, "Connected successfully to $host:$port")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to $host:$port", e)
            close()
            Result.failure(TransportException("TCP connection failed: ${e.message}", e))
        }
    }
    
    override suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val stream = outputStream ?: return@withContext Result.failure(
                TransportException("Not connected")
            )
            
            stream.write(data)
            stream.flush()
            
            Log.d(TAG, "Sent ${data.size} bytes")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data", e)
            Result.failure(TransportException("Send failed: ${e.message}", e))
        }
    }
    
    override suspend fun receive(buffer: ByteArray): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val stream = inputStream ?: return@withContext Result.failure(
                TransportException("Not connected")
            )
            
            val bytesRead = stream.read(buffer)
            
            if (bytesRead == -1) {
                Log.d(TAG, "Connection closed by remote")
                return@withContext Result.failure(
                    TransportException("Connection closed by remote")
                )
            }
            
            Log.d(TAG, "Received $bytesRead bytes")
            Result.success(bytesRead)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive data", e)
            Result.failure(TransportException("Receive failed: ${e.message}", e))
        }
    }
    
    override fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false
    }
    
    override fun close() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            
            Log.d(TAG, "Transport closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing transport", e)
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
}
