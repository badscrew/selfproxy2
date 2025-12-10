# Server Setup Scripts

This directory contains automated setup scripts for deploying VPN servers that work with the SelfProxy Android application.

## Supported Protocols

### WireGuard (Recommended)
- **Best for**: Most users
- **Advantages**: Fast, efficient, simple, excellent battery life
- **Script**: `setup-wireguard.sh`

### VLESS (Advanced)
- **Best for**: Users requiring obfuscation in restrictive networks
- **Advantages**: Multiple transports, TLS/Reality support, traffic obfuscation
- **Script**: `setup-vless.sh`

## Requirements

- Ubuntu 20.04, 22.04, or 24.04 LTS
- Root access or sudo privileges
- Public IP address or domain name
- Open firewall ports (configured automatically by scripts)

## Quick Start

### WireGuard Setup

```bash
# Download and run the script
wget https://raw.githubusercontent.com/your-repo/selfproxy/main/docs/server-setup/setup-wireguard.sh
chmod +x setup-wireguard.sh
sudo ./setup-wireguard.sh
```

The script will:
1. Install WireGuard
2. Generate server and client keys
3. Configure the WireGuard interface
4. Set up IP forwarding and firewall rules
5. Generate client configuration with QR code

### VLESS Setup

```bash
# Download and run the script
wget https://raw.githubusercontent.com/your-repo/selfproxy/main/docs/server-setup/setup-vless.sh
chmod +x setup-vless.sh
sudo ./setup-vless.sh
```

The script will:
1. Install Xray-core
2. Generate UUID for authentication
3. Configure transport protocol (TCP/WebSocket/gRPC/HTTP2)
4. Set up TLS with Let's Encrypt (optional)
5. Configure firewall rules
6. Generate client configuration (URI and JSON)

## Configuration Options

### WireGuard Options

- **Port**: Default 51820 (customizable)
- **Network**: Default 10.8.0.0/24
- **DNS**: Default 1.1.1.1, 1.0.0.1
- **Persistent Keepalive**: Optional (for NAT traversal)
- **Preshared Key**: Optional (post-quantum security)

### VLESS Options

- **Port**: Default 443 (HTTPS)
- **Transport**: TCP, WebSocket, gRPC, or HTTP/2
- **TLS**: Let's Encrypt automatic certificate
- **Reality**: Advanced obfuscation (optional)
- **Fallback**: Nginx fallback for unrecognized traffic

## Security Considerations

### WireGuard
- Uses Curve25519 for key exchange
- ChaCha20-Poly1305 for encryption
- Optional preshared keys for post-quantum security
- Minimal attack surface

### VLESS
- TLS 1.3 encryption
- Certificate validation
- Reality protocol for traffic obfuscation
- Fallback to legitimate website

## Firewall Configuration

Both scripts automatically configure UFW (Uncomplicated Firewall):

**WireGuard**:
- UDP port 51820 (or custom port)
- IP forwarding enabled
- NAT masquerading configured

**VLESS**:
- TCP port 443 (HTTPS)
- Optional HTTP port 80 (for Let's Encrypt)
- IP forwarding enabled

## Client Configuration

### WireGuard

After setup, you'll receive:
1. **Configuration file**: `client.conf`
2. **QR code**: Displayed in terminal and saved as `client-qr.png`
3. **Manual configuration details**: For manual entry

Import into SelfProxy app:
- Scan QR code, or
- Import configuration file, or
- Enter details manually

### VLESS

After setup, you'll receive:
1. **VLESS URI**: `vless://uuid@server:port?...`
2. **JSON configuration**: `client-config.json`
3. **Connection details**: For manual entry

Import into SelfProxy app:
- Paste VLESS URI, or
- Import JSON configuration, or
- Enter details manually

## Troubleshooting

### WireGuard

**Connection fails**:
- Check firewall: `sudo ufw status`
- Verify WireGuard is running: `sudo wg show`
- Check server logs: `sudo journalctl -u wg-quick@wg0`

**No internet through tunnel**:
- Verify IP forwarding: `sysctl net.ipv4.ip_forward`
- Check NAT rules: `sudo iptables -t nat -L`

### VLESS

**Connection fails**:
- Check Xray status: `sudo systemctl status xray`
- Verify port is open: `sudo netstat -tlnp | grep 443`
- Check logs: `sudo journalctl -u xray`

**TLS certificate issues**:
- Verify domain points to server IP
- Check Let's Encrypt logs: `sudo certbot certificates`
- Ensure port 80 is accessible for HTTP challenge

## Updating

### WireGuard
```bash
sudo apt update && sudo apt upgrade wireguard
sudo systemctl restart wg-quick@wg0
```

### VLESS
```bash
# Download latest Xray-core
bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install
sudo systemctl restart xray
```

## Uninstallation

### WireGuard
```bash
sudo wg-quick down wg0
sudo apt remove --purge wireguard
sudo rm -rf /etc/wireguard
```

### VLESS
```bash
sudo systemctl stop xray
sudo systemctl disable xray
bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ remove
sudo rm -rf /usr/local/etc/xray
```

## Support

For issues or questions:
- Check the [troubleshooting guide](../docs/troubleshooting.md)
- Open an issue on GitHub
- Consult the [SelfProxy documentation](../README.md)

## License

These scripts are provided under the same license as the SelfProxy project.
