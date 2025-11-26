package com.unoshield.mdm.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.unoshield.mdm.AdminReceiver
import com.unoshield.mdm.R
import com.unoshield.mdm.api.ApiClient
import com.unoshield.mdm.api.DeviceRegistrationRequest
import com.unoshield.mdm.util.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Enrollment Activity - Handles device registration after provisioning
 */
class EnrollmentActivity : AppCompatActivity() {
    
    private val TAG = "EnrollmentActivity"
    
    private lateinit var statusText: TextView
    private lateinit var registerButton: Button
    
    private var enrollmentId: String? = null
    private var enrollmentCode: String? = null
    private var baseUrl: String? = null
    private var retryCount = 0
    private val maxRetries = 3
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)
        
        statusText = findViewById(R.id.status_text)
        registerButton = findViewById(R.id.register_button)
        
        // Get enrollment data from intent
        enrollmentId = intent.getStringExtra("enrollment_id")
        enrollmentCode = intent.getStringExtra("enrollment_code")
        baseUrl = intent.getStringExtra("base_url")
        
        // Set base URL for API client
        baseUrl?.let { ApiClient.setBaseUrl(it) }
        
        // Display enrollment info with BASE_URL for debugging
        val statusInfo = buildString {
            append("Enrollment ID: $enrollmentId\n")
            append("Enrollment Code: $enrollmentCode\n")
            append("Server URL: $baseUrl")
        }
        statusText.text = statusInfo
        
        Log.d(TAG, "EnrollmentActivity started")
        Log.d(TAG, "Enrollment ID: $enrollmentId")
        Log.d(TAG, "Enrollment Code: $enrollmentCode")
        Log.d(TAG, "Base URL: $baseUrl")
        
        registerButton.setOnClickListener {
            retryCount = 0
            registerDevice()
        }
        
        // Auto-register if we have all required data
        if (enrollmentId != null && enrollmentCode != null) {
            // Wait a bit for WiFi to stabilize
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000) // Wait 2 seconds for WiFi to stabilize
                registerDevice()
            }
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    private fun registerDevice() {
        if (enrollmentId == null || enrollmentCode == null) {
            val errorMsg = "Missing enrollment data\nEnrollment ID: $enrollmentId\nEnrollment Code: $enrollmentCode"
            statusText.text = errorMsg
            Toast.makeText(this, "Missing enrollment data", Toast.LENGTH_LONG).show()
            Log.e(TAG, errorMsg)
            return
        }
        
        // Check network connectivity first
        if (!isNetworkAvailable()) {
            val errorMsg = "No internet connection\nPlease check your WiFi connection\n\nServer: $baseUrl"
            statusText.text = errorMsg
            registerButton.isEnabled = true
            Toast.makeText(this, "No internet connection. Please check WiFi.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No network available")
            return
        }
        
        registerButton.isEnabled = false
        val attemptText = if (retryCount > 0) " (Attempt ${retryCount + 1}/$maxRetries)" else ""
        statusText.text = "Registering device...$attemptText\nServer: $baseUrl"
        
        Log.d(TAG, "Starting device registration (attempt ${retryCount + 1})")
        Log.d(TAG, "Base URL: $baseUrl")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = DeviceInfo.getDeviceId(this@EnrollmentActivity)
                val serialNumber = DeviceInfo.getSerialNumber()
                val model = DeviceInfo.getModel()
                val androidVersion = DeviceInfo.getAndroidVersion()
                
                Log.d(TAG, "Device Info - ID: $deviceId, Model: $model, Android: $androidVersion")
                
                val request = DeviceRegistrationRequest(
                    enrollment_id = enrollmentId!!,
                    device_id = deviceId,
                    serial_number = serialNumber,
                    model = model,
                    android_version = androidVersion
                )
                
                Log.d(TAG, "Sending registration request to: $baseUrl/api/enrollment/register")
                val response = ApiClient.getApiService().registerDevice(request)
                
                Log.d(TAG, "Response received - Success: ${response.isSuccessful}, Code: ${response.code()}")
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val successMsg = "Device registered successfully!\nDevice ID: ${response.body()?.device_id}"
                        statusText.text = successMsg
                        Toast.makeText(this@EnrollmentActivity, "Device enrolled successfully", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Device registration successful: ${response.body()?.device_id}")
                        
                        // Navigate to main activity after a delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val intent = android.content.Intent(this@EnrollmentActivity, MainActivity::class.java)
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }, 2000)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        val errorMsg = when (response.code()) {
                            404 -> "Enrollment not found\nPlease generate a new QR code\n\nServer: $baseUrl"
                            400 -> "Registration failed: ${response.message()}\nError: $errorBody\n\nServer: $baseUrl"
                            else -> "Registration failed\nCode: ${response.code()}\nMessage: ${response.message()}\n\nServer: $baseUrl"
                        }
                        statusText.text = errorMsg
                        registerButton.isEnabled = true
                        Toast.makeText(this@EnrollmentActivity, "Registration failed: ${response.message()}", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Registration failed - Code: ${response.code()}, Message: ${response.message()}, Body: $errorBody")
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout error during registration", e)
                retryCount++
                withContext(Dispatchers.Main) {
                    if (retryCount < maxRetries) {
                        val errorMsg = "Connection timeout\nRetrying... (${retryCount + 1}/$maxRetries)\n\nServer: $baseUrl"
                        statusText.text = errorMsg
                    } else {
                        val errorMsg = "Connection timeout after $maxRetries attempts\n\nPlease check:\n1. Server is running\n2. Correct IP address\n3. Firewall settings\n\nServer: $baseUrl"
                        statusText.text = errorMsg
                        registerButton.isEnabled = true
                        Toast.makeText(this@EnrollmentActivity, "Connection timeout. Please check server.", Toast.LENGTH_LONG).show()
                    }
                }
                if (retryCount < maxRetries) {
                    delay(2000)
                    registerDevice()
                }
            } catch (e: ConnectException) {
                Log.e(TAG, "Connection error during registration", e)
                retryCount++
                withContext(Dispatchers.Main) {
                    if (retryCount < maxRetries) {
                        val errorMsg = "Cannot connect to server\nRetrying... (${retryCount + 1}/$maxRetries)\n\nServer: $baseUrl"
                        statusText.text = errorMsg
                    } else {
                        val errorMsg = "Cannot connect to server after $maxRetries attempts\n\nPlease check:\n1. Server is running: $baseUrl\n2. Device and server on same WiFi\n3. Firewall allows port\n\nServer: $baseUrl"
                        statusText.text = errorMsg
                        registerButton.isEnabled = true
                        Toast.makeText(this@EnrollmentActivity, "Cannot connect to server. Please check network.", Toast.LENGTH_LONG).show()
                    }
                }
                if (retryCount < maxRetries) {
                    delay(2000)
                    registerDevice()
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unknown host error during registration", e)
                withContext(Dispatchers.Main) {
                    val errorMsg = "Cannot resolve server address\n\nServer URL: $baseUrl\n\nPlease check:\n1. Correct IP address\n2. Server is running\n3. Same WiFi network"
                    statusText.text = errorMsg
                    registerButton.isEnabled = true
                    Toast.makeText(this@EnrollmentActivity, "Cannot resolve server address", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error registering device", e)
                withContext(Dispatchers.Main) {
                    val errorMsg = "Error: ${e.message}\n\nServer: $baseUrl\n\nPlease check server logs"
                    statusText.text = errorMsg
                    registerButton.isEnabled = true
                    Toast.makeText(this@EnrollmentActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

