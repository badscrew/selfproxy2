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
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Property-based tests for protocol adapter selection in ConnectionManager.
 * 
 * Feature: selfproxy, Property 11: Protocol Adapter Selection
 * Validates: Requirements 3.3
 */
class ProtocolAdapterSelectionPropertiesTest {
    
    @Test
    fun `protocol adapter selection should use WireGuard adapter for WireGuard profiles`() = runTest {
        // Feature: selfproxy, Property 11: Protocol Adapter Selection
        // Validates: Requirements 3.3
        checkAll(
            iterations = 100,
            Arb.wireGuardProfile()
        ) { profile ->
            // Arrange
            var wireGuardAdapterUsed = false
            var vlessAdapterUsed = false
            
            val wireGuardAdapter = createMockAdapter { wireGuardAdapterUsed = true }
            val vlessAdapter = createMockAdapter { vlessAdapterUsed = true }
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = wireGuardAdapter,
                vlessAdapter = vlessAdapter,
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            connectionManager.connect(profile.id)
            
            // Assert
            wireGuardAdapterUsed shouldBe true
            vlessAdapterUsed shouldBe false
        }
    }
    
    @Test
    fun `protocol adapter selection should use VLESS adapter for VLESS profiles`() = runTest {
        // Feature: selfproxy, Property 11: Protocol Adapter Selection
        // Validates: Requirements 3.3
        checkAll(
            iterations = 100,
            Arb.vlessProfile()
        ) { profile ->
            // Arrange
            var wireGuardAdapterUsed = false
            var vlessAdapterUsed = false
            
            val wireGuardAdapter = createMockAdapter { wireGuardAdapterUsed = true }
            val vlessAdapter = createMockAdapter { vlessAdapterUsed = true }
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = wireGuardAdapter,
                vlessAdapter = vlessAdapter,
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            connectionManager.connect(profile.id)
            
            // Assert
            wireGuardAdapterUsed shouldBe false
            vlessAdapterUsed shouldBe true
        }
    }
    
    @Test
    fun `protocol adapter selection should consistently use correct adapter for any profile`() = runTest {
        // Feature: selfproxy, Property 11: Protocol Adapter Selection
        // Validates: Requirements 3.3
        checkAll(
            iterations = 100,
            Arb.serverProfile()
        ) { profile ->
            // Arrange
            var wireGuardAdapterUsed = false
            var vlessAdapterUsed = false
            
            val wireGuardAdapter = createMockAdapter { wireGuardAdapterUsed = true }
            val vlessAdapter = createMockAdapter { vlessAdapterUsed = true }
            val repository = createMockRepository(profile)
            
            val connectionManager = ConnectionManager(
                wireGuardAdapter = wireGuardAdapter,
                vlessAdapter = vlessAdapter,
                profileRepository = repository,
                dispatcher = Dispatchers.Unconfined
            )
            
            // Act
            connectionManager.connect(profile.id)
            
            // Assert - exactly one adapter should be used based on protocol
            when (profile.protocol) {
                Protocol.WIREGUARD -> {
                    wireGuardAdapterUsed shouldBe true
                    vlessAdapterUsed shouldBe false
                }
                Protocol.VLESS -> {
                    wireGuardAdapterUsed shouldBe false
                    vlessAdapterUsed shouldBe true
                }
            }
        }
    }
    
    // Helper functions and generators
    
    private fun createMockAdapter(onConnect: () -> Unit): ProtocolAdapter {
        return object : ProtocolAdapter {
            private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
            
            override suspend fun connect(profile: ServerProfile): Result<Connection> {
                onConnect()
                val connection = Connection(
                    profileId = profile.id,
                    protocol = profile.protocol,
                    connectedAt = System.currentTimeMillis(),
                    serverAddress = profile.hostname
                )
                _state.value = ConnectionState.Connected(connection)
                return Result.success(connection)
            }
            
            override suspend fun disconnect() {
                _state.value = ConnectionState.Disconnected
            }
            
            override suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult> {
                return Result.success(ConnectionTestResult(success = true, latencyMs = 50))
            }
            
            override fun observeConnectionState(): Flow<ConnectionState> {
                return _state.asStateFlow()
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
            
            override suspend fun importConfiguration(configText: String): Result<ServerProfile> {
                return Result.success(profile)
            }
        }
    }
    
    companion object {
        fun Arb.Companion.wireGuardProfile(): Arb<ServerProfile> = arbitrary {
            val id = Arb.long(1L..1000L).bind()
            val name = Arb.string(5..20).bind()
            val hostname = Arb.string(5..15).bind() + ".com"
            val port = Arb.int(1..65535).bind()
            
            val config = WireGuardConfig(
                publicKey = com.selfproxy.vpn.TestKeys.VALID_PUBLIC_KEY,
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
