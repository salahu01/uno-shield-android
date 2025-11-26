package com.unoshield.mdm.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API Client for UNO Shield MDM
 */
object ApiClient {
    private var baseUrl: String = "http://localhost:8000/"
    
    fun setBaseUrl(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
    }
    
    fun getApiService(): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)  // Increased from 30 to 60 seconds
            .readTimeout(60, TimeUnit.SECONDS)      // Increased from 30 to 60 seconds
            .writeTimeout(60, TimeUnit.SECONDS)      // Increased from 30 to 60 seconds
            .retryOnConnectionFailure(true)          // Enable automatic retry
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
    
    fun getBaseUrl(): String {
        return baseUrl
    }
}

