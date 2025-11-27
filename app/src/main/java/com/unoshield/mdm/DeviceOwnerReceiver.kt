package com.unoshield.mdm

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Owner Receiver for UNO Shield MDM
 * Handles device owner mode events and provisioning
 * This receiver MUST handle provisioning to be set as Device Owner during QR code enrollment
 */
class DeviceOwnerReceiver : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "DeviceOwnerReceiver"
        // Android action constant for provisioning complete
        private const val ACTION_PROVISIONING_COMPLETE = "android.app.action.PROVISIONING_COMPLETE"
        
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, DeviceOwnerReceiver::class.java)
        }
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device owner enabled")
        
        // Check if we're actually Device Owner
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
        Log.d(TAG, "Device Owner status: $isDeviceOwner")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device owner disabled")
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device owner will remove MDM control"
    }
    
    /**
     * Called when profile provisioning is complete.
     * This is CRITICAL for QR code enrollment - Android sets the component that handles
     * provisioning as Device Owner. This receiver must handle provisioning to be set as Device Owner.
     */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        Log.d(TAG, "onProfileProvisioningComplete called - Device Owner provisioning")
        handleProvisioningComplete(context, intent)
    }
    
    /**
     * Handle provisioning complete broadcast
     * This is called when Android sends the PROVISIONING_COMPLETE broadcast
     * CRITICAL: This receiver must handle provisioning to be set as Device Owner
     */
    private fun handleProvisioningComplete(context: Context, intent: Intent) {
        Log.d(TAG, "Handling provisioning complete in DeviceOwnerReceiver")
        
        // Verify we're Device Owner
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(context.packageName)
        Log.d(TAG, "Device Owner status after provisioning: $isDeviceOwner")
        
        try {
            // Extract enrollment data from provisioning extras
            val extras = intent.getBundleExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
            
            if (extras == null) {
                Log.e(TAG, "No provisioning extras bundle found!")
                return
            }
            
            val enrollmentId = extras.getString("com.unoshield.ENROLLMENT_ID")
            val enrollmentCode = extras.getString("com.unoshield.ENROLLMENT_CODE")
            val baseUrl = extras.getString("com.unoshield.BASE_URL")
            
            Log.d(TAG, "Extracted enrollment data:")
            Log.d(TAG, "  Enrollment ID: $enrollmentId")
            Log.d(TAG, "  Enrollment Code: $enrollmentCode")
            Log.d(TAG, "  Base URL: $baseUrl")
            
            // Validate we have required data
            if (enrollmentId.isNullOrBlank() || enrollmentCode.isNullOrBlank()) {
                Log.e(TAG, "Missing required enrollment data! ID: $enrollmentId, Code: $enrollmentCode")
                return
            }
            
            // Start enrollment activity to register device
            val enrollmentIntent = Intent(context, com.unoshield.mdm.ui.EnrollmentActivity::class.java).apply {
                putExtra("enrollment_id", enrollmentId)
                putExtra("enrollment_code", enrollmentCode)
                putExtra("base_url", baseUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            Log.d(TAG, "Starting EnrollmentActivity with enrollment data")
            context.startActivity(enrollmentIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing provisioning complete", e)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        // Handle provisioning complete action (sent as broadcast)
        // CRITICAL: This receiver must handle provisioning to be set as Device Owner
        if (intent.action == ACTION_PROVISIONING_COMPLETE) {
            Log.d(TAG, "Provisioning complete broadcast received in DeviceOwnerReceiver - processing enrollment data")
            handleProvisioningComplete(context, intent)
        } else {
            Log.d(TAG, "Ignoring action: ${intent.action}")
        }
    }
}

