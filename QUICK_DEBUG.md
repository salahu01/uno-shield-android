# Quick Debug Reference

## Set Device Owner via ADB (Fastest Method)

### Prerequisites
- Remove ALL user accounts from device
- Remove lock screen password/PIN
- Connect device via USB with USB Debugging enabled

### Quick Command
```bash
adb shell dpm set-device-owner com.unoshield.mdm/.DeviceOwnerReceiver
```

### Or Use the Helper Script
```bash
./set-device-owner.sh
```

### Verify It Worked
```bash
adb shell dpm list-owners
```

Should show:
```
Device owner set to package com.unoshield.mdm
Active admin set to {com.unoshield.mdm/.DeviceOwnerReceiver}
```

## Check Status in App

1. Open the app
2. Look at the **Settings** screen
3. Check the status indicator at the top:
   - ✅ **Green**: Device Owner active - All policies work
   - ⚠️ **Orange**: Device Admin only - Some policies won't work
   - ❌ **Red**: Not enrolled - Policies won't work

## Debug Info in App (Debug Builds Only)

In debug builds, the Settings screen shows:
- ADB command to run
- Current Device Owner status
- Package and component names
- Troubleshooting tips

## Common Errors

### "Can't set the given component as device owner"
**Solution**: Remove all user accounts and lock screen

### "Not allowed to set the device owner"
**Solution**: Factory reset device or ensure no accounts exist

### "Device owner is already set"
**Solution**: Another app is Device Owner - Factory reset needed

## View Logs

```bash
# All logs
adb logcat

# Filter for Device Owner
adb logcat | grep -i "deviceowner\|DeviceOwnerReceiver"

# Filter for app
adb logcat | grep -i "unoshield"
```

## Reset Device Owner

Only way to remove Device Owner:
```bash
# Factory reset device
adb reboot recovery
# Then wipe data/factory reset
```

Or use device Settings → System → Reset → Factory reset

