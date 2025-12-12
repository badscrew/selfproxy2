# Privacy Policy & Features

## Our Privacy Commitment

SelfProxy is built with privacy as the foundation. We believe your data belongs to you, and only you.

## What We DON'T Collect

SelfProxy collects **absolutely zero** user data:

### ❌ No Browsing Data
- No websites visited
- No URLs accessed
- No search queries
- No HTTP headers
- No cookies or tracking data

### ❌ No Connection Data
- No connection timestamps
- No connection duration
- No bandwidth usage logs
- No server locations
- No IP addresses (yours or destinations)

### ❌ No DNS Data
- No DNS queries logged
- No domain names recorded
- No DNS resolution history

### ❌ No Personal Information
- No email addresses
- No phone numbers
- No names
- No payment information (app is free)
- No device identifiers

### ❌ No Analytics or Telemetry
- No Google Analytics
- No Firebase Analytics
- No crash reporting with personal data
- No usage statistics
- No feature usage tracking

### ❌ No Third-Party Services
- No advertising networks
- No tracking pixels
- No social media integrations
- No third-party SDKs (except VPN protocol libraries)

## What We DO Store (Locally Only)

All data is stored **only on your device** and **never transmitted** to us or anyone else:

### ✅ VPN Server Profiles
- Server addresses (hostnames/IPs)
- Port numbers
- Protocol type (VLESS)
- Profile names (your custom names)
- Last connection time (for sorting)

**Storage**: Room database on your device  
**Encryption**: Database encrypted with Android's built-in encryption

### ✅ VPN Credentials
- VLESS UUIDs
- TLS certificates (if used)

**Storage**: Android Keystore  
**Encryption**: Hardware-backed AES-256-GCM with StrongBox when available

### ✅ App Settings
- DNS server preferences
- Per-app routing configuration
- Auto-reconnect settings
- Theme preferences

**Storage**: SharedPreferences on your device  
**Encryption**: Android's encrypted SharedPreferences

### ✅ Temporary Connection State
- Current connection status
- Real-time bandwidth statistics
- Connection duration

**Storage**: In-memory only (lost when app closes)  
**Encryption**: Not persisted to disk

## How Your Privacy is Protected

### 1. Secure Credential Storage

All sensitive credentials are encrypted using Android Keystore:

```
Your Private Key → Android Keystore → Hardware Security Module
                                    ↓
                              Encrypted Storage
```

**Features**:
- Hardware-backed encryption when available
- StrongBox support on compatible devices
- Keys never leave secure hardware
- Biometric authentication support (planned)

### 2. DNS Leak Prevention

All DNS queries are routed through your VPN tunnel:

```
Your App → DNS Query → VPN Tunnel → Your VPN Server → Internet
                    ↓
              (NOT to ISP)
```

**Protection**:
- System DNS bypassed
- Custom DNS servers (1.1.1.1, 1.0.0.1 by default)
- DNS queries encrypted within VPN tunnel
- No DNS leaks to ISP or local network

**Verification**:
- Built-in DNS leak test
- Traffic verification feature
- Real-time leak detection

### 3. IPv6 Leak Prevention

IPv6 traffic is properly handled to prevent leaks:

**If server supports IPv6**:
```
IPv6 Traffic → VPN Tunnel → Your VPN Server → Internet
```

**If server doesn't support IPv6**:
```
IPv6 Traffic → BLOCKED (prevents leak)
```

### 4. Traffic Encryption

All traffic is encrypted end-to-end:

**VLESS**:
- TLS 1.3 encryption
- Reality protocol for obfuscation
- Optional preshared keys for post-quantum security

**VLESS**:
- TLS 1.3 encryption
- Reality protocol for obfuscation
- Certificate validation

### 5. Log Sanitization

When debugging logs are enabled:

**Redacted**:
- ✅ Private keys → `[REDACTED]`
- ✅ UUIDs → `[REDACTED]`
- ✅ Preshared keys → `[REDACTED]`
- ✅ Passwords → `[REDACTED]`
- ✅ IP addresses → `[REDACTED]`

**Logged**:
- Connection status (connected/disconnected)
- Error messages (without sensitive data)
- Protocol types
- Timestamps

### 6. No Network Requests to Us

SelfProxy makes **zero** network requests to our servers:

**We don't have**:
- Analytics servers
- Telemetry endpoints
- Update servers
- Crash reporting servers
- API servers

**The app only connects to**:
- Your VPN server (that you control)
- Traffic verification service (optional, only when you request it)

### 7. Open Source Transparency

Complete transparency through open source:

**You can verify**:
- No hidden tracking code
- No data collection
- No third-party analytics
- No backdoors

**Audit the code**:
```bash
git clone https://github.com/badscrew/selfproxy2.git
# Search for any suspicious code
grep -r "analytics" .
grep -r "tracking" .
grep -r "telemetry" .
# Result: Nothing found
```

## Data Retention

### On Your Device
- **VPN profiles**: Until you delete them
- **Credentials**: Until you delete profiles or uninstall app
- **Settings**: Until you uninstall app
- **Connection state**: Until you disconnect or close app

### On Our Servers
- **Nothing**: We don't have servers that store your data

### On Your VPN Server
- **Depends on your configuration**: You control your server
- **Recommendation**: Disable logging on your VPN server for maximum privacy

## Third-Party Services

### VPN Protocol Libraries

We use open-source libraries for VPN protocols:

**VLESS**:
- Library: `AndroidLibXrayLite`
- License: MPL 2.0
- Privacy: No data collection
- Source: https://github.com/2dust/AndroidLibXrayLite

### Traffic Verification (Optional)

When you use the "Verify Traffic" feature:

**Service**: https://api.ipify.org (or similar)  
**Purpose**: Check your public IP address  
**Data sent**: HTTP request (no personal data)  
**Data received**: Your public IP address  
**Frequency**: Only when you tap "Verify Traffic"  
**Can be disabled**: Yes, it's optional

## Your Rights

### Access Your Data
All your data is on your device. You can:
- View all profiles in the app
- Export configurations
- Access app settings

### Delete Your Data
You can delete your data at any time:
- Delete individual profiles
- Clear all profiles
- Uninstall the app (removes everything)

### Export Your Data
You can export your configurations:
- VLESS: Export as URI or JSON

## Compliance

### GDPR (EU)
- ✅ No personal data collected
- ✅ No data processing
- ✅ No data transfers
- ✅ Right to deletion (uninstall app)
- ✅ Data portability (export configs)

### CCPA (California)
- ✅ No personal information collected
- ✅ No personal information sold
- ✅ No personal information shared

### Other Regulations
Since we collect no data, we comply with virtually all privacy regulations worldwide.

## Security Measures

### App Security
- ✅ Hardware-backed credential encryption
- ✅ StrongBox support
- ✅ Secure key generation
- ✅ Certificate validation
- ✅ No root required
- ✅ Sandboxed execution

### Code Security
- ✅ Regular security audits (planned)
- ✅ Dependency updates
- ✅ ProGuard/R8 obfuscation
- ✅ No hardcoded secrets
- ✅ Secure coding practices

### Network Security
- ✅ TLS 1.3 for VLESS
- ✅ Reality protocol for obfuscation
- ✅ Certificate pinning (planned)
- ✅ DNS over VPN
- ✅ IPv6 leak prevention

## Transparency Report

### Data Requests
- **Received**: 0
- **Complied**: 0
- **Reason**: We have no data to provide

### Vulnerabilities
- **Reported**: 0
- **Fixed**: 0
- **Pending**: 0

### Breaches
- **Occurred**: 0
- **Reason**: We store no data on our servers

## Contact

### Security Issues
Email: security@example.com  
PGP Key: [To be added]

### Privacy Questions
Email: privacy@example.com

### General Support
GitHub Issues: https://github.com/badscrew/selfproxy2/issues

## Changes to This Policy

We will notify users of any changes to this privacy policy through:
- App update notes
- GitHub releases
- In-app notification (if significant changes)

**Last Updated**: December 2024

## Summary

**In plain English**:

1. We collect **nothing**
2. We store **nothing** on our servers
3. We track **nothing**
4. Your data stays on **your device**
5. Your VPN traffic goes to **your server**
6. We can't see your data because **we don't have it**

**That's it. That's the policy.**

---

**Questions?** Open an issue on GitHub or email privacy@example.com

**Want to verify?** Audit our open-source code: https://github.com/badscrew/selfproxy2
