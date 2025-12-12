# VLESS QR Code Generator

Generate QR codes for easy import of VLESS configurations into the SelfProxy Android app.

## Overview

The `show-vless-qr.sh` script automatically:
- ✅ Extracts configuration from your Xray server
- ✅ Builds complete VLESS URI with all parameters
- ✅ Generates QR code in terminal
- ✅ Saves URI to file for later use
- ✅ Displays all values for manual entry

## Quick Start

```bash
# Download the script
wget https://raw.githubusercontent.com/badscrew/selfproxy2/main/docs/server-setup/show-vless-qr.sh
chmod +x show-vless-qr.sh

# Run it
./show-vless-qr.sh
```

## What It Extracts

The script automatically reads your Xray configuration and extracts:

| Parameter | Description | Example |
|-----------|-------------|---------|
| UUID | Client identifier | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| Server IP | Your server's public IP | `203.0.113.42` |
| Port | VLESS listening port | `443` |
| Transport | Connection transport | `tcp` |
| Security | Security protocol | `reality` or `tls` |
| SNI | Server Name Indication | `www.microsoft.com` |
| Public Key | Reality public key | `SomeBase64EncodedKey...` |
| Short ID | Reality short identifier | `0123456789abcdef` |
| Fingerprint | TLS fingerprint | `chrome` |

## Output

### Configuration Values

```
==========================================
Configuration Values:
==========================================
Server IP:   203.0.113.42
Port:        443
UUID:        a1b2c3d4-e5f6-7890-abcd-ef1234567890
Public Key:  SomeBase64EncodedPublicKey
Short ID:    0123456789abcdef
SNI:         www.microsoft.com
```

### Complete URI

```
==========================================
Complete VLESS URI:
==========================================
vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@203.0.113.42:443?type=tcp&security=reality&sni=www.microsoft.com&pbk=SomeBase64EncodedPublicKey&sid=0123456789abcdef&fp=chrome#Reality-Server
```

### QR Code

```
==========================================
QR Code (scan with SelfProxy app):
==========================================

█████████████████████████████████
█████████████████████████████████
███ ▄▄▄▄▄ █▀█ █▄▄▀▄▀█ ▄▄▄▄▄ ███
███ █   █ █▀▀▀█ ▀ █▀█ █   █ ███
███ █▄▄▄█ █▀ █▀▀█▄ ▀█ █▄▄▄█ ███
███▄▄▄▄▄▄▄█▄▀ ▀▄█ █▄█▄▄▄▄▄▄▄███
███ ▄ ▄ ▄▄ ▄▀▀▄▀▀▄ ▀▄ ▄▀▀▄▀▄███
███▄▀█▄█▀▄▀▄█▄ ▀▄▀▀▄▀▄▀▄▀▄▀▄███
███ ▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄███
███▄▄▄▄▄▄▄█▄██▄█▄██▄▄▄▄▄▄▄▄▄███
█████████████████████████████████
```

## Usage Methods

### Method 1: Scan QR Code (Recommended)

**Fastest way - 30 seconds total!**

1. **On server**: Run the script
   ```bash
   ./show-vless-qr.sh
   ```

2. **On phone**: 
   - Open SelfProxy app
   - Go to Profiles screen
   - Tap QR scanner icon (📷 top right)
   - Point camera at QR code
   - Profile imported automatically!

### Method 2: Manual Entry

**When QR scanning isn't convenient**

1. **On server**: Run the script to see all values
   ```bash
   ./show-vless-qr.sh
   ```

2. **On phone**:
   - Open SelfProxy app
   - Tap "+" → "VLESS"
   - Fill in the form with values from script output:
     - Name: Your choice
     - Hostname: Server IP from script
     - Port: Port from script
     - UUID: UUID from script
     - Transport: TCP (or as shown)
     - Security: Reality (if shown)
     - Reality Server Name: SNI from script
     - Reality Public Key: Public Key from script
     - Reality Short ID: Short ID from script
     - Fingerprint: chrome (or as shown)
   - Save and connect!

### Method 3: Copy URI

**For advanced users or sharing**

1. **On server**: Run the script
   ```bash
   ./show-vless-qr.sh
   ```

2. **Copy the URI** from output or from file:
   ```bash
   cat ~/vless-uri.txt
   ```

3. **Share or convert**:
   - Convert to QR code online: https://www.qr-code-generator.com/
   - Share URI directly (keep it secure!)
   - Save for backup

## Requirements

### System Requirements

- Linux server with Xray installed
- Xray configuration at `/usr/local/etc/xray/config.json`
- Internet connection (to detect public IP)

### Automatic Installation

The script automatically installs `qrencode` if not present:
- **Ubuntu/Debian**: Uses `apt-get`
- **Amazon Linux/RHEL**: Uses `yum`
- **Other**: Manual installation required

### Manual Installation

If automatic installation fails:

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install qrencode

# Amazon Linux/RHEL/CentOS
sudo yum install qrencode

# Fedora
sudo dnf install qrencode

# Arch Linux
sudo pacman -S qrencode
```

## Advanced Usage

### Generate Larger QR Code

```bash
# Larger QR code (scale 10)
qrencode -t ANSIUTF8 -s 10 < ~/vless-uri.txt
```

### Save QR Code as Image

```bash
# PNG image
qrencode -t PNG -o ~/vless-qr.png < ~/vless-uri.txt

# SVG image (scalable)
qrencode -t SVG -o ~/vless-qr.svg < ~/vless-uri.txt

# Transfer to phone
scp ~/vless-qr.png user@phone-ip:~/
```

### View Saved URI

```bash
# View URI
cat ~/vless-uri.txt

# Copy to clipboard (if xclip installed)
cat ~/vless-uri.txt | xclip -selection clipboard
```

### Regenerate QR Code

```bash
# From saved URI
qrencode -t ANSIUTF8 < ~/vless-uri.txt
```

## Supported Configurations

### Transport Protocols

- ✅ TCP
- ✅ WebSocket
- ✅ gRPC
- ✅ HTTP/2

### Security Types

- ✅ None (no encryption)
- ✅ TLS (standard encryption)
- ✅ Reality (advanced obfuscation)

### Flow Control

- ✅ None
- ✅ XTLS-RPRX-Vision

## Troubleshooting

### Script Cannot Find Config

**Error**: `Xray config not found`

**Solution**:
```bash
# Check if Xray is installed
xray version

# Find config location
sudo find / -name "config.json" -path "*/xray/*"

# Create symlink if needed
sudo ln -s /actual/path/config.json /usr/local/etc/xray/config.json
```

### Cannot Extract Values

**Error**: `Could not extract all required values`

**Solution**:
```bash
# Check config manually
sudo cat /usr/local/etc/xray/config.json

# Ensure it has these fields:
# - "id" (UUID)
# - "publicKey" (for Reality)
# - "shortIds" (for Reality)
```

### QR Code Too Small

**Solution**:
```bash
# Maximize terminal window
# Or generate larger QR code
qrencode -t ANSIUTF8 -s 10 < ~/vless-uri.txt

# Or save as image
qrencode -t PNG -o ~/vless-qr.png < ~/vless-uri.txt
```

### Cannot Scan QR Code

**Solutions**:
1. Maximize terminal window
2. Increase screen brightness
3. Clean camera lens
4. Save as image and scan from phone gallery
5. Use the URI directly for manual entry

### Wrong IP Address

**Symptom**: Script shows internal IP

**Solution**:
```bash
# Check public IP manually
curl https://api.ipify.org

# Edit script or manually build URI with correct IP
```

## Security Considerations

### Keep Your Configuration Secure

- ⚠️ **UUID is sensitive** - Don't share publicly
- ⚠️ **QR code contains full config** - Don't post screenshots
- ⚠️ **URI file is unencrypted** - Protect `~/vless-uri.txt`
- ✅ **Delete after use** if concerned:
  ```bash
  rm ~/vless-uri.txt ~/vless-qr.png
  ```

### Regenerate When Needed

You can safely regenerate the QR code anytime:
- After changing server configuration
- When adding new devices
- If you lost the original configuration

The script always reads the current server configuration.

## Integration with Setup Scripts

The QR code generator works with configurations created by:
- `setup-vless.sh` - Standard VLESS setup
- `setup-vless-amazon.sh` - Amazon Linux setup
- Manual Xray configurations

## Examples

### Reality Configuration

```bash
$ ./show-vless-qr.sh

==========================================
Configuration Values:
==========================================
Server IP:   203.0.113.42
Port:        443
UUID:        a1b2c3d4-e5f6-7890-abcd-ef1234567890
Public Key:  SomeBase64EncodedPublicKey
Short ID:    0123456789abcdef
SNI:         www.microsoft.com

==========================================
Complete VLESS URI:
==========================================
vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@203.0.113.42:443?type=tcp&security=reality&sni=www.microsoft.com&pbk=SomeBase64EncodedPublicKey&sid=0123456789abcdef&fp=chrome#Reality-Server

URI saved to: ~/vless-uri.txt
```

### TLS Configuration

```bash
$ ./show-vless-qr.sh

==========================================
Configuration Values:
==========================================
Server IP:   203.0.113.42
Port:        443
UUID:        a1b2c3d4-e5f6-7890-abcd-ef1234567890
SNI:         vpn.example.com

==========================================
Complete VLESS URI:
==========================================
vless://a1b2c3d4-e5f6-7890-abcd-ef1234567890@203.0.113.42:443?type=tcp&security=tls&sni=vpn.example.com#TLS-Server

URI saved to: ~/vless-uri.txt
```

## FAQ

**Q: Do I need to run this after every server restart?**
A: No, the configuration doesn't change. Run it once after setup.

**Q: Can I use this for multiple clients?**
A: Yes! VLESS supports multiple devices with the same UUID.

**Q: What if I change my server configuration?**
A: Run the script again to generate a new QR code with updated settings.

**Q: Is the QR code secure?**
A: The QR code contains your full configuration including UUID. Don't share it publicly.

**Q: Can I use this without Reality?**
A: Yes! The script works with TLS-only and even no-security configurations.

**Q: What if qrencode installation fails?**
A: You can still use the URI manually. Copy it from `~/vless-uri.txt`.

**Q: Can I customize the QR code?**
A: Yes! Use `qrencode` directly with your preferred options:
```bash
qrencode -t PNG -s 10 -o custom-qr.png < ~/vless-uri.txt
```

## Support

For issues or questions:
- Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- Open an issue on GitHub
- Review Xray documentation

## Related Documentation

- [QUICKSTART.md](QUICKSTART.md) - Server setup guide
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues
- [Xray Documentation](https://xtls.github.io/) - Xray-core docs
- [SelfProxy README](../../README.md) - App documentation
