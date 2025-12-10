#!/bin/bash

################################################################################
# WireGuard VPN Server Setup Script for Amazon Linux 2023
# 
# This script automates the installation and configuration of a WireGuard VPN
# server on Amazon Linux 2023 (RedHat-based).
#
# Features:
# - Automatic WireGuard installation
# - Secure key generation (server and client)
# - Firewall configuration (firewalld)
# - IP forwarding setup
# - Client configuration generation with QR code
# - Optional preshared key for post-quantum security
#
# Usage: sudo ./setup-wireguard-amazon.sh
#
# Requirements: 10.1-10.12
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration variables
WG_PORT=${WG_PORT:-51820}
WG_INTERFACE="wg0"
WG_CONFIG_DIR="/etc/wireguard"
WG_CONFIG_FILE="${WG_CONFIG_DIR}/${WG_INTERFACE}.conf"
CLIENT_CONFIG_DIR="$HOME/wireguard-client"
SERVER_NETWORK="10.8.0.0/24"
SERVER_IP="10.8.0.1"
CLIENT_IP="10.8.0.2"
DNS_SERVERS="1.1.1.1,1.0.0.1"
PERSISTENT_KEEPALIVE=25

################################################################################
# Helper Functions
################################################################################

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "This script must be run as root (use sudo)"
        exit 1
    fi
}

check_amazon_linux() {
    if [[ ! -f /etc/os-release ]]; then
        print_error "Cannot determine OS version"
        exit 1
    fi
    
    source /etc/os-release
    
    if [[ "$ID" != "amzn" ]]; then
        print_error "This script is designed for Amazon Linux only"
        print_info "Detected OS: $ID $VERSION_ID"
        print_info "For Ubuntu, use setup-wireguard.sh instead"
        exit 1
    fi
    
    case "$VERSION_ID" in
        2023*)
            print_success "Amazon Linux $VERSION_ID detected"
            ;;
        2)
            print_warning "Amazon Linux 2 detected - this script is optimized for Amazon Linux 2023"
            read -p "Continue anyway? (y/N) " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
            ;;
        *)
            print_warning "Amazon Linux $VERSION_ID is not officially tested"
            read -p "Continue anyway? (y/N) " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
            ;;
    esac
}

get_public_ip() {
    # Try multiple services to get public IP
    PUBLIC_IP=$(curl -s https://api.ipify.org || curl -s https://ifconfig.me || curl -s https://icanhazip.com)
    
    if [[ -z "$PUBLIC_IP" ]]; then
        print_error "Could not determine public IP address"
        read -p "Please enter your server's public IP or domain: " PUBLIC_IP
    fi
    
    print_info "Server public address: $PUBLIC_IP"
}

get_network_interface() {
    # Get the default network interface
    NET_INTERFACE=$(ip route | grep default | awk '{print $5}' | head -n1)
    
    if [[ -z "$NET_INTERFACE" ]]; then
        print_error "Could not determine network interface"
        read -p "Please enter your network interface (e.g., eth0, ens5): " NET_INTERFACE
    fi
    
    print_info "Network interface: $NET_INTERFACE"
}

################################################################################
# Installation Functions
################################################################################

install_dependencies() {
    print_header "Installing Dependencies"
    
    # Update system
    dnf update -y
    
    # Install EPEL repository for additional packages
    dnf install -y epel-release
    
    # Install WireGuard and dependencies
    dnf install -y \
        wireguard-tools \
        qrencode \
        iptables \
        firewalld \
        curl \
        kernel-devel \
        kernel-headers
    
    print_success "Dependencies installed"
}

generate_keys() {
    print_header "Generating Cryptographic Keys"
    
    mkdir -p "$WG_CONFIG_DIR"
    cd "$WG_CONFIG_DIR"
    
    # Generate server keys
    print_info "Generating server private key..."
    wg genkey | tee server_private.key | wg pubkey > server_public.key
    chmod 600 server_private.key
    
    # Generate client keys
    print_info "Generating client private key..."
    wg genkey | tee client_private.key | wg pubkey > client_public.key
    chmod 600 client_private.key
    
    # Ask about preshared key
    read -p "Generate preshared key for post-quantum security? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Generating preshared key..."
        wg genpsk > preshared.key
        chmod 600 preshared.key
        USE_PSK=true
    else
        USE_PSK=false
    fi
    
    SERVER_PRIVATE_KEY=$(cat server_private.key)
    SERVER_PUBLIC_KEY=$(cat server_public.key)
    CLIENT_PRIVATE_KEY=$(cat client_private.key)
    CLIENT_PUBLIC_KEY=$(cat client_public.key)
    
    if [[ "$USE_PSK" == "true" ]]; then
        PRESHARED_KEY=$(cat preshared.key)
    fi
    
    print_success "Keys generated successfully"
}

configure_server() {
    print_header "Configuring WireGuard Server"
    
    # Create server configuration
    cat > "$WG_CONFIG_FILE" <<EOF
[Interface]
Address = ${SERVER_IP}/24
ListenPort = ${WG_PORT}
PrivateKey = ${SERVER_PRIVATE_KEY}

# IP forwarding
PostUp = sysctl -w net.ipv4.ip_forward=1
PostUp = sysctl -w net.ipv6.conf.all.forwarding=1

# NAT rules
PostUp = iptables -A FORWARD -i ${WG_INTERFACE} -j ACCEPT
PostUp = iptables -A FORWARD -o ${WG_INTERFACE} -j ACCEPT
PostUp = iptables -t nat -A POSTROUTING -o ${NET_INTERFACE} -j MASQUERADE
PostUp = ip6tables -A FORWARD -i ${WG_INTERFACE} -j ACCEPT
PostUp = ip6tables -A FORWARD -o ${WG_INTERFACE} -j ACCEPT
PostUp = ip6tables -t nat -A POSTROUTING -o ${NET_INTERFACE} -j MASQUERADE

# Cleanup rules on shutdown
PostDown = iptables -D FORWARD -i ${WG_INTERFACE} -j ACCEPT
PostDown = iptables -D FORWARD -o ${WG_INTERFACE} -j ACCEPT
PostDown = iptables -t nat -D POSTROUTING -o ${NET_INTERFACE} -j MASQUERADE
PostDown = ip6tables -D FORWARD -i ${WG_INTERFACE} -j ACCEPT
PostDown = ip6tables -D FORWARD -o ${WG_INTERFACE} -j ACCEPT
PostDown = ip6tables -t nat -D POSTROUTING -o ${NET_INTERFACE} -j MASQUERADE

[Peer]
# Client
PublicKey = ${CLIENT_PUBLIC_KEY}
AllowedIPs = ${CLIENT_IP}/32
EOF

    # Add preshared key if generated
    if [[ "$USE_PSK" == "true" ]]; then
        echo "PresharedKey = ${PRESHARED_KEY}" >> "$WG_CONFIG_FILE"
    fi
    
    chmod 600 "$WG_CONFIG_FILE"
    
    print_success "Server configuration created"
}

configure_firewall() {
    print_header "Configuring Firewall (firewalld)"
    
    # Start and enable firewalld
    systemctl start firewalld
    systemctl enable firewalld
    
    # Allow SSH (important!)
    firewall-cmd --permanent --add-service=ssh
    
    # Allow WireGuard port
    firewall-cmd --permanent --add-port=${WG_PORT}/udp
    
    # Enable masquerading for VPN traffic
    firewall-cmd --permanent --add-masquerade
    
    # Reload firewall
    firewall-cmd --reload
    
    print_success "Firewall configured"
}

enable_ip_forwarding() {
    print_header "Enabling IP Forwarding"
    
    # Enable IP forwarding permanently
    if ! grep -q "net.ipv4.ip_forward=1" /etc/sysctl.conf; then
        echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
    fi
    
    if ! grep -q "net.ipv6.conf.all.forwarding=1" /etc/sysctl.conf; then
        echo "net.ipv6.conf.all.forwarding=1" >> /etc/sysctl.conf
    fi
    
    # Apply immediately
    sysctl -w net.ipv4.ip_forward=1
    sysctl -w net.ipv6.conf.all.forwarding=1
    
    print_success "IP forwarding enabled"
}

start_wireguard() {
    print_header "Starting WireGuard Service"
    
    # Enable and start WireGuard
    systemctl enable wg-quick@${WG_INTERFACE}
    systemctl start wg-quick@${WG_INTERFACE}
    
    # Check status
    if systemctl is-active --quiet wg-quick@${WG_INTERFACE}; then
        print_success "WireGuard service started successfully"
    else
        print_error "Failed to start WireGuard service"
        systemctl status wg-quick@${WG_INTERFACE}
        exit 1
    fi
}

generate_client_config() {
    print_header "Generating Client Configuration"
    
    mkdir -p "$CLIENT_CONFIG_DIR"
    
    # Create client configuration file
    CLIENT_CONFIG="${CLIENT_CONFIG_DIR}/client.conf"
    
    cat > "$CLIENT_CONFIG" <<EOF
[Interface]
PrivateKey = ${CLIENT_PRIVATE_KEY}
Address = ${CLIENT_IP}/24
DNS = ${DNS_SERVERS}

[Peer]
PublicKey = ${SERVER_PUBLIC_KEY}
Endpoint = ${PUBLIC_IP}:${WG_PORT}
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = ${PERSISTENT_KEEPALIVE}
EOF

    # Add preshared key if generated
    if [[ "$USE_PSK" == "true" ]]; then
        echo "PresharedKey = ${PRESHARED_KEY}" >> "$CLIENT_CONFIG"
    fi
    
    chmod 600 "$CLIENT_CONFIG"
    
    print_success "Client configuration created: $CLIENT_CONFIG"
}

generate_qr_code() {
    print_header "Generating QR Code"
    
    QR_CODE_FILE="${CLIENT_CONFIG_DIR}/client-qr.png"
    
    # Generate QR code
    qrencode -t png -o "$QR_CODE_FILE" < "$CLIENT_CONFIG"
    
    print_success "QR code saved: $QR_CODE_FILE"
    
    # Display QR code in terminal
    echo
    print_info "Scan this QR code with the SelfProxy app:"
    echo
    qrencode -t ansiutf8 < "$CLIENT_CONFIG"
    echo
}

print_summary() {
    print_header "Installation Complete!"
    
    echo
    print_success "WireGuard VPN server is now running on Amazon Linux"
    echo
    print_info "Server Details:"
    echo "  - Interface: $WG_INTERFACE"
    echo "  - Port: $WG_PORT (UDP)"
    echo "  - Server IP: $SERVER_IP"
    echo "  - Client IP: $CLIENT_IP"
    echo "  - Public Endpoint: ${PUBLIC_IP}:${WG_PORT}"
    if [[ "$USE_PSK" == "true" ]]; then
        echo "  - Preshared Key: Enabled (post-quantum security)"
    fi
    echo
    print_info "Client Configuration:"
    echo "  - Config file: $CLIENT_CONFIG"
    echo "  - QR code: $QR_CODE_FILE"
    echo
    print_info "Import into SelfProxy app:"
    echo "  1. Open SelfProxy app"
    echo "  2. Tap 'Add Profile'"
    echo "  3. Select 'WireGuard'"
    echo "  4. Scan QR code or import config file"
    echo
    print_info "Server Management:"
    echo "  - Check status: sudo wg show"
    echo "  - View logs: sudo journalctl -u wg-quick@${WG_INTERFACE}"
    echo "  - Restart: sudo systemctl restart wg-quick@${WG_INTERFACE}"
    echo "  - Stop: sudo systemctl stop wg-quick@${WG_INTERFACE}"
    echo "  - Firewall status: sudo firewall-cmd --list-all"
    echo
    print_warning "Important: Keep your private keys secure!"
    echo "  - Server private key: ${WG_CONFIG_DIR}/server_private.key"
    echo "  - Client private key: ${WG_CONFIG_DIR}/client_private.key"
    if [[ "$USE_PSK" == "true" ]]; then
        echo "  - Preshared key: ${WG_CONFIG_DIR}/preshared.key"
    fi
    echo
    print_info "Amazon Linux Specific Notes:"
    echo "  - Firewall: firewalld (not ufw)"
    echo "  - Package manager: dnf (not apt)"
    echo "  - SELinux: May need configuration for advanced setups"
    echo
}

################################################################################
# Main Installation Flow
################################################################################

main() {
    clear
    
    print_header "WireGuard VPN Server Setup for Amazon Linux"
    echo
    print_info "This script will install and configure WireGuard VPN server"
    print_info "on Amazon Linux 2023 (RedHat-based distribution)"
    echo
    
    # Pre-flight checks
    check_root
    check_amazon_linux
    get_public_ip
    get_network_interface
    
    echo
    read -p "Continue with installation? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Installation cancelled"
        exit 0
    fi
    
    # Installation steps
    install_dependencies
    generate_keys
    configure_server
    configure_firewall
    enable_ip_forwarding
    start_wireguard
    generate_client_config
    generate_qr_code
    print_summary
    
    print_success "Setup completed successfully!"
}

# Run main function
main "$@"