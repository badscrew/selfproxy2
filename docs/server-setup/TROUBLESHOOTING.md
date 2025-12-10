# Troubleshooting Guide

Common issues and solutions for WireGuard and VLESS server setup.

## General Issues

### Script Fails to Run

**Error**: `Permission denied`

**Solution**:
```bash
chmod +x setup-wireguard.sh
# or
chmod +x setup-vless.sh
```

**Error**: `This script must be run as root`

**Solution**:
```bash
sudo ./setup-wireguard.sh
# or
sudo ./setup-vless.sh
```

### Cannot Determine Public IP

**Symptom**: Script cannot detect your public IP address

**Solution**:
```bash
# Manually check your public IP
curl https://api.ipify.org

# Or use another service
curl https://ifconfig.me

# Enter it when prompted by the script
```

---

## WireGuard Issues

### Installation Fails

**Error**: `Package wireguard not found`

**Solution**:
```bash
# Update package lists
sudo apt update

# Try installing from backports (Ubuntu 20.04)
sudo apt install wireguard-dkms wireguard-tools

# Check kernel version (WireGuard requires kernel 5.6+)
uname -r
```

### WireGuard Won't Start

**Error**: `Failed to start wg-quick@wg0.service`

**Check logs**:
```bash
sudo journalctl -u wg-quick@wg0 -n 50
```

**Common causes**:

1. **Port already in use**:
```bash
# Check if port is in use
sudo netstat -ulnp | grep 51820

# Kill the process or change WG_PORT
export WG_PORT=51821
sudo ./setup-wireguard.sh
```

2. **Invalid configuration**:
```bash
# Check config syntax
sudo wg-quick up /etc/wireguard/wg0.conf

# Look for error messages
```

3. **Firewall blocking**:
```bash
# Check firewall
sudo ufw status

# Allow WireGuard port
sudo ufw allow 51820/udp
```

### Can Connect But No Internet

**Symptom**: VPN connects but cannot access internet

**Check IP forwarding**:
```bash
# Check if enabled
sysctl net.ipv4.ip_forward

# Should return: net.ipv4.ip_forward = 1
# If not:
sudo sysctl -w net.ipv4.ip_forward=1
```

**Check NAT rules**:
```bash
# View NAT rules
sudo iptables -t nat -L -v

# Should see MASQUERADE rule
# If not, add it:
sudo iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
# (replace eth0 with your interface)
```

**Check routing**:
```bash
# On server, check if packets are being forwarded
sudo tcpdump -i wg0

# Should see traffic when client is connected
```

### Handshake Fails

**Symptom**: Client shows "Handshake timeout" or "No recent handshake"

**Check server status**:
```bash
sudo wg show

# Should show:
# - interface: wg0
# - public key
# - listening port
# - peer with your client's public key
```

**Verify keys**:
```bash
# Server public key should match client config
cat /etc/wireguard/server_public.key

# Client public key should be in server config
sudo grep -A 5 "\[Peer\]" /etc/wireguard/wg0.conf
```

**Check firewall**:
```bash
# On server
sudo ufw status | grep 51820

# On client's network, check if UDP 51820 is blocked
# Try from another network
```

### DNS Not Working

**Symptom**: VPN connects but DNS resolution fails

**Check DNS configuration**:
```bash
# On client, check DNS servers
# Should be set in WireGuard config: DNS = 1.1.1.1, 1.0.0.1
```

**Test DNS**:
```bash
# On client, while connected
nslookup google.com 1.1.1.1

# Should resolve successfully
```

**Alternative DNS servers**:
- Cloudflare: `1.1.1.1, 1.0.0.1`
- Google: `8.8.8.8, 8.8.4.4`
- Quad9: `9.9.9.9, 149.112.112.112`

---

## VLESS Issues

### Xray Installation Fails

**Error**: `Failed to install Xray-core`

**Solution**:
```bash
# Try manual installation
bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install

# Check if installed
xray version

# If still fails, download manually
wget https://github.com/XTLS/Xray-core/releases/latest/download/Xray-linux-64.zip
unzip Xray-linux-64.zip
sudo mv xray /usr/local/bin/
sudo chmod +x /usr/local/bin/xray
```

### TLS Certificate Fails

**Error**: `Failed to obtain TLS certificate`

**Common causes**:

1. **Domain doesn't point to server**:
```bash
# Check DNS
dig +short vpn.example.com

# Should return your server's IP
# If not, update DNS records and wait for propagation
```

2. **Port 80 blocked**:
```bash
# Check if port 80 is accessible
sudo netstat -tlnp | grep :80

# Check firewall
sudo ufw status | grep 80

# Allow port 80
sudo ufw allow 80/tcp
```

3. **Nginx or Apache running**:
```bash
# Stop other web servers
sudo systemctl stop nginx
sudo systemctl stop apache2

# Try certbot again
sudo certbot certonly --standalone -d vpn.example.com
```

4. **Rate limit reached**:
```bash
# Let's Encrypt has rate limits
# Wait an hour and try again
# Or use staging environment for testing:
sudo certbot certonly --staging --standalone -d vpn.example.com
```

### Xray Won't Start

**Error**: `Failed to start xray.service`

**Check logs**:
```bash
sudo journalctl -u xray -n 50
```

**Common causes**:

1. **Invalid configuration**:
```bash
# Test config
xray -test -config /usr/local/etc/xray/config.json

# Should say "Configuration OK"
```

2. **Port already in use**:
```bash
# Check if port 443 is in use
sudo netstat -tlnp | grep :443

# Kill the process or change port
```

3. **Certificate file not found**:
```bash
# Check certificate paths
sudo ls -la /etc/letsencrypt/live/vpn.example.com/

# Should see fullchain.pem and privkey.pem
```

### Can Connect But No Internet

**Symptom**: VLESS connects but cannot access internet

**Check Xray logs**:
```bash
sudo journalctl -u xray -f

# Should see connection logs when client connects
```

**Check outbound configuration**:
```bash
# View config
sudo cat /usr/local/etc/xray/config.json

# Should have outbound with protocol: "freedom"
```

**Check firewall**:
```bash
# Make sure outbound traffic is allowed
sudo iptables -L -v

# Check if IP forwarding is enabled
sysctl net.ipv4.ip_forward
```

### Reality Protocol Issues

**Symptom**: Reality connection fails

**Check SNI**:
```bash
# Test if SNI is accessible
curl -I https://www.microsoft.com

# Should return 200 OK
# If not, choose a different SNI
```

**Verify keys**:
```bash
# Generate new keys if needed
xray x25519

# Update config with new keys
```

**Check logs**:
```bash
sudo journalctl -u xray -f

# Look for Reality-specific errors
```

### WebSocket Connection Fails

**Symptom**: WebSocket transport doesn't work

**Check path**:
```bash
# Path in server config should match client
# Server: "path": "/ws"
# Client: path=/ws in URI
```

**Test WebSocket**:
```bash
# Use wscat to test
npm install -g wscat
wscat -c wss://vpn.example.com/ws

# Should connect successfully
```

### gRPC Connection Fails

**Symptom**: gRPC transport doesn't work

**Check service name**:
```bash
# Service name in server config should match client
# Server: "serviceName": "vless-grpc"
# Client: serviceName=vless-grpc in URI
```

**Check ALPN**:
```bash
# gRPC requires h2 ALPN
# Should be in TLS settings: "alpn": ["h2"]
```

---

## Client-Side Issues

### Invalid Configuration

**Error**: "Invalid WireGuard configuration"

**Check**:
- Private key is valid base64
- Public key is valid base64
- Endpoint format is correct: `hostname:port`
- AllowedIPs format is correct: `0.0.0.0/0, ::/0`

**Error**: "Invalid VLESS URI"

**Check**:
- URI starts with `vless://`
- UUID is valid RFC 4122 format
- Parameters are properly encoded
- No spaces in URI

### Connection Timeout

**WireGuard**:
```bash
# On server, check if client's handshake is received
sudo wg show

# Should show "latest handshake" with recent timestamp
```

**VLESS**:
```bash
# On server, check logs
sudo journalctl -u xray -f

# Should see connection attempts
```

**Network issues**:
- Check if server is reachable: `ping server-ip`
- Check if port is open: `telnet server-ip port`
- Try from different network (mobile data vs WiFi)

### DNS Leaks

**Test for leaks**:
```bash
# While connected, check DNS
nslookup google.com

# Should use VPN's DNS servers
```

**Fix**:
- WireGuard: Ensure `DNS = 1.1.1.1, 1.0.0.1` in config
- VLESS: DNS should go through tunnel automatically
- Android: Check "Block connections without VPN" in settings

---

## Performance Issues

### Slow Connection

**WireGuard**:
```bash
# Check MTU
# Try different values: 1420, 1400, 1380
# In client config: MTU = 1420
```

**VLESS**:
- TCP transport: Best performance
- Try different transport protocols
- Check server load: `top`, `htop`

### High CPU Usage

**WireGuard**:
- Should be very low CPU usage
- Check for kernel module: `lsmod | grep wireguard`
- If using wireguard-go, consider kernel module

**VLESS**:
- Check Xray version: `xray version`
- Update to latest: `bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install`
- Disable unnecessary logging

### Packet Loss

**Check network**:
```bash
# Ping server
ping -c 100 server-ip

# Should have <1% packet loss
```

**Check MTU**:
```bash
# Test MTU
ping -M do -s 1472 server-ip

# If fails, reduce MTU in config
```

---

## Debugging Commands

### WireGuard

```bash
# Show interface status
sudo wg show

# Show detailed interface info
sudo wg show wg0

# Show config
sudo cat /etc/wireguard/wg0.conf

# Test config
sudo wg-quick up /etc/wireguard/wg0.conf

# View logs
sudo journalctl -u wg-quick@wg0 -f

# Capture packets
sudo tcpdump -i wg0 -n

# Check routing
ip route show table all | grep wg0
```

### VLESS

```bash
# Check Xray status
sudo systemctl status xray

# Test config
xray -test -config /usr/local/etc/xray/config.json

# View logs
sudo journalctl -u xray -f

# View access log
sudo tail -f /var/log/xray/access.log

# View error log
sudo tail -f /var/log/xray/error.log

# Check listening ports
sudo netstat -tlnp | grep xray

# Capture packets
sudo tcpdump -i any port 443 -n
```

### System

```bash
# Check firewall
sudo ufw status verbose

# Check IP forwarding
sysctl net.ipv4.ip_forward
sysctl net.ipv6.conf.all.forwarding

# Check NAT rules
sudo iptables -t nat -L -v

# Check routes
ip route show

# Check DNS
cat /etc/resolv.conf

# Check network interfaces
ip addr show

# Check system resources
top
htop
free -h
df -h
```

---

## Getting More Help

If you're still having issues:

1. **Check logs**: Always check server logs first
2. **Search issues**: Look for similar issues on GitHub
3. **Ask for help**: Open an issue with:
   - Server OS and version
   - Protocol (WireGuard or VLESS)
   - Error messages
   - Relevant log output
   - Steps to reproduce

---

## Useful Resources

- [WireGuard Documentation](https://www.wireguard.com/)
- [Xray Documentation](https://xtls.github.io/)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [UFW Documentation](https://help.ubuntu.com/community/UFW)
- [SelfProxy GitHub](https://github.com/badscrew/selfproxy2)
