# Server Setup Scripts

This directory contains automated setup scripts for deploying VPN servers that work with the SelfProxy Android application.

## Supported Protocols

### WireGuard (Recommended)
- **Best for**: Most users
- **Advantages**: Fast, efficient, simple, excellent battery life
- **Script**: `setup-wireguard.sh` (Ubuntu), `setup-wireguard-amazon.sh` (Amazon Linux)

### VLESS (Advanced)
- **Best for**: Users requiring obfuscation in restrictive networks
- **Advantages**: Multiple transports, TLS/Reality support, traffic obfuscation
- **Script**: `setup-vless.sh` (Ubuntu), `setup-vless-amazon.sh` (Amazon Linux)

> **Note**: Amazon Linux scripts are currently available by cloning the repository. Direct download URLs will be available once the latest changes are pushed to the main branch.

## Requirements

### Supported Operating Systems
- **Ubuntu**: 20.04, 22.04, or 24.04 LTS
- **Amazon Linux**: 2023 (RedHat-based)
- **Amazon Linux 2**: Supported but not optimized

### System Requirements
- Root access or sudo privileges
- Public IP address or domain name
- Open firewall ports (configured automatically by scripts)

## Quick Start

### WireGuard Setup

**Ubuntu 20.04/22.04/24.04:**
```bash
# Download and run the script
wget https://raw.githubusercontent.com/badscrew/selfproxy2/main/docs/server-setup/setup-wireguard.sh
chmod +x setup-wireguard.sh
sudo ./setup-wireguard.sh
```

**Amazon Linux 2023:**
```bash
# Note: Amazon Linux scripts are currently in development
# Clone the repository to access the latest scripts:
git clone https://github.com/badscrew/selfproxy2.git
cd selfproxy2/docs/server-setup
chmod +x setup-wireguard-amazon.sh
sudo ./setup-wireguard-amazon.sh
```

The script will:
1. Install WireGuard
2. Generate server and client keys
3. Configure the WireGuard interface
4. Set up IP forwarding and firewall rules
5. Generate client configuration with QR code

### VLESS Setup

**Ubuntu 20.04/22.04/24.04:**
```bash
# Download and run the script
wget https://raw.githubusercontent.com/badscrew/selfproxy2/main/docs/server-setup/setup-vless.sh
chmod +x setup-vless.sh
sudo ./setup-vless.sh
```

**Amazon Linux 2023:**
```bash
# Note: Amazon Linux scripts are currently in development
# Clone the repository to access the latest scripts:
git clone https://github.com/badscrew/selfproxy2.git
cd selfproxy2/docs/server-setup
chmod +x setup-vless-amazon.sh
sudo ./setup-vless-amazon.sh
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

## Operating System Differences

### Ubuntu vs Amazon Linux

| Feature | Ubuntu Scripts | Amazon Linux Scripts |
|---------|---------------|---------------------|
| Package Manager | `apt-get` | `dnf` |
| Firewall | `ufw` | `firewalld` |
| Repository | Default Ubuntu repos | EPEL repository |
| Service Management | `systemctl` | `systemctl` |
| File Paths | Same | Same |

### Amazon Linux Specific Notes

- **Package Manager**: Uses `dnf` (modern replacement for `yum`)
- **Firewall**: Uses `firewalld` instead of `ufw`
- **EPEL Repository**: Automatically installed for additional packages
- **SELinux**: May require configuration for advanced setups
- **Kernel Modules**: WireGuard kernel module included in Amazon Linux 2023

### Alternative: Manual Script Creation

If you prefer not to clone the repository, you can create the Amazon Linux scripts manually:

**Create WireGuard script for Amazon Linux:**
```bash
curl -o setup-wireguard-amazon.sh https://gist.githubusercontent.com/[gist-id]/raw/setup-wireguard-amazon.sh
# Or copy the script content from the repository and save as setup-wireguard-amazon.sh
```

**Create VLESS script for Amazon Linux:**
```bash
curl -o setup-vless-amazon.sh https://gist.githubusercontent.com/[gist-id]/raw/setup-vless-amazon.sh
# Or copy the script content from the repository and save as setup-vless-amazon.sh
```

## Firewall Configuration

### Ubuntu (UFW)
Scripts automatically configure UFW (Uncomplicated Firewall):

**WireGuard**:
- UDP port 51820 (or custom port)
- SSH access maintained

**VLESS**:
- TCP port 443 (HTTPS)
- Optional HTTP port 80 (for Let's Encrypt)
- SSH access maintained

### Amazon Linux (firewalld)
Scripts automatically configure firewalld:

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
