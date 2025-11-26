package com.unoshield.mdm.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.unoshield.mdm.AdminReceiver
import com.unoshield.mdm.R
import com.unoshield.mdm.util.DeviceInfo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Activity - Shows device status and enrollment information
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var deviceIdText: TextView
    private lateinit var deviceStatusText: TextView
    private lateinit var lastSyncText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        deviceIdText = findViewById(R.id.device_id_text)
        deviceStatusText = findViewById(R.id.device_status_text)
        lastSyncText = findViewById(R.id.last_sync_text)
        
        updateDeviceInfo()
    }
    
    private fun updateDeviceInfo() {
        val deviceId = DeviceInfo.getDeviceId(this)
        deviceIdText.text = "Device ID: $deviceId"
        
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = AdminReceiver.getComponentName(this)
        
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        val isDeviceAdmin = devicePolicyManager.isAdminActive(adminComponent)
        
        val status = when {
            isDeviceOwner -> "Device Owner Mode - Active"
            isDeviceAdmin -> "Device Admin Mode - Active"
            else -> "Not Enrolled"
        }
        
        deviceStatusText.text = "Status: $status"
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        lastSyncText.text = "Last Sync: ${dateFormat.format(Date())}"
    }
}

