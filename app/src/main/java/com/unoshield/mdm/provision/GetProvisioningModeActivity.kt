package com.unoshield.mdm.provision

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * Activity that gets launched by the android.app.admin.DevicePolicyManager#ACTION_GET_PROVISIONING_MODE intent.
 * This is critical for QR code enrollment - Android calls this to determine the provisioning mode.
 */
@SuppressLint("NewApi")
class GetProvisioningModeActivity : Activity() {

    private val TAG = "GetProvisioningMode"

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Get allowed provisioning modes from intent
        val allowedModes = intent.getIntegerArrayListExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES
        ) ?: arrayListOf(
            DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        )

        Log.d(TAG, "GetProvisioningModeActivity called with allowed modes: $allowedModes")

        // For QR code enrollment, we typically want fully managed device
        // But we'll check what's allowed and prefer fully managed device
        val provisioningMode = if (allowedModes.contains(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)) {
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        } else if (allowedModes.contains(DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE)) {
            DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
        } else {
            // Default to managed profile if nothing else is available
            DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
        }

        Log.d(TAG, "Selected provisioning mode: $provisioningMode")
        
        // For FULLY_MANAGED_DEVICE mode, Android will automatically set the component that handles
        // provisioning as Device Owner. DeviceOwnerReceiver now handles provisioning events,
        // so it will be set as Device Owner.
        
        // Return the provisioning mode to Android
        val resultIntent = Intent().apply {
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, provisioningMode)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}

