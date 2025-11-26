#!/bin/bash
# Generate release keystore for UNO Shield MDM Android app
# This keystore is required for Device Owner provisioning in production

KEYSTORE_NAME="unoshield-release.keystore"
KEYSTORE_PATH="app/$KEYSTORE_NAME"
KEY_ALIAS="unoshield-key"
KEY_VALIDITY_YEARS=25

echo "🔐 Generating release keystore for UNO Shield MDM"
echo "================================================"
echo ""
echo "This will create a keystore file at: $KEYSTORE_PATH"
echo "Key alias: $KEY_ALIAS"
echo "Validity: $KEY_VALIDITY_YEARS years"
echo ""
echo "⚠️  IMPORTANT: Save the passwords in a secure location!"
echo "   You will need them to build release APKs."
echo ""

# Check if keystore already exists
if [ -f "$KEYSTORE_PATH" ]; then
    echo "⚠️  Keystore already exists at $KEYSTORE_PATH"
    read -p "Do you want to overwrite it? (yes/no): " overwrite
    if [ "$overwrite" != "yes" ]; then
        echo "Aborted."
        exit 0
    fi
    rm -f "$KEYSTORE_PATH"
fi

# Prompt for passwords
read -sp "Enter keystore password (min 6 characters): " KEYSTORE_PASSWORD
echo ""
read -sp "Confirm keystore password: " KEYSTORE_PASSWORD_CONFIRM
echo ""

if [ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]; then
    echo "❌ Passwords do not match!"
    exit 1
fi

if [ ${#KEYSTORE_PASSWORD} -lt 6 ]; then
    echo "❌ Password must be at least 6 characters!"
    exit 1
fi

read -sp "Enter key password (can be same as keystore password): " KEY_PASSWORD
echo ""

# Generate the keystore
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity $((KEY_VALIDITY_YEARS * 365)) \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=UNO Shield MDM, OU=MDM, O=UNO Shield, L=Unknown, ST=Unknown, C=US"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Keystore generated successfully!"
    echo ""
    echo "📝 IMPORTANT: Save these credentials securely:"
    echo "   Keystore file: $KEYSTORE_PATH"
    echo "   Key alias: $KEY_ALIAS"
    echo "   Keystore password: [YOUR PASSWORD]"
    echo "   Key password: [YOUR PASSWORD]"
    echo ""
    echo "⚠️  If you lose these credentials, you won't be able to:"
    echo "   - Update the app on enrolled devices"
    echo "   - Build new release versions"
    echo ""
    echo "💡 Next steps:"
    echo "   1. Add keystore credentials to app/keystore.properties (see keystore.properties.example)"
    echo "   2. Update build.gradle.kts to use the release keystore"
    echo "   3. Build release APK: ./gradlew assembleRelease"
else
    echo ""
    echo "❌ Failed to generate keystore!"
    exit 1
fi

