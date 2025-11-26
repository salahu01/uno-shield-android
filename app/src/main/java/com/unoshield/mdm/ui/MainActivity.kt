package com.unoshield.mdm.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unoshield.mdm.R
import com.unoshield.mdm.util.LauncherHelper

/**
 * Main Activity - Acts as the default launcher
 * Displays installed apps and allows launching them
 * Includes "UNO Manager" app for managing MDM settings
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var appAdapter: AppAdapter
    private lateinit var emptyView: View
    private lateinit var gridLinesView: View
    
    companion object {
        // Special package name for UNO Manager app
        const val UNO_MANAGER_PACKAGE = "com.unoshield.mdm.UNO_MANAGER"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Nothing OS style: Full black background with transparent status bar
        setupNothingOSStyle()
        
        setContentView(R.layout.activity_launcher)
        
        // Check if this is being launched as a launcher (HOME intent)
        checkLauncherStatus()
        
        initializeViews()
        setupRecyclerView()
        loadInstalledApps()
    }
    
    private fun setupNothingOSStyle() {
        // Make status bar transparent and set light icons
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        // Set status bar to light icons (white icons on black background)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
        
        // Set navigation bar to blue
        window.navigationBarColor = ContextCompat.getColor(this, R.color.nothing_blue)
    }
    
    private fun checkLauncherStatus() {
        val isDefault = LauncherHelper.isDefaultLauncher(this)
        Log.d("MainActivity", "Is default launcher: $isDefault")
        
        // If launched from HOME intent and not default, this means user is selecting launcher
        if (intent.hasCategory(Intent.CATEGORY_HOME) && !isDefault) {
            Log.d("MainActivity", "Launched from HOME intent - user is selecting launcher")
        }
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.apps_recycler_view)
        emptyView = findViewById(R.id.empty_view)
        gridLinesView = findViewById(R.id.grid_lines_view)
        
        // Draw grid lines programmatically
        setupGridLines()
    }
    
    private fun setupGridLines() {
        gridLinesView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        gridLinesView.post {
            val customDrawable = GridLinesDrawable(
                gridLinesView.width,
                gridLinesView.height,
                ContextCompat.getColor(this, R.color.line_color_light)
            )
            gridLinesView.background = customDrawable
        }
    }
    
    private fun setupRecyclerView() {
        // Calculate responsive column count based on screen width
        val spanCount = calculateColumnCount()
        val layoutManager = GridLayoutManager(this, spanCount)
        recyclerView.layoutManager = layoutManager
        
        appAdapter = AppAdapter { appInfo ->
            launchApp(appInfo)
        }
        recyclerView.adapter = appAdapter
    }
    
    private fun calculateColumnCount(): Int {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            displayMetrics.widthPixels = bounds.width()
            displayMetrics.density = resources.displayMetrics.density
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }
        
        val screenWidthPx = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density
        
        // Account for RecyclerView padding: 24dp on each side = 48dp total
        // Convert to pixels
        val paddingPx = 24f * density * 2f // 24dp on each side
        val availableWidthPx = screenWidthPx - paddingPx
        
        // Item width in pixels based on actual layout:
        // Icon: 64dp + item margin: 4dp (2dp each side) + item padding: 16dp (8dp each side) = 84dp per item
        val itemWidthPx = 84f * density
        
        // Calculate optimal column count dynamically based on available width
        val calculatedColumns = (availableWidthPx / itemWidthPx).toInt()
        
        // Return the calculated value, ensuring reasonable bounds
        // Minimum 3 columns for small screens, maximum 6 for very large screens
        // This makes it fully responsive to any screen width
        return when {
            calculatedColumns < 3 -> 3
            calculatedColumns > 6 -> 6
            else -> calculatedColumns
        }
    }
    
    private fun loadInstalledApps() {
        val packageManager = packageManager
        val apps = mutableListOf<AppInfo>()
        
        // Add UNO Manager as the first app
        val unoManagerIcon = ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
            ?: ContextCompat.getDrawable(this, android.R.drawable.sym_def_app_icon)
        apps.add(
            AppInfo(
                packageName = UNO_MANAGER_PACKAGE,
                name = getString(R.string.uno_manager),
                icon = unoManagerIcon!!,
                className = SettingsActivity::class.java.name,
                isSpecialApp = true
            )
        )
        
        // Query for all apps that can be launched
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )
        
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            val applicationInfo = activityInfo.applicationInfo
            
            // Skip our MainActivity (it's the launcher/home screen)
            if (activityInfo.packageName == packageName && 
                activityInfo.name == MainActivity::class.java.name) {
                continue
            }
            
            // Skip SettingsActivity (it's shown as UNO Manager)
            if (activityInfo.packageName == packageName && 
                activityInfo.name == SettingsActivity::class.java.name) {
                continue
            }
            
            val appInfo = AppInfo(
                packageName = activityInfo.packageName,
                name = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager),
                className = activityInfo.name,
                isSpecialApp = false
            )
            
            apps.add(appInfo)
        }
        
        // Sort apps alphabetically by name (UNO Manager will be first)
        apps.sortBy { if (it.isSpecialApp) "" else it.name.lowercase() }
        
        // Update adapter
        appAdapter.submitList(apps)
        
        // Show/hide empty view
        if (apps.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
    
    private fun launchApp(appInfo: AppInfo) {
        try {
            // Handle special UNO Manager app
            if (appInfo.isSpecialApp && appInfo.packageName == UNO_MANAGER_PACKAGE) {
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                return
            }
            
            // Launch regular apps
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onBackPressed() {
        // Prevent back button from exiting launcher
        // Move launcher to back instead
        moveTaskToBack(true)
    }
}

/**
 * Data class representing an installed app
 */
data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable,
    val className: String,
    val isSpecialApp: Boolean = false // true for special apps like UNO Manager
)
