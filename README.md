# SelfProxy VPN

A native Android VPN application supporting WireGuard and VLESS protocols, giving users complete control over their privacy by using their own infrastructure.

## Features

- **Dual Protocol Support**: WireGuard (default) and VLESS (advanced)
- **Profile Management**: Create, edit, and manage multiple server profiles
- **Secure Credential Storage**: Hardware-backed encryption with Android Keystore
- **Per-App Routing**: Selective tunneling for specific applications
- **Auto-Reconnection**: Automatic recovery from connection drops
- **Connection Monitoring**: Real-time statistics and health indicators
- **Battery Optimized**: Efficient power management, especially with WireGuard
- **Privacy-First**: No data collection, tracking, or analytics

## Requirements

- Android 8.0 (API 26) or higher
- VPN permission
- Internet access

## Architecture

The application follows clean architecture principles with clear separation of concerns:

- **UI Layer**: Jetpack Compose with MVVM architecture
- **Domain Layer**: Business logic and use cases
- **Data Layer**: Repository pattern with Room database
- **Platform Layer**: Android-specific implementations (VPN service, credential storage)

## Project Structure

```
app/
├── src/main/kotlin/com/selfproxy/vpn/
│   ├── ui/              # Jetpack Compose UI
│   ├── domain/          # Business logic
│   ├── data/            # Data models and repositories
│   ├── platform/        # Android-specific implementations
│   └── di/              # Dependency injection
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Protocols

### WireGuard (Recommended)
- Modern, high-performance VPN protocol
- Superior battery efficiency
- Simplified cryptography (Curve25519 + ChaCha20-Poly1305)
- Best for most users

### VLESS (Advanced)
- Next-generation proxy protocol
- Advanced obfuscation capabilities
- Reality protocol support
- Ideal for restrictive network environments

## Security

- All credentials encrypted with Android Keystore
- Hardware-backed encryption when available
- No data collection or tracking
- DNS leak prevention
- IPv6 leak prevention

## License

[To be determined]

## Contributing

Contributions are welcome! Please read the contributing guidelines before submitting pull requests.

## Acknowledgments

- [WireGuard](https://www.wireguard.com/) - Fast, modern VPN protocol
- [Xray-core](https://github.com/XTLS/Xray-core) - VLESS protocol implementation
