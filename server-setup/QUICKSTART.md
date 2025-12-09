# Quick Start Guide

Get your VPN server running in minutes!

## Prerequisites

- Ubuntu 20.04, 22.04, or 24.04 LTS server
- Root access (sudo)
- Public IP address or domain name
- Open firewall ports (scripts will configure automatically)

## WireGuard Setup (Recommended)

**Best for**: Most users - fast, efficient, simple

### 1. Download the script

```bash
wget https://raw.githubusercontent.com/your-repo/selfproxy/main/server-setup/setup-wireguard.sh
chmod +x setup-wireguard.sh
```

### 2. Run the script

```bash
sudo ./setup-wireguard.sh
```

### 3. Follow the prompts

- Confirm your public IP
- Choose whether to generate a preshared key (recommended for post-quantum security)
- Wait for installation to complete

### 4. Import into SelfProxy app

**Option A: Scan QR Code** (Easiest)
1. Open SelfProxy app
2. Tap "Add Profile"
3. Select "WireGuard"
4. Tap "Scan QR Code"
5. Scan the QR code displayed in terminal

**Option B: Import Config File**
1. Transfer `~/wireguard-client/client.conf` to your phone
2. Open SelfProxy app
3. Tap "Add Profile"
4. Select "WireGuard"
5. Tap "Import Config"
6. Select the config file

### 5. Connect!

Tap "Connect" and you're done! 🎉

---

## VLESS Setup (Advanced)

**Best for**: Users requiring obfuscation in restrictive networks

### 1. Prerequisites

You'll need a domain name pointing to your server:
```bash
# Check your server's public IP
curl https://api.ipify.org

# Make sure your domain points to this IP
# Example: vpn.example.com → 203.0.113.42
```

### 2. Download the script

```bash
wget https://raw.githubusercontent.com/your-repo/selfproxy/main/server-setup/setup-vless.sh
chmod +x setup-vless.sh
```

### 3. Run the script

```bash
sudo ./setup-vless.sh
```

### 4. Follow the prompts

- Enter your domain name (e.g., vpn.example.com)
- Select transport protocol:
  - **TCP**: Simple, good performance
  - **WebSocket**: Best for restrictive networks
  - **gRPC**: Modern, efficient
  - **HTTP/2**: Good balance
- Choose whether to enable Reality protocol (advanced obfuscation)
- Wait for installation to complete

### 5. Import into SelfProxy app

**Option A: VLESS URI** (Easiest)
1. Copy the VLESS URI from terminal output
2. Open SelfProxy app
3. Tap "Add Profile"
4. Select "VLESS"
5. Tap "Import URI"
6. Paste the URI

**Option B: JSON Config**
1. Transfer `~/vless-client/client-config.json` to your phone
2. Open SelfProxy app
3. Tap "Add Profile"
4. Select "VLESS"
5. Tap "Import JSON"
6. Select the config file

### 6. Connect!

Tap "Connect" and enjoy obfuscated traffic! 🎉

---

## Troubleshooting

### WireGuard

**Can't connect?**
```bash
# Check if WireGuard is running
sudo wg show

# Check logs
sudo journalctl -u wg-quick@wg0 -n 50

# Restart service
sudo systemctl restart wg-quick@wg0
```

**Firewall issues?**
```bash
# Check firewall status
sudo ufw status

# Make sure WireGuard port is open
sudo ufw allow 51820/udp
```

### VLESS

**Can't connect?**
```bash
# Check if Xray is running
sudo systemctl status xray

# Check logs
sudo journalctl -u xray -n 50

# Restart service
sudo systemctl restart xray
```

**TLS certificate issues?**
```bash
# Check certificate
sudo certbot certificates

# Renew certificate
sudo certbot renew

# Make sure domain points to server
dig +short vpn.example.com
```

**Port not accessible?**
```bash
# Check if port is listening
sudo netstat -tlnp | grep 443

# Check firewall
sudo ufw status

# Make sure port is open
sudo ufw allow 443/tcp
```

---

## Next Steps

### Secure Your Server

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Configure SSH key authentication
# Disable password authentication in /etc/ssh/sshd_config

# Enable automatic security updates
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

### Monitor Your Server

```bash
# Check connection status
# WireGuard:
sudo wg show

# VLESS:
sudo systemctl status xray

# View logs
# WireGuard:
sudo journalctl -u wg-quick@wg0 -f

# VLESS:
sudo journalctl -u xray -f
```

### Add More Clients

#### WireGuard

To add additional clients, you'll need to:
1. Generate new client keys
2. Add peer to server config
3. Create client config
4. Restart WireGuard

See the full documentation for details.

#### VLESS

VLESS supports multiple clients with the same UUID, so you can use the same configuration on multiple devices. For better security, you can add multiple UUIDs to the server config.

---

## Getting Help

- **Documentation**: See [README.md](README.md) for detailed information
- **Issues**: Open an issue on GitHub
- **Community**: Join our discussion forum

---

## Security Notes

### WireGuard
- Keep your private keys secure
- Never share your private key
- Use preshared keys for post-quantum security
- Regularly update WireGuard

### VLESS
- Keep your UUID secure
- Use TLS for encryption
- Consider Reality protocol for obfuscation
- Regularly update Xray-core
- Monitor server logs for suspicious activity

---

## Performance Tips

### WireGuard
- Already optimized for performance!
- Adjust MTU if experiencing issues: `MTU = 1420`
- Use persistent keepalive for NAT: `PersistentKeepalive = 25`

### VLESS
- TCP transport: Best performance
- WebSocket: Good for restrictive networks
- gRPC: Good balance
- HTTP/2: Wide compatibility
- Enable Reality only if needed (adds overhead)

---

## Uninstallation

### WireGuard

```bash
sudo wg-quick down wg0
sudo systemctl disable wg-quick@wg0
sudo apt remove --purge wireguard wireguard-tools
sudo rm -rf /etc/wireguard
```

### VLESS

```bash
sudo systemctl stop xray
sudo systemctl disable xray
bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ remove
sudo rm -rf /usr/local/etc/xray
```

---

## FAQ

**Q: Which protocol should I use?**
A: WireGuard for most users. VLESS if you need obfuscation.

**Q: Can I use both protocols on the same server?**
A: Yes! Just use different ports.

**Q: How much does it cost?**
A: Just the cost of your VPS (typically $5-10/month).

**Q: Is it secure?**
A: Yes! Both protocols use modern cryptography. WireGuard uses Curve25519 + ChaCha20-Poly1305. VLESS uses TLS 1.3.

**Q: How many devices can connect?**
A: WireGuard: One device per client config. VLESS: Multiple devices with same UUID.

**Q: What about IPv6?**
A: Both protocols support IPv6. Scripts configure it automatically.

**Q: Can I use this in China/Iran/etc?**
A: WireGuard may be detected. VLESS with Reality protocol is designed for this use case.

---

Happy tunneling! 🚀
