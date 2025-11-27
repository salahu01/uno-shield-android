# Fix Device Owner Issue After QR Code Enrollment

## Problem
After enrolling via QR code, you're getting the error:
```
Permission denied. Device must be enrolled as Device Owner.
```

And `adb shell dpm list-owners` returns empty.

## Why This Happens
QR code enrollment **should** set Device Owner automatically, but it only works if:
- The device is factory reset **before** scanning the QR code
- **No user accounts** exist on the device when QR code is scanned

If you scanned the QR code on a device that already had accounts, it only sets **Device Admin**, not **Device Owner**.

## Solution: Set Device Owner via ADB

### Step 1: Check Prerequisites

First, verify your device is connected:
```bash
adb devices
```

You should see your device listed. If not:
1. Enable USB Debugging in Developer Options
2. Connect device via USB
3. Accept the USB debugging prompt on device

### Step 2: Check for User Accounts

**CRITICAL**: Device Owner can ONLY be set if there are NO user accounts on the device.

Check for accounts:
```bash
adb shell pm list users
```

If you see multiple users or accounts, you need to remove them first.

### Step 3: Remove User Accounts (If Needed)

**Option A: Remove Accounts via Settings**
1. Go to Settings → Accounts
2. Remove ALL accounts (Google, email, etc.)
3. Remove lock screen password/PIN if set
4. Go to Settings → System → Multiple users → Remove all secondary users

**Option B: Factory Reset (Cleanest)**
1. Backup your data
2. Settings → System → Reset → Factory data reset
3. After reset, scan QR code during setup (this will set Device Owner automatically)

### Step 4: Set Device Owner via ADB

Once you've removed all accounts, run:

```bash
adb shell dpm set-device-owner com.unoshield.mdm/.DeviceOwnerReceiver
```

**Expected Success Output:**
```
Success: Device owner set to package com.unoshield.mdm
```

**If you get an error:**
- `Error: Can't set the given component as device owner` → Accounts still exist, remove them
- `Error: Not allowed to set the device owner` → Device may have accounts or is not in the right state
- `Error: Trying to set the device owner, but device owner is already set` → Another app is Device Owner

### Step 5: Verify Device Owner is Set

```bash
adb shell dpm list-owners
```

**Expected Output:**
```
Device owner set to package com.unoshield.mdm
Active admin set to {com.unoshield.mdm/.DeviceOwnerReceiver}
```

### Step 6: Test in App

1. Open the UNO Shield MDM app
2. Go to Settings
3. You should see "Device Owner: Active" status
4. Try changing policies in Restriction Policy - they should work now!

## Quick Fix Script

You can also use the provided script:
```bash
./set-device-owner.sh
```

This script will:
- Check if ADB is available
- Verify device connection
- Check if app is installed
- Warn you about prerequisites
- Set Device Owner
- Verify it worked

## Why Android 15 Shows "Contact Admin"

On Android 15, Google has stricter policies. After QR code enrollment, it may show "Contact Admin" instead of automatically setting Device Owner. This is expected behavior on newer Android versions.

For Android 15:
1. Factory reset device
2. Scan QR code during initial setup (before completing setup wizard)
3. This will properly set Device Owner

## Troubleshooting

### Still Getting "Permission Denied" After Setting Device Owner

1. Verify Device Owner is actually set:
   ```bash
   adb shell dpm list-owners
   ```

2. Check if the app recognizes Device Owner:
   ```bash
   adb logcat | grep -i "deviceowner\|DeviceOwnerReceiver"
   ```

3. Restart the app after setting Device Owner

### Device Owner Won't Set

Common causes:
- **User accounts exist** → Remove all accounts
- **Lock screen password/PIN** → Remove it
- **Another app is Device Owner** → Factory reset needed
- **App not installed** → Install the app first

### Check App Status

In the app, go to Settings and check the status indicator:
- ✅ **Green**: Device Owner active - All policies work
- ⚠️ **Orange**: Device Admin only - Some policies won't work  
- ❌ **Red**: Not enrolled - Policies won't work

## Alternative: Factory Reset Method

If ADB method doesn't work, factory reset and enroll properly:

1. **Backup your data**
2. **Factory reset** the device
3. **During initial setup**, scan the QR code **before** completing the setup wizard
4. This will automatically set Device Owner during provisioning

## Summary

**Root Cause**: QR code was scanned on a device that already had user accounts, so only Device Admin was set.

**Fix**: Remove all user accounts, then set Device Owner via ADB, or factory reset and scan QR code during setup.

