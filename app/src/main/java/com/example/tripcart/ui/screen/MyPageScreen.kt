package com.example.tripcart.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.example.tripcart.util.BackgroundLocationPermissionHelper
import com.example.tripcart.util.NotificationPermissionHelper
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onSignOut: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestBackgroundLocationPermission: () -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 알림 권한 상태
    var isNotificationEnabled by remember {
        mutableStateOf(NotificationPermissionHelper.isNotificationPermissionGranted(context))
    }
    
    // 백그라운드 위치 권한 상태
    var isBackgroundLocationEnabled by remember {
        mutableStateOf(BackgroundLocationPermissionHelper.isBackgroundLocationPermissionGranted(context))
    }
    
    // Android 13 이상에서만 알림 권한 설정 표시
    val showNotificationSetting = NotificationPermissionHelper.isNotificationPermissionRequired()
    
    // Android 10 이상에서만 백그라운드 위치 권한 설정 표시
    val showBackgroundLocationSetting = BackgroundLocationPermissionHelper.isBackgroundLocationPermissionRequired()
    
    // 권한 상태를 업데이트하는 함수
    fun updatePermissionStates() {
        isNotificationEnabled = NotificationPermissionHelper.isNotificationPermissionGranted(context)
        isBackgroundLocationEnabled = BackgroundLocationPermissionHelper.isBackgroundLocationPermissionGranted(context)
    }
    
    // Activity가 다시 활성화될 때마다 권한 상태 업데이트
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { // Activity가 다시 활성화 (설정 앱에서 돌아왔을 때)
                updatePermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { // 다른 화면으로 이동, 앱 종료, Composable 컴포저블 제거 등의 상황에서 실행
            lifecycleOwner.lifecycle.removeObserver(observer) // 수동 제거 - 메모리 누수 방지
        }
    }
    
    // 초기 권한 상태 확인
    LaunchedEffect(Unit) {
        updatePermissionStates()
    }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "마이페이지",
                onNotificationClick = {
                    // TODO: 알림 기능 구현
                },
                onLogoClick = onNavigateToHome
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "my_page",
                onItemClick = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 사용자 정보
            user?.let {
                it.displayName?.let { name ->
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                it.email?.let { email ->
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
            
            // 알림 권한 설정 (Android 13 이상)
            if (showNotificationSetting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "푸시 알림 권한",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "채팅 및 근처 장소 알림을 받기 위해서는\n푸시 알림 권한 허용이 필요합니다.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // 알림 권한 요청
                                    onRequestNotificationPermission()
                                    coroutineScope.launch {
                                        // 마이페이지 Switch에 권한 여부를 실시간으로 반영하기 위해 확인 절차 반복
                                        repeat(5) {
                                            delay(200)
                                            updatePermissionStates()
                                            // 권한이 허용되면 즉시 중단
                                            if (isNotificationEnabled) {
                                                return@launch
                                            }
                                        }
                                    }
                                } else {
                                    // Android에서는 권한을 직접 비활성화할 수 없기 때문에 앱 상세 설정 화면으로 이동시킴
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFDC3545) // 비활성화시 배경색
                            )
                        )
                    }
                }
            }
            
            // 백그라운드 위치 권한 설정 (Android 10 이상)
            if (showBackgroundLocationSetting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                            // 마이페이지 Switch에 권한 여부를 실시간으로 반영하기 위해 확인 절차 반복
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "백그라운드 위치 권한",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text ="근처 장소 알림을 받기 위해서는\n백그라운드 위치 권한 허용이 필요합니다.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = isBackgroundLocationEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // 백그라운드 위치 권한 요청
                                    onRequestBackgroundLocationPermission()
                                    coroutineScope.launch {
                                        // 마이페이지 Switch에 권한 여부를 실시간으로 반영하기 위해 확인 절차 반복
                                        repeat(5) {
                                            delay(200)
                                            updatePermissionStates()
                                            // 권한이 허용되면 즉시 중단
                                            if (isBackgroundLocationEnabled) {
                                                return@launch
                                            }
                                        }
                                    }
                                } else {
                                    // Android에서는 권한을 직접 비활성화할 수 없기 때문에 앱 상세 설정 화면으로 이동시킴
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFDC3545) // 비활성화시 배경색
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 로그아웃 버튼
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC3545),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "로그아웃",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "로그아웃",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

