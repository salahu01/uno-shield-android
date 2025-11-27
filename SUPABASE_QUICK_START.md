# Supabase Quick Start Checklist

Follow these steps to quickly integrate Supabase into your UNO Shield MDM app.

## ✅ Setup Checklist

### 1. Supabase Project Setup (5 minutes)
- [ ] Create account at [supabase.com](https://supabase.com)
- [ ] Create new project
- [ ] Wait for project initialization
- [ ] Copy Project URL and Anon Key from Settings → API

### 2. Database Setup (10 minutes)
- [ ] Open Supabase Dashboard → SQL Editor
- [ ] Copy and run `supabase/migrations/001_initial_schema.sql`
- [ ] Verify tables are created (Database → Tables)
- [ ] Verify RLS is enabled (should see 🔒 icon on tables)

### 3. Storage Setup (2 minutes)
- [ ] Go to Storage → Create Bucket
- [ ] Create bucket: `device-logs` (Private)
- [ ] Create bucket: `policy-configs` (Private)

### 4. Android Dependencies (5 minutes)
- [ ] Update `gradle/libs.versions.toml` with Supabase versions
- [ ] Update `app/build.gradle.kts` with Supabase dependencies
- [ ] Sync Gradle files

### 5. Code Integration (15 minutes)
- [ ] Create `SupabaseClient.kt` singleton
- [ ] Create `MDMApplication.kt` and initialize Supabase
- [ ] Update `AndroidManifest.xml` with Application class
- [ ] Replace API calls with Supabase (start with device registration)

### 6. Configuration (5 minutes)
- [ ] Copy `supabase.config.example.kt` to your project
- [ ] Add Supabase URL and Key to `local.properties`:
  ```
  SUPABASE_URL=https://your-project.supabase.co
  SUPABASE_ANON_KEY=your-anon-key
  ```
- [ ] Update `SupabaseClient.kt` to read from `local.properties`

### 7. Testing (10 minutes)
- [ ] Test device registration
- [ ] Test authentication
- [ ] Test real-time subscriptions
- [ ] Verify data appears in Supabase Dashboard

## 🚀 Quick Code Snippets

### Initialize Supabase
```kotlin
// In MDMApplication.kt
override fun onCreate() {
    super.onCreate()
    SupabaseClient.initialize(this)
}
```

### Register Device
```kotlin
val device = Device(
    device_id = "device-123",
    enrollment_id = "enrollment-456"
)
databaseRepository.registerDevice(device)
```

### Subscribe to Real-time Updates
```kotlin
realtimeRepository.subscribeToPolicyUpdates(deviceId) { policy ->
    // Handle policy update
}
```

## 📚 Full Documentation

See `SUPABASE_INTEGRATION.md` for complete documentation.

## 🔒 Security Notes

- ✅ Use Anon Key in Android app (safe for client-side)
- ❌ Never use Service Role Key in Android app
- ✅ Enable RLS on all tables
- ✅ Store keys in `local.properties` (not in code)
- ✅ Use environment variables in CI/CD

## 🆘 Need Help?

1. Check Supabase Dashboard → Logs for errors
2. Review Android Logcat output
3. Verify RLS policies allow your operations
4. Check network connectivity
5. Consult `SUPABASE_INTEGRATION.md` troubleshooting section


