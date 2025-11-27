package com.unoshield.mdm.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unoshield.mdm.DeviceOwnerReceiver
import com.unoshield.mdm.R
import com.unoshield.mdm.data.MDMDatabase
import kotlinx.coroutines.launch

/**
 * App Control List Activity - Manage allowlist or blocklist of apps
 * Uses DevicePolicyManager to hide/suspend apps at system level
 */
class AppControlListActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppControlAdapter
    private lateinit var database: MDMDatabase
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    private val installedApps = mutableListOf<AppControlInfo>()
    private val controlledPackageNames = mutableSetOf<String>()
    private var controlMode: String = "blocklist" // "allowlist" or "blocklist"
    private var isDeviceOwner: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_apps)
        
        controlMode = intent.getStringExtra("mode") ?: "blocklist"
        
        database = MDMDatabase.getDatabase(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = DeviceOwnerReceiver.getComponentName(this)
        isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        
        setupToolbar()
        setupRecyclerView()
        loadControlledApps()
        loadInstalledApps()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (controlMode == "allowlist") {
            "Allowed Apps"
        } else {
            "Blocked Apps"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.apps_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        appAdapter = AppControlAdapter(
            controlMode = controlMode,
            controlledPackageNames = controlledPackageNames,
            isDeviceOwner = isDeviceOwner,
            onAppCheckedChange = { packageName, isControlled ->
                handleAppControlChange(packageName, isControlled)
            }
        )
        recyclerView.adapter = appAdapter
    }
    
    private fun loadControlledApps() {
        lifecycleScope.launch {
            if (controlMode == "allowlist") {
                // In allowlist mode, check which apps are currently visible (not hidden)
                // These are the allowed apps
                controlledPackageNames.clear()
                // We'll populate this by checking which apps are not hidden
            } else {
                // Load blocked apps (hidden apps)
                val blockedApps = database.blockedAppDao().getAllBlockedPackageNames()
                controlledPackageNames.clear()
                controlledPackageNames.addAll(blockedApps)
            }
            appAdapter.notifyDataSetChanged()
        }
    }
    
    private fun loadInstalledApps() {
        val packageManager = packageManager
        val apps = mutableListOf<AppControlInfo>()
        
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )
        
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val applicationInfo = activityInfo.applicationInfo
            
            // Skip our own app
            if (activityInfo.packageName == packageName) {
                continue
            }
            
            // Check if app is hidden
            val isHidden = if (isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    devicePolicyManager.isApplicationHidden(adminComponent, activityInfo.packageName)
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
            
            // In allowlist mode: if hidden, it's not in allowlist
            // In blocklist mode: if hidden, it's blocked
            if (controlMode == "allowlist" && !isHidden) {
                controlledPackageNames.add(activityInfo.packageName)
            } else if (controlMode == "blocklist" && isHidden) {
                controlledPackageNames.add(activityInfo.packageName)
            }
            
            val appInfo = AppControlInfo(
                packageName = activityInfo.packageName,
                name = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager),
                isHidden = isHidden
            )
            
            apps.add(appInfo)
        }
        
        apps.sortBy { it.name.lowercase() }
        
        installedApps.clear()
        installedApps.addAll(apps)
        appAdapter.submitList(installedApps)
    }
    
    private fun handleAppControlChange(packageName: String, isControlled: Boolean) {
        if (!isDeviceOwner) {
            Toast.makeText(this, "Device Owner privileges required", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                if (controlMode == "allowlist") {
                    handleAllowlistChange(packageName, isControlled)
                } else {
                    handleBlocklistChange(packageName, isControlled)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AppControlListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun handleAllowlistChange(packageName: String, isAllowed: Boolean) {
        if (isAllowed) {
            // Show/unhide the app (add to allowlist)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    devicePolicyManager.setApplicationHidden(adminComponent, packageName, false)
                    controlledPackageNames.add(packageName)
                    
                    // Also unsuspend if suspended
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val packages = arrayOf(packageName)
                        devicePolicyManager.setPackagesSuspended(adminComponent, packages, false)
                    }
                    
                    Toast.makeText(this, "App added to allowlist", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error allowing app: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Hide the app (remove from allowlist)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    devicePolicyManager.setApplicationHidden(adminComponent, packageName, true)
                    controlledPackageNames.remove(packageName)
                    Toast.makeText(this, "App removed from allowlist", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error hiding app: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private suspend fun handleBlocklistChange(packageName: String, isBlocked: Boolean) {
        if (isBlocked) {
            // Hide or suspend the app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    // Hide the app
                    devicePolicyManager.setApplicationHidden(adminComponent, packageName, true)
                    
                    // Also suspend the app if Android 7.0+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val packages = arrayOf(packageName)
                        devicePolicyManager.setPackagesSuspended(adminComponent, packages, true)
                    }
                    
                    controlledPackageNames.add(packageName)
                    Toast.makeText(this, "App blocked", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error blocking app: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Unhide and unsuspend the app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    devicePolicyManager.setApplicationHidden(adminComponent, packageName, false)
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val packages = arrayOf(packageName)
                        devicePolicyManager.setPackagesSuspended(adminComponent, packages, false)
                    }
                    
                    controlledPackageNames.remove(packageName)
                    Toast.makeText(this, "App unblocked", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error unblocking app: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    data class AppControlInfo(
        val packageName: String,
        val name: String,
        val icon: Drawable,
        val isHidden: Boolean = false
    )
    
    private class AppControlAdapter(
        private val controlMode: String,
        private val controlledPackageNames: MutableSet<String>,
        private val isDeviceOwner: Boolean,
        private val onAppCheckedChange: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppControlAdapter.AppViewHolder>() {
        
        private var apps = emptyList<AppControlInfo>()
        
        fun submitList(newApps: List<AppControlInfo>) {
            apps = newApps
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blockable_app, parent, false)
            return AppViewHolder(view, controlMode, controlledPackageNames, isDeviceOwner, onAppCheckedChange)
        }
        
        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(apps[position])
        }
        
        override fun getItemCount(): Int = apps.size
        
        class AppViewHolder(
            itemView: View,
            private val controlMode: String,
            private val controlledPackageNames: MutableSet<String>,
            private val isDeviceOwner: Boolean,
            private val onAppCheckedChange: (String, Boolean) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            
            private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
            private val appName: TextView = itemView.findViewById(R.id.app_name)
            private val blockCheckbox: CheckBox = itemView.findViewById(R.id.block_checkbox)
            
            fun bind(appInfo: AppControlInfo) {
                appIcon.setImageDrawable(appInfo.icon)
                appName.text = appInfo.name
                
                // In allowlist mode: checked = allowed (visible), unchecked = hidden
                // In blocklist mode: checked = blocked (hidden), unchecked = allowed (visible)
                val isControlled = if (controlMode == "allowlist") {
                    // In allowlist: checked means app is allowed (visible)
                    !appInfo.isHidden || controlledPackageNames.contains(appInfo.packageName)
                } else {
                    // In blocklist: checked means app is blocked (hidden)
                    appInfo.isHidden || controlledPackageNames.contains(appInfo.packageName)
                }
                
                blockCheckbox.isChecked = isControlled
                blockCheckbox.isEnabled = isDeviceOwner
                blockCheckbox.text = if (controlMode == "allowlist") {
                    "Allow"
                } else {
                    "Block"
                }
                
                blockCheckbox.setOnCheckedChangeListener(null)
                blockCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    onAppCheckedChange(appInfo.packageName, isChecked)
                }
            }
        }
    }
}

