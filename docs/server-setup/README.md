# Server Setup Documentation

Complete guides for setting up VPN servers that work with the SelfProxy Android application.

## 📚 Documentation Index

### Quick Start Guides

- **[QUICKSTART.md](QUICKSTART.md)** - Fast setup for both WireGuard and VLESS
  - Step-by-step installation
  - Import methods
  - Basic troubleshooting
  - Security tips

### Detailed Guides

- **[QR_CODE_GENERATOR.md](QR_CODE_GENERATOR.md)** - VLESS QR code generation
  - How to use `show-vless-qr.sh`
  - Multiple import methods
  - Advanced usage
  - Troubleshooting

- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common issues and solutions
  - WireGuard issues
  - VLESS issues
  - QR code generator issues
  - Client-side issues
  - Debugging commands

- **[CHANGELOG.md](CHANGELOG.md)** - Recent updates and changes
  - New features
  - Documentation updates
  - Migration guide

## 🚀 Quick Links

### Setup Scripts

| Script | Purpose | Platform |
|--------|---------|----------|
| [setup-vless.sh](setup-vless.sh) | VLESS server setup | Ubuntu |
| [setup-vless-amazon.sh](setup-vless-amazon.sh) | VLESS server setup | Amazon Linux 2023 |
| [show-vless-qr.sh](show-vless-qr.sh) | Generate QR code | Any Linux |

### Download Commands

```bash
# VLESS setup (Ubuntu)
wget https://raw.githubusercontent.com/badscrew/selfproxy2/main/docs/server-setup/setup-vless.sh
chmod +x setup-vless.sh
sudo ./setup-vless.sh

# VLESS setup (Amazon Linux)
wget https://raw.githubusercontent.com/badscrew/selfproxy2/main/docs/server-setup/setup-vless-amazon.sh
chmod +x setup-vless-amazon.sh
sudo ./setup-vless-amazon.sh

# QR code generator
wget https://raw.githubusercontent.com/badscrew/selfproxy2/main/docs/server-setup/show-vless-qr.sh
chmod +x show-vless-qr.sh
./show-vless-qr.sh
```

## 🎯 Choose Your Protocol

### WireGuard (Recommended for Most Users)

**Pros:**
- ✅ Fastest performance
- ✅ Lowest battery usage
- ✅ Simplest setup
- ✅ Modern cryptography
- ✅ Built into Linux kernel

**Cons:**
- ❌ May be detected/blocked in restrictive networks
- ❌ Less obfuscation options

**Best for:**
- General VPN use
- Privacy and security
- Fast connections
- Low latency applications

### VLESS (Advanced Users)

**Pros:**
- ✅ Advanced obfuscation (Reality protocol)
- ✅ Works in restrictive networks
- ✅ Multiple transport options
- ✅ TLS encryption
- ✅ Harder to detect

**Cons:**
- ❌ More complex setup
- ❌ Requires domain name (for TLS)
- ❌ Slightly higher overhead

**Best for:**
- Restrictive networks (China, Iran, etc.)
- Users needing obfuscation
- Advanced users
- When WireGuard is blocked

## 📖 Getting Started

### 1. Choose Your Protocol

Read the comparison above and decide which protocol fits your needs.

### 2. Follow the Quick Start

Open [QUICKSTART.md](QUICKSTART.md) and follow the guide for your chosen protocol.

### 3. Import Configuration

**For VLESS:**
- Use the QR code generator (easiest!)
- Or manually enter configuration
- See [QR_CODE_GENERATOR.md](QR_CODE_GENERATOR.md)

### 4. Connect

Open the SelfProxy app and connect to your server!

## 🔧 Troubleshooting

Having issues? Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for:
- Common error messages
- Step-by-step solutions
- Debugging commands
- Performance tips

## 📱 SelfProxy App

### Import Methods

#### QR Code (Recommended)
1. Generate QR code on server
2. Scan with app
3. Connect!

#### Manual Entry
1. Get configuration values
2. Enter in app form
3. Save and connect!

#### URI Import
1. Copy VLESS URI
2. Import in app
3. Connect!

### App Features

- ✅ Multiple profiles
- ✅ QR code scanner
- ✅ Manual configuration
- ✅ Connection statistics
- ✅ Per-app routing
- ✅ Battery optimization

## 🔒 Security Best Practices

### Server Security

```bash
# Keep system updated
sudo apt update && sudo apt upgrade -y

# Use SSH keys (not passwords)
# Disable password authentication in /etc/ssh/sshd_config

# Enable firewall
sudo ufw enable
sudo ufw allow 22/tcp  # SSH
sudo ufw allow 443/tcp # VLESS

# Enable automatic security updates
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

### Configuration Security

- 🔐 Keep UUIDs/keys private
- 🔐 Don't share QR codes publicly
- 🔐 Use strong passwords for server access
- 🔐 Regularly update software
- 🔐 Monitor server logs

### Client Security

- 🔐 Use device lock screen
- 🔐 Enable "Block connections without VPN"
- 🔐 Don't install untrusted apps
- 🔐 Keep app updated

## 📊 Performance Tips

### Server Optimization

```bash
# Check server load
top
htop

# Monitor bandwidth
iftop
nethogs

# Check disk space
df -h

# View logs
sudo journalctl -u xray -f
```

### Client Optimization

- Use TCP transport for best performance
- Adjust MTU if experiencing issues
- Enable persistent keepalive for NAT
- Choose nearby server location

## 🆘 Getting Help

### Documentation

1. Check [QUICKSTART.md](QUICKSTART.md)
2. Review [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
3. Read [QR_CODE_GENERATOR.md](QR_CODE_GENERATOR.md)

### Community Support

- 📝 Open an issue on GitHub
- 💬 Join the discussion forum
- 📧 Contact maintainers

### When Reporting Issues

Include:
- Server OS and version
- Protocol (WireGuard or VLESS)
- Error messages
- Relevant log output
- Steps to reproduce

## 🔄 Updates

### Checking for Updates

```bash
# VLESS (Xray)
xray version
bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install

# System updates
sudo apt update && sudo apt upgrade -y
```

### Changelog

See [CHANGELOG.md](CHANGELOG.md) for recent updates and changes.

## 🌟 Features

### Current Features

- ✅ Automated server setup
- ✅ QR code generation
- ✅ Multiple import methods
- ✅ Reality protocol support
- ✅ TLS encryption
- ✅ Multiple transport protocols
- ✅ Comprehensive documentation

### Coming Soon

- [ ] Web-based management panel
- [ ] Multi-user support
- [ ] Traffic statistics
- [ ] Automated backups
- [ ] Configuration templates

## 📄 License

See the main [LICENSE](../../LICENSE) file for details.

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📞 Support

- **GitHub Issues**: Bug reports and feature requests
- **Discussions**: Questions and community support
- **Documentation**: This folder!

---

**Happy tunneling! 🚀**

For more information, see the main [project README](../../README.md).
