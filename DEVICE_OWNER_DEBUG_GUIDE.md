# Device Owner Debug Guide

This guide explains how to set your app as Device Owner for debugging and testing purposes.

## Prerequisites

1. **ADB (Android Debug Bridge)** installed on your computer
2. **USB Debugging** enabled on your Android device
3. **No user accounts** on the device (or remove all accounts first)

## Method 1: Set Device Owner via ADB (Recommended for Testing)

### Step 1: Remove All User Accounts (Required)

Device Owner can only be set when there are no user accounts on the device.

**Option A: Factory Reset (Easiest)**

- Go to Settings → System → Reset options → Erase all data (factory reset)
- This will remove all accounts and data

**Option B: Remove Accounts Manually**

- Go to Settings → Accounts
- Remove all accounts (Google, email, etc.)
- You may need to remove the device lock screen password/PIN

### Step 2: Connect Device via ADB

```bash
# Connect your device via USB
adb devices

# Verify device is connected (should show your device)
```

### Step 3: Set Device Owner

```bash
# Set the app as Device Owner
adb shell dpm set-device-owner com.unoshield.mdm/.DeviceOwnerReceiver
```

**Expected Output:**

```
Success: Device owner set to package com.unoshield.mdm
```

**If you get an error:**

- `Error: Can't set the given component as device owner` - Make sure all user accounts are removed
- `Error: Trying to set the device owner, but device owner is already set` - Device owner is already set
- `Error: Not allowed to set the device owner` - Device may have accounts or is not in the right state

### Step 4: Verify Device Owner Status

```bash
# Check if Device Owner is set
adb shell dpm list-owners
```

**Expected Output:**

```
Device owner set to package com.unoshield.mdm
Active admin set to {com.unoshield.mdm/.DeviceOwnerReceiver}
```

## Method 2: Set Device Owner During Factory Reset (Production Method)

This is the proper way for production devices:

1. **Factory reset** the device
2. During initial setup, **scan the QR code** before completing setup
3. The app will automatically be set as Device Owner during provisioning

## Method 3: Using TestDPC (For Testing Only)

Google provides a test app called TestDPC that can help test Device Owner functionality:

1. Install TestDPC from Play Store
2. Use it to understand Device Owner behavior
3. Then apply the same concepts to your app

## Verification Commands

### Check Device Owner Status

```bash
adb shell dpm list-owners
```

### Check Active Device Admins

```bash
adb shell dpm list-owners
```

### Check App Package Name

```bash
adb shell pm list packages | grep unoshield
```

### View Logs

```bash
# View all logs
adb logcat

# Filter for Device Owner related logs
adb logcat | grep -i "deviceowner\|DeviceOwnerReceiver"

# Filter for your app
adb logcat | grep -i "unoshield"
```

## Troubleshooting

### Error: "Can't set the given component as device owner"

**Causes:**

- User accounts exist on the device
- Device lock screen is set
- Device is already managed by another app
- App is not installed

**Solutions:**

1. Remove all user accounts
2. Remove lock screen password/PIN
3. Uninstall any other MDM apps
4. Reinstall your app: `adb install -r app-debug.apk`

### Error: "Not allowed to set the device owner"

**Causes:**

- Device is not in the right state
- Android version doesn't support it
- Device is already provisioned

**Solutions:**

1. Factory reset the device
2. Try on Android 5.0+ (API 21+)
3. Make sure device is not already enrolled

### Check Current Status in App

The app now shows Device Owner status in:

- **Settings Activity**: Status indicator at the top
- **Enrollment Activity**: After enrollment completes
- **Restriction Policy Activity**: When trying to apply policies

## Quick Reference

```bash
# Full setup sequence
adb devices                                    # Check connection
adb shell pm list packages | grep unoshield   # Verify app installed
adb shell dpm set-device-owner com.unoshield.mdm/.DeviceOwnerReceiver  # Set owner
adb shell dpm list-owners                     # Verify it worked
```

## Component Name Format

The component name format is: `{package_name}/{receiver_class_name}`

For this app:

- Package: `com.unoshield.mdm`
- Receiver: `DeviceOwnerReceiver`
- Full component: `com.unoshield.mdm/.DeviceOwnerReceiver`

## Notes

- **Device Owner is permanent** until factory reset
- **Cannot be removed** without factory reset
- **Required for most MDM policies** (user restrictions, etc.)
- **Only one Device Owner** can exist per device
- **Works on Android 5.0+** (API level 21+)
