# Android 15 Provisioning Fix

## Problem
On Android 15, QR code enrollment shows error: **"Something went wrong, Please contact admin support."**

## Root Cause
Android 15 has stricter requirements for Device Owner provisioning:

1. **Invalid Role Holder Activity**: The manifest used `android.app.Activity` which is invalid
2. **Missing Error Handling**: GetProvisioningModeActivity lacked proper error handling
3. **Missing Activity Class**: Android 15 requires a proper Activity class for role holder intents

## Fixes Applied

### 1. Created Proper RoleHolderActivity

**Before:**
```xml
<activity android:name="android.app.Activity" ... />
```

**After:**
```xml
<activity android:name=".provision.RoleHolderActivity" ... />
```

Created `RoleHolderActivity.kt` - a proper Activity class that handles Android 15 role holder intents.

### 2. Improved GetProvisioningModeActivity Error Handling

**Added:**
- Try-catch blocks for proper error handling
- Better logging for debugging
- Proper result codes (RESULT_OK/RESULT_CANCELED)
- Android version logging

### 3. Enhanced ProvisioningSuccessActivity

**Added:**
- Better error handling
- More detailed logging
- Proper exception handling
- Android 15 specific validation

## Testing on Android 15

### Steps to Test

1. **Factory reset** the Android 15 device
2. **Connect to WiFi** during setup
3. **Scan QR code** during initial setup (before completing setup wizard)
4. Device should enroll successfully without "something went wrong" error

### Verify Success

```bash
# Check Device Owner status
adb shell dpm list-owners

# Should show:
# Device owner set to package com.unoshield.mdm
# Active admin set to {com.unoshield.mdm/.DeviceOwnerReceiver}
```

### Check Logs

If still having issues, check logs:

```bash
# Filter for provisioning logs
adb logcat | grep -i "provisioning\|roleholder\|GetProvisioningMode"

# Filter for Device Owner logs
adb logcat | grep -i "deviceowner\|DeviceOwnerReceiver"
```

## Android 15 Specific Requirements

1. **Proper Activity Classes**: Cannot use `android.app.Activity` - must use actual Activity class
2. **Error Handling**: All provisioning activities must handle errors gracefully
3. **Component Names**: Must be properly specified in manifest
4. **Intent Filters**: Must be correctly configured
5. **Permissions**: Must have proper permissions (`BIND_DEVICE_ADMIN`, `LAUNCH_DEVICE_MANAGER_SETUP`)

## Common Android 15 Errors

### "Something went wrong, Please contact admin support"

**Causes:**
- Invalid Activity class in manifest
- Missing error handling
- Missing required data in provisioning extras
- Component name mismatch

**Solution:**
- Ensure RoleHolderActivity is a proper class (not `android.app.Activity`)
- Add proper error handling
- Verify QR code includes all required data
- Check logs for specific error messages

### "Contact Admin"

**Causes:**
- Device Owner not set properly
- Provisioning flow incomplete
- Component mismatch

**Solution:**
- Verify DeviceOwnerReceiver handles provisioning
- Check that provisioning completes successfully
- Ensure component names match in manifest and code

## Verification Checklist

- [ ] RoleHolderActivity is a proper Activity class (not `android.app.Activity`)
- [ ] GetProvisioningModeActivity has proper error handling
- [ ] ProvisioningSuccessActivity handles errors gracefully
- [ ] DeviceOwnerReceiver has provisioning intent filters
- [ ] All activities have proper permissions
- [ ] QR code includes all required data (enrollment_id, enrollment_code, base_url)

## Summary

The main fix was replacing the invalid `android.app.Activity` with a proper `RoleHolderActivity` class. Android 15 is stricter about activity classes and error handling, so all provisioning activities now have proper error handling and logging.

After rebuilding with these fixes, Android 15 enrollment should work correctly.

