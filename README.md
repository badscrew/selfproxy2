# SelfProxy VPN

A native Android VPN application that gives you complete control over your privacy by routing traffic through your own VPN servers. Unlike commercial VPN services, SelfProxy lets you use your own infrastructure (cloud servers, home servers, or workplace servers) as secure tunnels.

## Why SelfProxy?

**Own Your Privacy**: Your data, your server, your rules. No third-party VPN provider can see your traffic.

**Modern Protocols**: Support for WireGuard (recommended) and VLESS (advanced obfuscation).

**Battery Efficient**: Optimized for all-day use, especially with WireGuard's efficient cryptography.

**No Tracking**: Zero data collection, no analytics, no telemetry. Open source and auditable.

## Features

### Core Functionality
- **Dual Protocol Support**: WireGuard (default) and VLESS (advanced)
- **Profile Management**: Create, edit, and manage multiple server profiles
- **One-Tap Connection**: Quick connection to any saved profile
- **QR Code Import**: Scan WireGuard QR codes for instant setup
- **URI Import**: Import VLESS configurations via URI

### Security & Privacy
- **Hardware-Backed Encryption**: Android Keystore with StrongBox support
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

### Protocols

#### WireGuard (Recommended)
- **Best for**: Most users
- **Advantages**: Fast, efficient, simple, excellent battery life
- **Cryptography**: Curve25519 + ChaCha20-Poly1305
- **Setup time**: < 2 minutes with QR code

#### VLESS (Advanced)
- **Best for**: Users requiring obfuscation in restrictive networks
- **Advantages**: Multiple transports, TLS/Reality support, traffic obfuscation
- **Transports**: TCP, WebSocket, gRPC, HTTP/2
- **Setup time**: 5-10 minutes

## Requirements

- Android 8.0 (API 26) or higher
- VPN permission (requested on first use)
- Internet access
- Your own VPN server (see [Server Setup](#server-setup))

## Quick Start

### 1. Set Up Your Server

Choose your protocol and follow the setup guide:

**WireGuard (Recommended)**:
```bash
wget https://raw.githubusercontent.com/your-repo/selfproxy/main/docs/server-setup/setup-wireguard.sh
chmod +x setup-wireguard.sh
sudo ./setup-wireguard.sh
```

**VLESS (Advanced)**:
```bash
wget https://raw.githubusercontent.com/your-repo/selfproxy/main/docs/server-setup/setup-vless.sh
chmod +x setup-vless.sh
sudo ./setup-vless.sh
```

See [Server Setup Guide](docs/server-setup/README.md) for detailed instructions.

### 2. Install the App

Download the latest APK from [Releases](https://github.com/your-repo/selfproxy/releases) or build from source.

### 3. Import Configuration

**WireGuard**:
- Scan the QR code displayed by the setup script, or
- Import the `client.conf` file, or
- Enter details manually

**VLESS**:
- Paste the VLESS URI from the setup script, or
- Import the JSON configuration file, or
- Enter details manually

### 4. Connect

Tap "Connect" and you're protected! 🎉

## Documentation

### User Guides
- [Quick Start Guide](docs/server-setup/QUICKSTART.md) - Get started in minutes
- [WireGuard Setup Guide](#wireguard-setup-guide) - Detailed WireGuard configuration
- [VLESS Setup Guide](#vless-setup-guide) - Detailed VLESS configuration
- [QR Code Import Guide](#qr-code-import) - How to scan and import QR codes
- [Troubleshooting Guide](docs/server-setup/TROUBLESHOOTING.md) - Common issues and solutions
- [Privacy Features](#privacy-features) - How we protect your privacy

### Technical Documentation
- [Architecture Overview](#architecture) - System design and components
- [Building from Source](#building-from-source) - Compilation instructions
- [Testing](#testing) - Running tests
- [Contributing](#contributing) - How to contribute

## WireGuard Setup Guide

### Prerequisites
- Ubuntu 20.04, 22.04, or 24.04 LTS server
- Root access (sudo)
- Public IP address

### Server Setup

1. **Run the setup script**:
```bash
sudo ./setup-wireguard.sh
```

2. **Follow the prompts**:
   - Confirm your public IP
   - Choose whether to generate a preshared key (recommended)
   - Wait for installation to complete

3. **Save the configuration**:
   - QR code will be displayed in terminal
   - Configuration saved to `~/wireguard-client/client.conf`
   - QR code image saved to `~/wireguard-client/client-qr.png`

### App Configuration

**Method 1: QR Code (Easiest)**
1. Open SelfProxy app
2. Tap "+" to add profile
3. Select "WireGuard"
4. Tap "Scan QR Code"
5. Scan the QR code from your terminal

**Method 2: Import File**
1. Transfer `client.conf` to your phone
2. Open SelfProxy app
3. Tap "+" to add profile
4. Select "WireGuard"
5. Tap "Import Config"
6. Select the config file

**Method 3: Manual Entry**
1. Open SelfProxy app
2. Tap "+" to add profile
3. Select "WireGuard"
4. Enter the following details:
   - **Name**: Any name (e.g., "My VPN Server")
   - **Server Address**: Your server's IP or hostname
   - **Port**: 51820 (or custom port)
   - **Private Key**: From `client.conf` [Interface] section
   - **Public Key**: From `client.conf` [Peer] section
   - **Allowed IPs**: `0.0.0.0/0, ::/0` (route all traffic)
   - **DNS**: `1.1.1.1, 1.0.0.1` (or custom DNS)

### Advanced Options

**Persistent Keepalive**:
- Set to `25` if behind NAT
- Set to `0` to disable (saves battery)

**Preshared Key**:
- Optional post-quantum security
- Generated by setup script if requested
- Copy from `client.conf` [Peer] section

**MTU**:
- Default: `1420`
- Reduce if experiencing connection issues: `1400`, `1380`

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

## QR Code Import

### Scanning QR Codes

1. **Open SelfProxy app**
2. **Tap "+" to add profile**
3. **Select "WireGuard"**
4. **Tap "Scan QR Code"**
5. **Grant camera permission** (if prompted)
6. **Point camera at QR code**
7. **Wait for automatic detection**
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
- Protocol Adapters: WireGuard and VLESS implementations
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
- WireGuard Adapter: WireGuard protocol implementation
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
│   │   ├── wireguard/               # WireGuard adapter
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
git clone https://github.com/your-repo/selfproxy.git
cd selfproxy
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
- Use WireGuard instead of VLESS
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

- [WireGuard](https://www.wireguard.com/) - Fast, modern VPN protocol
- [Xray-core](https://github.com/XTLS/Xray-core) - VLESS protocol implementation
- [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) - Android Xray library
- [wireguard-android](https://git.zx2c4.com/wireguard-android/) - WireGuard Android library

## Support

- **Documentation**: See guides above
- **Issues**: [GitHub Issues](https://github.com/your-repo/selfproxy/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-repo/selfproxy/discussions)

## Security

To report security vulnerabilities, please email security@example.com (do not open public issues).

## Roadmap

### Current Version (v1.0)
- ✅ WireGuard support
- ✅ VLESS support
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
- [ ] Additional protocols (if requested)

---

**Made with ❤️ for privacy-conscious users**
