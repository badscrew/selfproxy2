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
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Transport that binds to the active network explicitly.
 * 
 * This ensures the socket uses the system's active network interface,
 * which might be required on some Android devices.
 */
class NetworkBoundTransport(
    private val context: Context,
    private val serverName: String,
    private val allowInsecure: Boolean = false
) : Transport {
    
    private var socket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    companion object {
        private const val TAG = "NetworkBoundTransport"
    }
    
    override suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to $host:$port using network-bound socket (SNI: $serverName)")
            
            // Get the active network
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: Network? = connectivityManager.activeNetwork
            
            if (activeNetwork == null) {
                Log.e(TAG, "No active network available")
                return@withContext Result.failure(TransportException("No active network"))
            }
            
            Log.d(TAG, "Active network: $activeNetwork")
            
            // Create SSL context
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, if (allowInsecure) trustAllCerts else null, java.security.SecureRandom())
            
            // Create socket using the active network's socket factory
            Log.d(TAG, "Creating socket bound to active network...")
            val plainSocket = activeNetwork.socketFactory.createSocket()
            
            Log.d(TAG, "Connecting plain socket to $host:$port...")
            plainSocket.connect(InetSocketAddress(host, port), 30000)
            
            Log.d(TAG, "Plain socket connected, wrapping with SSL...")
            
            // Wrap with SSL
            val sslSocket = sslContext.socketFactory.createSocket(
                plainSocket,
                host,
                port,
                true
            ) as SSLSocket
            
            // Set SNI hostname
            val sslParams = sslSocket.sslParameters
            sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(serverName))
            sslSocket.sslParameters = sslParams
            
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
            Log.e(TAG, "Failed to establish network-bound connection", e)
            close()
            Result.failure(TransportException("Network-bound connection failed: ${e.message}", e))
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
            
            Log.d(TAG, "Network-bound transport closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing network-bound transport", e)
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
}
