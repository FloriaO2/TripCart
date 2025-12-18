package com.example.tripcart

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import com.example.tripcart.navigation.TripCartNavGraph
import com.example.tripcart.service.LocationTrackingService
import com.example.tripcart.ui.theme.TripCartTheme
import com.example.tripcart.util.BackgroundLocationPermissionHelper
import com.example.tripcart.util.NotificationPermissionHelper
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    // 푸시 알림으로부터 MapScreen으로 이동해야 하는지 여부를 추적
    private val shouldNavigateToMapState = mutableStateOf(false)
    
    // 푸시 알림으로부터 ListDetail로 이동해야 하는지 여부를 추적
    private val shouldNavigateToListDetailState = mutableStateOf<String?>(null)
    
    // ListDetail로 이동 시 채팅 팝업 열어야 하는지 추적 (푸시 알림 통해 접근 vs 앱 내 접근)
    private val shouldOpenChatState = mutableStateOf(false)
    
    // 알림 권한 요청 런처 (Android 13 이상)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission() // 한 번에 하나만 요청
    ) { _ ->
        // 알림 권한 요청 완료 후 일반 위치 권한 요청
        requestLocationPermissionsIfNeeded()
    }
    
    // 일반 위치 권한 요청 런처
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions() // 한 번에 여러 개 요청
    ) { permissions ->
        // 정확한 위치 권한
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        // 대략적인 위치 권한
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // 일반 위치 권한이 허용되면 백그라운드 위치 권한 요청
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 이상
                // 백그라운드 위치 권한이 허용되지 않은 경우에 if문 진입
                if (!BackgroundLocationPermissionHelper.isBackgroundLocationPermissionGranted(this)) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
            // 권한이 허용되면 Geofence 등록
            LocationTrackingService.startService(this)
        }
    }
    
    // 백그라운드 위치 권한 요청 런처
    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission() // 한 번에 하나만 요청
    ) { isGranted ->
        if (isGranted) {
            // 권한이 허용되면 Geofence 재등록
            LocationTrackingService.startService(this)
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Context의 위치 및 언어를 한국 관련으로 설정
        // 이렇게 해줘야 Places API가 한국어로 데이터 받아옴 ..
        val locale = Locale("ko", "KR")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 상태바 표시 후 시스템 윈도우에 맞춰 콘텐츠 배치
        WindowCompat.setDecorFitsSystemWindows(window, true)
        // enableEdgeToEdge() // 상태바 표시를 위해 주석 처리
        
        // 네비게이션 바는 하단바 색상과 동일하게 고정
        window.navigationBarColor = android.graphics.Color.WHITE
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightNavigationBars = true
        
        // 상태바 색상은 각 화면에서 동적으로 설정됨 (기본값은 흰색)
        window.statusBarColor = android.graphics.Color.WHITE
        windowInsetsController.isAppearanceLightStatusBars = true
        
        // 핸드폰 상태바 숨기기
        // windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        // windowInsetsController.systemBarsBehavior = 
        //     WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Intent에서 MapScreen으로 이동해야 하는지 확인
        shouldNavigateToMapState.value = intent.getBooleanExtra("navigate_to_map", false)
        
        // FCM 푸시 알림 클릭 시
        // Intent에서 데이터를 읽어 ListDetail로 이동하기 위한 정보 추출
        // 백그라운드 상태일 경우 Intent extras에서 데이터 직접 가져오기
        val listId = intent.extras?.getString("listId")
            ?: intent.getStringExtra("listId") 
            ?: intent.data?.getQueryParameter("listId")
        val navigateTo = intent.extras?.getString("navigateTo")
            ?: intent.getStringExtra("navigateTo") 
            ?: intent.data?.getQueryParameter("navigateTo")
        val openChatStr = intent.extras?.getString("openChat")
            ?: intent.getStringExtra("openChat") 
            ?: intent.data?.getQueryParameter("openChat")
        val openChat = openChatStr?.toBoolean() ?: intent.getBooleanExtra("openChat", false)

        // intent.data?.scheme == "tripcart"는 추후 딥링크랑 연동해서 사용할 수 있도록 미리 구현
        if ((navigateTo == "list_detail" || intent.data?.scheme == "tripcart") && !listId.isNullOrEmpty()) {
            shouldNavigateToListDetailState.value = listId
            shouldOpenChatState.value = openChat
        }
        
        setContent { // UI 조작
            // shouldNavigateToMapState 상태가 변경되면 재렌더링
            // shouldNavigateToMapState 자체가 mutableStateOf라 remember 불필요!
            val shouldNavigateToMap by shouldNavigateToMapState
            val shouldNavigateToListDetail by shouldNavigateToListDetailState
            val shouldOpenChat by shouldOpenChatState
            
            TripCartTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    TripCartNavGraph(
                        // 알림 권한
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        // 백그라운드 위치 권한
                        onRequestBackgroundLocationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 이상
                                // 일반 위치 권한이 있는지 우선 확인
                                // 정확한 위치 권한, 대략적인 위치 권한 둘 중 하나만 있어도 일반 위치 권한이 있는 것으로 간주
                                val hasLocationPermission = ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                // 일반 위치 권한이 있으면 백그라운드 위치 권한 요청
                                if (hasLocationPermission) {
                                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                            }
                        },
                        // 푸시 알림으로부터 MapScreen으로 이동해야 하는지 전달
                        shouldNavigateToMap = shouldNavigateToMap,
                        // MapScreen으로의 이동이 완료되면 shouldNavigateToMapState를 false로 리셋
                        onNavigateToMapComplete = {
                            shouldNavigateToMapState.value = false
                        },
                        // 푸시 알림으로부터 ListDetail로 이동해야 하는지 전달
                        shouldNavigateToListDetail = shouldNavigateToListDetail,
                        // ListDetail로의 이동이 완료되면 shouldNavigateToListDetailState를 null로 리셋
                        onNavigateToListDetailComplete = {
                            shouldNavigateToListDetailState.value = null
                            shouldOpenChatState.value = false
                        },
                        // 채팅 팝업을 열어야 하는지 전달
                        shouldOpenChat = shouldOpenChat
                    )
                }
            }
        }
        
        // SharedPreferences - Android에서 사용하는 간단 내장 DB
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true) // 기본값 true
        
        if (isFirstLaunch) {
            // 앱 최초 실행 시에만 알림 권한 요청
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
                // 알림 권한이 허용되지 않은 경우에 if문 진입
                if (!NotificationPermissionHelper.isNotificationPermissionGranted(this)) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // 알림 권한 요청이 완료되면 이어서 위치 권한 요청
                    requestLocationPermissionsIfNeeded()
                }
            } else {
                // Android 13 미만에서는 알림 권한이 필요 없음 -> 바로 위치 권한 요청
                requestLocationPermissionsIfNeeded()
            }
            
            // 최초 실행 플래그를 false로 설정 (이젠 최초 실행이 아니므로 앱 다시 실행해도 권한 요청 런처 안 뜸)
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
        
        // 앱 시작 시 Geofence 등록 (위치 권한이 있으면)
        LocationTrackingService.startService(this)
    }
    
    // 백그라운드에서 앱이 실행 중일 때 푸시 알림 클릭 시 호출됨
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 이미 Intent가 존재하는 상황이므로 굳이 새 Intent 만들지 말고 기존 Intent 사용
        setIntent(intent)
        
        // Intent에 navigate_to_map이 존재하면 shouldNavigateToMapState가 true, 없으면 false
        shouldNavigateToMapState.value = intent.getBooleanExtra("navigate_to_map", false)
        
        // FCM 푸시 알림 클릭 시
        // Intent에서 데이터를 읽어 ListDetail로 이동하기 위한 정보 추출
        // 백그라운드 상태일 경우 Intent extras에서 데이터 직접 가져오기
        val listId = intent.extras?.getString("listId") 
            ?: intent.getStringExtra("listId") 
            ?: intent.data?.getQueryParameter("listId")
        val navigateTo = intent.extras?.getString("navigateTo")
            ?: intent.getStringExtra("navigateTo") 
            ?: intent.data?.getQueryParameter("navigateTo")
        val openChatStr = intent.extras?.getString("openChat")
            ?: intent.getStringExtra("openChat") 
            ?: intent.data?.getQueryParameter("openChat")
        val openChat = openChatStr?.toBoolean() ?: intent.getBooleanExtra("openChat", false)
        
        // intent.data?.scheme == "tripcart"는 추후 딥링크랑 연동해서 사용할 수 있도록 미리 구현
        if ((navigateTo == "list_detail" || intent.data?.scheme == "tripcart") && !listId.isNullOrEmpty()) {
            shouldNavigateToListDetailState.value = listId
            shouldOpenChatState.value = openChat
        }
    }
    
    // 위치 권한 요청 함수
    private fun requestLocationPermissionsIfNeeded() {
        // 일반 위치 권한이 있는지 우선 확인
        // 정확한 위치 권한, 대략적인 위치 권한 둘 중 하나만 있어도 일반 위치 권한이 있는 것으로 간주
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasLocationPermission) {
            // 일반 위치 권한이 없으면 먼저 요청
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // 일반 위치 권한이 이미 있으면 백그라운드 위치 권한만 요청
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 이상
                // 백그라운드 위치 권한이 허용되지 않은 경우에 if문 진입
                if (!BackgroundLocationPermissionHelper.isBackgroundLocationPermissionGranted(this)) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }
    }
}