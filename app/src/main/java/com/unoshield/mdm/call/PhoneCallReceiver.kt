package com.unoshield.mdm.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import android.util.Log
import com.unoshield.mdm.data.MDMDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * BroadcastReceiver to intercept incoming phone calls and apply call filtering
 * This receiver handles call blocking based on whitelist/blacklist rules
 */
class PhoneCallReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "PhoneCallReceiver"
    
    companion object {
        private const val PREFS_NAME = "call_filter_prefs"
        private const val KEY_FILTER_MODE = "filter_mode"
        private const val KEY_ALLOW_CONTACTS = "allow_contacts"
        private const val KEY_BLOCK_NON_NUMERIC = "block_non_numeric"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }
        
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            if (phoneNumber != null) {
                scope.launch {
                    handleIncomingCall(context, phoneNumber)
                }
            }
        }
    }
    
    private suspend fun handleIncomingCall(context: Context, phoneNumber: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val filterMode = prefs.getString(KEY_FILTER_MODE, "whitelist") ?: "whitelist"
            val allowContacts = prefs.getBoolean(KEY_ALLOW_CONTACTS, false)
            val blockNonNumeric = prefs.getBoolean(KEY_BLOCK_NON_NUMERIC, false)
            
            val database = MDMDatabase.getDatabase(context)
            
            // Check if number is numeric (if block non-numeric is enabled)
            if (blockNonNumeric && !isNumeric(phoneNumber)) {
                Log.d(TAG, "Blocking non-numeric call: $phoneNumber")
                blockCall(context, phoneNumber)
                return
            }
            
            // Check if number is in contacts (if allow contacts is enabled)
            if (allowContacts && isInContacts(context, phoneNumber)) {
                Log.d(TAG, "Allowing call from contact: $phoneNumber")
                return // Allow the call
            }
            
            // Check blacklist/whitelist
            val blacklistNumbers = database.blacklistNumberDao().getAllBlacklistPhoneNumbers()
            val whitelistNumbers = database.whitelistNumberDao().getAllWhitelistPhoneNumbers()
            
            val normalizedPhone = normalizePhoneNumber(phoneNumber)
            val isInBlacklist = blacklistNumbers.any { normalizePhoneNumber(it) == normalizedPhone }
            val isInWhitelist = whitelistNumbers.any { normalizePhoneNumber(it) == normalizedPhone }
            
            when (filterMode) {
                "whitelist" -> {
                    // In whitelist mode, only allow numbers in whitelist (or contacts if enabled)
                    if (!isInWhitelist && (!allowContacts || !isInContacts(context, phoneNumber))) {
                        Log.d(TAG, "Blocking call (whitelist mode): $phoneNumber")
                        blockCall(context, phoneNumber)
                    } else {
                        Log.d(TAG, "Allowing call (whitelist mode): $phoneNumber")
                    }
                }
                "blacklist" -> {
                    // In blacklist mode, block numbers in blacklist
                    if (isInBlacklist) {
                        Log.d(TAG, "Blocking call (blacklist mode): $phoneNumber")
                        blockCall(context, phoneNumber)
                    } else {
                        Log.d(TAG, "Allowing call (blacklist mode): $phoneNumber")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming call", e)
        }
    }
    
    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Remove all non-digit characters except +
        return phoneNumber.replace(Regex("[^+\\d]"), "")
    }
    
    private fun isNumeric(phoneNumber: String): Boolean {
        // Check if phone number contains only digits and + sign
        val pattern = Pattern.compile("^[+]?[0-9]+$")
        return pattern.matcher(phoneNumber).matches()
    }
    
    private fun isInContacts(context: Context, phoneNumber: String): Boolean {
        // This is a simplified check - in production, you'd query ContactsContract
        // For now, return false - can be enhanced later
        return false
    }
    
    private fun blockCall(context: Context, phoneNumber: String) {
        // Note: Actually blocking calls requires ITelephony service access
        // which requires system-level permissions or root access
        // For now, we log the blocking action
        // In production, you might need to use ITelephony.endCall() via reflection
        // or use a foreground service with call screening API (Android 10+)
        
        Log.d(TAG, "Call blocked: $phoneNumber")
        
        // Send call details to server if enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sendCallDetails = prefs.getBoolean("send_call_details", false)
        if (sendCallDetails) {
            sendCallDetailsToServer(context, phoneNumber, "blocked")
        }
    }
    
    private fun sendCallDetailsToServer(context: Context, phoneNumber: String, status: String) {
        // TODO: Implement server API call to send call details
        Log.d(TAG, "Sending call details to server: $phoneNumber - $status")
    }
}

