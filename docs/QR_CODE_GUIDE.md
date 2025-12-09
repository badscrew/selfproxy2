# QR Code Import Guide

Complete guide to importing WireGuard configurations using QR codes.

## Table of Contents
- [What is a QR Code?](#what-is-a-qr-code)
- [Generating QR Codes](#generating-qr-codes)
- [Scanning QR Codes](#scanning-qr-codes)
- [Troubleshooting](#troubleshooting)
- [Security Considerations](#security-considerations)
- [Alternative Import Methods](#alternative-import-methods)

## What is a QR Code?

A QR code is a two-dimensional barcode that can store text data. For WireGuard, the QR code contains your complete client configuration, including:

- Private key
- Server public key
- Server endpoint (IP and port)
- Allowed IPs (routing rules)
- DNS servers
- Optional preshared key
- Optional persistent keepalive

**Example QR Code Content**:
```
[Interface]
PrivateKey = YourPrivateKeyHere==
Address = 10.8.0.2/24
DNS = 1.1.1.1, 1.0.0.1

[Peer]
PublicKey = ServerPublicKeyHere==
PresharedKey = OptionalPresharedKeyHere==
Endpoint = vpn.example.com:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

## Generating QR Codes

### Using the Setup Script

Our WireGuard setup script automatically generates QR codes:

```bash
sudo ./setup-wireguard.sh
```

The script will:
1. Install WireGuard
2. Generate server and client keys
3. Create client configuration
4. Display QR code in terminal
5. Save QR code as image: `~/wireguard-client/client-qr.png`

### Manual QR Code Generation

If you have a WireGuard config file, you can generate a QR code manually:

**Using qrencode (Linux/Mac)**:
```bash
# Install qrencode
sudo apt install qrencode  # Ubuntu/Debian
brew install qrencode      # macOS

# Generate QR code
qrencode -t ansiutf8 < client.conf

# Save as image
qrencode -t png -o client-qr.png < client.conf
```

**Using Python**:
```python
import qrcode

# Read config file
with open('client.conf', 'r') as f:
    config = f.read()

# Generate QR code
qr = qrcode.QRCode(version=1, box_size=10, border=5)
qr.add_data(config)
qr.make(fit=True)

# Save as image
img = qr.make_image(fill_color="black", back_color="white")
img.save("client-qr.png")
```

**Using Online Tools** (⚠️ Not Recommended):
- Never use online QR code generators for VPN configs
- Your private key would be exposed to the website
- Always generate QR codes locally

## Scanning QR Codes

### Step-by-Step Guide

1. **Open SelfProxy App**
   - Launch the SelfProxy VPN app on your Android device

2. **Add New Profile**
   - Tap the "+" button (usually in the bottom-right corner)
   - Or tap "Add Profile" if no profiles exist

3. **Select WireGuard**
   - Choose "WireGuard" from the protocol options
   - (VLESS doesn't support QR codes)

4. **Choose Import Method**
   - Tap "Scan QR Code"
   - Grant camera permission if prompted

5. **Scan the QR Code**
   - Point your phone's camera at the QR code
   - Hold steady and ensure good lighting
   - The app will automatically detect and scan the code

6. **Review Configuration**
   - The app will display the imported configuration
   - Review the details:
     - Profile name (you can change this)
     - Server address
     - Port
     - DNS servers
   - Tap "Save" to save the profile

7. **Connect**
   - Tap "Connect" on your new profile
   - Grant VPN permission if prompted
   - You're connected! 🎉

### Camera Permission

**If camera permission is denied**:

1. Go to Android Settings
2. Navigate to Apps → SelfProxy
3. Tap Permissions
4. Enable Camera permission
5. Return to SelfProxy and try again

### Scanning Tips

**For best results**:
- ✅ Use good lighting (natural light is best)
- ✅ Hold phone steady
- ✅ Keep QR code flat and unobstructed
- ✅ Ensure QR code is in focus
- ✅ Fill most of the camera frame with the QR code
- ✅ Clean your camera lens

**Avoid**:
- ❌ Dim lighting or shadows
- ❌ Blurry or out-of-focus QR codes
- ❌ Damaged or partially obscured QR codes
- ❌ Reflections or glare on screen/paper
- ❌ Moving the phone while scanning

## Troubleshooting

### QR Code Won't Scan

**Problem**: Camera doesn't detect the QR code

**Solutions**:

1. **Check lighting**:
   ```
   - Move to a brighter area
   - Avoid shadows on the QR code
   - Turn on room lights
   ```

2. **Adjust distance**:
   ```
   - Move phone closer or farther away
   - Try different distances until QR code is in focus
   ```

3. **Clean camera lens**:
   ```
   - Wipe camera lens with soft cloth
   - Remove any fingerprints or smudges
   ```

4. **Check QR code quality**:
   ```
   - Ensure QR code isn't damaged
   - If on screen, increase brightness
   - If printed, ensure high-quality print
   ```

5. **Restart camera**:
   ```
   - Go back and re-enter QR scan screen
   - Or restart the app
   ```

### Invalid QR Code Error

**Problem**: QR code scans but shows "Invalid configuration"

**Possible causes**:

1. **Not a WireGuard QR code**:
   - QR code might be for something else
   - Verify it's from WireGuard setup script

2. **Corrupted QR code**:
   - QR code might be damaged
   - Try regenerating the QR code

3. **Incomplete configuration**:
   - QR code might be missing required fields
   - Check the original config file

**Solutions**:

1. **Verify config file**:
   ```bash
   cat client.conf
   # Should contain [Interface] and [Peer] sections
   ```

2. **Regenerate QR code**:
   ```bash
   qrencode -t ansiutf8 < client.conf
   ```

3. **Try manual import**:
   - Use "Import Config File" instead
   - Or enter details manually

### Camera Permission Issues

**Problem**: Camera permission denied

**Solution**:

1. **Grant permission in settings**:
   ```
   Settings → Apps → SelfProxy → Permissions → Camera → Allow
   ```

2. **Check Android version**:
   - Android 6.0+: Runtime permissions required
   - Android 5.x and below: Permissions granted at install

3. **Reinstall app** (if permission is stuck):
   ```bash
   adb uninstall com.selfproxy.vpn
   adb install app-debug.apk
   ```

### QR Code Too Large

**Problem**: QR code is too complex to scan

**Cause**: Configuration has too much data (common with long preshared keys)

**Solutions**:

1. **Increase QR code size**:
   ```bash
   qrencode -t png -s 10 -o client-qr.png < client.conf
   # -s 10 increases module size
   ```

2. **Simplify configuration**:
   - Remove optional fields
   - Use shorter DNS entries
   - Remove comments

3. **Use file import instead**:
   - Transfer config file to phone
   - Use "Import Config File" option

### QR Code on Screen Issues

**Problem**: Scanning QR code from computer screen

**Tips**:

1. **Increase screen brightness**:
   - Set to maximum brightness
   - Disable auto-brightness

2. **Reduce glare**:
   - Tilt screen to avoid reflections
   - Turn off room lights if needed

3. **Use full screen**:
   - Open QR code image in full screen
   - Or zoom in on terminal QR code

4. **Alternative**: Save QR code as image and print it

## Security Considerations

### QR Code Security

**⚠️ Important**: QR codes contain your private key!

**Security best practices**:

1. **Never share QR codes**:
   - QR code = complete VPN access
   - Treat it like a password
   - Don't post on social media
   - Don't send via unencrypted channels

2. **Secure transmission**:
   - ✅ Scan directly from server terminal
   - ✅ Transfer via encrypted channel (SSH, HTTPS)
   - ✅ Use secure messaging (Signal, WhatsApp)
   - ❌ Don't email unencrypted
   - ❌ Don't post in public forums
   - ❌ Don't share via SMS

3. **Secure storage**:
   - Delete QR code images after import
   - Don't save to cloud storage
   - Don't print unless necessary
   - Shred printed QR codes when done

4. **Screen security**:
   - Be aware of cameras/people when displaying QR code
   - Don't display in public places
   - Lock screen after scanning

### What If QR Code Is Compromised?

If someone else scans your QR code:

1. **They can access your VPN**:
   - They have your private key
   - They can connect to your VPN server
   - They can use your VPN bandwidth

2. **Immediate actions**:
   ```bash
   # On server, remove the compromised peer
   sudo wg-quick down wg0
   sudo nano /etc/wireguard/wg0.conf
   # Remove the [Peer] section
   sudo wg-quick up wg0
   
   # Generate new client keys
   wg genkey | tee client_private.key | wg pubkey > client_public.key
   
   # Add new peer to server
   # Generate new QR code
   ```

3. **Prevention**:
   - Generate unique keys for each device
   - Use separate profiles for different devices
   - Regularly audit connected peers

## Alternative Import Methods

If QR code scanning doesn't work, try these alternatives:

### Method 1: Import Config File

1. **Transfer config file to phone**:
   ```bash
   # Using adb
   adb push client.conf /sdcard/Download/
   
   # Or use file transfer app
   # Or email to yourself (encrypted)
   ```

2. **Import in app**:
   - Tap "+" → "WireGuard" → "Import Config File"
   - Navigate to Downloads folder
   - Select `client.conf`

### Method 2: Manual Entry

1. **Get configuration details**:
   ```bash
   cat client.conf
   ```

2. **Enter in app**:
   - Tap "+" → "WireGuard" → "Manual Entry"
   - Fill in all fields from config file

3. **Required fields**:
   - Name: Any name you choose
   - Private Key: From [Interface] section
   - Public Key: From [Peer] section
   - Endpoint: From [Peer] section (server:port)
   - Allowed IPs: From [Peer] section
   - DNS: From [Interface] section

### Method 3: Copy-Paste

1. **Copy config text**:
   ```bash
   cat client.conf | xclip -selection clipboard  # Linux
   cat client.conf | pbcopy                      # macOS
   ```

2. **Paste in app**:
   - Some apps support pasting config text
   - Or paste into text editor on phone
   - Then import from file

## Best Practices

### For Server Administrators

1. **Generate QR codes securely**:
   ```bash
   # Generate on server (not on public machine)
   qrencode -t ansiutf8 < client.conf
   ```

2. **Display QR codes securely**:
   - Show only to intended user
   - Use SSH session (not public terminal)
   - Clear terminal after scanning

3. **Provide multiple import options**:
   - QR code (easiest)
   - Config file (backup)
   - Manual instructions (fallback)

4. **Document the process**:
   - Provide clear instructions
   - Include troubleshooting steps
   - Offer support contact

### For Users

1. **Verify QR code source**:
   - Only scan QR codes from trusted sources
   - Verify server administrator identity
   - Don't scan random QR codes

2. **Test connection**:
   - After import, test connection immediately
   - Verify internet works through VPN
   - Check for DNS leaks

3. **Backup configuration**:
   - Save config file as backup
   - Store securely (encrypted storage)
   - Don't rely only on QR code

4. **Keep credentials secure**:
   - Don't share profiles
   - Use separate profiles per device
   - Revoke access when device is lost

## FAQ

**Q: Can I scan a QR code from a photo?**  
A: Yes, but it's not recommended for security. Better to scan directly from source.

**Q: Can I reuse a QR code for multiple devices?**  
A: Technically yes, but not recommended. Generate unique keys per device for better security.

**Q: What if I lose my QR code?**  
A: Contact your server administrator to generate a new one. Old keys should be revoked.

**Q: Can I edit a profile after importing via QR code?**  
A: Yes, you can edit all fields in the app after import.

**Q: Does VLESS support QR codes?**  
A: No, VLESS uses URIs instead. Use "Import URI" for VLESS.

**Q: Is it safe to scan QR codes in public?**  
A: No, someone could photograph your screen. Scan in private.

**Q: Can I generate a QR code from an existing profile?**  
A: Yes, use the "Export" feature in the app to generate a QR code.

**Q: What's the maximum size for a WireGuard QR code?**  
A: Typically 2-3 KB of text. If larger, consider file import instead.

## Additional Resources

- [WireGuard QR Code Format](https://www.wireguard.com/quickstart/)
- [QR Code Specification](https://www.qrcode.com/en/about/)
- [SelfProxy Documentation](../README.md)
- [Troubleshooting Guide](../server-setup/TROUBLESHOOTING.md)

## Support

Having issues with QR code scanning?

- Check the [Troubleshooting](#troubleshooting) section above
- See the [main Troubleshooting Guide](../server-setup/TROUBLESHOOTING.md)
- Open an issue on [GitHub](https://github.com/your-repo/selfproxy/issues)
- Contact support: support@example.com

---

**Remember**: QR codes contain sensitive credentials. Treat them like passwords! 🔐
