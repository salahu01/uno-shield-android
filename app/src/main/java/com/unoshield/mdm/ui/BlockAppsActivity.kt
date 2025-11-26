package com.unoshield.mdm.ui

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unoshield.mdm.R
import com.unoshield.mdm.data.BlockedApp
import com.unoshield.mdm.data.MDMDatabase
import kotlinx.coroutines.launch

/**
 * Block Apps Activity - Allows selecting which apps to block
 * This simulates server data - in the future, this will come from the server
 */
class BlockAppsActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: BlockableAppAdapter
    private lateinit var database: MDMDatabase
    
    private val installedApps = mutableListOf<BlockableAppInfo>()
    private val blockedPackageNames = mutableSetOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_apps)
        
        database = MDMDatabase.getDatabase(this)
        
        setupToolbar()
        setupRecyclerView()
        loadBlockedApps()
        loadInstalledApps()
    }
    
    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.block_apps)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.apps_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        appAdapter = BlockableAppAdapter(
            blockedPackageNames = blockedPackageNames,
            onAppCheckedChange = { packageName, isBlocked ->
                handleAppBlockChange(packageName, isBlocked)
            }
        )
        recyclerView.adapter = appAdapter
    }
    
    private fun loadBlockedApps() {
        lifecycleScope.launch {
            val blockedApps = database.blockedAppDao().getAllBlockedPackageNames()
            blockedPackageNames.clear()
            blockedPackageNames.addAll(blockedApps)
            appAdapter.notifyDataSetChanged()
        }
    }
    
    private fun loadInstalledApps() {
        val packageManager = packageManager
        val apps = mutableListOf<BlockableAppInfo>()
        
        // Query for all apps that can be launched
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
            
            // Skip our own app activities
            if (activityInfo.packageName == packageName) {
                continue
            }
            
            val appInfo = BlockableAppInfo(
                packageName = activityInfo.packageName,
                name = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager)
            )
            
            apps.add(appInfo)
        }
        
        // Sort apps alphabetically
        apps.sortBy { it.name.lowercase() }
        
        installedApps.clear()
        installedApps.addAll(apps)
        appAdapter.submitList(installedApps)
    }
    
    private fun handleAppBlockChange(packageName: String, isBlocked: Boolean) {
        lifecycleScope.launch {
            if (isBlocked) {
                // Find app name
                val appInfo = installedApps.find { it.packageName == packageName }
                val appName = appInfo?.name ?: packageName
                
                // Block the app
                val blockedApp = BlockedApp(
                    packageName = packageName,
                    appName = appName
                )
                database.blockedAppDao().insertBlockedApp(blockedApp)
                blockedPackageNames.add(packageName)
            } else {
                // Unblock the app
                database.blockedAppDao().deleteBlockedAppByPackageName(packageName)
                blockedPackageNames.remove(packageName)
            }
        }
    }
    
    /**
     * Data class representing an app that can be blocked
     */
    data class BlockableAppInfo(
        val packageName: String,
        val name: String,
        val icon: Drawable
    )
    
    /**
     * Adapter for displaying apps with checkboxes
     */
    private class BlockableAppAdapter(
        private val blockedPackageNames: MutableSet<String>,
        private val onAppCheckedChange: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<BlockableAppAdapter.AppViewHolder>() {
        
        private var apps = emptyList<BlockableAppInfo>()
        
        fun submitList(newApps: List<BlockableAppInfo>) {
            apps = newApps
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blockable_app, parent, false)
            return AppViewHolder(view, blockedPackageNames, onAppCheckedChange)
        }
        
        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(apps[position])
        }
        
        override fun getItemCount(): Int = apps.size
        
        class AppViewHolder(
            itemView: View,
            private val blockedPackageNames: MutableSet<String>,
            private val onAppCheckedChange: (String, Boolean) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            
            private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
            private val appName: TextView = itemView.findViewById(R.id.app_name)
            private val blockCheckbox: CheckBox = itemView.findViewById(R.id.block_checkbox)
            
            fun bind(appInfo: BlockableAppInfo) {
                appIcon.setImageDrawable(appInfo.icon)
                appName.text = appInfo.name
                
                val isBlocked = blockedPackageNames.contains(appInfo.packageName)
                blockCheckbox.isChecked = isBlocked
                
                // Remove previous listener to avoid infinite loops
                blockCheckbox.setOnCheckedChangeListener(null)
                
                // Set new listener
                blockCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    onAppCheckedChange(appInfo.packageName, isChecked)
                }
            }
        }
    }
}

