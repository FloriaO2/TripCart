package com.example.tripcart.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tripcart.MainActivity
import com.example.tripcart.util.GeofenceHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

// GeofenceBroadcastReceiver - 상점 영역 진입 시 푸시 알림 서비스
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    
    // Geofence 이벤트를 전달할 때 호출되는 함수
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null) {
            return
        }
        
        if (geofencingEvent.hasError()) {
            return
        }
        
        val geofenceTransition = geofencingEvent.geofenceTransition // Geofence 이벤트 타입 (ENTER, DWELL, EXIT)
        
        // 테스트용! DWELL 이벤트는 추후 제거 예정
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || // 영역 진입
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) { // 영역 머무름
            val triggeringGeofences = geofencingEvent.triggeringGeofences // 트리거된 Geofence 목록!
                                                                          // 여러 Geofence가 동시에 트리거될 수 있음
            
            triggeringGeofences?.forEach { geofence ->
                val placeName = GeofenceHelper.extractPlaceName(geofence.requestId)
                
                // 알림 표시
                GeofenceBroadcastReceiver.showNotification(context, placeName)
            }
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "geofence_notification_channel"
        
        // 알림 표시 함수 (외부에서도 호출 가능)
        fun showNotification(context: Context, placeName: String) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 알림 권한 확인 (Android 13 이상)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!notificationManager.areNotificationsEnabled()) {
                    return
                }
            }
            
            // 알림 채널 생성
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0 이상
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "근처 장소 알림",
                    NotificationManager.IMPORTANCE_HIGH // 상단 토스트 알림 목적
                ).apply {
                    description = "근처에 있는 장소에 대한 알림"
                    enableVibration(true) // 진동 활성화
                    // setShowBadge(true) // 앱 아이콘에 알림 개수 표시
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // MainActivity로 이동하는 Intent
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0, // 요청 코드 (PendingIntent 구분용)
                intent,
                // FLAG_IMMUTABLE: PendingIntent 내용 변경 불가
                // FLAG_IMMUTABLE는 일반 알림 구현에서 사용 <-> FLAG_MUTABLE
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // 알림 생성
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("근처 장소 알림")
                .setContentText("$placeName 상점이 당신의 근처에 있어요. 시간이 되면 방문해보세요!")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation) // 앱 아이콘으로 변경하는 게 나을까 ..
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // 상단 토스트 알림 클릭 시 알림 자동 삭제
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7.1 이하일 때 사용
                                                               // 이땐 PRIORITY_HIGH로 상단 토스트 구현!
                                                               // 지금은 IMPORTANCE_HIGH로 상단 토스트 구현 중
                .setDefaults(NotificationCompat.DEFAULT_ALL) // 소리, 진동, LED 기본값 사용
                .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 메시지 카테고리로 분류
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금 화면에서도 표시
                .build()
            
            // 고유한 알림 ID 생성
            // placeName에 해시코드를 적용함으로써 같은 장소면 같은 알림 ID - 중복 알림 방지
            val notificationId = placeName.hashCode()
            notificationManager.notify(notificationId, notification)
        }
    }

    // 테스트 후 주석 없앨 예정
    // 1시간 제한 로직
    // private fun shouldShowNotification(context: Context, placeId: String): Boolean {
    //     val prefs = context.getSharedPreferences("geofence_notifications", Context.MODE_PRIVATE)
    //     val lastNotificationTime = prefs.getLong("notification_time_$placeId", 0)
    //     val currentTime = System.currentTimeMillis()
    //     val oneHourInMillis = 60 * 60 * 1000L // 1시간
    //
    //     // 마지막 알림으로부터 1시간이 지났거나, 아직 알림을 보낸 적이 없으면 알림 표시
    //     return (currentTime - lastNotificationTime) >= oneHourInMillis
    // }
    //
    // private fun saveNotificationTime(context: Context, placeId: String) {
    //     val prefs = context.getSharedPreferences("geofence_notifications", Context.MODE_PRIVATE)
    //     prefs.edit()
    //         .putLong("notification_time_$placeId", System.currentTimeMillis())
    //         .apply()
    // }
}

