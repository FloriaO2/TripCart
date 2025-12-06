package com.example.tripcart.util

import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest

object GeofenceHelper {
    
    // 두 지점 간의 거리를 미터 단위로 계산 (Haversine 공식)
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
    
    // Geofence 생성
    fun createGeofence(
        placeId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
        radiusInMeters: Float = 300f
    ): Geofence {
        // requestId에 placeId와 placeName을 함께 저장 (| 구분자 사용)
        val requestId = "$placeId|$placeName"
        
        return Geofence.Builder()
            .setRequestId(requestId) // 고유 식별자 설정
            .setCircularRegion(latitude, longitude, radiusInMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Geofence는 영구적으로 유지
            // 테스트용: 진입 + 머무름 모두 감지 (임시용)
            // 감지할 이벤트 타입
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setNotificationResponsiveness(1000) // 1초 내에 알림
            .setLoiteringDelay(1000) // 1초간 영역 내에 있을 때 DWELL 이벤트 발생
            .build()
    }
    
    // GeofencingRequest 생성
    fun createGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder()
            // ENTER + DWELL (임시용)
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL
            )
            .addGeofences(geofences)
            .build()
    }
    
    // requestId에서 placeId 추출
    fun extractPlaceId(requestId: String): String {
        return requestId.split("|").firstOrNull() ?: requestId
    }
    
    // requestId에서 placeName 추출
    fun extractPlaceName(requestId: String): String {
        return requestId.split("|").getOrNull(1) ?: "알 수 없는 장소"
    }
}

