#!/bin/bash

################################################################################
# VLESS VPN Server Setup Script for Ubuntu
# 
# This script automates the installation and configuration of a VLESS proxy
# server using Xray-core on Ubuntu 20.04, 22.04, or 24.04 LTS.
#
# Features:
# - Automatic Xray-core installation
# - UUID generation for authentication
# - Multiple transport protocols (TCP, WebSocket, gRPC, HTTP/2)
# - TLS configuration with Let's Encrypt
# - Optional Reality protocol for advanced obfuscation
# - Firewall configuration (UFW)
# - Client configuration generation (URI and JSON)
#
# Usage: sudo ./setup-vless.sh
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

check_ubuntu() {
    if [[ ! -f /etc/os-release ]]; then
        print_error "Cannot determine OS version"
        exit 1
    fi
    
    source /etc/os-release
    
    if [[ "$ID" != "ubuntu" ]]; then
        print_error "This script is designed for Ubuntu only"
        exit 1
    fi
    
    case "$VERSION_ID" in
        20.04|22.04|24.04)
            print_success "Ubuntu $VERSION_ID detected"
            ;;
        *)
            print_warning "Ubuntu $VERSION_ID is not officially tested"
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
    
    apt-get update
    apt-get install -y \
        curl \
        wget \
        unzip \
        ufw \
        certbot \
        nginx \
        jq
    
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
    ufw allow 80/tcp comment 'HTTP (Lets Encrypt)'
    
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
            "level": 0
          }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "${TRANSPORT}",
EOF

    # Add transport-specific settings
    case $TRANSPORT in
        ws)
            cat >> "$XRAY_CONFIG_FILE" <<EOF
        "wsSettings": {
          "path": "${WS_PATH}"
        },
EOF
            ;;
        grpc)
            cat >> "$XRAY_CONFIG_FILE" <<EOF
        "grpcSettings": {
          "serviceName": "${GRPC_SERVICE}"
        },
EOF
            ;;
        h2)
            cat >> "$XRAY_CONFIG_FILE" <<EOF
        "httpSettings": {
          "path": "${H2_PATH}"
        },
EOF
            ;;
    esac

    # Add TLS settings
    cat >> "$XRAY_CONFIG_FILE" <<EOF
        "security": "tls",
        "tlsSettings": {
          "alpn": ["h2", "http/1.1"],
          "certificates": [
            {
              "certificateFile": "${CERT_PATH}",
              "keyFile": "${KEY_PATH}"
            }
          ]
        }
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
            "flow": "xtls-rprx-vision",
            "level": 0
          }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "tcp",
        "security": "reality",
        "realitySettings": {
          "show": false,
          "dest": "${REALITY_SNI}:443",
          "xver": 0,
          "serverNames": ["${REALITY_SNI}"],
          "privateKey": "${REALITY_PRIVATE_KEY}",
          "shortIds": ["${REALITY_SHORT_ID}"]
        }
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

configure_firewall() {
    print_header "Configuring Firewall"
    
    # Enable UFW if not already enabled
    if ! ufw status | grep -q "Status: active"; then
        print_info "Enabling UFW..."
        ufw --force enable
    fi
    
    # Allow SSH (important!)
    ufw allow 22/tcp comment 'SSH'
    
    # Allow VLESS port
    ufw allow ${XRAY_PORT}/tcp comment 'VLESS'
    
    # Reload firewall
    ufw reload
    
    print_success "Firewall configured"
}

start_xray() {
    print_header "Starting Xray Service"
    
    # Enable and start Xray
    systemctl enable xray
    systemctl restart xray
    
    # Wait a moment for service to start
    sleep 2
    
    # Check status
    if systemctl is-active --quiet xray; then
        print_success "Xray service started successfully"
    else
        print_error "Failed to start Xray service"
        print_info "Checking logs..."
        journalctl -u xray -n 20 --no-pager
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
    
    print_success "Client configuration generated"
}

generate_tls_uri() {
    # Build VLESS URI for TLS
    VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?"
    
    case $TRANSPORT in
        tcp)
            VLESS_URI="${VLESS_URI}type=tcp&security=tls"
            ;;
        ws)
            VLESS_URI="${VLESS_URI}type=ws&path=${WS_PATH}&security=tls"
            ;;
        grpc)
            VLESS_URI="${VLESS_URI}type=grpc&serviceName=${GRPC_SERVICE}&security=tls"
            ;;
        h2)
            VLESS_URI="${VLESS_URI}type=http&path=${H2_PATH}&security=tls"
            ;;
    esac
    
    VLESS_URI="${VLESS_URI}&sni=${DOMAIN}#SelfProxy-VLESS"
    
    echo "$VLESS_URI" > "${CLIENT_CONFIG_DIR}/vless-uri.txt"
    print_info "VLESS URI saved: ${CLIENT_CONFIG_DIR}/vless-uri.txt"
}

generate_reality_uri() {
    # Build VLESS URI for Reality
    VLESS_URI="vless://${UUID}@${DOMAIN}:${XRAY_PORT}?"
    VLESS_URI="${VLESS_URI}type=tcp&security=reality&flow=xtls-rprx-vision"
    VLESS_URI="${VLESS_URI}&sni=${REALITY_SNI}&fp=chrome"
    VLESS_URI="${VLESS_URI}&pbk=${REALITY_PUBLIC_KEY}&sid=${REALITY_SHORT_ID}"
    VLESS_URI="${VLESS_URI}#SelfProxy-VLESS-Reality"
    
    echo "$VLESS_URI" > "${CLIENT_CONFIG_DIR}/vless-uri.txt"
    print_info "VLESS URI saved: ${CLIENT_CONFIG_DIR}/vless-uri.txt"
}

generate_json_config() {
    # Create JSON configuration file
    CLIENT_JSON="${CLIENT_CONFIG_DIR}/client-config.json"
    
    if [[ "$USE_REALITY" == "true" ]]; then
        cat > "$CLIENT_JSON" <<EOF
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
            "encryption": "none",
            "flow": "xtls-rprx-vision"
          }
        ]
      }
    ]
  },
  "streamSettings": {
    "network": "tcp",
    "security": "reality",
    "realitySettings": {
      "serverName": "${REALITY_SNI}",
      "fingerprint": "chrome",
      "publicKey": "${REALITY_PUBLIC_KEY}",
      "shortId": "${REALITY_SHORT_ID}"
    }
  }
}
EOF
    else
        cat > "$CLIENT_JSON" <<EOF
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
            "encryption": "none"
          }
        ]
      }
    ]
  },
  "streamSettings": {
    "network": "${TRANSPORT}",
EOF

        case $TRANSPORT in
            ws)
                cat >> "$CLIENT_JSON" <<EOF
    "wsSettings": {
      "path": "${WS_PATH}"
    },
EOF
                ;;
            grpc)
                cat >> "$CLIENT_JSON" <<EOF
    "grpcSettings": {
      "serviceName": "${GRPC_SERVICE}"
    },
EOF
                ;;
            h2)
                cat >> "$CLIENT_JSON" <<EOF
    "httpSettings": {
      "path": "${H2_PATH}"
    },
EOF
                ;;
        esac

        cat >> "$CLIENT_JSON" <<EOF
    "security": "tls",
    "tlsSettings": {
      "serverName": "${DOMAIN}",
      "alpn": ["h2", "http/1.1"]
    }
  }
}
EOF
    fi
    
    print_info "JSON config saved: $CLIENT_JSON"
}

print_summary() {
    print_header "Installation Complete!"
    
    echo
    print_success "VLESS proxy server is now running"
    echo
    print_info "Server Details:"
    echo "  - Protocol: VLESS"
    echo "  - Port: $XRAY_PORT (TCP)"
    echo "  - Domain: $DOMAIN"
    echo "  - Transport: $TRANSPORT"
    if [[ "$USE_REALITY" == "true" ]]; then
        echo "  - Security: Reality"
        echo "  - SNI: $REALITY_SNI"
        echo "  - Public Key: $REALITY_PUBLIC_KEY"
        echo "  - Short ID: $REALITY_SHORT_ID"
    else
        echo "  - Security: TLS 1.3"
        echo "  - Certificate: Let's Encrypt"
    fi
    echo
    print_info "Client Configuration:"
    echo "  - VLESS URI: ${CLIENT_CONFIG_DIR}/vless-uri.txt"
    echo "  - JSON config: ${CLIENT_CONFIG_DIR}/client-config.json"
    echo
    print_info "VLESS URI:"
    cat "${CLIENT_CONFIG_DIR}/vless-uri.txt"
    echo
    echo
    print_info "Import into SelfProxy app:"
    echo "  1. Open SelfProxy app"
    echo "  2. Tap 'Add Profile'"
    echo "  3. Select 'VLESS'"
    echo "  4. Paste VLESS URI or import JSON config"
    echo
    print_info "Server Management:"
    echo "  - Check status: sudo systemctl status xray"
    echo "  - View logs: sudo journalctl -u xray -f"
    echo "  - Restart: sudo systemctl restart xray"
    echo "  - Stop: sudo systemctl stop xray"
    echo
    print_warning "Important: Keep your UUID secure!"
    echo "  - UUID: $UUID"
    echo
}

################################################################################
# Main Installation Flow
################################################################################

main() {
    clear
    
    print_header "VLESS Proxy Server Setup"
    echo
    print_info "This script will install and configure VLESS proxy server"
    print_info "using Xray-core on Ubuntu 20.04, 22.04, or 24.04 LTS"
    echo
    
    # Pre-flight checks
    check_root
    check_ubuntu
    get_public_ip
    get_domain
    select_transport
    ask_reality
    
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
