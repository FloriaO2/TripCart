package com.example.tripcart.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.core.content.ContextCompat
import com.example.tripcart.data.local.entity.ListProductEntity
import com.example.tripcart.service.LocationTrackingService
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.ui.viewmodel.PlaceDetails
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 장소별 리스트 정보를 담는 데이터 클래스
data class PlaceWithLists(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lists: List<PlaceListInfo> // 해당 장소가 포함된 리스트들
)

// 상단 PlaceWithLists 중 lists에 사용
data class PlaceListInfo(
    val listId: String,
    val listName: String,
    val status: String, // "준비중", "진행중", "완료"
    val isFromFirestore: Boolean, // Firestore 리스트인지 여부
    val products: List<ListProductEntity>
)

@Composable
fun MapScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToListDetail: (String) -> Unit = {},
    listViewModel: ListViewModel
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
                    // 초기 위치 로드 시에는 항상 기본 줌 레벨(17f) 사용
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
                                    // 현재 줌 레벨 유지 (사용자가 변경한 줌 레벨 보존)
                                    val currentZoom = cameraPositionState.position.zoom
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.fromLatLngZoom(latLng, currentZoom)
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

    // 리스트 목록 가져오기
    val listUiState by listViewModel.uiState.collectAsState()
    val lists = listUiState.lists
    
    // 장소별 리스트 정보를 담는 맵 (placeId -> PlaceWithLists)
    var placesWithLists by remember { mutableStateOf<Map<String, PlaceWithLists>>(emptyMap()) }
    var isLoadingPlaces by remember { mutableStateOf(true) }
    
    // 선택된 장소 (bottom sheet 표시용)
    var selectedPlace by remember { mutableStateOf<PlaceWithLists?>(null) }
    
    // 모든 리스트의 장소들을 가져온 후 Firestore에서 위도/경도 조회
    LaunchedEffect(lists) {
        isLoadingPlaces = true
        val db = FirebaseFirestore.getInstance()
        val placeMap = mutableMapOf<String, PlaceWithLists>()
        
        try {
            // 모든 리스트를 순회하면서 장소 정보 수집
            lists.forEach { listItem ->
                listItem.places.forEach { place ->
                    try {
                        // Firestore에서 장소 정보 가져오기
                        val placeDoc = db.collection("places").document(place.placeId).get().await()
                        
                        if (placeDoc.exists()) {
                            val data = placeDoc.data
                            val latitude = (data?.get("latitude") as? Double) ?: 0.0
                            val longitude = (data?.get("longitude") as? Double) ?: 0.0
                            val placeName = data?.get("name") as? String ?: place.name
                            
                            // 위도/경도가 유효한 경우에만 추가
                            if (latitude != 0.0 && longitude != 0.0) {
                                // 이미 존재하는 장소인지 확인
                                val existingPlace = placeMap[place.placeId]
                                
                                // 해당 리스트의 상품 목록 가져오기
                                val productsFlow = if (listItem.isFromFirestore) {
                                    listViewModel.getFirestoreProductsByListId(listItem.listId)
                                } else {
                                    listViewModel.getProductsByListId(listItem.listId)
                                }
                                val products = productsFlow.first()
                                
                                val listInfo = PlaceListInfo(
                                    listId = listItem.listId,
                                    listName = listItem.name,
                                    status = listItem.status,
                                    isFromFirestore = listItem.isFromFirestore,
                                    products = products
                                )
                                
                                if (existingPlace != null) {
                                    // 이미 존재하는 장소면 리스트만 추가
                                    val updatedLists = existingPlace.lists + listInfo
                                    placeMap[place.placeId] = existingPlace.copy(lists = updatedLists)
                                } else {
                                    // 새로 추가
                                    placeMap[place.placeId] = PlaceWithLists(
                                        placeId = place.placeId,
                                        name = placeName,
                                        latitude = latitude,
                                        longitude = longitude,
                                        lists = listOf(listInfo)
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 오류 발생
                    }
                }
            }
        } catch (e: Exception) {
            // 오류 발생
        }
        
        placesWithLists = placeMap
        isLoadingPlaces = false
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
        Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL, // 위성 지도가 아닌 일반 지도 사용
                isMyLocationEnabled = locationPermissionGranted // 위치 권한 허용시 현재 위치 표시
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = locationPermissionGranted, // 위치 권한 허용시 현재 위치 버튼 표시
                compassEnabled = true // 나침반 표시
            )
            ) {
                // 마커 표시
                placesWithLists.values.forEach { placeWithLists ->
                    // 리스트들의 상태와 타입을 기반으로 마커 아이콘 생성
                    // 여러 리스트가 있을 경우 첫 번째 리스트의 상태와 타입 사용
                    val firstList = placeWithLists.lists.firstOrNull()
                    val markerIcon = if (firstList != null) {
                        createCustomMarkerIcon(
                            context = context,
                            status = firstList.status,
                            isShared = firstList.isFromFirestore
                        )
                    } else {
                        BitmapDescriptorFactory.defaultMarker() // 기본 마커 아이콘
                    }
                    
                    Marker(
                        state = MarkerState(position = LatLng(placeWithLists.latitude, placeWithLists.longitude)),
                        title = placeWithLists.name,
                        icon = markerIcon,
                        anchor = Offset(0.5f, 0.5f),
                        onClick = {
                            selectedPlace = placeWithLists
                            true // Google Maps에서의 기본 정보 창 표시 방지
                                 // Bottom Sheet로 별도의 동작을 할테니 기본 동작 하지 마세요!의 목적
                        }
                    )
                }
            }
            
            // Bottom Sheet
            selectedPlace?.let { place ->
                PlaceBottomSheet(
                    place = place,
                    onDismiss = { selectedPlace = null },
                    onNavigateToListDetail = { listId ->
                        selectedPlace = null
                        onNavigateToListDetail(listId)
                    },
                    onUpdateProductStatus = { productId, listId, boughtStatus ->
                        coroutineScope.launch {
                            listViewModel.updateProductBoughtStatus(productId, listId, boughtStatus)
                        }
                    },
                    listViewModel = listViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceBottomSheet(
    place: PlaceWithLists,
    onDismiss: () -> Unit,
    onNavigateToListDetail: (String) -> Unit,
    onUpdateProductStatus: (String, String, String) -> Unit,
    listViewModel: ListViewModel
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { place.lists.size }, initialPage = 0)
    
    // pagerState와 selectedTabIndex 동기화
    // 스와이프로 탭 변경
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }
    // 원하는 탭 클릭해서 탭 변경
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            scope.launch {
                pagerState.animateScrollToPage(selectedTabIndex)
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 장소 이름
            Text(
                text = place.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )
            
            // 탭 표시
            // TabRow - 각 리스트에 대한 탭을 동일한 너비만큼 분배!
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.White,
                contentColor = PrimaryAccent
            ) {
                place.lists.forEachIndexed { index, listInfo ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = listInfo.listName,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            
            // 상세페이지 보기 버튼
            TextButton(
                onClick = {
                    val currentList = place.lists[selectedTabIndex]
                    onNavigateToListDetail(currentList.listId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "상세페이지 보기",
                    fontSize = 14.sp,
                    color = PrimaryAccent
                )
            }
            
            // 상품 목록
            // HorizontalPager - 상품 목록 부분을 좌우로 스와이프하면 다른 탭으로 이동 가능
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
            ) { page ->
                val listInfo = place.lists[page]
                
                // 각 리스트의 상품을 Flow로 실시간 가져오기
                val productsFlow = if (listInfo.isFromFirestore) {
                    listViewModel.getFirestoreProductsByListId(listInfo.listId)
                } else {
                    listViewModel.getProductsByListId(listInfo.listId)
                }
                val products by productsFlow.collectAsState(initial = emptyList())
                
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "상품이 없습니다",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products) { product ->
                            MapProductCard(
                                product = product,
                                onStatusClick = { newStatus ->
                                    onUpdateProductStatus(product.id, listInfo.listId, newStatus)
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 커스텀 마커 아이콘 생성 함수
fun createCustomMarkerIcon(
    context: android.content.Context,
    status: String,
    isShared: Boolean
): BitmapDescriptor {
    // 마커 크기 설정
    val size = 140 // 동그란 마커 크기
    val iconSize = 100 // 중앙 아이콘 크기
    
    // 상태별 색상 설정
    val backgroundColor = when (status) {
        "준비중" -> android.graphics.Color.parseColor("#FFA500")
        "진행중" -> android.graphics.Color.parseColor("#5DADE2")
        "완료" -> android.graphics.Color.parseColor("#7C9A52")
        else -> android.graphics.Color.parseColor("#808080")
    }
    
    // 아이콘 설정
    val iconRes = if (isShared) {
        com.example.tripcart.R.drawable.lock_open
    } else {
        com.example.tripcart.R.drawable.lock
    }
    
    // 비트맵 생성
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // 배경 원 그리기
    // Paint - 그리기 도구 생성
    // ANTI_ALIAS_FLAG - 가장자리 부드럽게 처리
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = backgroundColor
    
    // 내부 원 그리기
    // RectF - 도형 크기 정의 (앞 두 자리, 뒤 두 자리 조합이 각각의 좌표값)
    // cornerRadius - 도형의 절반만큼을 둥글게 정의 = 원 생성!
    val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
    val cornerRadius = size.toFloat() / 2f
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    
    // 테두리 그리기
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    borderPaint.color = android.graphics.Color.WHITE // Paint에선 이런 식으로 색상 지정해야 함
                                                     // Color.White는 Compose용!
    borderPaint.style = Paint.Style.STROKE // 테두리만 그리기
    borderPaint.strokeWidth = 8f // 두께 증가
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    
    // 중앙 아이콘 그리기
    // ContextCompat - Drawable 리소스 불러오기 (Android가 권한 체크를 자동으로 처리해줘서 편리!)
    val iconDrawable = ContextCompat.getDrawable(context, iconRes)
    iconDrawable?.let { drawable ->
        // 아이콘 비트맵 생성
        val iconBitmap = drawable.toBitmap(iconSize, iconSize)
        
        // 중앙에 아이콘 배치
        val iconLeft = (size - iconSize) / 2f
        val iconTop = (size - iconSize) / 2f
        
        val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        canvas.drawBitmap(iconBitmap, null, iconRect, null)
    }
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

// MapScreen용 ProductCard
@Composable
fun MapProductCard(
    product: ListProductEntity,
    onStatusClick: (String) -> Unit
) {
    val context = LocalContext.current
    val images = product.imageUrls ?: emptyList()
    val hasImages = images.isNotEmpty()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 이미지가 있을 때만 상단에 표시
            if (hasImages) {
                if (images.size == 1) {
                    // 이미지가 한 장일 때
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(images[0])
                            .build()
                    )
                    val imageSize = painter.intrinsicSize
                    val isImageLoaded = painter.state is AsyncImagePainter.State.Success
                    val aspectRatio = if (isImageLoaded && imageSize.width > 0f && imageSize.height > 0f) {
                        imageSize.width / imageSize.height
                    } else {
                        4f / 3f
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // 이미지가 여러 장이면 HorizontalPager 사용
                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { images.size }
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(context)
                                            .data(images[page])
                                            .build()
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        // 페이지 인디케이터 (하단에 이미지 index 보여주는 점)
                        if (images.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(images.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(
                                                width = if (pagerState.currentPage == index) 8.dp else 6.dp,
                                                height = 6.dp
                                            )
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (pagerState.currentPage == index) 
                                                    Color.White 
                                                else 
                                                    Color.White.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 상품 박스 상단 영역 (체크박스, 상품 이름, 수량, 카테고리)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 좌측 - 체크박스 + 상품 정보 (상품 이름, 수량, 카테고리)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 체크박스
                    StatusCheckbox(
                        bought = product.bought,
                        onClick = {
                            val nextStatus = when (product.bought) {
                                "구매전" -> "구매중"
                                "구매중" -> "구매완료"
                                "구매완료" -> "구매전"
                                else -> "구매전"
                            }
                            onStatusClick(nextStatus)
                        }
                    )
                    
                    // 상품 정보 (상품 이름, 수량, 카테고리)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // 상품 이름
                        Text(
                            text = product.productName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp
                        )
                        
                        // 수량, 카테고리
                        if (product.quantity > 0 || product.category.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 수량
                                if (product.quantity > 0) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xC36A1B9A),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Text(
                                                text = "${product.quantity}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                lineHeight = 20.sp
                                            )
                                            Text(
                                                text = "개",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                lineHeight = 15.sp,
                                                modifier = Modifier.padding(bottom = 1.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // 카테고리
                                if (product.category.isNotEmpty()) {
                                    Text(
                                        text = product.category,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 상품 박스 하단 영역 (메모)
            if (!product.note.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.Gray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = Color.Gray.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = product.note!!,
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
