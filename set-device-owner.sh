#!/bin/bash

# Script to set Device Owner for UNO Shield MDM app
# Usage: ./set-device-owner.sh

PACKAGE_NAME="com.unoshield.mdm"
COMPONENT_NAME="com.unoshield.mdm/.DeviceOwnerReceiver"

echo "=========================================="
echo "UNO Shield MDM - Device Owner Setup"
echo "=========================================="
echo ""

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: ADB not found. Please install Android SDK Platform Tools."
    echo "   Download from: https://developer.android.com/studio/releases/platform-tools"
    exit 1
fi

echo "✓ ADB found"
echo ""

# Check device connection
echo "Checking device connection..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ Error: No device connected"
    echo ""
    echo "Please:"
    echo "1. Connect your device via USB"
    echo "2. Enable USB Debugging in Developer Options"
    echo "3. Accept the USB debugging prompt on your device"
    exit 1
fi

echo "✓ Device connected"
echo ""

# Check if app is installed
echo "Checking if app is installed..."
APP_INSTALLED=$(adb shell pm list packages | grep "$PACKAGE_NAME")

if [ -z "$APP_INSTALLED" ]; then
    echo "⚠ Warning: App not found. Make sure the app is installed."
    echo ""
    read -p "Do you want to continue anyway? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✓ App installed: $PACKAGE_NAME"
fi

echo ""

# Check current Device Owner status
echo "Checking current Device Owner status..."
CURRENT_OWNER=$(adb shell dpm list-owners 2>/dev/null | grep "Device owner set to package")

if [ ! -z "$CURRENT_OWNER" ]; then
    echo "⚠ Warning: Device Owner is already set!"
    echo "$CURRENT_OWNER"
    echo ""
    read -p "Do you want to continue? This will fail if another app is Device Owner. (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""

# Check for user accounts
echo "Checking for user accounts..."
USERS=$(adb shell pm list users 2>/dev/null | grep "UserInfo" | wc -l)
PRIMARY_USER=$(adb shell pm list users 2>/dev/null | grep "UserInfo.*running" | head -1)

if [ -z "$PRIMARY_USER" ]; then
    echo "⚠ Warning: Could not detect user accounts. Continuing..."
else
    echo "Found users on device:"
    adb shell pm list users 2>/dev/null | grep "UserInfo" || echo "  (Could not list users)"
    echo ""
    
    # Check if there are multiple users or if primary user has accounts
    if [ "$USERS" -gt 1 ]; then
        echo "⚠ WARNING: Multiple users detected!"
        echo "   Device Owner can only be set when there is ONE user with NO accounts."
        echo "   Please remove secondary users from Settings → System → Multiple users"
        echo ""
    fi
fi

echo ""

# Important warnings
echo "=========================================="
echo "⚠ IMPORTANT REQUIREMENTS ⚠"
echo "=========================================="
echo ""
echo "Device Owner can ONLY be set when:"
echo "1. ❌ NO user accounts exist on the device (Google, email, etc.)"
echo "2. ❌ NO lock screen password/PIN is set"
echo "3. ✅ Device is connected via USB"
echo "4. ✅ USB Debugging is enabled"
echo ""
echo "To remove accounts:"
echo "- Go to Settings → Accounts → Remove ALL accounts"
echo "- Go to Settings → Security → Remove lock screen password/PIN"
echo "- Go to Settings → System → Multiple users → Remove secondary users"
echo ""
echo "OR factory reset the device and scan QR code during setup."
echo ""
read -p "Have you removed all user accounts and lock screen? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "❌ Cannot proceed. Please remove accounts and try again."
    echo ""
    echo "Steps to remove accounts:"
    echo "1. Settings → Accounts → Remove all accounts"
    echo "2. Settings → Security → Remove lock screen"
    echo "3. Settings → System → Multiple users → Remove secondary users"
    echo ""
    echo "Then run this script again."
    echo ""
    echo "Alternative: Factory reset the device and scan QR code during setup."
    exit 1
fi

echo ""
echo "=========================================="
echo "Setting Device Owner..."
echo "=========================================="
echo ""
echo "Command: adb shell dpm set-device-owner $COMPONENT_NAME"
echo ""

# Set Device Owner
RESULT=$(adb shell dpm set-device-owner "$COMPONENT_NAME" 2>&1)

if echo "$RESULT" | grep -q "Success"; then
    echo "✅ SUCCESS! Device Owner set successfully!"
    echo ""
    echo "Verifying..."
    VERIFY=$(adb shell dpm list-owners 2>/dev/null)
    echo "$VERIFY"
    echo ""
    echo "=========================================="
    echo "Device Owner Setup Complete!"
    echo "=========================================="
    echo ""
    echo "You can now test MDM policies in the app."
    echo "The app should show 'Device Owner: Active' in Settings."
else
    echo "❌ FAILED to set Device Owner"
    echo ""
    echo "Error output:"
    echo "$RESULT"
    echo ""
    echo "Common issues:"
    echo "1. User accounts still exist - Remove all accounts"
    echo "2. Lock screen is set - Remove password/PIN"
    echo "3. Another app is Device Owner - Factory reset needed"
    echo "4. App not installed - Install the app first"
    echo ""
    echo "Try running: adb shell dpm list-owners"
    exit 1
fi

