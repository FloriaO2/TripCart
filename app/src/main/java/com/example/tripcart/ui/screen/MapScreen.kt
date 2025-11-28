package com.example.tripcart.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.tripcart.service.LocationTrackingService
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MapScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current

    // 권한 체크 (매번 최신 상태로 계산)
    val hasLocationPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission) }

    // 여러 권한 요청하기
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.values.any { it }
    }

    // 권한 요청 (화면 진입 시) - 자세한 위치 권한 요청
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // 백그라운드 위치 추적 서비스 시작
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            // 위치 권한이 허용되면 백그라운드 추적 서비스 시작
            LocationTrackingService.startService(context)
        }
    }

    // 숙명여자대학교를 기본 위치로 설정 - f 값이 커질 수록 확대됨
    val defaultLocation = LatLng(37.545944, 126.964694)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 17f)
    }

    // 현재 위치 상태
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }


    // Google Play 위치 서비스 클라이언트 - 실제 위치 정보 가져옴
    // remember 이용해서 처음에 한번 만들어놓고 재탕 (처음부터 값이 정해져있음)
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // 지도 카메라 이동 애니메이션
    // animate()는 suspend 함수라 코루틴 스코프 안에서만 호출 가능
    // - coroutineScope 이용해서 코루틴 스코프 생성!
    val coroutineScope = rememberCoroutineScope()

    // 위치 업데이트
    // fusedLocationClient랑 비슷한 맥락이긴 한데
    // 이건 처음엔 null이었다가 추후에 값 설정됨
    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }

    // 위치 업데이트 로직: 권한 허용 시에만 등록
    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) return@LaunchedEffect

        try {
            // 1) 마지막 알려진 위치 (있으면 카메라 이동)
            val lastLocation = try {
                fusedLocationClient.lastLocation.await()
            } catch (e: Exception) {
                null
            }

            lastLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                currentLocation = latLng
                // animate()는 suspend 함수라 코루틴 스코프 안에서만 호출 가능
                coroutineScope.launch {
                    // 카메라 애니메이션 구현
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(latLng, 17f)
                        ),
                        // 0.8초동안 이동 - 더 부드럽게 이동하도록!
                        durationMs = 800
                    )
                }
            }

            // 2) 실시간 위치 업데이트 요청 - 최소 2초, 최대 4초 간격으로 업데이트
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                2000L
            ).apply {
                setMinUpdateIntervalMillis(2000L)
                setMaxUpdateDelayMillis(4000L)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        val latLng = LatLng(location.latitude, location.longitude)

                        // 이전 위치와 비교해서 3m 이상 이동했을 때만 업데이트

                        // LocationCallback은 로컬 GPS랑 네트워크 위치를 사용해서
                        // 위치가 실제로 변경되었을 때만 호출되므로
                        // 서버 부하랑 관련 없긴 한데
                        // 또 너무 자주 업데이트하면 배터리 소모가 심해서 일단 3m로 지정!

                        val shouldUpdate = currentLocation?.let { prev ->
                            val distance = FloatArray(1)
                            android.location.Location.distanceBetween(
                                prev.latitude, prev.longitude,
                                latLng.latitude, latLng.longitude,
                                distance
                            )
                            distance[0] >= 3f // 3m 이상 이동했을 때만 업데이트
                        } ?: true

                        if (shouldUpdate) {
                            currentLocation = latLng

                            // animate()는 suspend 함수라 코루틴 스코프 안에서만 호출 가능
                            coroutineScope.launch {
                                try {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.fromLatLngZoom(latLng, 17f)
                                        ),
                                        durationMs = 800
                                    )
                                } catch (e: Exception) {
                                    // animation canceled or map not ready
                                }
                            }
                        }
                    }
                }
            }

            locationCallback = callback

            fusedLocationClient.requestLocationUpdates(
                locationRequest, // 위치 업데이트 요청
                callback, // 위치 업데이트 콜백
                Looper.getMainLooper() // 메인 스레드에서 콜백 실행 - UI 업데이트 가능
            )

        } catch (se: SecurityException) {
            // 권한 문제
        } catch (e: Exception) {
            // 기타 예외 처리
        }
    }

    // Composable이 dispose될 때 화면용 위치 업데이트만 해제
    // 백그라운드 서비스는 계속 실행됨
    DisposableEffect(locationPermissionGranted) {
        onDispose {
            // 화면에서만 사용하던 위치 업데이트는 해제
            // 하지만 백그라운드 서비스는 계속 실행되어 위치 추적을 유지
            locationCallback?.let { cb ->
                try {
                    fusedLocationClient.removeLocationUpdates(cb)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "지도",
                onNotificationClick = {
                    // TODO: 알림 기능 구현
                },
                onLogoClick = onNavigateToHome
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "map",
                onItemClick = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = locationPermissionGranted
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = locationPermissionGranted,
                compassEnabled = true
            )
        )
    }
}
