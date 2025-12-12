#!/bin/bash
# Generate and display VLESS QR code on server
# Usage: ./show-vless-qr.sh

set -e

echo "=========================================="
echo "VLESS Configuration QR Code Generator"
echo "=========================================="
echo ""

# Check if qrencode is installed
if ! command -v qrencode &> /dev/null; then
    echo "Installing qrencode..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get update -qq
        sudo apt-get install -y qrencode
    elif command -v yum &> /dev/null; then
        sudo yum install -y qrencode
    else
        echo "Error: Could not install qrencode. Please install it manually."
        exit 1
    fi
fi

# Check if config file exists
if [ ! -f /usr/local/etc/xray/config.json ]; then
    echo "Error: Xray config not found at /usr/local/etc/xray/config.json"
    echo "Please run this script on your VLESS server"
    exit 1
fi

echo "Reading Xray configuration..."
echo ""

# Extract configuration values
UUID=$(sudo cat /usr/local/etc/xray/config.json | grep -oP '"id"\s*:\s*"\K[^"]+' | head -1)
PRIVATE_KEY=$(sudo cat /usr/local/etc/xray/config.json | grep -oP '"privateKey"\s*:\s*"\K[^"]+' | head -1)
SHORT_ID=$(sudo cat /usr/local/etc/xray/config.json | grep -oP '"shortIds"\s*:\s*\[\s*"\K[^"]+' | head -1)
SNI=$(sudo cat /usr/local/etc/xray/config.json | grep -oP '"serverNames"\s*:\s*\[\s*"\K[^"]+' | head -1)
PORT=$(sudo cat /usr/local/etc/xray/config.json | grep -oP '"port"\s*:\s*\K[0-9]+' | head -1)
FLOW=$(sudo cat /usr/local/etc/xray/config.json | grep -oP '"flow"\s*:\s*"\K[^"]+' | head -1)

# Get server IP
SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || curl -s icanhazip.com 2>/dev/null || hostname -I | awk '{print $1}')

# Derive public key from private key if Reality is used
PUBLIC_KEY=""
if [ -n "$PRIVATE_KEY" ]; then
    # Check if xray command exists
    if command -v xray &> /dev/null; then
        # Derive public key from private key using xray x25519
        PUBLIC_KEY=$(xray x25519 -i "$PRIVATE_KEY" 2>/dev/null | grep -oP 'Password:\s*\K[^\s]+')
    fi
fi

# Validate extracted values
if [ -z "$UUID" ]; then
    echo "Error: Could not extract UUID from config"
    echo "Please check your Xray configuration manually:"
    echo "sudo cat /usr/local/etc/xray/config.json"
    exit 1
fi

# Check if Reality is configured
if [ -n "$PRIVATE_KEY" ] && [ -z "$PUBLIC_KEY" ]; then
    echo "Error: Reality private key found but could not derive public key"
    echo "Make sure 'xray' command is available"
    echo "Private Key: $PRIVATE_KEY"
    exit 1
fi

if [ -n "$PRIVATE_KEY" ] && [ -z "$SHORT_ID" ]; then
    echo "Error: Reality is configured but Short ID not found"
    echo "Please check your Xray configuration manually:"
    echo "sudo cat /usr/local/etc/xray/config.json"
    exit 1
fi

# Set defaults if not found
SNI=${SNI:-"www.microsoft.com"}
PORT=${PORT:-443}

echo "=========================================="
echo "Configuration Values:"
echo "=========================================="
echo "Server IP:   $SERVER_IP"
echo "Port:        $PORT"
echo "UUID:        $UUID"

# Build VLESS URI based on configuration
if [ -n "$PUBLIC_KEY" ] && [ -n "$SHORT_ID" ]; then
    # Reality configuration
    echo "Security:    Reality"
    echo "Public Key:  $PUBLIC_KEY"
    echo "Short ID:    $SHORT_ID"
    echo "SNI:         $SNI"
    if [ -n "$FLOW" ]; then
        echo "Flow:        $FLOW"
    fi
    echo ""
    
    # Build Reality URI
    VLESS_URI="vless://${UUID}@${SERVER_IP}:${PORT}?type=tcp&security=reality&sni=${SNI}&pbk=${PUBLIC_KEY}&sid=${SHORT_ID}&fp=chrome"
    if [ -n "$FLOW" ]; then
        VLESS_URI="${VLESS_URI}&flow=${FLOW}"
    fi
    VLESS_URI="${VLESS_URI}#Reality-Server"
else
    # Non-Reality configuration (TLS or none)
    if [ -n "$SNI" ]; then
        echo "Security:    TLS"
        echo "SNI:         $SNI"
        VLESS_URI="vless://${UUID}@${SERVER_IP}:${PORT}?type=tcp&security=tls&sni=${SNI}#VLESS-Server"
    else
        echo "Security:    None"
        VLESS_URI="vless://${UUID}@${SERVER_IP}:${PORT}?type=tcp&security=none#VLESS-Server"
    fi
    echo ""
fi

echo "=========================================="
echo "Complete VLESS URI:"
echo "=========================================="
echo "$VLESS_URI"
echo ""

# Save to file
echo "$VLESS_URI" > ~/vless-uri.txt
echo "URI saved to: ~/vless-uri.txt"
echo ""

# Generate QR code
echo "=========================================="
echo "QR Code (scan with SelfProxy app):"
echo "=========================================="
echo ""

# Generate QR code in terminal
qrencode -t ANSIUTF8 "$VLESS_URI"

echo ""
echo "=========================================="
echo "Instructions:"
echo "=========================================="
echo "1. Open SelfProxy app on your phone"
echo "2. Go to Profiles screen"
echo "3. Tap the QR scanner icon (📷)"
echo "4. Point camera at the QR code above"
echo "5. Profile will be imported automatically"
echo "6. Tap Connect!"
echo ""
echo "Tip: If QR code is too small, maximize your terminal"
echo "     or use: qrencode -t ANSIUTF8 -s 10 < ~/vless-uri.txt"
echo ""
echo "=========================================="
