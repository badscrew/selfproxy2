# SelfProxy VPN

A native Android VPN application that gives you complete control over your privacy by routing traffic through your own VPN servers using the VLESS protocol. Unlike commercial VPN services, SelfProxy lets you use your own infrastructure (cloud servers, home servers, or workplace servers) as secure tunnels.

## Why SelfProxy?

**Own Your Privacy**: Your data, your server, your rules. No third-party VPN provider can see your traffic.

**Modern Protocol**: VLESS with advanced obfuscation and multiple transport options.

**Bypass Restrictions**: Designed to work in restrictive network environments with deep packet inspection.

**No Tracking**: Zero data collection, no analytics, no telemetry. Open source and auditable.

## Features

### Core Functionality
- **VLESS Protocol**: Advanced VPN protocol with traffic obfuscation
- **Profile Management**: Create, edit, and manage multiple server profiles
- **One-Tap Connection**: Quick connection to any saved profile
- **URI Import**: Import VLESS configurations via URI
- **Multiple Transports**: TCP, WebSocket, gRPC, HTTP/2

### Security & Privacy
- **Hardware-Backed Encryption**: Android Keystore with StrongBox support
- **TLS 1.3 Encryption**: Modern encryption with Reality protocol support
- **DNS Leak Prevention**: All DNS queries routed through tunnel
- **IPv6 Leak Prevention**: Proper IPv6 handling or blocking
- **No Data Collection**: Zero tracking, analytics, or telemetry
- **Open Source**: Fully auditable code

### Advanced Features
- **Per-App Routing**: Choose which apps use the VPN tunnel
- **Auto-Reconnection**: Automatic recovery from connection drops
- **Network Change Handling**: Seamless transitions between WiFi and mobile data
- **Connection Monitoring**: Real-time bandwidth and connection statistics
- **Traffic Verification**: Verify your traffic is routed through the VPN
- **Battery Optimization**: Efficient power management for all-day protection

### VLESS Protocol

**Best for**: Users requiring obfuscation in restrictive networks

**Advantages**:
- Multiple transport protocols (TCP, WebSocket, gRPC, HTTP/2)
- TLS/Reality support for traffic obfuscation
- Bypasses deep packet inspection
- Flexible routing rules

**Transports**:
- **TCP**: Direct connection, fastest but least obfuscated
- **WebSocket**: HTTP upgrade, good for bypassing firewalls
- **gRPC**: Modern, efficient, HTTP/2 multiplexing
- **HTTP/2**: Standard HTTPS traffic, excellent obfuscation

**Setup time**: 5-10 minutes

## Requirements

- Android 8.0 (API 26) or higher
- VPN permission (requested on first use)
- Internet access
- Your own VPN server (see [Server Setup](#server-setup))

## Quick Start

### 1. Set Up Your Server

Run the automated setup script on your server:

```bash
wget https://raw.githubusercontent.com/badscrew/selfproxy2/main/docs/server-setup/setup-vless.sh
chmod +x setup-vless.sh
sudo ./setup-vless.sh
```

See [Server Setup Guide](docs/server-setup/QUICKSTART.md) for detailed instructions.

### 2. Install the App

Download the latest APK from [Releases](https://github.com/badscrew/selfproxy2/releases) or build from source.

### 3. Import Configuration

**Option 1: URI (Easiest)**
- Copy the VLESS URI from the setup script
- Open SelfProxy app
- Tap "+" to add profile
- Tap "Import URI"
- Paste the URI

**Option 2: Manual Entry**
- Open SelfProxy app
- Tap "+" to add profile
- Enter server details manually

### 4. Connect

Tap "Connect" and you're protected! 🎉

## Documentation

### User Guides
- [Quick Start Guide](docs/server-setup/QUICKSTART.md) - Get started in minutes
- [VLESS Setup Guide](#vless-setup-guide) - Detailed VLESS configuration
- [Troubleshooting Guide](docs/server-setup/TROUBLESHOOTING.md) - Common issues and solutions
- [Privacy Features](#privacy-features) - How we protect your privacy

### Technical Documentation
- [Architecture Overview](#architecture) - System design and components
- [Building from Source](#building-from-source) - Compilation instructions
- [Testing](#testing) - Running tests
- [Contributing](#contributing) - How to contribute

## VLESS Setup Guide

### Prerequisites
- Ubuntu 20.04, 22.04, or 24.04 LTS server
- Root access (sudo)
- Domain name pointing to your server (required for TLS)

### Server Setup

1. **Prepare your domain**:
```bash
# Check your server's public IP
curl https://api.ipify.org

# Make sure your domain points to this IP
# Example: vpn.example.com → 203.0.113.42
```

2. **Run the setup script**:
```bash
sudo ./setup-vless.sh
```

3. **Follow the prompts**:
   - Enter your domain name (e.g., `vpn.example.com`)
   - Select transport protocol:
     - **TCP**: Simple, good performance
     - **WebSocket**: Best for restrictive networks
     - **gRPC**: Modern, efficient
     - **HTTP/2**: Good balance
   - Choose whether to enable Reality protocol
   - Wait for installation to complete

4. **Save the configuration**:
   - VLESS URI will be displayed in terminal
   - Configuration saved to `~/vless-client/client-config.json`
   - URI saved to `~/vless-client/vless-uri.txt`

### App Configuration

**Method 1: URI (Easiest)**
1. Copy the VLESS URI from terminal
2. Open SelfProxy app
3. Tap "+" to add profile
4. Select "VLESS"
5. Tap "Import URI"
6. Paste the URI

**Method 2: Import JSON**
1. Transfer `client-config.json` to your phone
2. Open SelfProxy app
3. Tap "+" to add profile
4. Select "VLESS"
5. Tap "Import JSON"
6. Select the config file

**Method 3: Manual Entry**
1. Open SelfProxy app
2. Tap "+" to add profile
3. Select "VLESS"
4. Enter the following details:
   - **Name**: Any name (e.g., "My VLESS Server")
   - **Server Address**: Your domain (e.g., `vpn.example.com`)
   - **Port**: 443 (HTTPS)
   - **UUID**: From setup script output
   - **Transport**: TCP, WebSocket, gRPC, or HTTP/2
   - **TLS**: Enable
   - **SNI**: Your domain name

### Transport Protocols

**TCP**:
- Best performance
- Simple configuration
- Good for most users

**WebSocket**:
- Works through HTTP proxies
- Good for restrictive networks
- Custom path: `/ws` (default)

**gRPC**:
- Modern, efficient
- HTTP/2 multiplexing
- Service name: `vless-grpc` (default)

**HTTP/2**:
- Wide compatibility
- Good balance of performance and compatibility

### Reality Protocol

Reality provides advanced traffic obfuscation by mimicking legitimate HTTPS traffic.

**When to use**:
- Operating in restrictive network environments
- Need to bypass deep packet inspection
- Regular VLESS connections are blocked

**Configuration**:
- **SNI**: Target website (e.g., `www.microsoft.com`)
- **Public Key**: Generated by setup script
- **Short ID**: Random identifier

## URI Import

### Importing VLESS URIs

1. **Copy the VLESS URI** from your server setup
2. **Open SelfProxy app**
3. **Tap "+" to add profile**
4. **Tap "Import URI"**
5. **Paste the URI**
6. **Tap "Import"**
7. **Profile is ready to use**
8. **Review and save profile**

### QR Code Format

WireGuard QR codes contain the complete client configuration:
```
[Interface]
PrivateKey = <your-private-key>
Address = 10.8.0.2/24
DNS = 1.1.1.1, 1.0.0.1

[Peer]
PublicKey = <server-public-key>
PresharedKey = <optional-preshared-key>
Endpoint = <server-ip>:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

### Troubleshooting QR Codes

**QR code won't scan**:
- Ensure good lighting
- Hold phone steady
- Try zooming in/out
- Check camera focus
- Clean camera lens

**Invalid QR code error**:
- Verify QR code is for WireGuard
- Check QR code isn't damaged
- Try importing config file instead

**Camera permission denied**:
- Go to Android Settings
- Apps → SelfProxy → Permissions
- Enable Camera permission

## Privacy Features

### No Data Collection

SelfProxy collects **zero** user data:
- ❌ No browsing history
- ❌ No connection logs
- ❌ No IP addresses
- ❌ No DNS queries
- ❌ No traffic metadata
- ❌ No analytics or telemetry
- ❌ No crash reports with personal data

### Secure Credential Storage

All sensitive data is encrypted:
- **WireGuard private keys**: Encrypted with Android Keystore
- **VLESS UUIDs**: Encrypted with Android Keystore
- **Preshared keys**: Encrypted with Android Keystore
- **TLS certificates**: Encrypted with Android Keystore
- **Hardware-backed**: Uses StrongBox when available

### DNS Leak Prevention

All DNS queries are routed through the VPN tunnel:
- System DNS is bypassed
- Custom DNS servers used (1.1.1.1, 1.0.0.1 by default)
- DNS queries encrypted within VPN tunnel
- No DNS leaks to ISP or local network

### IPv6 Leak Prevention

IPv6 traffic is properly handled:
- Routed through tunnel if server supports IPv6
- Blocked if server doesn't support IPv6
- No IPv6 leaks to ISP

### Traffic Verification

Verify your traffic is protected:
1. Connect to VPN
2. Tap "Verify Traffic" in connection screen
3. App checks your public IP address
4. Confirms IP matches VPN server
5. Tests for DNS leaks

### Log Sanitization

When logging is enabled for debugging:
- Private keys are redacted
- UUIDs are redacted
- Preshared keys are redacted
- Passwords are redacted
- Only connection metadata is logged

### Open Source

Complete transparency:
- All source code is public
- No hidden tracking code
- Community auditable
- Reproducible builds (planned)

## Architecture

The application follows clean architecture principles with clear separation of concerns:

### Layers

**UI Layer** (Jetpack Compose):
- Screens: Connection, Profile List, Profile Form, Settings, App Routing
- Components: Reusable UI components
- ViewModels: State management with StateFlow
- Theme: Material Design 3

**Domain Layer** (Business Logic):
- Connection Manager: Protocol-agnostic connection management
- Protocol Adapter: VLESS implementation
- Auto-Reconnect Service: Automatic recovery
- Traffic Monitor: Bandwidth tracking
- Network Monitor: Network change detection

**Data Layer** (Persistence):
- Profile Repository: CRUD operations for profiles
- Settings Repository: App settings
- App Routing Repository: Per-app routing configuration
- Room Database: Local storage
- Credential Store: Encrypted credential storage

**Platform Layer** (Android-Specific):
- VPN Service: TUN interface and packet routing
- VLESS Adapter: VLESS protocol implementation
- Android Credential Store: Keystore integration

### Protocol Abstraction

```kotlin
interface ProtocolAdapter {
    suspend fun connect(profile: ServerProfile): Result<Connection>
    suspend fun disconnect()
    suspend fun testConnection(profile: ServerProfile): Result<ConnectionTestResult>
    fun observeConnectionState(): Flow<ConnectionState>
    fun getStatistics(): ConnectionStatistics?
}
```

This abstraction allows:
- Easy addition of new protocols
- Protocol-agnostic connection management
- Consistent error handling
- Unified testing approach

### Project Structure

```
app/
├── src/main/kotlin/com/selfproxy/vpn/
│   ├── ui/                          # Jetpack Compose UI
│   │   ├── screens/                 # Screen composables
│   │   ├── components/              # Reusable components
│   │   ├── theme/                   # Material Design theme
│   │   └── viewmodel/               # ViewModels
│   ├── domain/                      # Business logic
│   │   ├── manager/                 # Connection, traffic, network managers
│   │   ├── adapter/                 # Protocol adapter interface
│   │   ├── model/                   # Domain models
│   │   ├── repository/              # Repository interfaces
│   │   └── util/                    # Utilities
│   ├── data/                        # Data layer
│   │   ├── model/                   # Data models
│   │   ├── database/                # Room database
│   │   ├── repository/              # Repository implementations
│   │   └── config/                  # Config parsers/exporters
│   ├── platform/                    # Android-specific
│   │   ├── vpn/                     # VPN service
│   │   ├── vless/                   # VLESS adapter
│   │   └── security/                # Credential store
│   └── di/                          # Dependency injection
└── src/test/kotlin/                 # Unit tests
```

## Building from Source

### Prerequisites

- JDK 17 or higher
- Android SDK (API 34)
- Android Studio (optional, recommended)

### Clone Repository

```bash
git clone https://github.com/badscrew/selfproxy2.git
cd selfproxy2
```

### Build Debug APK

**Linux/Mac**:
```bash
./gradlew assembleDebug
```

**Windows**:
```powershell
.\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

1. **Create keystore** (first time only):
```bash
keytool -genkey -v -keystore selfproxy.keystore -alias selfproxy -keyalg RSA -keysize 2048 -validity 10000
```

2. **Create `local.properties`**:
```properties
sdk.dir=/path/to/android/sdk
storeFile=/path/to/selfproxy.keystore
storePassword=your-keystore-password
keyAlias=selfproxy
keyPassword=your-key-password
```

3. **Build release**:
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Install on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing

### Run Unit Tests

**Linux/Mac**:
```bash
./gradlew test
```

**Windows**:
```powershell
.\gradlew.bat test
```

### Run Property-Based Tests

```bash
./gradlew test --tests "*PropertiesTest"
```

### Run Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Test Coverage

```bash
./gradlew testDebugUnitTestCoverage
```

View report: `app/build/reports/coverage/test/debug/index.html`

## Troubleshooting

See the [Troubleshooting Guide](docs/server-setup/TROUBLESHOOTING.md) for common issues and solutions.

### Common Issues

**VPN won't connect**:
- Check server is running
- Verify firewall allows VPN port
- Check credentials are correct
- See detailed troubleshooting guide

**No internet through VPN**:
- Check server IP forwarding is enabled
- Verify NAT rules are configured
- Check DNS settings

**Battery drain**:
- Optimize transport protocol selection
- Disable persistent keepalive if not needed
- Check for app routing issues

**DNS leaks**:
- Verify DNS settings in profile
- Enable "Block connections without VPN" in Android settings
- Use traffic verification feature

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## License

[To be determined]

## Acknowledgments

- [Xray-core](https://github.com/XTLS/Xray-core) - VLESS protocol implementation
- [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) - Android Xray library

## Support

- **Documentation**: See guides above
- **Issues**: [GitHub Issues](https://github.com/badscrew/selfproxy2/issues)
- **Discussions**: [GitHub Discussions](https://github.com/badscrew/selfproxy2/discussions)

## Security

To report security vulnerabilities, please email security@example.com (do not open public issues).

## Roadmap

### Current Version (v1.0)
- ✅ VLESS protocol support
- ✅ Multiple transport protocols (TCP, WebSocket, gRPC, HTTP/2)
- ✅ Reality protocol support
- ✅ Profile management
- ✅ Per-app routing
- ✅ Auto-reconnection
- ✅ Traffic monitoring

### Planned Features
- [ ] Multiple simultaneous connections
- [ ] Advanced routing rules
- [ ] Traffic statistics history
- [ ] Tasker integration
- [ ] Quick settings tile
- [ ] Widget support
- [ ] Additional VLESS features (VMess compatibility, custom routing)

---

**Made with ❤️ for privacy-conscious users**
