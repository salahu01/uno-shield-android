package com.unoshield.mdm.provision

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.unoshield.mdm.AdminReceiver
import com.unoshield.mdm.ui.EnrollmentActivity

/**
 * Activity that gets launched by the android.app.admin.DevicePolicyManager#ACTION_PROVISIONING_SUCCESSFUL intent.
 * This is called after provisioning completes successfully.
 */
class ProvisioningSuccessActivity : Activity() {
    private val TAG = "ProvisioningSuccess"

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        Log.d(TAG, "ProvisioningSuccessActivity started")
        Log.d(TAG, "Android version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent extras keys: ${intent.extras?.keySet()}")

        try {
            // Extract enrollment data from provisioning extras
            val extras = intent.getBundleExtra(android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)

            if (extras == null) {
                Log.e(TAG, "No provisioning extras bundle found!")
                Log.e(TAG, "This may cause 'something went wrong' error on Android 15")
                // Android 15 requires proper error handling - don't crash
                finish()
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
                Log.e(TAG, "Android 15 requires all enrollment data to be present")
                // Android 15 will show error if data is missing
                finish()
                return
            }

            // Start enrollment activity to register device
            val enrollmentIntent = Intent(this, EnrollmentActivity::class.java).apply {
                putExtra("enrollment_id", enrollmentId)
                putExtra("enrollment_code", enrollmentCode)
                putExtra("base_url", baseUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            Log.d(TAG, "Starting EnrollmentActivity with enrollment data")
            startActivity(enrollmentIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing provisioning success", e)
            // Android 15 requires proper error handling
            // Don't let exceptions crash the provisioning flow
        } finally {
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ProvisioningSuccessActivity destroyed")
    }
}

