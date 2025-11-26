package com.unoshield.mdm.api

import retrofit2.Response
import retrofit2.http.*

/**
 * API Service for UNO Shield MDM Backend
 */
interface ApiService {
    
    @POST("api/enrollment/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>
    
    @POST("api/devices/{device_id}/heartbeat")
    suspend fun sendHeartbeat(@Path("device_id") deviceId: String): Response<HeartbeatResponse>
    
    @GET("api/devices/{device_id}")
    suspend fun getDevice(@Path("device_id") deviceId: String): Response<DeviceInfo>
}

/**
 * Data classes for API requests/responses
 */
data class DeviceRegistrationRequest(
    val enrollment_id: String,
    val device_id: String,
    val serial_number: String? = null,
    val model: String? = null,
    val android_version: String? = null
)

data class DeviceRegistrationResponse(
    val success: Boolean,
    val device_id: String,
    val message: String
)

data class HeartbeatResponse(
    val success: Boolean,
    val last_seen: String
)

data class DeviceInfo(
    val id: String,
    val device_id: String,
    val enrollment_id: String,
    val serial_number: String?,
    val model: String?,
    val android_version: String?,
    val enrolled_at: String,
    val last_seen: String?,
    val status: String
)

