package com.selfproxy.vpn.platform.vless.transport

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress

/**
 * OkHttp-based transport that uses OkHttp's connection pool and TLS configuration.
 * 
 * OkHttp has better TLS fingerprinting and connection handling than raw sockets,
 * which may work better with Reality servers that expect browser-like connections.
 * 
 * This uses OkHttp's SSLSocketFactory to create the socket, which mimics browser behavior.
 */
class OkHttpTransport(
    private val serverName: String,
    private val allowInsecure: Boolean = false
) : Transport {
    
    private var socket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    companion object {
        private const val TAG = "OkHttpTransport"
    }
    
    override suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to $host:$port using OkHttp SSLSocketFactory (SNI: $serverName)")
            
            // Build OkHttp client to get its SSLSocketFactory
            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
            
            // Configure SSL
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            if (allowInsecure) {
                clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                clientBuilder.hostnameVerifier { _, _ -> true }
            }
            
            val client = clientBuilder.build()
            
            // Use OkHttp's SSLSocketFactory to create socket
            val sslSocketFactory = client.sslSocketFactory
            
            Log.d(TAG, "Creating SSL socket...")
            val sslSocket = sslSocketFactory.createSocket() as SSLSocket
            
            // Set SNI hostname
            val sslParams = sslSocket.sslParameters
            sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(serverName))
            sslSocket.sslParameters = sslParams
            
            Log.d(TAG, "Connecting socket to $host:$port...")
            sslSocket.connect(InetSocketAddress(host, port), 30000)
            
            Log.d(TAG, "Starting TLS handshake with SNI: $serverName...")
            sslSocket.startHandshake()
            
            Log.d(TAG, "TLS handshake successful!")
            Log.d(TAG, "Protocol: ${sslSocket.session.protocol}")
            Log.d(TAG, "Cipher suite: ${sslSocket.session.cipherSuite}")
            
            socket = sslSocket
            inputStream = sslSocket.inputStream
            outputStream = sslSocket.outputStream
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish OkHttp connection", e)
            close()
            Result.failure(TransportException("OkHttp connection failed: ${e.message}", e))
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
        return socket != null && socket?.isConnected == true && !socket!!.isClosed
    }
    
    override fun close() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            
            Log.d(TAG, "OkHttp transport closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing OkHttp transport", e)
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
}
