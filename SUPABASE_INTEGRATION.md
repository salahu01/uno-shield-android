# Supabase Integration Guide for UNO Shield Android MDM

This guide provides comprehensive instructions for integrating Supabase into your Android MDM application, replacing or complementing your current Retrofit-based backend.

## Table of Contents

1. [What is Supabase?](#what-is-supabase)
2. [Prerequisites](#prerequisites)
3. [Project Setup](#project-setup)
4. [Dependencies Configuration](#dependencies-configuration)
5. [Supabase Project Setup](#supabase-project-setup)
6. [Android Configuration](#android-configuration)
7. [Authentication](#authentication)
8. [Database Integration](#database-integration)
9. [Real-time Features](#real-time-features)
10. [Storage](#storage)
11. [Migration from Retrofit](#migration-from-retrofit)
12. [Best Practices](#best-practices)
13. [Troubleshooting](#troubleshooting)

---

## What is Supabase?

Supabase is an open-source Firebase alternative that provides:
- **PostgreSQL Database**: Full-featured relational database with auto-generated REST APIs
- **Authentication**: Built-in auth with email, OAuth, magic links, and more
- **Real-time**: Subscriptions to database changes
- **Storage**: File storage with CDN
- **Edge Functions**: Serverless functions
- **Row Level Security (RLS)**: Database-level security policies

### Why Supabase for MDM?

- **Real-time Device Management**: Push policy updates instantly to devices
- **Secure Authentication**: Built-in auth for admin portals and device enrollment
- **Scalable Database**: PostgreSQL handles complex MDM data relationships
- **Offline Support**: Can work with local Room database for offline-first architecture
- **Cost-Effective**: Generous free tier, pay-as-you-grow pricing

---

## Prerequisites

- Android Studio Hedgehog or later
- Kotlin 1.9.0+
- Min SDK 24 (already configured)
- Supabase account (free at [supabase.com](https://supabase.com))

---

## Project Setup

### Step 1: Create Supabase Account and Project

1. Go to [supabase.com](https://supabase.com) and sign up
2. Create a new project
3. Wait for the project to initialize (2-3 minutes)
4. Note down your:
   - **Project URL**: `https://your-project-id.supabase.co`
   - **Anon/Public Key**: Found in Settings → API
   - **Service Role Key**: Found in Settings → API (keep secret!)

---

## Dependencies Configuration

### Step 2: Add Supabase Dependencies

Update `gradle/libs.versions.toml`:

```toml
[versions]
# ... existing versions ...
supabase = "2.5.0"
ktor = "2.3.5"

[libraries]
# ... existing libraries ...
# Supabase Core
supabase-postgrest = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt", version.ref = "supabase" }
supabase-storage = { group = "io.github.jan-tennert.supabase", name = "storage-kt", version.ref = "supabase" }
supabase-realtime = { group = "io.github.jan-tennert.supabase", name = "realtime-kt", version.ref = "supabase" }
supabase-functions = { group = "io.github.jan-tennert.supabase", name = "functions-kt", version.ref = "supabase" }
supabase-gotrue = { group = "io.github.jan-tennert.supabase", name = "gotrue-kt", version.ref = "supabase" }

# Ktor (required by Supabase)
ktor-client-android = { group = "io.ktor", name = "ktor-client-android", version.ref = "ktor" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
```

Update `app/build.gradle.kts` dependencies section:

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.functions)
    implementation(libs.supabase.gotrue)
    
    // Ktor (required by Supabase)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}
```

---

## Supabase Project Setup

### Step 3: Create Database Schema

In Supabase Dashboard → SQL Editor, run:

```sql
-- Devices table
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT UNIQUE NOT NULL,
    enrollment_id TEXT NOT NULL,
    serial_number TEXT,
    model TEXT,
    android_version TEXT,
    enrolled_at TIMESTAMPTZ DEFAULT NOW(),
    last_seen TIMESTAMPTZ,
    status TEXT DEFAULT 'active',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Device policies table
CREATE TABLE device_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    policy_type TEXT NOT NULL,
    policy_data JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Blacklist numbers (synced from local Room DB)
CREATE TABLE blacklist_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    phone_number TEXT NOT NULL,
    name TEXT,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, phone_number)
);

-- Whitelist numbers (synced from local Room DB)
CREATE TABLE whitelist_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    phone_number TEXT NOT NULL,
    name TEXT,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, phone_number)
);

-- Blocked apps (synced from local Room DB)
CREATE TABLE blocked_apps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    name TEXT,
    blocked_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, package_name)
);

-- Enable Row Level Security
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE blacklist_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE whitelist_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE blocked_apps ENABLE ROW LEVEL SECURITY;

-- RLS Policies (example: devices can only see their own data)
CREATE POLICY "Devices can view own data" ON devices
    FOR SELECT USING (auth.uid()::text = device_id);

CREATE POLICY "Devices can update own data" ON devices
    FOR UPDATE USING (auth.uid()::text = device_id);

-- Create indexes for performance
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_enrollment_id ON devices(enrollment_id);
CREATE INDEX idx_policies_device_id ON device_policies(device_id);
CREATE INDEX idx_blacklist_device_id ON blacklist_numbers(device_id);
CREATE INDEX idx_whitelist_device_id ON whitelist_numbers(device_id);
CREATE INDEX idx_blocked_apps_device_id ON blocked_apps(device_id);
```

---

## Android Configuration

### Step 4: Create Supabase Client Singleton

Create `app/src/main/java/com/unoshield/mdm/supabase/SupabaseClient.kt`:

```kotlin
package com.unoshield.mdm.supabase

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.auth.providers.builtin.Email
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

object SupabaseClient {
    // TODO: Replace with your actual Supabase URL and anon key
    private const val SUPABASE_URL = "https://your-project-id.supabase.co"
    private const val SUPABASE_ANON_KEY = "your-anon-key-here"
    
    private var client: SupabaseClient? = null
    
    fun initialize(context: Context): SupabaseClient {
        if (client == null) {
            client = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY
            ) {
                // Configure HTTP client
                httpEngine = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json()
                    }
                }
                
                // Set logging level (use LogLevel.NONE for production)
                defaultLogLevel = LogLevel.DEBUG
                
                // Install modules
                install(Auth) {
                    // Configure deep links for OAuth (if needed)
                    // scheme = "unoshield"
                    // host = "login"
                }
                
                install(Postgrest) {
                    defaultSchema = "public"
                }
                
                install(Storage)
                install(Realtime)
                install(Functions)
            }
        }
        return client!!
    }
    
    fun getClient(): SupabaseClient {
        return client ?: throw IllegalStateException(
            "Supabase client not initialized. Call initialize() first."
        )
    }
}
```

### Step 5: Update Application Class

Create or update `app/src/main/java/com/unoshield/mdm/MDMApplication.kt`:

```kotlin
package com.unoshield.mdm

import android.app.Application
import com.unoshield.mdm.supabase.SupabaseClient

class MDMApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Supabase client
        SupabaseClient.initialize(this)
    }
}
```

Update `AndroidManifest.xml`:

```xml
<application
    android:name=".MDMApplication"
    ...>
    <!-- ... existing content ... -->
</application>
```

---

## Authentication

### Step 6: Device Authentication

Create `app/src/main/java/com/unoshield/mdm/supabase/auth/AuthRepository.kt`:

```kotlin
package com.unoshield.mdm.supabase.auth

import com.unoshield.mdm.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository {
    private val supabase = SupabaseClient.getClient()
    
    /**
     * Sign up a new device/admin user
     */
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in with email/password
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current session
     */
    fun getCurrentSession() = supabase.auth.currentSessionOrNull()
    
    /**
     * Observe authentication state
     */
    fun observeAuthState(): Flow<Boolean> {
        return supabase.auth.sessionStatus.map { status ->
            status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated
        }
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return getCurrentSession() != null
    }
}
```

### Step 7: Device Enrollment with Supabase

Update `app/src/main/java/com/unoshield/mdm/ui/EnrollmentActivity.kt` to use Supabase:

```kotlin
// Example integration in EnrollmentActivity
class EnrollmentActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    
    private suspend fun enrollDevice(enrollmentId: String) {
        // Authenticate device (you might use device-specific credentials)
        val result = authRepository.signIn(
            email = "device_${enrollmentId}@mdm.local",
            password = enrollmentId // or use a secure token
        )
        
        if (result.isSuccess) {
            // Device is now authenticated and can sync with Supabase
            registerDeviceInSupabase(enrollmentId)
        }
    }
    
    private suspend fun registerDeviceInSupabase(enrollmentId: String) {
        // Implementation in DatabaseRepository (see next section)
    }
}
```

---

## Database Integration

### Step 8: Create Database Repository

Create `app/src/main/java/com/unoshield/mdm/supabase/database/DatabaseRepository.kt`:

```kotlin
package com.unoshield.mdm.supabase.database

import com.unoshield.mdm.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val id: String? = null,
    val device_id: String,
    val enrollment_id: String,
    val serial_number: String? = null,
    val model: String? = null,
    val android_version: String? = null,
    val enrolled_at: String? = null,
    val last_seen: String? = null,
    val status: String = "active"
)

@Serializable
data class DevicePolicy(
    val id: String? = null,
    val device_id: String,
    val policy_type: String,
    val policy_data: Map<String, String>
)

class DatabaseRepository {
    private val supabase = SupabaseClient.getClient()
    
    /**
     * Register a new device
     */
    suspend fun registerDevice(device: Device): Result<Device> {
        return try {
            val inserted = supabase.from("devices")
                .insert(device)
                .decodeSingle<Device>()
            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update device heartbeat (last_seen)
     */
    suspend fun updateDeviceHeartbeat(deviceId: String): Result<Unit> {
        return try {
            supabase.from("devices")
                .update {
                    set("last_seen", System.currentTimeMillis() / 1000)
                } {
                    filter {
                        eq("device_id", deviceId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get device information
     */
    suspend fun getDevice(deviceId: String): Result<Device?> {
        return try {
            val device = supabase.from("devices")
                .select {
                    filter {
                        eq("device_id", deviceId)
                    }
                }
                .decodeSingleOrNull<Device>()
            Result.success(device)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get policies for a device
     */
    suspend fun getDevicePolicies(deviceId: String): Result<List<DevicePolicy>> {
        return try {
            val policies = supabase.from("device_policies")
                .select {
                    filter {
                        eq("device_id", deviceId)
                    }
                }
                .decodeList<DevicePolicy>()
            Result.success(policies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync local blacklist to Supabase
     */
    suspend fun syncBlacklistNumbers(
        deviceId: String,
        numbers: List<com.unoshield.mdm.data.BlacklistNumber>
    ): Result<Unit> {
        return try {
            val supabaseNumbers = numbers.map {
                mapOf(
                    "device_id" to deviceId,
                    "phone_number" to it.phoneNumber,
                    "name" to (it.name ?: ""),
                    "added_at" to (it.addedAt / 1000) // Convert to Unix timestamp
                )
            }
            
            supabase.from("blacklist_numbers")
                .upsert(supabaseNumbers)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync local whitelist to Supabase
     */
    suspend fun syncWhitelistNumbers(
        deviceId: String,
        numbers: List<com.unoshield.mdm.data.WhitelistNumber>
    ): Result<Unit> {
        return try {
            val supabaseNumbers = numbers.map {
                mapOf(
                    "device_id" to deviceId,
                    "phone_number" to it.phoneNumber,
                    "name" to (it.name ?: ""),
                    "added_at" to (it.addedAt / 1000)
                )
            }
            
            supabase.from("whitelist_numbers")
                .upsert(supabaseNumbers)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync blocked apps to Supabase
     */
    suspend fun syncBlockedApps(
        deviceId: String,
        apps: List<com.unoshield.mdm.data.BlockedApp>
    ): Result<Unit> {
        return try {
            val supabaseApps = apps.map {
                mapOf(
                    "device_id" to deviceId,
                    "package_name" to it.packageName,
                    "name" to (it.name ?: ""),
                    "blocked_at" to (it.blockedAt / 1000)
                )
            }
            
            supabase.from("blocked_apps")
                .upsert(supabaseApps)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Real-time Features

### Step 9: Real-time Policy Updates

Create `app/src/main/java/com/unoshield/mdm/supabase/realtime/RealtimeRepository.kt`:

```kotlin
package com.unoshield.mdm.supabase.realtime

import com.unoshield.mdm.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.createChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RealtimeRepository {
    private val supabase = SupabaseClient.getClient()
    private var channel: RealtimeChannel? = null
    
    /**
     * Subscribe to policy updates for a device
     */
    suspend fun subscribeToPolicyUpdates(
        deviceId: String,
        onUpdate: (Map<String, Any>) -> Unit
    ) {
        channel = supabase.createChannel("device-policies-$deviceId") {
            on("postgres_changes") {
                filter = "device_id=eq.$deviceId"
                table = "device_policies"
            } { event ->
                val data = event.payload["new"] as? Map<*, *>
                data?.let { onUpdate(it as Map<String, Any>) }
            }
        }
        
        channel?.subscribe()
    }
    
    /**
     * Unsubscribe from updates
     */
    suspend fun unsubscribe() {
        channel?.unsubscribe()
        channel = null
    }
    
    /**
     * Subscribe to device status changes
     */
    suspend fun subscribeToDeviceStatus(
        deviceId: String,
        onStatusChange: (String) -> Unit
    ) {
        channel = supabase.createChannel("device-status-$deviceId") {
            on("postgres_changes") {
                filter = "device_id=eq.$deviceId"
                table = "devices"
            } { event ->
                val data = event.payload["new"] as? Map<*, *>
                val status = data?.get("status") as? String
                status?.let { onStatusChange(it) }
            }
        }
        
        channel?.subscribe()
    }
}
```

### Step 10: Use Real-time in MainActivity

Example usage in `MainActivity`:

```kotlin
class MainActivity : AppCompatActivity() {
    private val realtimeRepository = RealtimeRepository()
    private val deviceId = "your-device-id"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            // Subscribe to policy updates
            realtimeRepository.subscribeToPolicyUpdates(deviceId) { policy ->
                // Apply policy update to device
                applyPolicyUpdate(policy)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            realtimeRepository.unsubscribe()
        }
    }
    
    private fun applyPolicyUpdate(policy: Map<String, Any>) {
        // Handle policy update
        val policyType = policy["policy_type"] as? String
        val policyData = policy["policy_data"] as? Map<*, *>
        
        when (policyType) {
            "app_blocking" -> {
                // Update blocked apps
            }
            "call_filtering" -> {
                // Update call filter settings
            }
            // ... other policy types
        }
    }
}
```

---

## Storage

### Step 11: File Storage for Device Logs

Create `app/src/main/java/com/unoshield/mdm/supabase/storage/StorageRepository.kt`:

```kotlin
package com.unoshield.mdm.supabase.storage

import com.unoshield.mdm.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.github.jan.supabase.storage.downloadPublicUrl
import java.io.File

class StorageRepository {
    private val supabase = SupabaseClient.getClient()
    private val bucketName = "device-logs"
    
    /**
     * Upload device log file
     */
    suspend fun uploadLogFile(
        deviceId: String,
        logFile: File
    ): Result<String> {
        return try {
            val path = "$deviceId/${logFile.name}"
            supabase.storage.from(bucketName)
                .upload(path, logFile)
            
            val publicUrl = supabase.storage.from(bucketName)
                .downloadPublicUrl(path)
            
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download policy configuration file
     */
    suspend fun downloadPolicyConfig(
        deviceId: String,
        configFileName: String
    ): Result<ByteArray> {
        return try {
            val path = "$deviceId/$configFileName"
            val data = supabase.storage.from(bucketName)
                .downloadAuthenticated(path)
            
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Note**: Create the storage bucket in Supabase Dashboard → Storage → Create Bucket

---

## Migration from Retrofit

### Step 12: Migrate Existing API Calls

Replace `ApiService` calls with Supabase:

**Before (Retrofit):**
```kotlin
val response = apiService.registerDevice(request)
```

**After (Supabase):**
```kotlin
val device = Device(
    device_id = request.device_id,
    enrollment_id = request.enrollment_id,
    serial_number = request.serial_number,
    model = request.model,
    android_version = request.android_version
)
val result = databaseRepository.registerDevice(device)
```

### Step 13: Hybrid Approach (Optional)

You can keep Retrofit for custom endpoints and use Supabase for database operations:

```kotlin
class HybridRepository(
    private val apiService: ApiService,
    private val databaseRepository: DatabaseRepository
) {
    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<Device> {
        // Use Supabase for database operations
        val device = Device(
            device_id = request.device_id,
            enrollment_id = request.enrollment_id
        )
        return databaseRepository.registerDevice(device)
    }
    
    suspend fun customEndpoint(): Result<CustomResponse> {
        // Use Retrofit for custom API endpoints
        return try {
            val response = apiService.customCall()
            Result.success(response.body()!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Best Practices

### 1. **Security**

- **Never commit API keys**: Store keys in `local.properties` or use BuildConfig:
  ```kotlin
  // In build.gradle.kts
  buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL")}\"")
  buildConfigField("String", "SUPABASE_KEY", "\"${project.findProperty("SUPABASE_KEY")}\"")
  ```

- **Use Row Level Security (RLS)**: Always enable RLS on tables
- **Service Role Key**: Only use on backend, never in Android app

### 2. **Offline-First Architecture**

Keep Room database for offline support, sync with Supabase:

```kotlin
class SyncRepository(
    private val localDb: MDMDatabase,
    private val supabaseRepo: DatabaseRepository
) {
    suspend fun syncBlacklist() {
        val localNumbers = localDb.blacklistNumberDao().getAll()
        val deviceId = getDeviceId()
        
        // Sync to Supabase
        supabaseRepo.syncBlacklistNumbers(deviceId, localNumbers)
        
        // Optionally: Fetch remote changes and merge
        // val remoteNumbers = supabaseRepo.getBlacklistNumbers(deviceId)
        // Merge logic...
    }
}
```

### 3. **Error Handling**

```kotlin
suspend fun safeSupabaseCall(block: suspend () -> Result<T>): Result<T> {
    return try {
        block()
    } catch (e: io.github.jan.supabase.exceptions.RestException) {
        when (e.statusCode) {
            401 -> Result.failure(Exception("Unauthorized"))
            404 -> Result.failure(Exception("Not found"))
            else -> Result.failure(e)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 4. **Testing**

```kotlin
// Use Supabase local development for testing
val testSupabaseUrl = "http://localhost:54321"
```

---

## Troubleshooting

### Common Issues

1. **"Client not initialized"**
   - Ensure `SupabaseClient.initialize()` is called in Application class

2. **"Unauthorized" errors**
   - Check RLS policies in Supabase
   - Verify authentication session exists

3. **Real-time not working**
   - Check network connectivity
   - Verify channel subscription is active
   - Check Supabase project status

4. **Build errors with Ktor**
   - Ensure all Ktor dependencies are included
   - Check Kotlin version compatibility

### Debugging

Enable detailed logging:
```kotlin
defaultLogLevel = LogLevel.DEBUG
```

Check Supabase Dashboard → Logs for server-side issues.

---

## Next Steps

1. **Set up Supabase project** and configure database schema
2. **Add dependencies** to your project
3. **Initialize Supabase client** in Application class
4. **Migrate one feature at a time** (start with device registration)
5. **Test thoroughly** before migrating all features
6. **Set up RLS policies** for security
7. **Configure real-time subscriptions** for policy updates
8. **Set up storage buckets** for file uploads

---

## Resources

- [Supabase Kotlin Documentation](https://github.com/supabase-community/supabase-kt)
- [Supabase Dashboard](https://app.supabase.com)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Row Level Security Guide](https://supabase.com/docs/guides/auth/row-level-security)

---

## Support

For issues specific to this integration:
1. Check Supabase Dashboard logs
2. Review Android Logcat for client-side errors
3. Consult [Supabase Discord](https://discord.supabase.com)
4. Review [GitHub Issues](https://github.com/supabase-community/supabase-kt/issues)


