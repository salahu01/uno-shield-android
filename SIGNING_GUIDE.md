# Android App Signing Guide for Device Owner Provisioning

## ⚠️ Why You Need Proper Signing

**Debug signing can cause problems with Device Owner provisioning:**

1. **Device Rejection**: Some Android devices/manufacturers (especially Samsung, Huawei, Xiaomi) may **reject debug-signed apps** for Device Owner mode
2. **Android Version Issues**: Newer Android versions (Android 8+) are more strict about debug signing
3. **Security**: Debug keys are publicly known and not secure for production
4. **Update Problems**: You cannot update an app signed with a different key, so you'll be locked to debug signing forever

## ✅ Current Status

The project was previously using the **debug keystore** for release builds, which can cause QR code provisioning to fail on some devices.

## 🔐 Solution: Use Proper Release Keystore

We've set up a proper signing configuration that:
- Uses a release keystore for production builds
- Falls back to debug keystore only if release keystore is not configured (with warnings)
- Keeps keystore files out of version control

## 📝 Setup Instructions

### Step 1: Generate Release Keystore

Run the keystore generation script:

```bash
cd uno-shield/uno-shield-android
./generate-keystore.sh
```

This will:
- Create a keystore file at `app/unoshield-release.keystore`
- Prompt you for passwords
- Set validity to 25 years (recommended for MDM apps)

### Step 2: Configure Build

Create `app/keystore.properties` from the example:

```bash
cp app/keystore.properties.example app/keystore.properties
```

Edit `app/keystore.properties` and fill in your actual passwords:

```properties
storeFile=unoshield-release.keystore
storePassword=YOUR_KEYSTORE_PASSWORD_HERE
keyAlias=unoshield-key
keyPassword=YOUR_KEY_PASSWORD_HERE
```

### Step 3: Build Release APK

Build a release APK with proper signing:

```bash
./gradlew assembleRelease
```

The signed APK will be at:
```
app/build/outputs/apk/release/app-release.apk
```

## 🔒 Security Best Practices

1. **Never commit keystore files or passwords to git**
   - `*.keystore` and `keystore.properties` are already in `.gitignore`

2. **Backup your keystore**
   - Store the keystore file and passwords in a secure location
   - You **cannot** recover them if lost
   - Losing the keystore means you cannot update the app on enrolled devices

3. **Use strong passwords**
   - Minimum 8 characters recommended
   - Use a password manager to store credentials

## ⚙️ How It Works

The `build.gradle.kts` now:
1. Checks if `app/keystore.properties` exists
2. If found: Uses the release keystore from properties
3. If not found: Falls back to debug keystore (with warnings) for development

This allows:
- **Development**: Works without keystore setup (using debug key)
- **Production**: Properly signed with release keystore

## 🧪 Testing

### Test with Debug Signing (Development Only)
```bash
# Works but may fail on some devices
./gradlew assembleRelease
```

### Test with Release Signing (Production)
```bash
# 1. Create keystore.properties first
# 2. Then build
./gradlew assembleRelease
```

## 📱 Device Owner Provisioning

After building with release signing:
1. Copy `app-release.apk` to your backend directory
2. Generate QR code (the checksum will be based on the properly signed APK)
3. Scan QR code on factory-reset Android device
4. Provisioning should work on devices that previously failed with debug signing

## 🔍 Verify Signing

Check if your APK is properly signed:

```bash
# Check signature
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# View certificate info
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

## ❓ FAQ

**Q: Do I need to sign the app for Device Owner provisioning?**
A: For production, **yes**. Debug signing may work for testing, but many devices will reject it.

**Q: Can I use the same keystore for updates?**
A: Yes! You **must** use the same keystore for all updates to the same app package.

**Q: What happens if I lose the keystore?**
A: You cannot update the app. You'd need to uninstall and reinstall with a new package name.

**Q: Can I use Android Studio's generated keystore?**
A: Yes, but the script provides better defaults and instructions for MDM apps.

## 📚 References

- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Device Owner Provisioning](https://developer.android.com/work/dpc/provision-device-owner)
- [Troubleshooting Device Owner Setup](https://developer.android.com/work/dpc/provision-device-owner#troubleshooting)

