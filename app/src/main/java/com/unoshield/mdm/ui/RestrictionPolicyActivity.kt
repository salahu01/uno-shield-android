package com.unoshield.mdm.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.unoshield.mdm.AdminReceiver
import com.unoshield.mdm.DeviceOwnerReceiver
import com.unoshield.mdm.R

/**
 * Restriction Policy Activity - Manage DPC (Device Policy Controller) restrictions
 * Allows enabling/disabling various device restrictions using DevicePolicyManager
 * Requires Device Owner privileges for most restrictions
 */
class RestrictionPolicyActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PolicyAdapter
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var isDeviceOwner: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restriction_policy)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        
        // Check if app is device owner (required for user restrictions)
        // This checks at the package level, not component level
        isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        
        // Use DeviceOwnerReceiver component (this should be set as Device Owner during provisioning)
        adminComponent = DeviceOwnerReceiver.getComponentName(this)
        
        // Fallback: If DeviceOwnerReceiver is not active, check AdminReceiver (for backward compatibility)
        if (!isDeviceOwner) {
            val deviceOwnerAdmin = devicePolicyManager.isAdminActive(adminComponent)
            val adminReceiverComponent = AdminReceiver.getComponentName(this)
            val adminReceiverActive = devicePolicyManager.isAdminActive(adminReceiverComponent)
            
            // If AdminReceiver is active but DeviceOwnerReceiver is not, log a warning
            if (adminReceiverActive && !deviceOwnerAdmin) {
                android.util.Log.w("RestrictionPolicy", 
                    "AdminReceiver is active but DeviceOwnerReceiver is not. " +
                    "This may indicate a provisioning issue. Device Owner status: $isDeviceOwner")
            }
        }
        
        if (!isDeviceOwner) {
            val isDeviceAdmin = devicePolicyManager.isAdminActive(adminComponent) || 
                               devicePolicyManager.isAdminActive(AdminReceiver.getComponentName(this))
            
            val message = if (isDeviceAdmin) {
                "Device Admin is active, but Device Owner is required for these policies.\n\n" +
                "The device was enrolled but not as Device Owner.\n\n" +
                "To enable Device Owner:\n" +
                "1. Factory reset device\n" +
                "2. Scan QR code during initial setup (before completing setup wizard)\n" +
                "OR\n" +
                "3. Use ADB (if no user accounts exist):\n" +
                "   adb shell dpm set-device-owner com.unoshield.mdm/.DeviceOwnerReceiver\n\n" +
                "Note: QR code enrollment should automatically set Device Owner if done during factory reset."
            } else {
                "Device Owner privileges required.\n\n" +
                "The device was not enrolled as Device Owner.\n" +
                "Please factory reset and enroll during setup, or use ADB."
            }
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        
        setupToolbar()
        setupRecyclerView()
        loadPolicies()
        setupUnoEmmSettings()
        setupDeviceAdminSettings()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.restriction_policy)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.policies_recycler_view)
        // Use GridLayoutManager with 2 columns to match screenshot layout
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        
        adapter = PolicyAdapter(
            devicePolicyManager = devicePolicyManager,
            adminComponent = adminComponent,
            isDeviceOwner = isDeviceOwner,
            onPolicyChanged = { policy, enabled ->
                handlePolicyChange(policy, enabled)
            }
        )
        recyclerView.adapter = adapter
    }
    
    private fun loadPolicies() {
        // Restrictions ordered exactly as shown in the screenshot
        // Left Column (first 14 items)
        val policies = listOf(
            // 1. Disallow Camera
            PolicyItem(
                id = "disallow_camera",
                title = getString(R.string.disallow_camera),
                description = getString(R.string.disallow_camera_description),
                restrictionKey = getRestrictionKey("DISALLOW_CAMERA", "no_camera")
            ),
            // 2. Disable Microphone
            PolicyItem(
                id = "disallow_microphone",
                title = getString(R.string.disallow_microphone),
                description = getString(R.string.disallow_microphone_description),
                restrictionKey = getRestrictionKey("DISALLOW_UNMUTE_MICROPHONE", "no_unmute_microphone")
            ),
            // 3. Disable Cross Profile Copy Paste
            PolicyItem(
                id = "disallow_cross_profile_copy_paste",
                title = getString(R.string.disallow_cross_profile_copy_paste),
                description = getString(R.string.disallow_cross_profile_copy_paste_description),
                restrictionKey = getRestrictionKey("DISALLOW_CROSS_PROFILE_COPY_PASTE", "no_cross_profile_copy_paste")
            ),
            // 4. Disallow USB File Transfer
            PolicyItem(
                id = "disallow_usb_file_transfer",
                title = getString(R.string.disallow_usb_file_transfer),
                description = getString(R.string.disallow_usb_file_transfer_description),
                restrictionKey = getRestrictionKey("DISALLOW_USB_FILE_TRANSFER", "no_usb_file_transfer")
            ),
            // 5. Disallow Bluetooth Configuration
            PolicyItem(
                id = "disallow_config_bluetooth",
                title = getString(R.string.disallow_config_bluetooth),
                description = getString(R.string.disallow_config_bluetooth_description),
                restrictionKey = getRestrictionKey("DISALLOW_CONFIG_BLUETOOTH", "no_config_bluetooth")
            ),
            // 6. Disallow Factory Reset
            PolicyItem(
                id = "disallow_factory_reset",
                title = getString(R.string.disallow_factory_reset),
                description = getString(R.string.disallow_factory_reset_description),
                restrictionKey = getRestrictionKey("DISALLOW_FACTORY_RESET", "no_factory_reset")
            ),
            // 7. Disallow Data Roaming
            PolicyItem(
                id = "disallow_data_roaming",
                title = getString(R.string.disallow_data_roaming),
                description = getString(R.string.disallow_data_roaming_description),
                restrictionKey = getRestrictionKey("DISALLOW_DATA_ROAMING", "no_data_roaming")
            ),
            // 8. Disable Volume Control
            PolicyItem(
                id = "disallow_adjust_volume",
                title = getString(R.string.disallow_adjust_volume),
                description = getString(R.string.disallow_adjust_volume_description),
                restrictionKey = getRestrictionKey("DISALLOW_ADJUST_VOLUME", "no_adjust_volume")
            ),
            // 9. Disallow Outgoing Phone Calls
            PolicyItem(
                id = "disallow_outgoing_calls",
                title = getString(R.string.disallow_outgoing_calls),
                description = getString(R.string.disallow_outgoing_calls_description),
                restrictionKey = getRestrictionKey("DISALLOW_OUTGOING_CALLS", "no_outgoing_calls")
            ),
            // 10. Disable Status Bar
            PolicyItem(
                id = "disallow_status_bar",
                title = getString(R.string.disallow_status_bar),
                description = getString(R.string.disallow_status_bar_description),
                restrictionKey = getRestrictionKey("DISALLOW_STATUS_BAR", "no_status_bar")
            ),
            // 11. Disallow Adding Users
            PolicyItem(
                id = "disallow_add_user",
                title = getString(R.string.disallow_add_user),
                description = getString(R.string.disallow_add_user_description),
                restrictionKey = getRestrictionKey("DISALLOW_ADD_USER", "no_add_user")
            ),
            // 12. Disallow User Icon Change
            PolicyItem(
                id = "disallow_user_icon_change",
                title = getString(R.string.disallow_user_icon_change),
                description = getString(R.string.disallow_user_icon_change_description),
                restrictionKey = getRestrictionKey("DISALLOW_USER_ICON", "no_user_icon")
            ),
            // 13. Disallow App Installation Manually
            PolicyItem(
                id = "disallow_install_apps",
                title = getString(R.string.disallow_install_apps),
                description = getString(R.string.disallow_install_apps_description),
                restrictionKey = getRestrictionKey("DISALLOW_INSTALL_APPS", "no_install_apps")
            ),
            // 14. Disable Using NFC Beam
            PolicyItem(
                id = "disallow_nfc_beam",
                title = getString(R.string.disallow_nfc_beam),
                description = getString(R.string.disallow_nfc_beam_description),
                restrictionKey = getRestrictionKey("DISALLOW_NFC_BEAM", "no_nfc_beam")
            ),
            // Right Column (next 14 items)
            // 15. Disallow USB Tethering and Portable Hotspots
            PolicyItem(
                id = "disallow_usb_tethering",
                title = getString(R.string.disallow_usb_tethering),
                description = getString(R.string.disallow_usb_tethering_description),
                restrictionKey = getRestrictionKey("DISALLOW_CONFIG_TETHERING", "no_config_tethering")
            ),
            // 16. Disallow Screen Capture
            PolicyItem(
                id = "disallow_screen_capture",
                title = getString(R.string.disallow_screen_capture),
                description = getString(R.string.disallow_screen_capture_description),
                restrictionKey = getRestrictionKey("DISALLOW_SCREEN_CAPTURE", "no_screen_capture")
            ),
            // 17. Disable Developer Options
            PolicyItem(
                id = "disallow_debugging_features",
                title = getString(R.string.disallow_debugging_features),
                description = getString(R.string.disallow_debugging_features_description),
                restrictionKey = getRestrictionKey("DISALLOW_DEBUGGING_FEATURES", "no_debugging_features")
            ),
            // 18. Disable Bluetooth
            PolicyItem(
                id = "disallow_bluetooth",
                title = getString(R.string.disallow_bluetooth),
                description = getString(R.string.disallow_bluetooth_description),
                restrictionKey = getRestrictionKey("DISALLOW_BLUETOOTH", "no_bluetooth")
            ),
            // 19. Disallow Keyguard
            PolicyItem(
                id = "disallow_keyguard",
                title = getString(R.string.disallow_keyguard),
                description = getString(R.string.disallow_keyguard_description),
                restrictionKey = getRestrictionKey("DISALLOW_KEYGUARD", "no_keyguard")
            ),
            // 20. Disallow Reset Network Settings
            PolicyItem(
                id = "disallow_network_reset",
                title = getString(R.string.disallow_network_reset),
                description = getString(R.string.disallow_network_reset_description),
                restrictionKey = getRestrictionKey("DISALLOW_NETWORK_RESET", "no_network_reset")
            ),
            // 21. Disallow Mobile Networks Configuration
            PolicyItem(
                id = "disallow_config_mobile_networks",
                title = getString(R.string.disallow_config_mobile_networks),
                description = getString(R.string.disallow_config_mobile_networks_description),
                restrictionKey = getRestrictionKey("DISALLOW_CONFIG_MOBILE_NETWORKS", "no_config_mobile_networks")
            ),
            // 22. Disable Cell Broadcast
            PolicyItem(
                id = "disallow_cell_broadcast",
                title = getString(R.string.disallow_cell_broadcast),
                description = getString(R.string.disallow_cell_broadcast_description),
                restrictionKey = getRestrictionKey("DISALLOW_CELL_BROADCAST", "no_cell_broadcast")
            ),
            // 23. Disallow Send/Receive SMS
            PolicyItem(
                id = "disallow_sms",
                title = getString(R.string.disallow_sms),
                description = getString(R.string.disallow_sms_description),
                restrictionKey = getRestrictionKey("DISALLOW_SMS", "no_sms")
            ),
            // 24. Disallow Adding and Removing of Accounts
            PolicyItem(
                id = "disallow_modify_accounts",
                title = getString(R.string.disallow_modify_accounts),
                description = getString(R.string.disallow_modify_accounts_description),
                restrictionKey = getRestrictionKey("DISALLOW_MODIFY_ACCOUNTS", "no_modify_accounts")
            ),
            // 25. Disallow Removing Users
            PolicyItem(
                id = "disallow_remove_user",
                title = getString(R.string.disallow_remove_user),
                description = getString(R.string.disallow_remove_user_description),
                restrictionKey = getRestrictionKey("DISALLOW_REMOVE_USER", "no_remove_user")
            ),
            // 26. Disallow Wallpaper Change
            PolicyItem(
                id = "disallow_set_wallpaper",
                title = getString(R.string.disallow_set_wallpaper),
                description = getString(R.string.disallow_set_wallpaper_description),
                restrictionKey = getRestrictionKey("DISALLOW_SET_WALLPAPER", "no_set_wallpaper")
            ),
            // 27. Disallow Multi Window
            PolicyItem(
                id = "disallow_multi_window",
                title = getString(R.string.disallow_system_error_dialogs),
                description = getString(R.string.disallow_system_error_dialogs_description),
                restrictionKey = getRestrictionKey("DISALLOW_MULTI_WINDOW", "no_multi_window")
            ),
            // 28. Disable Bluetooth Contact Sharing
            PolicyItem(
                id = "disallow_bluetooth_sharing",
                title = getString(R.string.disallow_bluetooth_sharing),
                description = getString(R.string.disallow_bluetooth_sharing_description),
                restrictionKey = getRestrictionKey("DISALLOW_BLUETOOTH_SHARING", "no_bluetooth_sharing")
            )
        )
        adapter.submitList(policies)
    }
    
    private fun setupUnoEmmSettings() {
        val container = findViewById<ViewGroup>(R.id.uno_emm_settings_container)
        container.removeAllViews()
        
        val policies = listOf(
            Triple("disable_wifi_settings", getString(R.string.disable_wifi_settings), getRestrictionKey("DISALLOW_CONFIG_WIFI", "no_config_wifi")),
            Triple("disable_gps_setting", getString(R.string.disable_gps_setting), getRestrictionKey("DISALLOW_CONFIG_LOCATION", "no_config_location")),
            Triple("disallow_incoming_call", getString(R.string.disallow_incoming_call), getRestrictionKey("DISALLOW_INCOMING_CALLS", "no_incoming_calls"))
        )
        
        policies.forEach { (id, title, restrictionKey) ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_simple_policy, container, false)
            val switch: SwitchMaterial = view.findViewById(R.id.policy_switch)
            val titleText: TextView = view.findViewById(R.id.policy_title)
            
            titleText.text = title
            
            // Check current state
            val isRestricted = try {
                if (isDeviceOwner) {
                    devicePolicyManager.getUserRestrictions(adminComponent).getBoolean(restrictionKey, false)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
            
            switch.isEnabled = isDeviceOwner
            switch.isChecked = isRestricted
            
            switch.setOnCheckedChangeListener { _, isChecked ->
                handlePolicyChange(PolicyItem(id, title, "", restrictionKey), isChecked)
            }
            
            container.addView(view)
        }
    }
    
    private fun setupDeviceAdminSettings() {
        val container = findViewById<ViewGroup>(R.id.device_admin_settings_container)
        container.removeAllViews()
        
        val policies = listOf(
            Triple("disable_mount_physical_data", getString(R.string.disable_mount_physical_data), getRestrictionKey("DISALLOW_MOUNT_PHYSICAL_MEDIA", "no_mount_physical_media")),
            Triple("show_lock_screen_message", getString(R.string.show_lock_screen_message), ""), // Special handling needed
            Triple("disable_date_time_setting", getString(R.string.disable_date_time_setting), getRestrictionKey("DISALLOW_CONFIG_DATE_TIME", "no_config_date_time")),
            Triple("ensure_verify_apps_setting", getString(R.string.ensure_verify_apps_setting), ""), // Special handling needed
            Triple("disable_airplane_mode_setting", getString(R.string.disable_airplane_mode_setting), getRestrictionKey("DISALLOW_AIRPLANE_MODE", "no_airplane_mode")),
            Triple("disable_usb_data_access", getString(R.string.disable_usb_data_access), getRestrictionKey("DISALLOW_USB_FILE_TRANSFER", "no_usb_file_transfer"))
        )
        
        policies.forEach { (id, title, restrictionKey) ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_simple_policy, container, false)
            val switch: SwitchMaterial = view.findViewById(R.id.policy_switch)
            val titleText: TextView = view.findViewById(R.id.policy_title)
            
            titleText.text = title
            
            // Check current state
            val isRestricted = try {
                if (isDeviceOwner && restrictionKey.isNotEmpty()) {
                    devicePolicyManager.getUserRestrictions(adminComponent).getBoolean(restrictionKey, false)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
            
            switch.isEnabled = isDeviceOwner
            switch.isChecked = isRestricted
            
            switch.setOnCheckedChangeListener { _, isChecked ->
                if (restrictionKey.isNotEmpty()) {
                    handlePolicyChange(PolicyItem(id, title, "", restrictionKey), isChecked)
                } else {
                    handleSpecialPolicy(id, isChecked)
                }
            }
            
            container.addView(view)
        }
        
        // Add Location Mode dropdown
        val locationView = LayoutInflater.from(this).inflate(R.layout.item_location_mode, container, false)
        val spinner: Spinner = locationView.findViewById(R.id.location_mode_spinner)
        
        val locationModes = listOf(
            getString(R.string.location_mode_high_accuracy),
            getString(R.string.location_mode_device_only),
            getString(R.string.location_mode_battery_saving),
            getString(R.string.location_mode_off)
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationModes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        // Set current location mode
        try {
            val locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
            val selectedIndex = when (locationMode) {
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY -> 0
                Settings.Secure.LOCATION_MODE_SENSORS_ONLY -> 1
                Settings.Secure.LOCATION_MODE_BATTERY_SAVING -> 2
                Settings.Secure.LOCATION_MODE_OFF -> 3
                else -> 0
            }
            spinner.setSelection(selectedIndex)
        } catch (e: Exception) {
            spinner.setSelection(0) // Default to High Accuracy
        }
        
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isDeviceOwner) {
                    setLocationMode(position)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        spinner.isEnabled = isDeviceOwner
        container.addView(locationView)
    }
    
    private fun setLocationMode(position: Int) {
        try {
            val locationMode = when (position) {
                0 -> Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
                1 -> Settings.Secure.LOCATION_MODE_SENSORS_ONLY
                2 -> Settings.Secure.LOCATION_MODE_BATTERY_SAVING
                3 -> Settings.Secure.LOCATION_MODE_OFF
                else -> Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            }
            
            // Try to use DevicePolicyManager first (API 28+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val method = DevicePolicyManager::class.java.getMethod(
                        "setLocationEnabled",
                        ComponentName::class.java,
                        Boolean::class.java
                    )
                    val enabled = locationMode != Settings.Secure.LOCATION_MODE_OFF
                    method.invoke(devicePolicyManager, adminComponent, enabled)
                } catch (e: Exception) {
                    // Fallback to Settings.Secure
                    Settings.Secure.putInt(contentResolver, Settings.Secure.LOCATION_MODE, locationMode)
                }
            } else {
                Settings.Secure.putInt(contentResolver, Settings.Secure.LOCATION_MODE, locationMode)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting location mode: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleSpecialPolicy(id: String, enabled: Boolean) {
        if (!isDeviceOwner) {
            Toast.makeText(
                this,
                "Device Owner privileges required.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        try {
            when (id) {
                "show_lock_screen_message" -> {
                    // Set lock screen message
                    val message = if (enabled) "Device managed by UNO Shield MDM" else ""
                    devicePolicyManager.setDeviceOwnerLockScreenInfo(adminComponent, message)
                    Toast.makeText(this, if (enabled) "Lock screen message enabled" else "Lock screen message disabled", Toast.LENGTH_SHORT).show()
                }
                "ensure_verify_apps_setting" -> {
                    // Ensure verify apps is enabled
                    if (enabled) {
                        try {
                            // Use reflection to set package verifier (may not be available on all devices)
                            Settings.Global::class.java.getField("PACKAGE_VERIFIER_ENABLE")?.let { field ->
                                val constant = field.get(null) as String
                                Settings.Global.putInt(contentResolver, constant, 1)
                                Toast.makeText(this, "App verification enabled", Toast.LENGTH_SHORT).show()
                            } ?: run {
                                Toast.makeText(this, "App verification setting not available", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error enabling app verification: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error applying policy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getRestrictionKey(constantName: String, fallback: String): String {
        return try {
            val field = UserManager::class.java.getField(constantName)
            field.get(null) as String
        } catch (e: Exception) {
            fallback
        }
    }
    
    private fun handlePolicyChange(policy: PolicyItem, enabled: Boolean) {
        // Verify device owner status before applying restrictions
        if (!isDeviceOwner) {
            val isDeviceAdmin = devicePolicyManager.isAdminActive(adminComponent)
            
            val message = if (isDeviceAdmin) {
                "Device Admin is active, but Device Owner is required for this policy.\n\n" +
                "Device Owner can only be set:\n" +
                "• During factory reset (scan QR code during setup)\n" +
                "• Via ADB (if no user accounts exist)\n\n" +
                "Current enrollment method doesn't support Device Owner."
            } else {
                "Device Owner privileges required.\n\n" +
                "The device was not properly enrolled as Device Owner.\n" +
                "Please factory reset and enroll during setup."
            }
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            // Reset the switch to previous state
            adapter.notifyDataSetChanged()
            return
        }
        
        try {
            if (enabled) {
                devicePolicyManager.addUserRestriction(adminComponent, policy.restrictionKey)
                
                // Special handling for tethering/hotspots - also disable directly if possible
                if (policy.id == "disallow_usb_tethering") {
                    disableTetheringAndHotspots()
                }
                
                Toast.makeText(this, "${policy.title} enabled", Toast.LENGTH_SHORT).show()
            } else {
                devicePolicyManager.clearUserRestriction(adminComponent, policy.restrictionKey)
                Toast.makeText(this, "${policy.title} disabled", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied. Device must be enrolled as Device Owner.", Toast.LENGTH_LONG).show()
            // Reset the switch to previous state
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "Error applying policy: ${e.message}", Toast.LENGTH_SHORT).show()
            // Reset the switch to previous state
            adapter.notifyDataSetChanged()
        }
    }
    
    /**
     * Disable tethering and portable hotspots directly using DevicePolicyManager
     * This provides additional enforcement beyond user restrictions
     */
    private fun disableTetheringAndHotspots() {
        try {
            // Try to disable WiFi tethering using reflection (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val method = DevicePolicyManager::class.java.getMethod(
                        "setWifiTetheringDisabled",
                        ComponentName::class.java,
                        Boolean::class.java
                    )
                    method.invoke(devicePolicyManager, adminComponent, true)
                } catch (e: Exception) {
                    // Method not available, continue with restriction only
                }
            }
            
            // Try to disable USB tethering using reflection (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val method = DevicePolicyManager::class.java.getMethod(
                        "setUsbTetheringDisabled",
                        ComponentName::class.java,
                        Boolean::class.java
                    )
                    method.invoke(devicePolicyManager, adminComponent, true)
                } catch (e: Exception) {
                    // Method not available, continue with restriction only
                }
            }
        } catch (e: Exception) {
            // If direct methods fail, user restriction should still work
        }
    }
    
    /**
     * Data class representing a DPC policy restriction
     */
    data class PolicyItem(
        val id: String,
        val title: String,
        val description: String,
        val restrictionKey: String
    )
    
    /**
     * Adapter for displaying policy restrictions
     */
    private class PolicyAdapter(
        private val devicePolicyManager: DevicePolicyManager,
        private val adminComponent: ComponentName,
        private val isDeviceOwner: Boolean,
        private val onPolicyChanged: (PolicyItem, Boolean) -> Unit
    ) : RecyclerView.Adapter<PolicyAdapter.PolicyViewHolder>() {
        
        private var policies = emptyList<PolicyItem>()
        
        fun submitList(newPolicies: List<PolicyItem>) {
            policies = newPolicies
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PolicyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_policy_restriction, parent, false)
            return PolicyViewHolder(view, devicePolicyManager, adminComponent, isDeviceOwner, onPolicyChanged)
        }
        
        override fun onBindViewHolder(holder: PolicyViewHolder, position: Int) {
            holder.bind(policies[position])
        }
        
        override fun getItemCount(): Int = policies.size
        
        class PolicyViewHolder(
            itemView: View,
            private val devicePolicyManager: DevicePolicyManager,
            private val adminComponent: ComponentName,
            private val isDeviceOwner: Boolean,
            private val onPolicyChanged: (PolicyItem, Boolean) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            
            private val titleText: TextView = itemView.findViewById(R.id.policy_title)
            private val descriptionText: TextView = itemView.findViewById(R.id.policy_description)
            private val policySwitch: SwitchMaterial = itemView.findViewById(R.id.policy_switch)
            
            fun bind(policy: PolicyItem) {
                titleText.text = policy.title
                descriptionText.text = policy.description
                
                // Check current state of the restriction
                val isRestricted = try {
                    if (isDeviceOwner) {
                        devicePolicyManager.getUserRestrictions(adminComponent).getBoolean(policy.restrictionKey, false)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
                
                // Disable switch if not device owner
                policySwitch.isEnabled = isDeviceOwner
                
                // Remove previous listener to avoid infinite loops
                policySwitch.setOnCheckedChangeListener(null)
                
                // Set current state
                policySwitch.isChecked = isRestricted
                
                // Set new listener
                policySwitch.setOnCheckedChangeListener { _, isChecked ->
                    onPolicyChanged(policy, isChecked)
                }
            }
        }
    }
}

