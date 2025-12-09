# Implementation Plan

- [x] 1. Set up Android project structure





  - Create new Android project with Kotlin and Jetpack Compose
  - Configure Gradle build files with required dependencies
  - Set up project package structure (ui, domain, data, platform)
  - Configure ProGuard/R8 rules for release builds
  - _Requirements: Foundation for all requirements_

- [x] 2. Define data models and database schema





  - Create ServerProfile, WireGuardConfig, VlessConfig data classes
  - Create Protocol, FlowControl, TransportProtocol enums
  - Define Room entities for server profiles
  - Create Room DAOs for profile operations
  - Set up Room database with migrations
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2.1 Write property test for data model serialization
  - **Property 1: Valid WireGuard Profile Acceptance**
  - **Validates: Requirements 1.2**

- [x] 2.2 Write property test for data model serialization
  - **Property 2: Valid VLESS Profile Acceptance**
  - **Validates: Requirements 1.3**

- [x] 3. Implement Profile Repository
  - Create ProfileRepository interface
  - Implement ProfileRepositoryImpl with Room
  - Add CRUD operations (create, read, update, delete)
  - Implement profile validation logic
  - Add error handling with Result types
  - _Requirements: 1.4, 1.5, 1.7, 1.8_

- [x] 3.1 Write property test for profile repository
  - **Property 4: Profile Listing Completeness**
  - **Validates: Requirements 1.5**

- [x] 3.2 Write property test for profile repository
  - **Property 5: Profile Update Round-Trip**
  - **Validates: Requirements 1.7**

- [x] 3.3 Write property test for profile repository
  - **Property 6: Profile Deletion Completeness**
  - **Validates: Requirements 1.8**

- [x] 3.4 Write property test for profile validation
  - **Property 3: Profile Validation Rejects Incomplete Data**
  - **Validates: Requirements 1.4**

- [x] 4. Implement Credential Store with Android Keystore
  - Create CredentialStore interface
  - Implement Android Keystore integration
  - Add encryption/decryption for WireGuard keys
  - Add encryption/decryption for VLESS credentials
  - Implement secure credential deletion
  - Use StrongBox when available
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.9, 2.10_

- [x] 4.1 Write property test for credential encryption
  - **Property 8: Credential Encryption**
  - **Validates: Requirements 2.1, 2.3, 2.4**

- [x] 4.2 Write property test for credential deletion
  - **Property 9: Credential Deletion on Profile Deletion**
  - **Validates: Requirements 2.5**

- [x] 5. Implement Protocol Adapter interface
  - Create ProtocolAdapter interface with connect, disconnect, test methods
  - Define ConnectionState sealed class
  - Define Connection and ConnectionStatistics data classes
  - Create ConnectionTestResult data class
  - _Requirements: 3.3_

- [x] 6. Implement WireGuard Protocol Adapter
  - Create WireGuardAdapter implementing ProtocolAdapter
  - Integrate wireguard-android library
  - Implement connection establishment with handshake
  - Implement connection state observation
  - Add statistics tracking (handshake time, bytes transferred)
  - Implement connection testing
  - _Requirements: 3.1, 3.9, 7.7, 9.1-9.8_

- [x] 6.1 Write unit tests for WireGuard adapter
  - Test key validation
  - Test endpoint parsing
  - Test configuration validation
  - _Requirements: 3.1, 9.4_

- [x] 7. Implement VLESS Protocol Adapter
  - Create VlessAdapter implementing ProtocolAdapter
  - Integrate AndroidLibXrayLite library
  - Implement connection establishment with UUID auth
  - Support multiple transport protocols (TCP, WebSocket, gRPC, HTTP/2)
  - Add TLS/Reality configuration support
  - Implement connection testing
  - _Requirements: 3.2, 3.10, 7.8, 9.9-9.15_

- [x] 7.1 Write unit tests for VLESS adapter
  - Test UUID validation
  - Test transport protocol selection
  - Test TLS configuration
  - _Requirements: 3.2, 9.12_

- [x] 8. Implement Connection Manager
  - Create ConnectionManager class
  - Implement protocol adapter selection logic
  - Add connection state management with StateFlow
  - Implement connect/disconnect operations
  - Add error handling and error message generation
  - Integrate with Auto-Reconnect Service
  - _Requirements: 3.3, 3.6, 3.7_

- [x] 8.1 Write property test for protocol adapter selection
  - **Property 11: Protocol Adapter Selection**
  - **Validates: Requirements 3.3**

- [x] 8.2 Write property test for error messaging
  - **Property 12: Connection Error Messaging**
  - **Validates: Requirements 3.6**

- [x] 9. Implement VPN Service
  - Create TunnelVpnService extending VpnService
  - Implement TUN interface creation and configuration
  - Add packet routing logic
  - Implement DNS configuration
  - Add IPv6 handling (route or block)
  - Create foreground service with notification
  - _Requirements: 3.4, 3.5, 4.1, 4.2, 4.3, 4.7, 4.8_

- [x] 9.1 Write unit tests for VPN service configuration
  - Test TUN interface builder configuration
  - Test DNS server configuration
  - Test route configuration
  - _Requirements: 4.3, 4.7_

- [x] 10. Implement Per-App Routing
  - Add app list retrieval with package manager
  - Implement app selection UI
  - Add VpnService.Builder app allow/disallow logic
  - Persist app routing configuration
  - Apply routing on VPN start
  - Support dynamic routing updates
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

- [x] 10.1 Write unit tests for app routing
  - Test app list filtering
  - Test routing configuration persistence
  - Test self-exclusion logic
  - _Requirements: 5.7_

- [x] 11. Implement Auto-Reconnection Service
  - Create AutoReconnectService class
  - Implement connection drop detection
  - Add exponential backoff logic (1s to 60s)
  - Implement network change monitoring
  - Add reconnection attempt counter
  - Implement user notification after failures
  - Handle manual disconnect (disable auto-reconnect)
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.8, 6.9, 6.10_

- [x] 11.1 Write unit tests for auto-reconnect logic
  - Test exponential backoff calculation
  - Test reconnection attempt counting
  - Test manual disconnect handling
  - _Requirements: 6.3, 6.10_

- [x] 12. Implement Traffic Monitor
  - Create TrafficMonitor class
  - Track bytes sent/received from TUN interface
  - Calculate upload/download speeds
  - Track connection duration
  - Implement statistics reset
  - Add real-time statistics updates
  - _Requirements: 7.2, 7.3, 7.4, 7.5, 7.10_

- [x] 12.1 Write unit tests for traffic monitoring
  - Test byte counting
  - Test speed calculation
  - Test statistics reset
  - _Requirements: 7.2, 7.3, 7.10_

- [x] 13. Implement Network Monitor
  - Create NetworkMonitor class using ConnectivityManager
  - Observe network state changes (WiFi ↔ Mobile)
  - Detect network availability
  - Trigger reconnection on network change
  - _Requirements: 6.5_

- [x] 14. Implement Configuration Import/Export
  - Create WireGuard config parser (INI format)
  - Create VLESS URI parser (vless://)
  - Implement QR code scanning for WireGuard
  - Create WireGuard config exporter
  - Create VLESS URI exporter
  - Add protocol auto-detection
  - _Requirements: 1.10, 16.1, 16.2, 16.3, 16.4, 16.6, 16.7, 16.8, 16.10_

- [x] 14.1 Write property test for configuration import
  - **Property 7: Configuration Import Protocol Detection**
  - **Validates: Requirements 1.10**

- [x] 14.2 Write unit tests for config parsing
  - Test WireGuard INI parsing
  - Test VLESS URI parsing
  - Test invalid format handling
  - _Requirements: 16.10_

- [x] 15. Implement Connection Testing
  - Add connection test method to ProtocolAdapter
  - Implement WireGuard handshake test
  - Implement VLESS connectivity test
  - Add latency measurement
  - Display test results with error details
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.9, 8.10_

- [x] 15.1 Write unit tests for connection testing
  - Test timeout handling
  - Test error message generation
  - Test latency calculation
  - _Requirements: 8.6, 8.7_

- [x] 16. Implement Logging with Sanitization
  - Create logging utility with sanitization
  - Add regex patterns for sensitive data (keys, UUIDs)
  - Implement log export functionality
  - Add verbose logging toggle
  - _Requirements: 2.6, 12.8, 12.9, 12.10_

- [x] 16.1 Write property test for log sanitization
  - **Property 10: Log Sanitization**
  - **Validates: Requirements 2.6**

- [x] 17. Create Profile Management UI
  - Design profile list screen with Compose
  - Add profile creation/edit screen
  - Implement protocol selection UI
  - Add WireGuard configuration form
  - Add VLESS configuration form
  - Show profile details (name, protocol, address, last used)
  - Add delete confirmation dialog
  - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.7, 1.8, 1.9_

- [x] 17.1 Write UI tests for profile management
  - Test profile list display
  - Test profile creation flow
  - Test profile deletion
  - _Requirements: 1.5, 1.8_

- [x] 18. Create Connection Screen UI
  - Design main connection screen with Compose
  - Add connect/disconnect button
  - Display connection status
  - Show real-time statistics (speed, data, duration)
  - Display protocol-specific info (handshake time, latency)
  - Add connection test button
  - _Requirements: 3.5, 7.1, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

- [x] 18.1 Write UI tests for connection screen
  - Test connection button states
  - Test statistics display
  - Test status updates
  - _Requirements: 7.1, 7.9_

- [x] 19. Create Settings Screen UI
  - Design settings screen with Compose
  - Add DNS configuration options
  - Add IPv6 enable/disable toggle
  - Add MTU configuration
  - Add protocol-specific settings
  - Add logging options
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

- [x] 20. Create App Routing Screen UI
  - Design app list screen with Compose
  - Display installed apps with icons
  - Add app selection checkboxes
  - Implement "Route All" / "Route Selected" modes
  - Add search/filter functionality
  - _Requirements: 5.1, 5.8_

- [x] 21. Implement ViewModels
  - Create ProfileViewModel for profile management
  - Create ConnectionViewModel for connection state
  - Create StatisticsViewModel for traffic monitoring
  - Create SettingsViewModel for app configuration
  - Implement state management with StateFlow
  - Add error handling and user feedback
  - _Requirements: All UI-related requirements_

- [x] 22. Implement Dependency Injection
  - Set up Koin modules
  - Define dependencies for repositories
  - Define dependencies for adapters
  - Define dependencies for services
  - Define dependencies for ViewModels
  - _Requirements: Architecture requirement_

- [x] 23. Implement VPN Permission Handling
  - Request VPN permission using VpnService.prepare()
  - Handle permission grant/denial
  - Show permission rationale to user
  - _Requirements: 3.4_

- [x] 24. Implement Battery Optimization
  - Request battery optimization exemption
  - Configure efficient keep-alive intervals
  - Implement doze mode handling
  - Add battery saver mode detection
  - Optimize WireGuard persistent keepalive
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.7_

- [x] 25. Implement Notifications
  - Create notification channel
  - Design foreground service notification
  - Add connection status to notification
  - Add disconnect action button
  - Update notification on state changes
  - _Requirements: 3.5, 11.9_

- [x] 26. Implement Error Handling
  - Create error types for each protocol
  - Implement error message generation
  - Add user-friendly error dialogs
  - Implement timeout handling
  - Add diagnostic information collection
  - _Requirements: 3.6, 3.8, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_

- [x] 27. Implement Protocol Recommendations UI
  - Add protocol selection guidance
  - Highlight WireGuard as recommended default
  - Explain VLESS use cases
  - Show protocol advantages/disadvantages
  - Provide protocol documentation links
  - _Requirements: 17.1, 17.2, 17.3, 17.8, 17.9, 17.10_

- [x] 28. Add Welcome Screen
  - Design first-run welcome screen
  - Add quick setup wizard for WireGuard
  - Add quick setup wizard for VLESS
  - Implement QR code import on welcome
  - _Requirements: 1.9_

- [x] 29. Implement Security Features
  - Validate WireGuard key formats (base64, 32 bytes)
  - Validate VLESS UUID format (RFC 4122)
  - Implement certificate validation for VLESS TLS
  - Add preshared key support for WireGuard
  - _Requirements: 9.1-9.15_

- [x] 29.1 Fix remaining test failures from security validation
  - Update ConnectionTestingTest to use TestKeys and add Base64 mocking
  - Update domain manager property tests to use TestKeys
  - Update ProfileValidationPropertiesTest to use TestKeys
  - Update AppRoutingRepositoryTest to use TestKeys
  - Ensure all test files that create WireGuard configs use valid keys
  - Target: All 221 tests passing (currently 34 failures remaining)
  - _Requirements: Test infrastructure_

- [x] 30. Add Traffic Verification
  - Implement IP address check via external service
  - Display current IP and VPN server IP
  - Add DNS leak test
  - Show verification results
  - _Requirements: 8.9, 8.10_

- [x] 31. Checkpoint - Ensure all tests pass
  - Run all unit tests
  - Run all property tests
  - Run all UI tests
  - Fix any failing tests
  - Verify test coverage meets targets
  - _Requirements: All_

- [x] 32. Create Server Setup Scripts
  - Write WireGuard setup script for Ubuntu
  - Write VLESS setup script for Ubuntu
  - Add key generation for WireGuard
  - Add UUID generation for VLESS
  - Configure firewall rules
  - Add Let's Encrypt integration for VLESS
  - Generate client configurations
  - _Requirements: 10.1-10.19_

- [x] 33. Write Documentation
  - Create README with app overview
  - Write WireGuard setup guide
  - Write VLESS setup guide
  - Document QR code import process
  - Add troubleshooting guide
  - Document privacy features
  - _Requirements: Documentation requirements_

- [x] 34. Implement App Icon and Branding
  - Design app icon
  - Create adaptive icon
  - Add splash screen
  - Design app theme colors
  - _Requirements: User experience_

- [ ] 35. Configure ProGuard/R8
  - Add ProGuard rules for WireGuard library
  - Add ProGuard rules for Xray library
  - Add ProGuard rules for Room
  - Add ProGuard rules for Kotlin serialization
  - Test release build
  - _Requirements: Release preparation_

- [ ] 36. Final Testing and Polish
  - Test on multiple Android versions (8.0+)
  - Test on different device manufacturers
  - Test network switching scenarios
  - Test battery consumption
  - Verify DNS leak prevention
  - Test per-app routing
  - Polish UI animations and transitions
  - _Requirements: All_

- [ ] 37. Prepare for Release
  - Create release build
  - Generate signed APK
  - Test release APK on real devices
  - Prepare Play Store listing
  - Create screenshots
  - Write app description
  - _Requirements: Release preparation_
