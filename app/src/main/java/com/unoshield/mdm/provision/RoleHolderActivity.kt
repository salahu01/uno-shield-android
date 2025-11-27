package com.unoshield.mdm.provision

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * Role Holder Activity for Android 10+ (especially Android 15)
 * Handles role holder provisioning intents for managed device setup
 * Android 15 requires this to be a proper Activity class, not android.app.Activity
 */
class RoleHolderActivity : Activity() {
    private val TAG = "RoleHolderActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "RoleHolderActivity started with action: ${intent.action}")
        
        // Android 15 requires proper handling of role holder intents
        // These intents are used during QR code provisioning
        when (intent.action) {
            "android.app.action.ROLE_HOLDER_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE" -> {
                Log.d(TAG, "Handling ROLE_HOLDER_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE")
                // This intent is called during QR code provisioning
                // The actual provisioning is handled by GetProvisioningModeActivity
                // We just need to acknowledge this intent
            }
            "android.app.action.ROLE_HOLDER_PROVISION_MANAGED_PROFILE" -> {
                Log.d(TAG, "Handling ROLE_HOLDER_PROVISION_MANAGED_PROFILE")
            }
            "android.app.action.ROLE_HOLDER_PROVISION_FINALIZATION" -> {
                Log.d(TAG, "Handling ROLE_HOLDER_PROVISION_FINALIZATION")
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
        
        // Finish immediately - this activity just needs to exist for Android 15
        // The actual provisioning flow is handled by GetProvisioningModeActivity
        finish()
    }
}

