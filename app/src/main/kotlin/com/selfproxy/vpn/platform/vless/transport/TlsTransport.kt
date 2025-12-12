package com.selfproxy.vpn.platform.vless.transport

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * TLS transport implementation.
 * 
 * Provides encrypted TCP connectivity using TLS/SSL.
 * Supports:
 * - Custom SNI (Server Name Indication)
 * - ALPN (Application-Layer Protocol Negotiation)
 * - Certificate validation (can be disabled for testing)
 * - Custom TLS versions
 */
class TlsTransport(
    private val serverName: String,
    private val alpn: List<String> = emptyList(),
    private val allowInsecure: Boolean = false,
    private val connectTimeout: Int = 30000, // Increased to 30 seconds
    private val readTimeout: Int = 30000
) : Transport {
    
    private var sslSocket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    companion object {
        private const val TAG = "TlsTransport"
    }
    
    override suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to $host:$port with TLS (SNI: $serverName)")
            
            // Try to resolve the hostname first to see if DNS works
            try {
                val addresses = java.net.InetAddress.getAllByName(host)
                Log.d(TAG, "DNS resolved $host to ${addresses.size} address(es):")
                addresses.forEach { addr ->
                    Log.d(TAG, "  - ${addr.hostAddress}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "DNS resolution failed for $host", e)
                return@withContext Result.failure(
                    TransportException("DNS resolution failed: ${e.message}", e)
                )
            }
            
            // Create SSL context
            val sslContext = createSSLContext()
            val socketFactory = sslContext.socketFactory
            
            // Create underlying TCP socket first
            val plainSocket = java.net.Socket()
            plainSocket.soTimeout = readTimeout
            plainSocket.tcpNoDelay = true
            plainSocket.keepAlive = true
            plainSocket.reuseAddress = true
            
            Log.d(TAG, "Connecting plain socket first...")
            plainSocket.connect(InetSocketAddress(host, port), connectTimeout)
            Log.d(TAG, "Plain socket connected, wrapping with TLS...")
            
            // Wrap with SSL
            val newSocket = socketFactory.createSocket(
                plainSocket,
                host,
                port,
                true // autoClose
            ) as SSLSocket
            
            // Set SNI
            val sniHostName = javax.net.ssl.SNIHostName(serverName)
            val sslParameters = newSocket.sslParameters
            sslParameters.serverNames = listOf(sniHostName)
            
            // Set ALPN if provided
            if (alpn.isNotEmpty()) {
                sslParameters.applicationProtocols = alpn.toTypedArray()
            }
            
            newSocket.sslParameters = sslParameters
            
            // Start TLS handshake
            Log.d(TAG, "Starting TLS handshake...")
            val handshakeStart = System.currentTimeMillis()
            newSocket.startHandshake()
            val handshakeTime = System.currentTimeMillis() - handshakeStart
            
            sslSocket = newSocket
            inputStream = newSocket.inputStream
            outputStream = newSocket.outputStream
            
            Log.d(TAG, "TLS handshake completed in ${handshakeTime}ms")
            Log.d(TAG, "Protocol: ${newSocket.session.protocol}")
            Log.d(TAG, "Cipher Suite: ${newSocket.session.cipherSuite}")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish TLS connection to $host:$port", e)
            close()
            Result.failure(TransportException("TLS connection failed: ${e.message}", e))
        }
    }
    
    override suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val stream = outputStream ?: return@withContext Result.failure(
                TransportException("Not connected")
            )
            
            stream.write(data)
            stream.flush()
            
            Log.d(TAG, "Sent ${data.size} bytes over TLS")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data over TLS", e)
            Result.failure(TransportException("TLS send failed: ${e.message}", e))
        }
    }
    
    override suspend fun receive(buffer: ByteArray): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val stream = inputStream ?: return@withContext Result.failure(
                TransportException("Not connected")
            )
            
            val bytesRead = stream.read(buffer)
            
            if (bytesRead == -1) {
                Log.d(TAG, "TLS connection closed by remote")
                return@withContext Result.failure(
                    TransportException("Connection closed by remote")
                )
            }
            
            Log.d(TAG, "Received $bytesRead bytes over TLS")
            Result.success(bytesRead)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive data over TLS", e)
            Result.failure(TransportException("TLS receive failed: ${e.message}", e))
        }
    }
    
    override fun isConnected(): Boolean {
        return sslSocket?.isConnected == true && sslSocket?.isClosed == false
    }
    
    override fun close() {
        try {
            inputStream?.close()
            outputStream?.close()
            sslSocket?.close()
            
            Log.d(TAG, "TLS transport closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TLS transport", e)
        } finally {
            inputStream = null
            outputStream = null
            sslSocket = null
        }
    }
    
    /**
     * Creates an SSL context with appropriate trust manager.
     */
    private fun createSSLContext(): SSLContext {
        val sslContext = SSLContext.getInstance("TLS")
        
        val trustManagers = if (allowInsecure) {
            // Trust all certificates (for testing only!)
            arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
        } else {
            // Use system default trust manager
            null
        }
        
        sslContext.init(null, trustManagers, SecureRandom())
        return sslContext
    }
}
