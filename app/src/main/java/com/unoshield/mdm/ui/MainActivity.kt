package com.unoshield.mdm.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.unoshield.mdm.AdminReceiver
import com.unoshield.mdm.R
import com.unoshield.mdm.util.DeviceInfo
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Activity - Shows device status and MDM client controls
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var lastSyncText: TextView
    private lateinit var syncButton: MaterialButton
    private lateinit var appEnableSwitch: SwitchMaterial
    private lateinit var enableNotificationsSwitch: SwitchMaterial
    private lateinit var serverInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var deviceSerialText: TextView
    
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
        
        // Check device admin status (for future use)
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = AdminReceiver.getComponentName(this)
        
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        val isDeviceAdmin = devicePolicyManager.isAdminActive(adminComponent)
        
        // You can use these statuses to enable/disable features
        // For now, we'll just log them
        if (isDeviceOwner || isDeviceAdmin) {
            // Device is enrolled, can enable MDM features
        }
    }
}
