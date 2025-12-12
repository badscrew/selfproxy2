# Server Setup Documentation Changelog

## 2025-01-XX - QR Code Generator and Manual Entry Support

### Added

#### New Script: `show-vless-qr.sh`
- Automatically extracts VLESS configuration from Xray server
- Generates QR code in terminal for easy scanning
- Displays all configuration values for manual entry
- Saves URI to `~/vless-uri.txt` for later use
- Auto-installs `qrencode` if not present
- Supports Reality, TLS, and no-security configurations

#### New Documentation: `QR_CODE_GENERATOR.md`
- Complete guide for using the QR code generator
- Usage examples for all methods (QR scan, manual entry, URI copy)
- Troubleshooting section
- Security considerations
- Advanced usage examples
- FAQ section

### Updated

#### `QUICKSTART.md`
- Added "VLESS QR Code Generator" section
- Updated import instructions with three methods:
  1. Scan QR Code (recommended)
  2. Manual Entry (now fully supported)
  3. VLESS URI (for advanced users)
- Added example output from QR script
- Added "When to Use" guidance

#### `TROUBLESHOOTING.md`
- Added "QR Code Generator Issues" section
- Troubleshooting for:
  - Configuration not found
  - Cannot extract values
  - QR code display issues
  - Scanning problems
  - Wrong IP detection
  - Reality settings not found

### App Changes

#### `ProfileFormScreen.kt`
- Added UUID input field for VLESS profiles
- Added Security type selector (None/TLS/Reality)
- Added complete Reality settings section:
  - Server Name (SNI)
  - Public Key
  - Short ID
  - Fingerprint
  - Spider X (optional)
- Added TLS settings section:
  - Server Name (SNI)
  - Fingerprint
  - Allow Insecure toggle
- Added validation for all required fields
- Added helpful placeholder text and descriptions

#### `ProfileRepositoryImpl.kt`
- Updated to store VLESS UUID in CredentialStore
- Added validation for UUID requirement
- Updated both createProfile and updateProfile methods
- Proper error handling for missing credentials

## Benefits

### For Users
- ✅ **Faster setup**: Scan QR code in 30 seconds
- ✅ **Manual entry now works**: All Reality fields available
- ✅ **Better error messages**: Clear validation feedback
- ✅ **Multiple import methods**: Choose what works best
- ✅ **No external tools needed**: Everything on server

### For Developers
- ✅ **Complete documentation**: All methods documented
- ✅ **Troubleshooting guide**: Common issues covered
- ✅ **Example outputs**: Clear expectations
- ✅ **Security notes**: Best practices included

## Migration Guide

### For Existing Users

No migration needed! Existing profiles continue to work.

To use the new features:
1. Update the SelfProxy app to latest version
2. Download `show-vless-qr.sh` to your server
3. Run it to generate QR code or get values for manual entry

### For New Users

Follow the updated QUICKSTART.md guide:
1. Set up VLESS server with `setup-vless.sh`
2. Run `show-vless-qr.sh` to generate QR code
3. Scan with SelfProxy app
4. Connect!

## Technical Details

### QR Code Format

The generated QR code contains a complete VLESS URI:

```
vless://UUID@IP:PORT?type=TRANSPORT&security=SECURITY&sni=SNI&pbk=PUBLIC_KEY&sid=SHORT_ID&fp=FINGERPRINT#NAME
```

### Supported Parameters

| Parameter | Values | Required |
|-----------|--------|----------|
| UUID | RFC 4122 UUID | Yes |
| IP | Server IP or domain | Yes |
| PORT | 1-65535 | Yes |
| type | tcp, ws, grpc, h2 | Yes |
| security | none, tls, reality | Yes |
| sni | Domain name | If TLS/Reality |
| pbk | Base64 public key | If Reality |
| sid | Hex short ID | If Reality |
| fp | Fingerprint | Optional |

### Security Considerations

- UUID is sensitive - treat like a password
- QR codes contain full configuration
- Don't share QR codes publicly
- Regenerate if compromised
- Delete saved files if concerned

## Future Enhancements

Potential future additions:
- [ ] Support for multiple UUIDs
- [ ] Batch QR code generation
- [ ] Web-based QR code viewer
- [ ] Encrypted QR codes
- [ ] Configuration backup/restore

## Feedback

We welcome feedback on these changes:
- Open an issue on GitHub
- Submit a pull request
- Join the discussion forum

## Credits

- QR code generation: `qrencode` library
- VLESS protocol: Xray-core project
- Reality protocol: XTLS project
