package com.selfproxy.vpn.platform.vless.transport

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * HTTP-based transport that uses HttpURLConnection.
 * 
 * This mimics how browsers connect and may work better than raw sockets
 * in some network environments.
 */
class HttpTransport(
    private val serverName: String,
    private val allowInsecure: Boolean = false,
    private val connectTimeout: Int = 10000,
    private val readTimeout: Int = 30000
) : Transport {
    
    private var connection: HttpsURLConnection? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    companion object {
        private const val TAG = "HttpTransport"
    }
    
    override suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to https://$host:$port using HttpURLConnection")
            
            val url = URL("https://$host:$port/")
            val conn = url.openConnection() as HttpsURLConnection
            
            // Configure SSL if needed
            if (allowInsecure) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                conn.sslSocketFactory = sslContext.socketFactory
                conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            }
            
            // Set timeouts
            conn.connectTimeout = connectTimeout
            conn.readTimeout = readTimeout
            
            // Set method to CONNECT for tunneling
            conn.requestMethod = "CONNECT"
            conn.doInput = true
            conn.doOutput = true
            
            // Set headers to mimic browser
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            conn.setRequestProperty("Connection", "Upgrade")
            conn.setRequestProperty("Upgrade", "websocket")
            
            Log.d(TAG, "Attempting connection...")
            conn.connect()
            
            Log.d(TAG, "Connection established, response code: ${conn.responseCode}")
            
            connection = conn
            inputStream = conn.inputStream
            outputStream = conn.outputStream
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish HTTP connection", e)
            close()
            Result.failure(TransportException("HTTP connection failed: ${e.message}", e))
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
        return connection != null && inputStream != null && outputStream != null
    }
    
    override fun close() {
        try {
            inputStream?.close()
            outputStream?.close()
            connection?.disconnect()
            
            Log.d(TAG, "HTTP transport closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing HTTP transport", e)
        } finally {
            inputStream = null
            outputStream = null
            connection = null
        }
    }
}
