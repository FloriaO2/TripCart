package com.example.tripcart.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.tripcart.MainActivity
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var sharedPreferences: SharedPreferences? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }
    
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences("location_tracking", Context.MODE_PRIVATE)
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY // 서비스가 종료되어도 재시작
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "위치 추적",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백그라운드에서 위치를 추적합니다"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("위치 추적 중")
            .setContentText("백그라운드에서 위치를 추적하고 있습니다")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // 위치 아이콘 사용
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startLocationUpdates() {
        // 위치 업데이트 요청
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            2000L
        ).apply {
            // 두 조건을 모두 갖추어야만 위치 업데이트 요청 가능!
            // 2초가 지났어도 위치가 변경되지 않았거나,
            // 빠르게 위치가 바뀌었어도 이전 요청으로부터 2초가 지나지 않았다면 요청이 안 됨
            setSmallestDisplacement(1f) // 1m 이상 이동했을 때 업데이트
            setMinUpdateIntervalMillis(2000L) // 최소 2초 간격 보장 (빠르게 이동해도 2초마다만)
        }.build()
        
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // 위치 데이터 저장
                    saveLocation(location.latitude, location.longitude)
                }
            }
        }
        
        locationCallback = callback
        
        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // 권한 오류
        }
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
            locationCallback = null
        }
    }
    
    private fun saveLocation(latitude: Double, longitude: Double) {
        serviceScope.launch {
            sharedPreferences?.edit()?.apply {
                putFloat("last_latitude", latitude.toFloat())
                putFloat("last_longitude", longitude.toFloat())
                putLong("last_update_time", System.currentTimeMillis())
                apply()
            }
        }
    }
    
    fun getLastLocation(): Pair<Double, Double>? {
        val lat = sharedPreferences?.getFloat("last_latitude", 0f)?.toDouble()
        val lng = sharedPreferences?.getFloat("last_longitude", 0f)?.toDouble()
        return if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            Pair(lat, lng)
        } else {
            null
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1
        
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }
}

