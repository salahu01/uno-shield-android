# Device Owner Bug Fix

## The Bug

**Root Cause**: There was a mismatch between which receiver handled provisioning and which receiver was checked for Device Owner status.

### The Problem

1. **AdminReceiver** was handling provisioning events (`PROVISIONING_COMPLETE`, `PROFILE_PROVISIONING_COMPLETE`)
2. **DeviceOwnerReceiver** was what the code checked for Device Owner status
3. When Android provisions a device via QR code with `FULLY_MANAGED_DEVICE` mode, it sets **the component that handles provisioning** as Device Owner
4. So Android was setting `AdminReceiver` as Device Owner, but the code was checking `DeviceOwnerReceiver`
5. Result: Device Owner was set, but the app couldn't detect it, and policies failed with "Permission denied"

### Why Android 15 Shows "Contact Admin"

On Android 15, Google has stricter provisioning policies. If the provisioning flow doesn't complete properly or the component isn't correctly configured, it may show "Contact Admin" instead of completing enrollment.

## The Fix

### Changes Made

1. **DeviceOwnerReceiver.kt**: 
   - Added provisioning event handling (`onProfileProvisioningComplete`, `onReceive` for `PROVISIONING_COMPLETE`)
   - Now handles provisioning just like AdminReceiver did
   - This ensures Android sets `DeviceOwnerReceiver` as Device Owner during QR code enrollment

2. **AndroidManifest.xml**:
   - Added intent filters to `DeviceOwnerReceiver` for provisioning events:
     - `PROFILE_PROVISIONING_COMPLETE`
     - `PROVISIONING_COMPLETE`
     - `DEVICE_OWNER_CHANGED`
   - This allows `DeviceOwnerReceiver` to receive provisioning broadcasts

3. **RestrictionPolicyActivity.kt**:
   - Added fallback check for `AdminReceiver` (for backward compatibility)
   - Improved error messages to explain the issue

### How It Works Now

1. QR code is scanned during factory reset
2. `GetProvisioningModeActivity` returns `PROVISIONING_MODE_FULLY_MANAGED_DEVICE`
3. Android provisions the device and sends `PROVISIONING_COMPLETE` broadcast
4. **DeviceOwnerReceiver** receives the broadcast (because it now has the intent filter)
5. Android sets **DeviceOwnerReceiver** as Device Owner (because it handles provisioning)
6. The app checks `DeviceOwnerReceiver` for Device Owner status → ✅ Match!
7. Policies work correctly

## Testing

### For New Enrollments

1. **Factory reset** the device
2. **Scan QR code** during initial setup (before completing setup wizard)
3. Device should be enrolled as Device Owner automatically
4. Check: `adb shell dpm list-owners` should show `com.unoshield.mdm`
5. Policies should work without "Permission denied" errors

### For Existing Enrollments

If you already enrolled via QR code and Device Owner wasn't set:

**Option 1: Re-enroll (Recommended)**
1. Factory reset device
2. Scan QR code during setup
3. Device Owner will be set automatically

**Option 2: Set via ADB (If no accounts exist)**
```bash
adb shell dpm set-device-owner com.unoshield.mdm/.DeviceOwnerReceiver
```

## Android 15 Compatibility

The fix also improves Android 15 compatibility:

1. **Proper component handling**: DeviceOwnerReceiver now properly handles provisioning, which Android 15 requires
2. **Intent filters**: All necessary intent filters are registered for provisioning events
3. **Provisioning flow**: The complete provisioning flow is now handled by DeviceOwnerReceiver

If Android 15 still shows "Contact Admin":
- Ensure QR code is scanned **during factory reset** (before completing setup wizard)
- Verify the QR code includes the correct component name
- Check device logs: `adb logcat | grep -i "deviceowner\|provisioning"`

## Verification

After enrolling, verify Device Owner is set:

```bash
adb shell dpm list-owners
```

Should show:
```
Device owner set to package com.unoshield.mdm
Active admin set to {com.unoshield.mdm/.DeviceOwnerReceiver}
```

In the app:
- Settings screen should show "Device Owner: Active" (green status)
- Policies in Restriction Policy should work without errors

## Summary

**Before**: AdminReceiver handled provisioning → Android set AdminReceiver as Device Owner → Code checked DeviceOwnerReceiver → Mismatch → Policies failed

**After**: DeviceOwnerReceiver handles provisioning → Android sets DeviceOwnerReceiver as Device Owner → Code checks DeviceOwnerReceiver → Match → Policies work ✅

