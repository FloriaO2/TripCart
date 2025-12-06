package com.example.tripcart.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.tripcart.data.local.TripCartDatabase
import com.example.tripcart.util.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {
    
    // SupervisorJob: 각 자식 코루틴의 오류가 서로에게 영향을 주지 않아 독립적으로 작동 가능
    // Dispatchers.IO: 입출력 작업 전용 - UI 스레드와 별도로 작업을 처리함으로써 성능 유지
    //                                  (백그라운드 작업에 유용)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // 서비스 시작 시 모든 장소에 대한 Geofence 등록
        // Geofence는 시스템 레벨에서 관리되므로 등록 후 서비스 종료 가능
        serviceScope.launch {
            registerAllGeofences()
            // Geofence 등록 완료 후 서비스 종료
            stopSelf()
        }
    }
    
    // Android의 Service 클래스에서 구현 없이 선언만 달랑 해놔버려서 함수 설명을 해놓긴 해야 함 (생략 불가)
    override fun onBind(intent: Intent?): IBinder? {
        // 바인딩을 사용하지 않으므로 null 반환
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Geofence는 시스템 레벨에서 관리되므로 Foreground Service 불필요 -> 등록만 하고 종료
        return START_NOT_STICKY // 서비스가 강제 종료되어도 재시작하지 않음
                                // (Geofence가 이미 등록되어있으므로!)
    }
    
    // 삭제해도 될 것 같기도 ..?
    override fun onDestroy() {
        super.onDestroy()
        // Geofence는 시스템이 관리하므로 제거하지 않음
    }
    
    // 모든 장소에 대한 Geofence 등록 (초기화용)
    private suspend fun registerAllGeofences() {
        try {
            val roomDb = TripCartDatabase.getDatabase(applicationContext)
            val placeDao = roomDb.placeDao()
            val places = placeDao.getAllPlaces().first()
            
            // GeofenceManager를 사용하여 모든 Geofence 등록 (await로 완료 대기)
            GeofenceManager.registerAllGeofences(applicationContext, places)
        } catch (e: Exception) {
            // 에러 발생
        }
    }
    
    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.startService(intent)
        }
    }
}

