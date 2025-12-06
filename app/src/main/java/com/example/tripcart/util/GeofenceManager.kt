package com.example.tripcart.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.tripcart.data.local.entity.PlaceEntity
import com.example.tripcart.service.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

// Geofence 관리 유틸리티 클래스
object GeofenceManager {
    
    private fun getGeofencingClient(context: Context): GeofencingClient {
        return LocationServices.getGeofencingClient(context)
    }
    
    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            // 푸시 알림 메시지와 연관이 깊은 함수 ..
            // FLAG_IMMUTABLE면 푸시 알림 메시지 동적 변경이 불가해서 MUTABLE 사용해야 함
            // 이거 바꾸면 바로 알림 안 됨 ..
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    // Geofence 추가
    fun addGeofence(context: Context, place: PlaceEntity) {
        // 위치 권한 확인
        if (!hasLocationPermission(context)) {
            return
        }
        
        // 위치 서비스 활성화 확인
        if (!isLocationEnabled(context)) {
            return
        }
        
        val geofence = GeofenceHelper.createGeofence(
            placeId = place.placeId,
            placeName = place.name,
            latitude = place.lat,
            longitude = place.lng,
            radiusInMeters = 300f
        )
        
        val geofencingRequest = GeofenceHelper.createGeofencingRequest(listOf(geofence))
        val geofencingClient = getGeofencingClient(context)
        
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent(context))
    }
    
    // Geofence 제거
    fun removeGeofence(context: Context, placeId: String, placeName: String) {
        val requestId = "$placeId|$placeName" // GeofenceHelper에서 생성한 형식
        val geofencingClient = getGeofencingClient(context)
        geofencingClient.removeGeofences(listOf(requestId))
    }
    
    // 위치 권한 확인
    private fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation || coarseLocation
    }
    
    // 위치 서비스 활성화 확인
    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    // 모든 장소에 대한 Geofence 등록 (초기화용)
    suspend fun registerAllGeofences(context: Context, places: List<PlaceEntity>) {
        // 위치 권한 확인
        if (!hasLocationPermission(context)) {
            return
        }
        
        // 위치 서비스 활성화 확인
        if (!isLocationEnabled(context)) {
            return
        }
        
        // Android 10 이상에서 백그라운드 위치 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 이상일 때
            val hasBackgroundPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasBackgroundPermission) {
                // 에러 발생
            }
        }
        
        if (places.isEmpty()) {
            return
        }
        
        // Geofence 생성 (최대 100개까지 등록 가능)
        val geofences = places.take(100).map { place ->
            GeofenceHelper.createGeofence(
                placeId = place.placeId,
                placeName = place.name,
                latitude = place.lat,
                longitude = place.lng,
                radiusInMeters = 300f
            )
        }
        
        if (geofences.isEmpty()) {
            return
        }
        
        val geofencingRequest = GeofenceHelper.createGeofencingRequest(geofences)
        val geofencingClient = getGeofencingClient(context)
        
        try {
            geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent(context)).await()
            
            // Geofence 등록 후 현재 위치 확인 - 이미 영역 내에 있으면 즉시 알림 표시
            checkCurrentLocationAndNotify(context, places)
        } catch (e: Exception) {
            // 에러 발생
        }
    }
    
    // 현재 위치 확인 및 이미 영역 내에 있으면 즉시 알림 표시
    private suspend fun checkCurrentLocationAndNotify(context: Context, places: List<PlaceEntity>) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationResult = fusedLocationClient.getCurrentLocation(
                // 최대 정확도!
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
            
            if (locationResult != null) {
                val currentLat = locationResult.latitude
                val currentLng = locationResult.longitude
                
                // 각 장소와의 거리 확인
                places.forEach { place ->
                    val distance = GeofenceHelper.calculateDistance(
                        currentLat, currentLng,
                        place.lat, place.lng
                    )
                    
                    // 이미 영역 내에 있으면 즉시 알림 표시
                    if (distance <= 300f) {
                        // BroadcastReceiver의 showNotification 메서드를 직접 호출해 알림 표시
                        GeofenceBroadcastReceiver.showNotification(context, place.name)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GeofenceManager", "현재 위치 확인 실패", e)
            // 위치 확인 실패해도 Geofence는 정상 작동하므로 무시
        }
    }
    
    // 모든 Geofence 제거
    fun removeAllGeofences(context: Context) {
        val geofencingClient = getGeofencingClient(context)
        geofencingClient.removeGeofences(getGeofencePendingIntent(context))
    }
}


