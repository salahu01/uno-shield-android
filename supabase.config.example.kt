/**
 * Supabase Configuration Template
 * 
 * Copy this file to your project and fill in your Supabase credentials.
 * DO NOT commit actual credentials to version control.
 * 
 * Option 1: Use local.properties
 *   Add: SUPABASE_URL=https://your-project.supabase.co
 *   Add: SUPABASE_ANON_KEY=your-anon-key
 * 
 * Option 2: Use BuildConfig (recommended for production)
 *   See build.gradle.kts configuration in SUPABASE_INTEGRATION.md
 */

package com.unoshield.mdm.supabase.config

object SupabaseConfig {
    // Get from Supabase Dashboard → Settings → API
    const val SUPABASE_URL = "https://your-project-id.supabase.co"
    
    // Anon/Public Key (safe to use in client apps)
    const val SUPABASE_ANON_KEY = "your-anon-key-here"
    
    // Service Role Key (NEVER use in client apps - backend only!)
    // const val SUPABASE_SERVICE_ROLE_KEY = "your-service-role-key"
    
    // Storage bucket names
    const val DEVICE_LOGS_BUCKET = "device-logs"
    const val POLICY_CONFIGS_BUCKET = "policy-configs"
    
    // Database schema
    const val DEFAULT_SCHEMA = "public"
    
    // Real-time channel names
    const val CHANNEL_DEVICE_POLICIES = "device-policies"
    const val CHANNEL_DEVICE_STATUS = "device-status"
}


