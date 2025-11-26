package com.unoshield.mdm.util

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Utility class to get device information
 */
object DeviceInfo {
    
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: Build.SERIAL
    }
    
    fun getSerialNumber(): String {
        return Build.SERIAL
    }
    
    fun getModel(): String {
        return Build.MODEL
    }
    
    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE
    }
    
    fun getManufacturer(): String {
        return Build.MANUFACTURER
    }
}

