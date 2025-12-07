# Design Document: Multi-Protocol VPN Proxy

## Overview

The Multi-Protocol VPN Proxy is a native Android application that enables users to route their device's internet traffic through their own VPN servers using WireGuard (default) or VLESS (optional) protocols. The application is built using modern Android development practices with Kotlin, Jetpack Compose, and Android's native VpnService API.

### Design Goals

1. **Simplicity**: WireGuard as the default protocol provides the simplest, most efficient experience for most users
2. **Flexibility**: VLESS support for users requiring advanced obfuscation in restrictive network environments
3. **Performance**: Leverage WireGuard's efficient cryptography and native Android integration for superior battery life and connection speed
4. **Privacy**: No data collection, all credentials encrypted with hardware-backed storage
5. **Maintainability**: Clean architecture with protocol abstraction for easy future enhancements
6. **Native Android**: Pure Kotlin implementation optimized for Android platform

### Key Design Decisions

1. **Native Android**: Pure Kotlin implementation for best performance and smallest APK size
2. **WireGuard as Default**: WireGuard is recommended for 95% of users due to its simplicity, performance, and battery efficiency
3. **VLESS for Advanced Users**: VLESS provides obfuscation capabilities for users in restrictive environments
4. **Protocol Abstraction**: Clean Protocol_Adapter interface allows easy addition of protocols in the future
5. **Android VpnService**: Use Android's native VpnService API for traffic routing
6. **Hardware-Backed Encryption**: Leverage Android Keystore with StrongBox for credential security
7. **Jetpack Compose**: Modern declarative UI framework for Android
8. **Room Database**: Local persistence with type-safe database access

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Application                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              UI Layer (Jetpack Compose)               │  │
│  │  - Profile Management Screen                          │  │
│  │  - Connection Screen                                  │  │
│  │  - Statistics Screen                                  │  │
│  │  - Settings Screen                                    │  │
│  │  - ViewModels (State Management)                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Domain Layer (Business Logic)                 │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         Connection Manager                    │   │  │
│  │  │  - Protocol selection                         │   │  │
│  │  │  - Connection state management                │   │  │
│  │  │  - Auto-reconnection logic                    │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         Protocol Adapter Interface            │   │  │
│  │  │  ┌────────────────┐  ┌────────────────┐     │   │  │
│  │  │  │ WireGuard      │  │ VLESS          │     │   │  │
│  │  │  │ Adapter        │  │ Adapter        │     │   │  │
│  │  │  └────────────────┘  └────────────────┘     │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Data Layer                                    │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         Profile Repository                    │   │  │
│  │  │  - CRUD operations                            │   │  │
│  │  │  - Profile validation                         │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         Room Database                         │   │  │
│  │  │  - Profile entities                           │   │  │
│  │  │  - DAOs                                       │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         Credential Store                      │   │  │
│  │  │  - Android Keystore integration               │   │  │
│  │  │  - Hardware-backed encryption                 │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Platform Layer (Android Services)             │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         VPN Service                           │   │  │
│  │  │  - TUN interface management                   │   │  │
│  │  │  - Packet routing                             │   │  │
│  │  │  - Per-app routing                            │   │  │
│  │  │  - Foreground service                         │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         Protocol Implementations              │   │  │
│  │  │  - WireGuard: wireguard-android library       │   │  │
│  │  │  - VLESS: AndroidLibXrayLite                  │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  │                                                        │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │         Network Monitor                       │   │  │
│  │  │  - Network state changes                      │   │  │
│  │  │  - Connectivity monitoring                    │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

#### UI Layer
- Jetpack Compose UI components
- ViewModels for state management
- Navigation between screens
- User input handling
- State observation and UI updates

#### Domain Layer (Business Logic)
- Connection state management
- Profile management
- Protocol selection logic
- Auto-reconnection logic
- Traffic statistics tracking
- Business rules and validation

#### Data Layer
- Profile repository implementation
- Room database operations
- Credential encryption/decryption
- Data persistence

#### Platform Layer (Android Services)
- VPN service implementation
- Protocol library integration
- Network monitoring
- System notifications
- Foreground service management

## Components and Interfaces

### 1. Protocol Adapter Interface

The Protocol_Adapter interface provides a unified abstraction for different VPN protocols.

```kotlin
interface ProtocolAdapter {
    /**
     * Establishes a connection using this protocol
     * @param profile The server profile containing connection details
     * @return Result containing Connection on success or error on failure
     */
    suspend fun connect(profile: ServerProfile): Result<Connection>
    
    /**
     * Disconnects the current connection
     */
    suspend fun disconnect()
    
    /**
     * Tests if the server is reachable and credentials are valid
     * @param profile The server profile to test
     * @return Result containing connection latency on success
     */
    suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult>
    
    /**
     * Observes the connection state
     * @return Flow of connection states
     */
    fun observeConnectionState(): Flow<ConnectionState>
    
    /**
     * Gets current connection statistics
     * @return Current statistics or null if not connected
     */
    fun getStatistics(): ConnectionStatistics?
}
```

### 2. Connection Manager

Manages VPN connections across all protocols.

```kotlin
class ConnectionManager(
    private val wireGuardAdapter: ProtocolAdapter,
    private val vlessAdapter: ProtocolAdapter,
    private val profileRepository: ProfileRepository,
    private val autoReconnectService: AutoReconnectService
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    suspend fun connect(profileId: Long): Result<Unit> {
        val profile = profileRepository.getProfile(profileId) ?: return Result.failure(ProfileNotFoundException())
        
        val adapter = when (profile.protocol) {
            Protocol.WIREGUARD -> wireGuardAdapter
            Protocol.VLESS -> vlessAdapter
        }
        
        _connectionState.value = ConnectionState.Connecting
        
        return adapter.connect(profile)
            .onSuccess { connection ->
                _connectionState.value = ConnectionState.Connected(connection)
                autoReconnectService.enable(profile)
            }
            .onFailure { error ->
                _connectionState.value = ConnectionState.Error(error)
            }
            .map { }
    }
    
    suspend fun disconnect() {
        autoReconnectService.disable()
        // Disconnect logic
    }
}
```

### 3. Profile Repository

Manages server profile storage and retrieval.

```kotlin
interface ProfileRepository {
    suspend fun createProfile(profile: ServerProfile): Result<Long>
    suspend fun getProfile(id: Long): ServerProfile?
    suspend fun getAllProfiles(): List<ServerProfile>
    suspend fun updateProfile(profile: ServerProfile): Result<Unit>
    suspend fun deleteProfile(id: Long): Result<Unit>
    suspend fun getProfilesByProtocol(protocol: Protocol): List<ServerProfile>
}

// Implementation using Room
class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val credentialStore: CredentialStore
) : ProfileRepository {
    override suspend fun createProfile(profile: ServerProfile): Result<Long> {
        return try {
            val id = profileDao.insert(profile.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Other implementations...
}
```

### 4. Credential Store

Securely stores VPN credentials using Android Keystore.

```kotlin
interface CredentialStore {
    suspend fun storeWireGuardKeys(
        profileId: Long,
        privateKey: ByteArray,
        presharedKey: ByteArray?
    ): Result<Unit>
    
    suspend fun retrieveWireGuardKeys(profileId: Long): Result<WireGuardKeys>
    
    suspend fun storeVlessCredentials(
        profileId: Long,
        uuid: String,
        tlsCertificate: ByteArray?
    ): Result<Unit>
    
    suspend fun retrieveVlessCredentials(profileId: Long): Result<VlessCredentials>
    
    suspend fun deleteCredentials(profileId: Long): Result<Unit>
}
```

### 5. VPN Service

Android VpnService implementation for packet routing.

```kotlin
class TunnelVpnService : VpnService() {
    private var tunInterface: ParcelFileDescriptor? = null
    private var currentAdapter: ProtocolAdapter? = null
    
    fun startTunnel(profile: ServerProfile, adapter: ProtocolAdapter) {
        tunInterface = createTunInterface(profile)
        currentAdapter = adapter
        startForeground(NOTIFICATION_ID, createNotification())
        startPacketRouting()
    }
    
    private fun createTunInterface(profile: ServerProfile): ParcelFileDescriptor? {
        return Builder()
            .setSession("VPN Proxy")
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setMtu(1500)
            .setBlocking(true)
            .establish()
    }
}
```

## Data Models

### Server Profile

```kotlin
@Serializable
data class ServerProfile(
    val id: Long = 0,
    val name: String,
    val protocol: Protocol,
    val hostname: String,
    val port: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    
    // WireGuard-specific
    val wireGuardConfig: WireGuardConfig? = null,
    
    // VLESS-specific
    val vlessConfig: VlessConfig? = null
)

@Serializable
data class WireGuardConfig(
    val publicKey: String,  // Server's public key
    val allowedIPs: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int? = null,  // 0 to disable, 1-65535 seconds
    val endpoint: String  // hostname:port
)

@Serializable
data class VlessConfig(
    val uuid: String,
    val flowControl: FlowControl = FlowControl.NONE,
    val transport: TransportProtocol,
    val tlsSettings: TlsSettings? = null,
    val realitySettings: RealitySettings? = null
)

enum class Protocol {
    WIREGUARD,
    VLESS
}

enum class FlowControl {
    NONE,
    XTLS_RPRX_VISION
}

enum class TransportProtocol {
    TCP,
    WEBSOCKET,
    GRPC,
    HTTP2
}
```

### Connection State

```kotlin
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val connection: Connection) : ConnectionState()
    object Reconnecting : ConnectionState()
    data class Error(val error: Throwable) : ConnectionState()
}

data class Connection(
    val profileId: Long,
    val protocol: Protocol,
    val connectedAt: Long,
    val serverAddress: String
)
```

### Connection Statistics

```kotlin
data class ConnectionStatistics(
    val bytesReceived: Long,
    val bytesSent: Long,
    val downloadSpeed: Long,  // bytes per second
    val uploadSpeed: Long,    // bytes per second
    val connectionDuration: Long,  // milliseconds
    
    // WireGuard-specific
    val lastHandshakeTime: Long? = null,
    
    // VLESS-specific
    val latency: Long? = null  // milliseconds
)
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Valid WireGuard Profile Acceptance
*For any* valid WireGuard configuration (hostname, port, valid Private_Key, valid Public_Key, valid Allowed_IPs), the system should accept and store the profile successfully.
**Validates: Requirements 1.2**

### Property 2: Valid VLESS Profile Acceptance
*For any* valid VLESS configuration (hostname, port, valid UUID, valid Flow_Control, valid Transport_Protocol, valid TLS_Settings), the system should accept and store the profile successfully.
**Validates: Requirements 1.3**

### Property 3: Profile Validation Rejects Incomplete Data
*For any* profile with missing required fields, the validation should fail and prevent profile creation.
**Validates: Requirements 1.4**

### Property 4: Profile Listing Completeness
*For any* set of saved profiles, retrieving all profiles should return exactly the profiles that were saved with correct names, protocol types, server addresses, and last connection times.
**Validates: Requirements 1.5**

### Property 5: Profile Update Round-Trip
*For any* saved profile, if it is modified and saved again, then retrieving it should return the profile with the updated values.
**Validates: Requirements 1.7**

### Property 6: Profile Deletion Completeness
*For any* saved profile, after deletion, the profile should not appear in the profile list and should not be retrievable by ID.
**Validates: Requirements 1.8**

### Property 7: Configuration Import Protocol Detection
*For any* valid WireGuard or VLESS configuration string/file, importing it should correctly detect the protocol type and create a profile of the appropriate type.
**Validates: Requirements 1.10**

### Property 8: Credential Encryption
*For any* stored credentials (WireGuard keys or VLESS UUIDs), the data in persistent storage should be encrypted (not plaintext).
**Validates: Requirements 2.1, 2.3, 2.4**

### Property 9: Credential Deletion on Profile Deletion
*For any* profile with stored credentials, after deleting the profile, the associated credentials should no longer be retrievable from the credential store.
**Validates: Requirements 2.5**

### Property 10: Log Sanitization
*For any* log entry generated during operations involving sensitive data, the log output should not contain private keys, UUIDs, preshared keys, or other sensitive credentials.
**Validates: Requirements 2.6**

### Property 11: Protocol Adapter Selection
*For any* profile, when initiating a connection, the Connection_Manager should select the WireGuard adapter for WireGuard profiles and the VLESS adapter for VLESS profiles.
**Validates: Requirements 3.3**

### Property 12: Connection Error Messaging
*For any* connection failure, the system should provide a specific error message indicating the failure reason (authentication failed, server unreachable, invalid configuration, handshake timeout, etc.).
**Validates: Requirements 3.6**

## Error Handling

### Connection Errors

**WireGuard-Specific Errors:**
- Invalid key format (not base64-encoded 32-byte key)
- Handshake timeout (no response from server)
- Invalid endpoint (malformed hostname or port)
- Invalid Allowed_IPs format
- Network unreachable

**VLESS-Specific Errors:**
- Invalid UUID format (not RFC 4122 compliant)
- TLS certificate validation failure
- Transport protocol connection failure
- Reality configuration errors
- Authentication failure (invalid UUID)

**General Errors:**
- Network connectivity issues
- VPN permission denied
- Profile not found
- Credential decryption failure
- Timeout errors

### Error Recovery Strategies

1. **Automatic Reconnection**: For transient network errors, use exponential backoff
2. **User Notification**: For authentication errors, notify user to check credentials
3. **Fallback**: For VLESS transport failures, suggest trying different transport
4. **Diagnostic Logging**: Capture detailed error information for troubleshooting

## Testing Strategy

### Dual Testing Approach

The application will use both unit testing and property-based testing:

**Unit Tests:**
- Specific examples and edge cases
- Integration points between components
- UI component behavior
- Error handling scenarios

**Property-Based Tests:**
- Universal properties that should hold across all inputs
- Profile validation logic
- Credential encryption/decryption
- Protocol adapter selection
- Configuration import/export

### Property-Based Testing Framework

**Framework**: Kotest Property Testing for Kotlin Multiplatform

**Configuration:**
- Minimum 100 iterations per property test
- Custom generators for domain types (ServerProfile, WireGuardConfig, VlessConfig)
- Each property test tagged with design document reference

**Example Property Test:**
```kotlin
class ProfilePropertiesTest {
    
    @Test
    fun `profile update round-trip should preserve changes`() = runTest {
        // Feature: selfproxy, Property 5: Profile Update Round-Trip
        // Validates: Requirements 1.7
        checkAll(
            iterations = 100,
            Arb.serverProfile()
        ) { originalProfile ->
            // Save original profile
            val profileId = repository.createProfile(originalProfile).getOrThrow()
            
            // Modify profile
            val updatedProfile = originalProfile.copy(
                id = profileId,
                name = "Updated ${originalProfile.name}",
                port = originalProfile.port + 1
            )
            
            // Update and retrieve
            repository.updateProfile(updatedProfile).getOrThrow()
            val retrieved = repository.getProfile(profileId)
            
            // Verify changes persisted
            retrieved shouldNotBe null
            retrieved?.name shouldBe updatedProfile.name
            retrieved?.port shouldBe updatedProfile.port
        }
    }
}
```

### Test Coverage Targets

- **Shared Code (commonMain)**: 80% line coverage
- **Android Platform Code**: 70% line coverage
- **UI Code**: 60% line coverage (focus on critical paths)

### Integration Testing

**Test Scenarios:**
1. Connect to real WireGuard server
2. Connect to real VLESS server with different transports
3. Network switching (WiFi ↔ Mobile data)
4. Per-app routing functionality
5. Auto-reconnection on connection drop
6. DNS leak prevention verification
7. Battery consumption benchmarks

## Security Considerations

### Threat Model

**Protected Against:**
- Network eavesdropping (WireGuard/VLESS encryption)
- ISP tracking (traffic routed through user's server)
- DNS leaks (DNS through tunnel)
- Credential theft (encrypted storage with hardware backing)
- Man-in-the-middle attacks (WireGuard authentication, VLESS TLS)

**Not Protected Against:**
- Compromised VPN server (user responsibility)
- Malicious apps on device (Android sandbox limitation)
- Physical device access without device encryption
- Traffic analysis at server exit point

### Security Best Practices

1. **Credential Storage**: Always use Android Keystore with StrongBox when available
2. **Key Generation**: Use cryptographically secure random number generation
3. **Log Sanitization**: Never log sensitive data (keys, UUIDs, passwords)
4. **Memory Management**: Clear sensitive data from memory after use
5. **Certificate Validation**: Properly validate TLS certificates for VLESS
6. **DNS Leak Prevention**: Block all DNS queries outside the tunnel
7. **IPv6 Leak Prevention**: Route or block IPv6 traffic appropriately

## Performance Considerations

### WireGuard Performance

**Advantages:**
- Minimal CPU usage due to efficient ChaCha20-Poly1305 cipher
- Low memory footprint
- Fast handshake (1-RTT)
- Excellent battery life
- Native kernel support on modern Android versions

**Optimizations:**
- Use kernel WireGuard module when available (Android 12+)
- Minimize Persistent_Keepalive to reduce battery drain
- Efficient packet routing through TUN interface

### VLESS Performance

**Considerations:**
- Transport protocol choice affects performance:
  - TCP: Most compatible, moderate overhead
  - WebSocket: Good for HTTP-based networks, moderate overhead
  - gRPC: Efficient multiplexing, higher CPU usage
  - HTTP/2: Good balance of performance and compatibility
- TLS 1.3 reduces handshake latency
- Reality protocol adds minimal overhead for obfuscation

**Optimizations:**
- Connection pooling for transport protocols
- Efficient buffer management
- Minimize unnecessary protocol overhead

### Battery Optimization

**Strategies:**
1. **Efficient Keep-Alive**: Use minimal keep-alive intervals
2. **Doze Mode Handling**: Request battery optimization exemption
3. **Network Monitoring**: Efficient network change detection
4. **Foreground Service**: Only run when VPN is active
5. **WireGuard Preference**: Recommend WireGuard for best battery life

**Expected Battery Impact:**
- WireGuard: 2-5% additional battery drain per hour of active use
- VLESS: 3-7% additional battery drain per hour (varies by transport)

## Deployment Architecture

### Client-Side (Android App)

```
┌─────────────────────────────────────┐
│         Android Device              │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   VPN Proxy App              │  │
│  │                              │  │
│  │  - Profile Management        │  │
│  │  - Connection Manager        │  │
│  │  - VPN Service               │  │
│  │  - Credential Store          │  │
│  └──────────────────────────────┘  │
│              │                      │
│              ▼                      │
│  ┌──────────────────────────────┐  │
│  │   TUN Interface              │  │
│  │   (10.0.0.2/24)              │  │
│  └──────────────────────────────┘  │
│              │                      │
└──────────────┼──────────────────────┘
               │
               │ Encrypted Tunnel
               │
               ▼
┌─────────────────────────────────────┐
│      User's VPN Server              │
│                                     │
│  ┌──────────────────────────────┐  │
│  │  WireGuard Server            │  │
│  │  or                          │  │
│  │  Xray-core (VLESS)           │  │
│  └──────────────────────────────┘  │
│              │                      │
│              ▼                      │
│         Internet                    │
└─────────────────────────────────────┘
```

### Server-Side Setup

**WireGuard Server:**
- Ubuntu 20.04/22.04/24.04 LTS
- WireGuard kernel module or wireguard-go
- IP forwarding enabled
- Firewall configured (UFW/iptables)
- Automated setup script provided

**VLESS Server:**
- Ubuntu 20.04/22.04/24.04 LTS
- Xray-core latest version
- TLS certificates (Let's Encrypt)
- Optional Reality protocol configuration
- Automated setup script provided

## Future Enhancements

### Phase 1 (Post-MVP)
1. **Advanced Routing**: Custom routing rules and split tunneling
2. **Statistics History**: Track bandwidth usage over time
3. **Widget Support**: Home screen widget for quick connect
4. **Quick Settings Tile**: Quick toggle from notification shade
5. **Tasker Integration**: Automation support

### Phase 2 (Future)
1. **Additional Protocols**: Shadowsocks, Trojan if user demand exists
2. **WireGuard Kernel Module**: Direct kernel integration for maximum performance
3. **Custom DNS**: DNS over HTTPS/TLS support
4. **Traffic Analysis**: Detailed traffic statistics and analytics
5. **Backup/Sync**: Encrypted profile backup/restore

### Extensibility

The Protocol_Adapter interface makes it easy to add new protocols:

```kotlin
class NewProtocolAdapter : ProtocolAdapter {
    override suspend fun connect(profile: ServerProfile): Result<Connection> {
        // Implement new protocol connection logic
    }
    
    // Implement other interface methods
}
```

Adding a new protocol requires:
1. Implement Protocol_Adapter interface
2. Add protocol enum value
3. Add protocol-specific configuration data class
4. Update UI to support new protocol
5. Add server setup scripts

## Dependencies

### Core Dependencies

```kotlin
dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // WireGuard
    implementation("com.wireguard.android:tunnel:1.0.20230706")
    
    // VLESS (Xray-core integration)
    implementation("io.github.2dust:AndroidLibXrayLite:1.8.5")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Dependency Injection
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.9")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

## Conclusion

This design provides a solid foundation for a modern, efficient native Android VPN proxy application with:

1. **Native Performance**: Pure Kotlin implementation optimized for Android
2. **Clean Architecture**: Clear separation of concerns with MVVM pattern
3. **Protocol Abstraction**: Easy to extend with new protocols
4. **Security First**: Hardware-backed encryption and privacy-by-design
5. **Performance**: Optimized for battery life, especially with WireGuard
6. **Testability**: Comprehensive testing strategy with property-based tests
7. **User Experience**: Simple for most users (WireGuard), powerful for advanced users (VLESS)
8. **Modern Android**: Jetpack Compose, Room, Kotlin Coroutines

The focus on native Android development ensures the best possible performance, smallest APK size, and deepest integration with Android's VPN APIs. WireGuard as the default protocol provides excellent performance and battery life for the majority of users, while VLESS support offers advanced obfuscation capabilities for users in restrictive environments.
