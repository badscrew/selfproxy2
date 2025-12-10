#!/bin/bash

################################################################################
# VLESS VPN Server Setup Script for Amazon Linux 2023
# 
# This script automates the installation and configuration of a VLESS proxy
# server using Xray-core on Amazon Linux 2023 (RedHat-based).
#
# Features:
# - Automatic Xray-core installation
# - UUID generation for authentication
# - Multiple transport protocols (TCP, WebSocket, gRPC, HTTP/2)
# - TLS configuration with Let's Encrypt
# - Optional Reality protocol for advanced obfuscation
# - Firewall configuration (firewalld)
# - Client configuration generation (URI and JSON)
#
# Usage: sudo ./setup-vless-amazon.sh
#
# Requirements: 10.1-10.19
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration variables
XRAY_PORT=${XRAY_PORT:-443}
XRAY_CONFIG_DIR="/usr/local/etc/xray"
XRAY_CONFIG_FILE="${XRAY_CONFIG_DIR}/config.json"
CLIENT_CONFIG_DIR="$HOME/vless-client"
XRAY_LOG_DIR="/var/log/xray"

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
        print_info "For Ubuntu, use setup-vless.sh instead"
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

get_domain() {
    print_info "VLESS with TLS requires a domain name pointing to this server"
    read -p "Enter your domain name (e.g., vpn.example.com): " DOMAIN
    
    if [[ -z "$DOMAIN" ]]; then
        print_error "Domain name is required"
        exit 1
    fi
    
    print_info "Domain: $DOMAIN"
}

get_public_ip() {
    PUBLIC_IP=$(curl -s https://api.ipify.org || curl -s https://ifconfig.me || curl -s https://icanhazip.com)
    
    if [[ -z "$PUBLIC_IP" ]]; then
        print_warning "Could not determine public IP address"
    else
        print_info "Server public IP: $PUBLIC_IP"
    fi
}

select_transport() {
    print_header "Select Transport Protocol"
    echo
    echo "1) TCP (Simple, good performance)"
    echo "2) WebSocket (HTTP-compatible, good for restrictive networks)"
    echo "3) gRPC (Efficient multiplexing, modern)"
    echo "4) HTTP/2 (Good balance, widely supported)"
    echo
    read -p "Select transport protocol [1-4]: " transport_choice
    
    case $transport_choice in
        1)
            TRANSPORT="tcp"
            print_info "Selected: TCP"
            ;;
        2)
            TRANSPORT="ws"
            read -p "Enter WebSocket path (default: /ws): " WS_PATH
            WS_PATH=${WS_PATH:-/ws}
            print_info "Selected: WebSocket (path: $WS_PATH)"
            ;;
        3)
            TRANSPORT="grpc"
            read -p "Enter gRPC service name (default: vless-grpc): " GRPC_SERVICE
            GRPC_SERVICE=${GRPC_SERVICE:-vless-grpc}
            print_info "Selected: gRPC (service: $GRPC_SERVICE)"
            ;;
        4)
            TRANSPORT="h2"
            read -p "Enter HTTP/2 path (default: /h2): " H2_PATH
            H2_PATH=${H2_PATH:-/h2}
            print_info "Selected: HTTP/2 (path: $H2_PATH)"
            ;;
        *)
            print_error "Invalid selection"
            exit 1
            ;;
    esac
}

ask_reality() {
    echo
    read -p "Enable Reality protocol for advanced obfuscation? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        USE_REALITY=true
        read -p "Enter SNI (e.g., www.microsoft.com): " REALITY_SNI
        REALITY_SNI=${REALITY_SNI:-www.microsoft.com}
        print_info "Reality enabled with SNI: $REALITY_SNI"
    else
        USE_REALITY=false
        print_info "Using standard TLS"
    fi
}

################################################################################
# Installation Functions
################################################################################

install_dependencies() {
    print_header "Installing Dependencies"
    
    # Update system
    dnf update -y
    
    # Try to install EPEL repository (needed for certbot and some packages)
    print_info "Installing EPEL repository (if available)..."
    if ! dnf install -y epel-release 2>/dev/null; then
        print_warning "EPEL not available in default repos, trying alternative methods..."
        
        # Try Amazon Linux 2023 specific method
        if [[ "$VERSION_ID" == "2023"* ]]; then
            print_info "Trying direct EPEL installation for Amazon Linux 2023..."
            if ! dnf install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm 2>/dev/null; then
                print_warning "EPEL installation failed, some packages may not be available"
            fi
        else
            # For Amazon Linux 2
            print_info "Trying Amazon Linux 2 EPEL method..."
            if command -v amazon-linux-extras &> /dev/null; then
                amazon-linux-extras install epel -y || print_warning "EPEL installation failed"
            fi
        fi
    fi
    
    # Install dependencies
    print_info "Installing dependencies..."
    dnf install -y \
        curl \
        wget \
        unzip \
        firewalld \
        nginx \
        jq \
        openssl
    
    # Try to install certbot (might need EPEL)
    if ! dnf install -y certbot 2>/dev/null; then
        print_warning "certbot not available from repositories"
        print_info "Will try to install certbot via snap or pip as fallback"
        CERTBOT_FALLBACK=true
    fi
    
    print_success "Dependencies installed"
}

install_xray() {
    print_header "Installing Xray-core"
    
    # Download and run official installation script
    bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)" @ install
    
    # Create log directory
    mkdir -p "$XRAY_LOG_DIR"
    
    # Check installation
    if command -v xray &> /dev/null; then
        XRAY_VERSION=$(xray version | head -n1)
        print_success "Xray-core installed: $XRAY_VERSION"
    else
        print_error "Xray-core installation failed"
        exit 1
    fi
}

generate_uuid() {
    print_header "Generating UUID"
    
    # Generate UUID for authentication
    if command -v uuidgen &> /dev/null; then
        UUID=$(uuidgen)
    else
        # Fallback: use xray's UUID generator
        UUID=$(xray uuid)
    fi
    
    print_success "UUID generated: $UUID"
}

setup_tls() {
    print_header "Setting up TLS Certificate"
    
    # Stop nginx if running
    systemctl stop nginx 2>/dev/null || true
    
    # Configure firewall for certbot
    firewall-cmd --permanent --add-port=80/tcp
    firewall-cmd --reload
    
    # Obtain certificate
    print_info "Obtaining Let's Encrypt certificate..."
    certbot certonly --standalone \
        --non-interactive \
        --agree-tos \
        --register-unsafely-without-email \
        -d "$DOMAIN"
    
    if [[ $? -eq 0 ]]; then
        CERT_PATH="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
        KEY_PATH="/etc/letsencrypt/live/${DOMAIN}/privkey.pem"
        print_success "TLS certificate obtained"
    else
        print_error "Failed to obtain TLS certificate"
        print_info "Make sure:"
        print_info "  1. Domain $DOMAIN points to this server ($PUBLIC_IP)"
        print_info "  2. Port 80 is accessible from the internet"
        exit 1
    fi
    
    # Set up automatic renewal
    (crontab -l 2>/dev/null; echo "0 3 * * * certbot renew --quiet --post-hook 'systemctl restart xray'") | crontab -
    print_success "Automatic certificate renewal configured"
}

generate_reality_keys() {
    print_header "Generating Reality Keys"
    
    # Generate Reality key pair
    REALITY_KEYS=$(xray x25519)
    REALITY_PRIVATE_KEY=$(echo "$REALITY_KEYS" | grep "Private key:" | awk '{print $3}')
    REALITY_PUBLIC_KEY=$(echo "$REALITY_KEYS" | grep "Public key:" | awk '{print $3}')
    
    # Generate short ID
    REALITY_SHORT_ID=$(openssl rand -hex 8)
    
    print_success "Reality keys generated"
}

configure_xray() {
    print_header "Configuring Xray-core"
    
    mkdir -p "$XRAY_CONFIG_DIR"
    
    # Build configuration based on transport and TLS/Reality
    if [[ "$USE_REALITY" == "true" ]]; then
        generate_reality_keys
        create_reality_config
    else
        create_tls_config
    fi
    
    print_success "Xray configuration created"
}

create_tls_config() {
    # Create configuration with standard TLS
    cat > "$XRAY_CONFIG_FILE" <<EOF
{
  "log": {
    "loglevel": "warning",
    "access": "${XRAY_LOG_DIR}/access.log",
    "error": "${XRAY_LOG_DIR}/error.log"
  },
  "inbounds": [
    {
      "port": ${XRAY_PORT},
      "protocol": "vless",
      "settings": {
        "clients": [
          {
            "id": "${UUID}",
            "flow": "xtls-rprx-vision"
          }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "${TRANSPORT}",
        "security": "tls",
        "tlsSettings": {
          "certificates": [
            {
              "certificateFile": "${CERT_PATH}",
              "keyFile": "${KEY_PATH}"
            }
          ]
        }$(create_transport_config)
      }
    }
  ],
  "outbounds": [
    {
      "protocol": "freedom",
      "settings": {}
    }
  ]
}
EOF
}

create_reality_config() {
    # Create configuration with Reality protocol
    cat > "$XRAY_CONFIG_FILE" <<EOF
{
  "log": {
    "loglevel": "warning",
    "access": "${XRAY_LOG_DIR}/access.log",
    "error": "${XRAY_LOG_DIR}/error.log"
  },
  "inbounds": [
    {
      "port": ${XRAY_PORT},
      "protocol": "vless",
      "settings": {
        "clients": [
          {
            "id": "${UUID}",
            "flow": "xtls-rprx-vision"
          }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "${TRANSPORT}",
        "security": "reality",
        "realitySettings": {
          "dest": "${REALITY_SNI}:443",
          "serverNames": ["${REALITY_SNI}"],
          "privateKey": "${REALITY_PRIVATE_KEY}",
          "shortIds": ["${REALITY_SHORT_ID}"]
        }$(create_transport_config)
      }
    }
  ],
  "outbounds": [
    {
      "protocol": "freedom",
      "settings": {}
    }
  ]
}
EOF
}

create_transport_config() {
    case "$TRANSPORT" in
        "ws")
            echo ",
        \"wsSettings\": {
          \"path\": \"${WS_PATH}\"
        }"
            ;;
        "grpc")
            echo ",
        \"grpcSettings\": {
          \"serviceName\": \"${GRPC_SERVICE}\"
        }"
            ;;
        "h2")
            echo ",
        \"httpSettings\": {
          \"path\": \"${H2_PATH}\"
        }"
            ;;
        *)
            echo ""
            ;;
    esac
}

configure_firewall() {
    print_header "Configuring Firewall (firewalld)"
    
    # Start and enable firewalld
    systemctl start firewalld
    systemctl enable firewalld
    
    # Allow SSH (important!)
    firewall-cmd --permanent --add-service=ssh
    
    # Allow HTTPS port
    firewall-cmd --permanent --add-port=${XRAY_PORT}/tcp
    
    # Allow HTTP for Let's Encrypt (if using TLS)
    if [[ "$USE_REALITY" != "true" ]]; then
        firewall-cmd --permanent --add-port=80/tcp
    fi
    
    # Reload firewall
    firewall-cmd --reload
    
    print_success "Firewall configured"
}

start_xray() {
    print_header "Starting Xray Service"
    
    # Enable and start Xray
    systemctl enable xray
    systemctl start xray
    
    # Check status
    if systemctl is-active --quiet xray; then
        print_success "Xray service started successfully"
    else
        print_error "Failed to start Xray service"
        systemctl status xray
        exit 1
    fi
}

generate_client_config() {
    print_header "Generating Client Configuration"
    
    mkdir -p "$CLIENT_CONFIG_DIR"
    
    # Generate VLESS URI
    if [[ "$USE_REALITY" == "true" ]]; then
        generate_reality_uri
    else
        generate_tls_uri
    fi
    
    # Generate JSON configuration
    generate_json_config
    
    print_success "Client configurations generated"
}

generate_tls_uri() {
    case "$TRANSPORT" in
        "tcp")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=tcp&security=tls&flow=xtls-rprx-vision#VLESS-${DOMAIN}"
            ;;
        "ws")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=ws&security=tls&path=${WS_PATH}#VLESS-${DOMAIN}"
            ;;
        "grpc")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=grpc&security=tls&serviceName=${GRPC_SERVICE}#VLESS-${DOMAIN}"
            ;;
        "h2")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=http&security=tls&path=${H2_PATH}#VLESS-${DOMAIN}"
            ;;
    esac
}

generate_reality_uri() {
    case "$TRANSPORT" in
        "tcp")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=tcp&security=reality&pbk=${REALITY_PUBLIC_KEY}&fp=chrome&sni=${REALITY_SNI}&sid=${REALITY_SHORT_ID}&flow=xtls-rprx-vision#VLESS-Reality-${DOMAIN}"
            ;;
        "ws")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=ws&security=reality&pbk=${REALITY_PUBLIC_KEY}&fp=chrome&sni=${REALITY_SNI}&sid=${REALITY_SHORT_ID}&path=${WS_PATH}#VLESS-Reality-${DOMAIN}"
            ;;
        "grpc")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=grpc&security=reality&pbk=${REALITY_PUBLIC_KEY}&fp=chrome&sni=${REALITY_SNI}&sid=${REALITY_SHORT_ID}&serviceName=${GRPC_SERVICE}#VLESS-Reality-${DOMAIN}"
            ;;
        "h2")
            VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?type=http&security=reality&pbk=${REALITY_PUBLIC_KEY}&fp=chrome&sni=${REALITY_SNI}&sid=${REALITY_SHORT_ID}&path=${H2_PATH}#VLESS-Reality-${DOMAIN}"
            ;;
    esac
}

generate_json_config() {
    # Create JSON configuration for advanced clients
    cat > "${CLIENT_CONFIG_DIR}/client-config.json" <<EOF
{
  "outbounds": [
    {
      "protocol": "vless",
      "settings": {
        "vnext": [
          {
            "address": "${DOMAIN}",
            "port": ${XRAY_PORT},
            "users": [
              {
                "id": "${UUID}",
                "flow": "xtls-rprx-vision"
              }
            ]
          }
        ]
      },
      "streamSettings": {
        "network": "${TRANSPORT}",
        "security": "$(if [[ "$USE_REALITY" == "true" ]]; then echo "reality"; else echo "tls"; fi)"$(create_client_transport_config)
      }
    }
  ]
}
EOF
}

create_client_transport_config() {
    local config=""
    
    # Add security settings
    if [[ "$USE_REALITY" == "true" ]]; then
        config+=",
        \"realitySettings\": {
          \"serverName\": \"${REALITY_SNI}\",
          \"publicKey\": \"${REALITY_PUBLIC_KEY}\",
          \"shortId\": \"${REALITY_SHORT_ID}\",
          \"fingerprint\": \"chrome\"
        }"
    else
        config+=",
        \"tlsSettings\": {
          \"serverName\": \"${DOMAIN}\"
        }"
    fi
    
    # Add transport settings
    case "$TRANSPORT" in
        "ws")
            config+=",
        \"wsSettings\": {
          \"path\": \"${WS_PATH}\"
        }"
            ;;
        "grpc")
            config+=",
        \"grpcSettings\": {
          \"serviceName\": \"${GRPC_SERVICE}\"
        }"
            ;;
        "h2")
            config+=",
        \"httpSettings\": {
          \"path\": \"${H2_PATH}\"
        }"
            ;;
    esac
    
    echo "$config"
}

print_summary() {
    print_header "Installation Complete!"
    
    echo
    print_success "VLESS proxy server is now running on Amazon Linux"
    echo
    print_info "Server Details:"
    echo "  - Protocol: VLESS"
    echo "  - Port: $XRAY_PORT (TCP)"
    echo "  - Domain: $DOMAIN"
    echo "  - Transport: $TRANSPORT"
    if [[ "$USE_REALITY" == "true" ]]; then
        echo "  - Security: Reality Protocol"
        echo "  - SNI: $REALITY_SNI"
    else
        echo "  - Security: TLS"
    fi
    echo "  - UUID: $UUID"
    echo
    print_info "Client Configuration:"
    echo "  - VLESS URI: saved to ${CLIENT_CONFIG_DIR}/vless-uri.txt"
    echo "  - JSON config: ${CLIENT_CONFIG_DIR}/client-config.json"
    echo
    print_info "VLESS URI:"
    echo "$VLESS_URI"
    echo "$VLESS_URI" > "${CLIENT_CONFIG_DIR}/vless-uri.txt"
    echo
    print_info "Import into SelfProxy app:"
    echo "  1. Open SelfProxy app"
    echo "  2. Tap 'Add Profile'"
    echo "  3. Select 'VLESS'"
    echo "  4. Paste the VLESS URI or import JSON config"
    echo
    print_info "Server Management:"
    echo "  - Check status: sudo systemctl status xray"
    echo "  - View logs: sudo journalctl -u xray -f"
    echo "  - Restart: sudo systemctl restart xray"
    echo "  - Stop: sudo systemctl stop xray"
    echo "  - Firewall status: sudo firewall-cmd --list-all"
    echo
    print_warning "Important Security Notes:"
    echo "  - Keep your UUID secure: $UUID"
    if [[ "$USE_REALITY" == "true" ]]; then
        echo "  - Reality private key: ${REALITY_PRIVATE_KEY}"
        echo "  - Reality short ID: ${REALITY_SHORT_ID}"
    else
        echo "  - TLS certificate auto-renews via cron"
    fi
    echo
    print_info "Amazon Linux Specific Notes:"
    echo "  - Firewall: firewalld (not ufw)"
    echo "  - Package manager: dnf (not apt)"
    echo "  - SELinux: May need configuration for advanced setups"
    echo "  - Logs: /var/log/xray/"
    echo
}

################################################################################
# Main Installation Flow
################################################################################

main() {
    clear
    
    print_header "VLESS Proxy Server Setup for Amazon Linux"
    echo
    print_info "This script will install and configure VLESS proxy server"
    print_info "using Xray-core on Amazon Linux 2023 (RedHat-based distribution)"
    echo
    
    # Pre-flight checks
    check_root
    check_amazon_linux
    get_public_ip
    get_domain
    select_transport
    ask_reality
    
    echo
    print_info "Configuration Summary:"
    echo "  - Domain: $DOMAIN"
    echo "  - Transport: $TRANSPORT"
    echo "  - Security: $(if [[ "$USE_REALITY" == "true" ]]; then echo "Reality"; else echo "TLS"; fi)"
    echo
    read -p "Continue with installation? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Installation cancelled"
        exit 0
    fi
    
    # Installation steps
    install_dependencies
    install_xray
    generate_uuid
    
    if [[ "$USE_REALITY" != "true" ]]; then
        setup_tls
    fi
    
    configure_xray
    configure_firewall
    start_xray
    generate_client_config
    print_summary
    
    print_success "Setup completed successfully!"
}

# Run main function
main "$@"