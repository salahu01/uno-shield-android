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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unoshield.mdm.DeviceOwnerReceiver
import com.unoshield.mdm.R
import com.unoshield.mdm.data.MDMDatabase
import kotlinx.coroutines.launch

/**
 * Application Control Activity - Control app access using DevicePolicyManager
 * Supports hiding apps, suspending apps, and allow/block lists
 * Works at system level - doesn't depend on being the default launcher
 */
class ApplicationControlActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppControlAdapter
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var database: MDMDatabase
    private var isDeviceOwner: Boolean = false
    
    private val installedApps = mutableListOf<AppControlInfo>()
    private val blockedPackageNames = mutableSetOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_control)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = DeviceOwnerReceiver.getComponentName(this)
        database = MDMDatabase.getDatabase(this)
        isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        
        setupToolbar()
        initializeViews()
        setupListeners()
        loadBlockedApps()
        loadInstalledApps()
    }
    
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.application_control)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.apps_recycler_view)
        
        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        appAdapter = AppControlAdapter(
            blockedPackageNames = blockedPackageNames,
            isDeviceOwner = isDeviceOwner,
            onAppCheckedChange = { packageName, isBlocked ->
                handleAppBlockChange(packageName, isBlocked)
            }
        )
        recyclerView.adapter = appAdapter
        
        // Disable controls if not device owner
        if (!isDeviceOwner) {
            Toast.makeText(
                this,
                "Device Owner privileges required for application control",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun setupListeners() {
        // No listeners needed
    }
    
    private fun loadBlockedApps() {
        lifecycleScope.launch {
            // Load blocked apps from database
            val blockedApps = database.blockedAppDao().getAllBlockedPackageNames()
            blockedPackageNames.clear()
            blockedPackageNames.addAll(blockedApps)
            
            // Also check for hidden apps (blocked via DevicePolicyManager)
            if (isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val packageManager = packageManager
                val allPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
                for (packageInfo in allPackages) {
                    try {
                        if (devicePolicyManager.isApplicationHidden(adminComponent, packageInfo.packageName)) {
                            blockedPackageNames.add(packageInfo.packageName)
                        }
                    } catch (e: Exception) {
                        // Ignore errors
                    }
                }
            }
            
            appAdapter.notifyDataSetChanged()
        }
    }
    
    private fun loadInstalledApps() {
        lifecycleScope.launch {
            val packageManager = packageManager
            val apps = mutableListOf<AppControlInfo>()
            
            // Query all installed packages - use 0 to get ALL packages including system apps
            // MATCH_DISABLED_COMPONENTS includes hidden/disabled apps
            val flags = PackageManager.MATCH_DISABLED_COMPONENTS or 
                       PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS or
                       PackageManager.GET_META_DATA
            val allPackages = packageManager.getInstalledPackages(flags)
            
            android.util.Log.d("ApplicationControl", "Total packages found: ${allPackages.size}")
            
            // Get launcher apps for better icon/label resolution
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = try {
                packageManager.queryIntentActivities(
                    launcherIntent, 
                    PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_COMPONENTS
                )
            } catch (e: Exception) {
                android.util.Log.w("ApplicationControl", "Failed to query launcher apps: ${e.message}")
                emptyList()
            }
            val launcherPackageMap = launcherApps.associateBy { it.activityInfo.packageName }
            
            var skippedCount = 0
            var errorCount = 0
            
            // Add ALL packages (including system apps and hidden apps)
            for (packageInfo in allPackages) {
                // Skip our own app
                if (packageInfo.packageName == packageName) {
                    skippedCount++
                    continue
                }
                
                val applicationInfo = packageInfo.applicationInfo
                if (applicationInfo == null) {
                    skippedCount++
                    continue
                }
                
                // Only show user-installed apps (exclude pure system apps)
                // FLAG_SYSTEM means app is part of system image
                // FLAG_UPDATED_SYSTEM_APP means system app that was updated (user can manage these)
                val isSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val hasLauncherActivity = launcherPackageMap.containsKey(packageInfo.packageName)
                
                // Include if:
                // 1. Not a system app (user-installed), OR
                // 2. Is an updated system app (user-manageable), OR
                // 3. Has a launcher activity (user can interact with it, even if system)
                val isUserInstalled = !isSystemApp || isUpdatedSystemApp
                val shouldInclude = isUserInstalled || hasLauncherActivity
                
                if (!shouldInclude) {
                    skippedCount++
                    android.util.Log.d("ApplicationControl", "Skipping system app: ${packageInfo.packageName} (flags: ${applicationInfo.flags}, hasLauncher: $hasLauncherActivity)")
                    continue
                }
                
                android.util.Log.d("ApplicationControl", "Including app: ${packageInfo.packageName} (system: $isSystemApp, updated: $isUpdatedSystemApp, hasLauncher: $hasLauncherActivity)")
                
                // Check if app is hidden (even if hidden, we want to show it in the list)
                val isHidden = if (isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    try {
                        devicePolicyManager.isApplicationHidden(adminComponent, packageInfo.packageName)
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
                
                try {
                    var appLabel: String
                    var appIcon: Drawable
                    
                    // Try to get info from launcher ResolveInfo first (better icon/label)
                    val resolveInfo = launcherPackageMap[packageInfo.packageName]
                    if (resolveInfo != null) {
                        appLabel = resolveInfo.loadLabel(packageManager).toString()
                        appIcon = resolveInfo.loadIcon(packageManager)
                    } else {
                        // For non-launcher apps, get info from PackageInfo
                        appLabel = try {
                            packageManager.getApplicationLabel(applicationInfo).toString()
                        } catch (e: Exception) {
                            packageInfo.packageName // Fallback to package name
                        }
                        
                        appIcon = try {
                            packageManager.getApplicationIcon(packageInfo.packageName)
                        } catch (e: Exception) {
                            // Fallback to default icon if we can't get the app icon
                            try {
                                // Try to get default Android icon
                                applicationInfo.loadIcon(packageManager)
                            } catch (e2: Exception) {
                                // Last resort - use a simple drawable
                                ContextCompat.getDrawable(this@ApplicationControlActivity, android.R.drawable.ic_dialog_info) 
                                    ?: ContextCompat.getDrawable(this@ApplicationControlActivity, android.R.drawable.ic_menu_more)!!
                            }
                        }
                    }
                    
                    // Always add the app (use package name if label is blank)
                    val displayName = if (appLabel.isNotBlank()) {
                        appLabel
                    } else {
                        packageInfo.packageName
                    }
                    
                    apps.add(AppControlInfo(
                        packageName = packageInfo.packageName,
                        name = displayName,
                        icon = appIcon,
                        isHidden = isHidden
                    ))
                } catch (e: Exception) {
                    // Log but still try to add with package name
                    errorCount++
                    android.util.Log.w("ApplicationControl", "Error loading app ${packageInfo.packageName}: ${e.message}")
                    try {
                        apps.add(AppControlInfo(
                            packageName = packageInfo.packageName,
                            name = packageInfo.packageName,
                            icon = ContextCompat.getDrawable(this@ApplicationControlActivity, android.R.drawable.ic_dialog_info)!!,
                            isHidden = isHidden
                        ))
                    } catch (e2: Exception) {
                        android.util.Log.e("ApplicationControl", "Failed to add app ${packageInfo.packageName}: ${e2.message}")
                    }
                }
            }
            
            android.util.Log.d("ApplicationControl", "Loaded ${apps.size} apps (skipped: $skippedCount, errors: $errorCount)")
            
            apps.sortBy { it.name.lowercase() }
            
            installedApps.clear()
            installedApps.addAll(apps)
            appAdapter.submitList(installedApps)
            
            // Show toast with count for debugging
            runOnUiThread {
                Toast.makeText(
                    this@ApplicationControlActivity,
                    "Loaded ${apps.size} applications",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun handleAppBlockChange(packageName: String, isBlocked: Boolean) {
        if (!isDeviceOwner) {
            Toast.makeText(this, "Device Owner privileges required", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                if (isBlocked) {
                    blockApp(packageName)
                } else {
                    unblockApp(packageName)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ApplicationControlActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun blockApp(packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                // Hide the app
                devicePolicyManager.setApplicationHidden(adminComponent, packageName, true)
                
                // Suspend the app (Android 7.0+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val packages = arrayOf(packageName)
                    devicePolicyManager.setPackagesSuspended(adminComponent, packages, true)
                }
                
                // Save to database
                val appInfo = installedApps.find { it.packageName == packageName }
                val appName = appInfo?.name ?: packageName
                val blockedApp = com.unoshield.mdm.data.BlockedApp(
                    packageName = packageName,
                    appName = appName
                )
                database.blockedAppDao().insertBlockedApp(blockedApp)
                
                blockedPackageNames.add(packageName)
                appAdapter.notifyDataSetChanged()
                
                Toast.makeText(this, "App blocked", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error blocking app: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun unblockApp(packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                // Unhide the app
                devicePolicyManager.setApplicationHidden(adminComponent, packageName, false)
                
                // Unsuspend the app (Android 7.0+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val packages = arrayOf(packageName)
                    devicePolicyManager.setPackagesSuspended(adminComponent, packages, false)
                }
                
                // Remove from database
                database.blockedAppDao().deleteBlockedAppByPackageName(packageName)
                
                blockedPackageNames.remove(packageName)
                appAdapter.notifyDataSetChanged()
                
                Toast.makeText(this, "App unblocked", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error unblocking app: ${e.message}", Toast.LENGTH_SHORT).show()
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
        private val blockedPackageNames: MutableSet<String>,
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
            return AppViewHolder(view, blockedPackageNames, isDeviceOwner, onAppCheckedChange)
        }
        
        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(apps[position])
        }
        
        override fun getItemCount(): Int = apps.size
        
        class AppViewHolder(
            itemView: View,
            private val blockedPackageNames: MutableSet<String>,
            private val isDeviceOwner: Boolean,
            private val onAppCheckedChange: (String, Boolean) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            
            private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
            private val appName: TextView = itemView.findViewById(R.id.app_name)
            private val blockCheckbox: CheckBox = itemView.findViewById(R.id.block_checkbox)
            
            fun bind(appInfo: AppControlInfo) {
                appIcon.setImageDrawable(appInfo.icon)
                appName.text = appInfo.name
                
                // Check if app is blocked (hidden or in blocked list)
                val isBlocked = appInfo.isHidden || blockedPackageNames.contains(appInfo.packageName)
                
                blockCheckbox.isChecked = isBlocked
                blockCheckbox.isEnabled = isDeviceOwner
                blockCheckbox.text = "Block"
                
                blockCheckbox.setOnCheckedChangeListener(null)
                blockCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    onAppCheckedChange(appInfo.packageName, isChecked)
                }
            }
        }
    }
}

