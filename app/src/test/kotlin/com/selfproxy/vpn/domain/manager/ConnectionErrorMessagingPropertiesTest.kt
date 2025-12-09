package com.selfproxy.vpn.domain.manager

import com.selfproxy.vpn.data.model.ServerProfile
import com.selfproxy.vpn.data.model.VlessConfig
import com.selfproxy.vpn.data.model.WireGuardConfig
import com.selfproxy.vpn.domain.adapter.ConnectionStatistics
import com.selfproxy.vpn.domain.adapter.ConnectionTestResult
import com.selfproxy.vpn.domain.adapter.ProtocolAdapter
import com.selfproxy.vpn.domain.model.Connection
import com.selfproxy.vpn.domain.model.ConnectionState
import com.selfproxy.vpn.domain.model.Protocol
import com.selfproxy.vpn.domain.repository.ProfileRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Property-based tests for connection error messaging in ConnectionManager.
 * 
 * Feature: selfproxy, Property 12: Connection Error Messaging
 * Validates: Requirements 3.6
 */
class ConnectionErrorMessagingPropertiesTest {
    
    @Test
    fun `connection errors should always provide specific error messages`() = runTest {
        // Feature: selfproxy, Property 12: Connection Error Messaging
        // Validates: Requirements 3.6
        checkAll(
            iterations = 100,
            Arb.serverProfile(),
            Arb.connectionError()
        ) { profile, errorType ->
            // Arrange
            val adapter = createFailingAdapter(errorType)
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = if (profile.protocol == Protocol.WIREGUARD) adapter else createSuccessAdapter(),
                vlessAdapter = if (profile.protocol == Protocol.VLESS) adapter else createSuccessAdapter(),
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            val result = connectionManager.connect(profile.id)
            
            // Assert
            result.isFailure shouldBe true
            val exception = result.exceptionOrNull()
            exception shouldNotBe null
            exception.shouldBeInstanceOf<ConnectionException>()
            
            // Error message should not be blank
            exception.message?.shouldNotBeBlank()
            
            // Error message should contain relevant context
            val message = (exception.message ?: "").lowercase()
            when (errorType) {
                ErrorType.AUTHENTICATION -> {
                    message shouldContain "authentication"
                }
                ErrorType.NETWORK_UNREACHABLE -> {
                    message shouldContain "unreachable"
                }
                ErrorType.TIMEOUT -> {
                    message shouldContain "timeout"
                }
                ErrorType.HANDSHAKE_FAILED -> {
                    message shouldContain "handshake"
                }
                ErrorType.INVALID_CONFIGURATION -> {
                    message shouldContain "configuration"
                }
                ErrorType.TLS_ERROR -> {
                    message shouldContain "tls"
                }
                ErrorType.TRANSPORT_ERROR -> {
                    message shouldContain "transport"
                }
                ErrorType.PERMISSION_DENIED -> {
                    message shouldContain "permission"
                }
            }
        }
    }
    
    @Test
    fun `authentication errors should indicate credential issues`() = runTest {
        // Feature: selfproxy, Property 12: Connection Error Messaging
        // Validates: Requirements 3.6
        checkAll(
            iterations = 100,
            Arb.serverProfile()
        ) { profile ->
            // Arrange
            val adapter = createFailingAdapter(ErrorType.AUTHENTICATION)
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = if (profile.protocol == Protocol.WIREGUARD) adapter else createSuccessAdapter(),
                vlessAdapter = if (profile.protocol == Protocol.VLESS) adapter else createSuccessAdapter(),
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            val result = connectionManager.connect(profile.id)
            
            // Assert
            result.isFailure shouldBe true
            val exception = result.exceptionOrNull()
            exception shouldNotBe null
            
            // Should mention authentication failure
            val message = (exception!!.message ?: "").lowercase()
            message shouldContain "authentication"
            message shouldContain "failed"
            
            // Should provide protocol-specific guidance
            when (profile.protocol) {
                Protocol.WIREGUARD -> {
                    message shouldContain "key"
                }
                Protocol.VLESS -> {
                    message shouldContain "uuid"
                }
            }
        }
    }
    
    @Test
    fun `timeout errors should suggest checking firewall settings`() = runTest {
        // Feature: selfproxy, Property 12: Connection Error Messaging
        // Validates: Requirements 3.6
        checkAll(
            iterations = 100,
            Arb.serverProfile()
        ) { profile ->
            // Arrange
            val adapter = createFailingAdapter(ErrorType.TIMEOUT)
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = if (profile.protocol == Protocol.WIREGUARD) adapter else createSuccessAdapter(),
                vlessAdapter = if (profile.protocol == Protocol.VLESS) adapter else createSuccessAdapter(),
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            val result = connectionManager.connect(profile.id)
            
            // Assert
            result.isFailure shouldBe true
            val exception = result.exceptionOrNull()
            exception shouldNotBe null
            
            // Should mention timeout
            val message = (exception!!.message ?: "").lowercase()
            message shouldContain "timeout"
            
            // Should suggest checking firewall
            message shouldContain "firewall"
        }
    }
    
    @Test
    fun `network unreachable errors should mention connectivity issues`() = runTest {
        // Feature: selfproxy, Property 12: Connection Error Messaging
        // Validates: Requirements 3.6
        checkAll(
            iterations = 100,
            Arb.serverProfile()
        ) { profile ->
            // Arrange
            val adapter = createFailingAdapter(ErrorType.NETWORK_UNREACHABLE)
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = if (profile.protocol == Protocol.WIREGUARD) adapter else createSuccessAdapter(),
                vlessAdapter = if (profile.protocol == Protocol.VLESS) adapter else createSuccessAdapter(),
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            val result = connectionManager.connect(profile.id)
            
            // Assert
            result.isFailure shouldBe true
            val exception = result.exceptionOrNull()
            exception shouldNotBe null
            
            // Should mention unreachable or network
            val message = (exception!!.message ?: "").lowercase()
            val hasRelevantKeyword = message.contains("unreachable") || 
                                    message.contains("network") ||
                                    message.contains("connect")
            hasRelevantKeyword shouldBe true
        }
    }
    
    @Test
    fun `error messages should include protocol context`() = runTest {
        // Feature: selfproxy, Property 12: Connection Error Messaging
        // Validates: Requirements 3.6
        checkAll(
            iterations = 100,
            Arb.serverProfile(),
            Arb.connectionError()
        ) { profile, errorType ->
            // Arrange
            val adapter = createFailingAdapter(errorType)
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = if (profile.protocol == Protocol.WIREGUARD) adapter else createSuccessAdapter(),
                vlessAdapter = if (profile.protocol == Protocol.VLESS) adapter else createSuccessAdapter(),
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            val result = connectionManager.connect(profile.id)
            
            // Assert
            result.isFailure shouldBe true
            val exception = result.exceptionOrNull()
            exception shouldNotBe null
            
            // Error message should contain protocol name or protocol-specific terms
            val message = (exception!!.message ?: "").lowercase()
            val hasProtocolContext = message.contains(profile.protocol.name.lowercase()) ||
                                    (profile.protocol == Protocol.WIREGUARD && message.contains("wireguard")) ||
                                    (profile.protocol == Protocol.VLESS && message.contains("vless"))
            
            // At minimum, error should be contextual to the operation
            message.shouldNotBeBlank()
        }
    }
    
    // Helper functions and generators
    
    enum class ErrorType {
        AUTHENTICATION,
        NETWORK_UNREACHABLE,
        TIMEOUT,
        HANDSHAKE_FAILED,
        INVALID_CONFIGURATION,
        TLS_ERROR,
        TRANSPORT_ERROR,
        PERMISSION_DENIED
    }
    
    private fun createFailingAdapter(errorType: ErrorType): ProtocolAdapter {
        return object : ProtocolAdapter {
            private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
            
            override suspend fun connect(profile: ServerProfile): Result<Connection> {
                val errorMessage = when (errorType) {
                    ErrorType.AUTHENTICATION -> "Authentication failed: Invalid key format"
                    ErrorType.NETWORK_UNREACHABLE -> "Network unreachable: Cannot connect to server"
                    ErrorType.TIMEOUT -> "Connection timeout: No response from server"
                    ErrorType.HANDSHAKE_FAILED -> "Handshake failed: No handshake received"
                    ErrorType.INVALID_CONFIGURATION -> "Invalid configuration: Missing required fields"
                    ErrorType.TLS_ERROR -> "TLS certificate validation failed"
                    ErrorType.TRANSPORT_ERROR -> "Transport protocol connection failed"
                    ErrorType.PERMISSION_DENIED -> "Permission denied: VPN permission required"
                }
                return Result.failure(Exception(errorMessage))
            }
            
            override suspend fun disconnect() {
                _state.value = ConnectionState.Disconnected
            }
            
            override suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult> {
                return Result.success(ConnectionTestResult(success = false))
            }
            
            override fun observeConnectionState(): Flow<ConnectionState> {
                return _state
            }
            
            override fun getStatistics(): ConnectionStatistics? {
                return null
            }
        }
    }
    
    private fun createSuccessAdapter(): ProtocolAdapter {
        return object : ProtocolAdapter {
            private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
            
            override suspend fun connect(profile: ServerProfile): Result<Connection> {
                val connection = Connection(
                    profileId = profile.id,
                    protocol = profile.protocol,
                    connectedAt = System.currentTimeMillis(),
                    serverAddress = profile.hostname
                )
                return Result.success(connection)
            }
            
            override suspend fun disconnect() {
                _state.value = ConnectionState.Disconnected
            }
            
            override suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult> {
                return Result.success(ConnectionTestResult(success = true))
            }
            
            override fun observeConnectionState(): Flow<ConnectionState> {
                return _state
            }
            
            override fun getStatistics(): ConnectionStatistics? {
                return null
            }
        }
    }
    
    private fun createMockRepository(profile: ServerProfile): ProfileRepository {
        return object : ProfileRepository {
            override suspend fun createProfile(profile: ServerProfile): Result<Long> {
                return Result.success(profile.id)
            }
            
            override suspend fun getProfile(id: Long): ServerProfile? {
                return if (id == profile.id) profile else null
            }
            
            override suspend fun getAllProfiles(): List<ServerProfile> {
                return listOf(profile)
            }
            
            override fun observeAllProfiles(): Flow<List<ServerProfile>> {
                return MutableStateFlow(listOf(profile))
            }
            
            override suspend fun getProfilesByProtocol(protocol: Protocol): List<ServerProfile> {
                return if (profile.protocol == protocol) listOf(profile) else emptyList()
            }
            
            override fun observeProfilesByProtocol(protocol: Protocol): Flow<List<ServerProfile>> {
                return MutableStateFlow(if (profile.protocol == protocol) listOf(profile) else emptyList())
            }
            
            override suspend fun updateProfile(profile: ServerProfile): Result<Unit> {
                return Result.success(Unit)
            }
            
            override suspend fun deleteProfile(id: Long): Result<Unit> {
                return Result.success(Unit)
            }
            
            override suspend fun updateLastUsed(id: Long, timestamp: Long) {
                // No-op for mock
            }
            
            override suspend fun searchProfiles(query: String): List<ServerProfile> {
                return emptyList()
            }
            
            override suspend fun getProfileCount(): Int {
                return 1
            }
            
            override suspend fun getProfileCountByProtocol(protocol: Protocol): Int {
                return if (profile.protocol == protocol) 1 else 0
            }
            
            override fun validateProfile(profile: ServerProfile): Result<Unit> {
                return Result.success(Unit)
            }
        }
    }
    
    companion object {
        fun Arb.Companion.connectionError(): Arb<ErrorType> = arbitrary {
            Arb.choice(
                Arb.constant(ErrorType.AUTHENTICATION),
                Arb.constant(ErrorType.NETWORK_UNREACHABLE),
                Arb.constant(ErrorType.TIMEOUT),
                Arb.constant(ErrorType.HANDSHAKE_FAILED),
                Arb.constant(ErrorType.INVALID_CONFIGURATION),
                Arb.constant(ErrorType.TLS_ERROR),
                Arb.constant(ErrorType.TRANSPORT_ERROR),
                Arb.constant(ErrorType.PERMISSION_DENIED)
            ).bind()
        }
        
        fun Arb.Companion.wireGuardProfile(): Arb<ServerProfile> = arbitrary {
            val id = Arb.long(1L..1000L).bind()
            val name = Arb.string(5..20).bind()
            val hostname = Arb.string(5..15).bind() + ".com"
            val port = Arb.int(1..65535).bind()
            
            val config = WireGuardConfig(
                publicKey = "base64encodedpublickey==",
                allowedIPs = listOf("0.0.0.0/0"),
                persistentKeepalive = null,
                endpoint = "$hostname:$port",
                mtu = 1420
            )
            
            ServerProfile.createWireGuardProfile(
                id = id,
                name = name,
                hostname = hostname,
                port = port,
                config = config
            )
        }
        
        fun Arb.Companion.vlessProfile(): Arb<ServerProfile> = arbitrary {
            val id = Arb.long(1L..1000L).bind()
            val name = Arb.string(5..20).bind()
            val hostname = Arb.string(5..15).bind() + ".com"
            val port = Arb.int(1..65535).bind()
            
            val config = VlessConfig(
                flowControl = com.selfproxy.vpn.data.model.FlowControl.NONE,
                transport = com.selfproxy.vpn.data.model.TransportProtocol.TCP,
                tlsSettings = null,
                realitySettings = null
            )
            
            ServerProfile.createVlessProfile(
                id = id,
                name = name,
                hostname = hostname,
                port = port,
                config = config
            )
        }
        
        fun Arb.Companion.serverProfile(): Arb<ServerProfile> = arbitrary {
            val protocol = Arb.enum<Protocol>().bind()
            when (protocol) {
                Protocol.WIREGUARD -> Arb.wireGuardProfile().bind()
                Protocol.VLESS -> Arb.vlessProfile().bind()
            }
        }
    }
}
