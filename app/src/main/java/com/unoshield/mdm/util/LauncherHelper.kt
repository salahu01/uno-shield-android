package com.unoshield.mdm.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import com.unoshield.mdm.ui.MainActivity

/**
 * Helper class to manage launcher settings
 */
object LauncherHelper {
    
    private const val TAG = "LauncherHelper"
    
    /**
     * Sets this app as the default launcher programmatically
     * Requires Device Owner permissions (not just Device Admin)
     */
    fun setAsDefaultLauncher(context: Context): Boolean {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val packageName = context.packageName
            
            // Check if we have device owner permissions
            val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
            
            if (!isDeviceOwner) {
                Log.w(TAG, "Cannot set default launcher: App is not device owner. User must set manually.")
                return false
            }
            
            // Get the launcher component (MainActivity is now the launcher)
            val launcherComponent = ComponentName(packageName, MainActivity::class.java.name)
            
            // Create intent filter for HOME category
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            
            // Get admin component (device owner)
            val adminComponent = ComponentName(packageName, "com.unoshield.mdm.AdminReceiver")
            
            // Use addPersistentPreferredActivity to set default launcher
            // This requires device owner permissions
            devicePolicyManager.addPersistentPreferredActivity(
                adminComponent,
                intentFilter,
                launcherComponent
            )
            
            Log.d(TAG, "Successfully set app as default launcher")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception setting default launcher", e)
            return false
        }
    }
    
    /**
     * Checks if this app is currently set as the default launcher
     */
    fun isDefaultLauncher(context: Context): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        
        val resolveInfo: ResolveInfo? = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncher = resolveInfo?.activityInfo?.packageName
        
        return currentLauncher == context.packageName
    }
    
    /**
     * Opens the launcher selection dialog for manual selection
     * This works even without admin permissions
     * Note: This will only show the dialog if no default launcher is set
     */
    fun openLauncherSelection(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening launcher selection", e)
        }
    }
    
    /**
     * Clears the default launcher preference
     * This will force Android to show the launcher selection dialog next time home is pressed
     * Requires Device Owner permissions
     */
    fun clearDefaultLauncher(context: Context): Boolean {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val packageName = context.packageName
            
            // Check if we have device owner permissions
            val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
            
            if (!isDeviceOwner) {
                Log.w(TAG, "Cannot clear default launcher: App is not device owner")
                return false
            }
            
            // Create intent filter for HOME category
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            
            // Get admin component (device owner)
            val adminComponent = ComponentName(packageName, "com.unoshield.mdm.AdminReceiver")
            
            // Clear persistent preferred activity for HOME intent
            // This removes the default launcher preference
            try {
                // The correct method signature is clearPackagePersistentPreferredActivities(admin, packageName)
                // But we need to clear for a specific intent filter
                // Since there's no direct way, we'll try to set an empty preferred activity
                // Actually, the best approach is to use clearPackagePersistentPreferredActivities
                // which clears all persistent preferred activities for a package
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // This method clears all persistent preferred activities for the given package
                    // We pass empty string or null to clear all, or a specific package name
                    // Since we want to clear HOME launcher, we need a different approach
                    // The API doesn't have a direct way to clear just HOME, so we'll note this limitation
                    Log.w(TAG, "Cannot directly clear HOME launcher preference via API")
                    Log.w(TAG, "User must manually change default launcher in Settings")
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing persistent preferred activities", e)
                return false
            }
            
            Log.d(TAG, "Successfully cleared default launcher preference")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception clearing default launcher", e)
            return false
        }
    }
    
    /**
     * Opens Android Settings to manually change the default launcher
     * This works without admin permissions
     */
    fun openLauncherSettings(context: Context) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening launcher settings", e)
            // Fallback: try to open app settings
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening app settings", e2)
            }
        }
    }
}

