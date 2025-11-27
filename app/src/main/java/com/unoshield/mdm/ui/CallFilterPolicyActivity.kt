package com.unoshield.mdm.ui

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.unoshield.mdm.R

/**
 * Call Filter Policy Activity - Manage call filtering and blocking rules
 * Supports whitelist/blacklist modes, contact filtering, and call event logging
 */
class CallFilterPolicyActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var expandCollapseIcon: android.widget.ImageView
    private lateinit var callFilterModeSpinner: Spinner
    private lateinit var allowContactsCheckbox: MaterialCheckBox
    private lateinit var blockNonNumericCallsCheckbox: MaterialCheckBox
    private lateinit var blacklistButton: MaterialButton
    private lateinit var whitelistButton: MaterialButton
    private lateinit var sendCallDetailsCheckbox: MaterialCheckBox
    
    private lateinit var sharedPreferences: SharedPreferences
    
    private var isExpanded = true
    
    companion object {
        private const val PREFS_NAME = "call_filter_prefs"
        private const val KEY_FILTER_MODE = "filter_mode" // "whitelist" or "blacklist"
        private const val KEY_ALLOW_CONTACTS = "allow_contacts"
        private const val KEY_BLOCK_NON_NUMERIC = "block_non_numeric"
        private const val KEY_SEND_CALL_DETAILS = "send_call_details"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_filter_policy)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        setupToolbar()
        initializeViews()
        loadSavedPreferences()
        setupListeners()
    }
    
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.call_filter_policy)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun initializeViews() {
        expandCollapseIcon = findViewById(R.id.expand_collapse_icon)
        callFilterModeSpinner = findViewById(R.id.call_filter_mode_spinner)
        allowContactsCheckbox = findViewById(R.id.allow_contacts_checkbox)
        blockNonNumericCallsCheckbox = findViewById(R.id.block_non_numeric_calls_checkbox)
        blacklistButton = findViewById(R.id.blacklist_button)
        whitelistButton = findViewById(R.id.whitelist_button)
        sendCallDetailsCheckbox = findViewById(R.id.send_call_details_checkbox)
        
        // Setup spinner adapter
        val filterModes = listOf(
            getString(R.string.whitelist),
            getString(R.string.blacklist)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterModes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        callFilterModeSpinner.adapter = adapter
    }
    
    private fun loadSavedPreferences() {
        // Load filter mode
        val savedMode = sharedPreferences.getString(KEY_FILTER_MODE, "whitelist") ?: "whitelist"
        val modeIndex = if (savedMode == "whitelist") 0 else 1
        callFilterModeSpinner.setSelection(modeIndex)
        updateButtonStates(modeIndex == 0)
        
        // Load checkboxes
        allowContactsCheckbox.isChecked = sharedPreferences.getBoolean(KEY_ALLOW_CONTACTS, false)
        blockNonNumericCallsCheckbox.isChecked = sharedPreferences.getBoolean(KEY_BLOCK_NON_NUMERIC, false)
        sendCallDetailsCheckbox.isChecked = sharedPreferences.getBoolean(KEY_SEND_CALL_DETAILS, false)
    }
    
    private fun setupListeners() {
        // Expand/Collapse icon
        expandCollapseIcon.setOnClickListener {
            toggleExpandCollapse()
        }
        
        // Call Filter Mode Spinner
        callFilterModeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isWhitelist = position == 0
                updateButtonStates(isWhitelist)
                saveFilterMode(isWhitelist)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Allow Contacts checkbox
        allowContactsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(KEY_ALLOW_CONTACTS, isChecked)
                .apply()
            Toast.makeText(this, if (isChecked) "Allow contacts enabled" else "Allow contacts disabled", Toast.LENGTH_SHORT).show()
        }
        
        // Block Non-Numeric Calls checkbox
        blockNonNumericCallsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(KEY_BLOCK_NON_NUMERIC, isChecked)
                .apply()
            Toast.makeText(this, if (isChecked) "Block non-numeric calls enabled" else "Block non-numeric calls disabled", Toast.LENGTH_SHORT).show()
        }
        
        // Blacklist Button
        blacklistButton.setOnClickListener {
            callFilterModeSpinner.setSelection(1)
            Toast.makeText(this, "Switched to Blacklist mode", Toast.LENGTH_SHORT).show()
            // Open blacklist management screen
            val intent = Intent(this, BlacklistActivity::class.java)
            startActivity(intent)
        }
        
        // Whitelist Button
        whitelistButton.setOnClickListener {
            callFilterModeSpinner.setSelection(0)
            Toast.makeText(this, "Switched to Whitelist mode", Toast.LENGTH_SHORT).show()
            // Open whitelist management screen
            val intent = Intent(this, WhitelistActivity::class.java)
            startActivity(intent)
        }
        
        // Send Call Details checkbox
        sendCallDetailsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(KEY_SEND_CALL_DETAILS, isChecked)
                .apply()
            Toast.makeText(this, if (isChecked) "Call details logging enabled" else "Call details logging disabled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateButtonStates(isWhitelist: Boolean) {
        val brandBlue = ContextCompat.getColor(this, R.color.brand_blue)
        val transparent = Color.TRANSPARENT
        
        if (isWhitelist) {
            // Whitelist is selected - filled button
            whitelistButton.setBackgroundColor(brandBlue)
            whitelistButton.setTextColor(Color.WHITE)
            // Blacklist is not selected - outlined button
            blacklistButton.setBackgroundColor(transparent)
            blacklistButton.setTextColor(brandBlue)
        } else {
            // Blacklist is selected - filled button
            blacklistButton.setBackgroundColor(brandBlue)
            blacklistButton.setTextColor(Color.WHITE)
            // Whitelist is not selected - outlined button
            whitelistButton.setBackgroundColor(transparent)
            whitelistButton.setTextColor(brandBlue)
        }
    }
    
    private fun saveFilterMode(isWhitelist: Boolean) {
        sharedPreferences.edit()
            .putString(KEY_FILTER_MODE, if (isWhitelist) "whitelist" else "blacklist")
            .apply()
    }
    
    private fun toggleExpandCollapse() {
        isExpanded = !isExpanded
        // For now, just toggle the icon - in future can collapse/expand content
        expandCollapseIcon.setImageResource(
            if (isExpanded) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float
        )
    }
}

