package com.unoshield.mdm.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.unoshield.mdm.DeviceOwnerReceiver
import com.unoshield.mdm.R
import com.unoshield.mdm.util.DeviceInfo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Settings Activity - Main activity showing device status and MDM client controls
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var lastSyncText: TextView
    private lateinit var syncButton: MaterialButton
    private lateinit var appEnableSwitch: SwitchMaterial
    private lateinit var enableNotificationsSwitch: SwitchMaterial
    private lateinit var serverInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var deviceSerialText: TextView
    private lateinit var serverManagerButton: MaterialButton
    private lateinit var deviceOwnerStatusText: TextView
    private lateinit var debugSectionTitle: TextView
    private lateinit var debugAdbCommand: TextView
    private lateinit var debugStatusInfo: TextView
    
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        private const val PREFS_NAME = "mdm_prefs"
        private const val KEY_APP_ENABLED = "app_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SERVER = "server"
        private const val KEY_PORT = "port"
        private const val KEY_LAST_SYNC = "last_sync"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        initializeViews()
        loadSavedPreferences()
        setupListeners()
        updateDeviceInfo()
    }
    
    private fun initializeViews() {
        lastSyncText = findViewById(R.id.last_sync_text)
        syncButton = findViewById(R.id.sync_button)
        appEnableSwitch = findViewById(R.id.app_enable_switch)
        enableNotificationsSwitch = findViewById(R.id.enable_notifications_switch)
        serverInput = findViewById(R.id.server_input)
        portInput = findViewById(R.id.port_input)
        deviceSerialText = findViewById(R.id.device_serial_text)
        serverManagerButton = findViewById(R.id.server_manager_button)
        deviceOwnerStatusText = findViewById(R.id.device_owner_status)
        debugSectionTitle = findViewById(R.id.debug_section_title)
        debugAdbCommand = findViewById(R.id.debug_adb_command)
        debugStatusInfo = findViewById(R.id.debug_status_info)
    }
    
    private fun loadSavedPreferences() {
        // Load app enabled state
        appEnableSwitch.isChecked = sharedPreferences.getBoolean(KEY_APP_ENABLED, true)
        
        // Load notifications enabled state
        enableNotificationsSwitch.isChecked = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        
        // Load server and port
        serverInput.setText(sharedPreferences.getString(KEY_SERVER, "112.36.15.12"))
        portInput.setText(sharedPreferences.getString(KEY_PORT, "2222"))
        
        // Load last sync time
        val lastSyncTime = sharedPreferences.getString(KEY_LAST_SYNC, null)
        if (lastSyncTime != null) {
            lastSyncText.text = getString(R.string.last_sync_time, lastSyncTime)
        } else {
            updateLastSyncTime()
        }
    }
    
    private fun setupListeners() {
        // App Enable Switch
        appEnableSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(KEY_APP_ENABLED, isChecked)
                .apply()
            
            val message = if (isChecked) "App enabled" else "App disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // Notifications Switch
        enableNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked)
                .apply()
            
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // Sync Button
        syncButton.setOnClickListener {
            performSync()
        }
        
        // Server Input
        serverInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val server = serverInput.text?.toString() ?: ""
                sharedPreferences.edit()
                    .putString(KEY_SERVER, server)
                    .apply()
            }
        }
        
        // Port Input
        portInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val port = portInput.text?.toString() ?: ""
                sharedPreferences.edit()
                    .putString(KEY_PORT, port)
                    .apply()
            }
        }
        
        // Server Manager Button
        serverManagerButton.setOnClickListener {
            val intent = Intent(this, ServerActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun performSync() {
        // Show loading state
        syncButton.isEnabled = false
        syncButton.text = "Syncing..."
        
        // Simulate sync operation (replace with actual sync logic)
        syncButton.postDelayed({
            updateLastSyncTime()
            syncButton.isEnabled = true
            syncButton.text = getString(R.string.sync)
            Toast.makeText(this, "Sync completed successfully", Toast.LENGTH_SHORT).show()
        }, 1500)
    }
    
    private fun updateLastSyncTime() {
        val dateFormat = SimpleDateFormat("h:mm a EEE d MMM yyyy", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        
        lastSyncText.text = getString(R.string.last_sync_time, currentTime)
        
        sharedPreferences.edit()
            .putString(KEY_LAST_SYNC, currentTime)
            .apply()
    }
    
    private fun updateDeviceInfo() {
        // Get device serial number
        val serialNumber = DeviceInfo.getSerialNumber()
        deviceSerialText.text = serialNumber
        
        // Check device admin and device owner status
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = DeviceOwnerReceiver.getComponentName(this)
        
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        val isDeviceAdmin = devicePolicyManager.isAdminActive(adminComponent)
        
        // Update Device Owner status indicator
        val statusText = if (isDeviceOwner) {
            "✓ Device Owner: Active - All policies available"
        } else if (isDeviceAdmin) {
            "⚠ Device Admin: Active - Device Owner required for some policies"
        } else {
            "✗ Not Enrolled - Device Admin/Owner not active"
        }
        
        val statusColor = if (isDeviceOwner) {
            ContextCompat.getColor(this, android.R.color.holo_green_dark)
        } else if (isDeviceAdmin) {
            ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.holo_red_dark)
        }
        
        deviceOwnerStatusText.text = statusText
        deviceOwnerStatusText.setTextColor(statusColor)
        
        // Show debug info in debug builds (check using ApplicationInfo flags)
        val isDebugBuild = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebugBuild) {
            debugSectionTitle.visibility = android.view.View.VISIBLE
            debugAdbCommand.visibility = android.view.View.VISIBLE
            debugStatusInfo.visibility = android.view.View.VISIBLE
            
            debugAdbCommand.text = "adb shell dpm set-device-owner com.unoshield.mdm/.DeviceOwnerReceiver"
            
            val debugInfo = buildString {
                append("Package: ${packageName}\n")
                append("Component: com.unoshield.mdm/.DeviceOwnerReceiver\n")
                append("Device Owner: $isDeviceOwner\n")
                append("Device Admin: $isDeviceAdmin\n")
                if (!isDeviceOwner) {
                    append("\n⚠ To set Device Owner:\n")
                    append("1. Remove all user accounts\n")
                    append("2. Run the ADB command above\n")
                    append("3. Or factory reset and scan QR code")
                }
            }
            debugStatusInfo.text = debugInfo
        } else {
            debugSectionTitle.visibility = android.view.View.GONE
            debugAdbCommand.visibility = android.view.View.GONE
            debugStatusInfo.visibility = android.view.View.GONE
        }
    }
}

