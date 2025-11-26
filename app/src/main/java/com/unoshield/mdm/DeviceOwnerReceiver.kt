package com.unoshield.mdm

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Owner Receiver for UNO Shield MDM
 * Handles device owner mode events
 */
class DeviceOwnerReceiver : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "DeviceOwnerReceiver"
        
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, DeviceOwnerReceiver::class.java)
        }
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device owner enabled")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device owner disabled")
    }
}

