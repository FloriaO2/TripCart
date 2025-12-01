package com.example.tripcart.data.remote

import com.example.tripcart.util.EnvUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object PlacesApiClient {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: PlacesApiService = retrofit.create(PlacesApiService::class.java)
    
    // API 키 가져오기
    fun getApiKey(context: android.content.Context): String {
        return EnvUtils.getMapsApiKey(context)
    }
    
    // Places 이미지 url 형태로 불러오기
    fun getPhotoUrl(photoReference: String, maxWidth: Int = 800, maxHeight: Int = 800, apiKey: String): String {
        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=$maxWidth&maxheight=$maxHeight&photo_reference=$photoReference&key=$apiKey"
    }
}

