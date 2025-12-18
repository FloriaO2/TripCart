package com.example.tripcart.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.tripcart.data.local.entity.ListProductEntity
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.theme.BoxBackground
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.ui.viewmodel.PlaceDetails
import com.example.tripcart.ui.viewmodel.NotificationViewModel
import com.example.tripcart.util.SetStatusBarColor
import com.example.tripcart.R
import androidx.compose.ui.res.painterResource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tripcart.ui.components.InviteCodeDialog
import com.example.tripcart.ui.components.InviteCodeDisplayDialog
import com.example.tripcart.ui.components.ChatDialog
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: String,
    onBack: () -> Unit,
    onEditList: () -> Unit = {},
    onEditProduct: (String, String) -> Unit = { _, _ -> }, // productId, listId
    onGroupAddClick: () -> Unit = {}, // 그룹 추가 버튼 클릭
    openChatOnStart: Boolean = false, // 푸시 알림으로부터 이동 시 채팅 팝업 자동 열기
    onAddPlace: () -> Unit = {}, // 상점 추가 버튼 클릭
    onAddProduct: () -> Unit = {}, // 상품 추가 버튼 클릭
    viewModel: ListViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 리스트 정보
    var listEntity by remember { mutableStateOf<com.example.tripcart.data.local.entity.ListEntity?>(null) }
    var placesDetails by remember { mutableStateOf<List<PlaceDetails>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 초대코드 관련 상태
    var showInviteCodeDialog by remember { mutableStateOf(false) }
    var showInviteCodeDisplayDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    
    // 리스트가 Firestore에 있는지 확인
    var isFirestoreList by remember { mutableStateOf<Boolean?>(null) }
    
    // 참여자 정보
    var participantInfo by remember { mutableStateOf<com.example.tripcart.ui.viewmodel.ListParticipantInfo?>(null) }
    
    // 상품 목록 - 리스트가 Firestore에 있으면 Firestore에서, 없으면 Room DB에서 가져오기
    val productsFlow = when (isFirestoreList) {
        true -> viewModel.getFirestoreProductsByListId(listId)
        false -> viewModel.getProductsByListId(listId)
        null -> kotlinx.coroutines.flow.flowOf(emptyList()) // 아직 확인 중
    }
    val products by productsFlow.collectAsState(initial = emptyList())
    
    // 이미지 확대 오버레이
    var expandedImageIndex by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    
    // 채팅 다이얼로그 표시 여부
    var showChatDialog by remember { mutableStateOf(false) }
    
    // 읽지 않은 채팅 알림 확인
    val notificationState by notificationViewModel.uiState.collectAsState()
    val hasUnreadChat = notificationState.unreadChatListIds.contains(listId)
    
    // 장소 삭제 관련
    var showDeletePlaceDialog by remember { mutableStateOf(false) }
    var placeToDelete by remember { mutableStateOf<PlaceDetails?>(null) }
    
    // 리스트 이름 편집 관련
    var showEditListNameDialog by remember { mutableStateOf(false) }
    var editedListName by remember { mutableStateOf("") }
    
    // Firestore에 리스트가 있는지 확인
    LaunchedEffect(listId) {
        isFirestoreList = viewModel.hasInviteCode(listId)
    }
    
    // 리스트 정보 실시간 업데이트 (Firestore 리스트인 경우 Flow 사용)
    var isFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(isFirestoreList, listId) {
        if (isFirestoreList == true) {
            isFirstLoad = true
            viewModel.getFirestoreListDetailFlow(listId).collect { entity ->
                if (entity != null) {
                    if (isFirstLoad) {
                        isLoading = true
                        isFirstLoad = false
                    }
                    listEntity = entity
                }
            }
        } else if (isFirestoreList == false) {
            // Room DB 리스트: 한 번만 로드
            isLoading = true
            listEntity = viewModel.getListDetail(listId)
        }
    }
    
    // 공유 리스트일 때 참여자 정보 실시간 업데이트
    LaunchedEffect(isFirestoreList, listId) {
        if (isFirestoreList == true) {
            viewModel.getListParticipantsFlow(listId).collect { result ->
                result.onSuccess {
                    participantInfo = it
                }.onFailure {
                    // 에러 처리
                }
            }
        }
    }
    
    // 푸시 알림으로부터 이동한 경우 채팅 팝업 자동 열기
    LaunchedEffect(isFirestoreList, openChatOnStart) {
        if (openChatOnStart && isFirestoreList == true) {
            // Firestore 리스트 확인이 완료된 후 채팅 팝업 열기
            showChatDialog = true
        }
    }
    
    // 리스트 정보가 업데이트되면 firestore의 places 컬렉션 이용해서 placesDetails도 업데이트
    LaunchedEffect(listEntity) {
        if (listEntity != null) {
            val db = FirebaseFirestore.getInstance()
            placesDetails = listEntity!!.places.map { place ->
                try {
                    // Firestore에서 상점 상세 정보 가져오기 시도
                    val placeDoc = db.collection("places").document(place.placeId).get().await()
                    if (placeDoc.exists()) {
                        val data = placeDoc.data
                        PlaceDetails(
                            placeId = data?.get("placeId") as? String ?: place.placeId,
                            name = data?.get("name") as? String ?: place.name,
                            latitude = (data?.get("latitude") as? Double) ?: 0.0,
                            longitude = (data?.get("longitude") as? Double) ?: 0.0,
                            address = data?.get("address") as? String,
                            country = data?.get("country") as? String,
                            phoneNumber = data?.get("phoneNumber") as? String,
                            websiteUri = data?.get("websiteUri") as? String,
                            openingHours = (data?.get("openingHours") as? List<*>)?.mapNotNull { it as? String },
                            photoUrl = data?.get("photoUrl") as? String
                        )
                    } else {
                        // Firestore에 없으면 Room DB의 기본 정보만 사용
                        PlaceDetails(
                            placeId = place.placeId,
                            name = place.name,
                            latitude = 0.0,
                            longitude = 0.0,
                            address = null,
                            country = null,
                            phoneNumber = null,
                            websiteUri = null,
                            openingHours = null,
                            photoUrl = null
                        )
                    }
                } catch (e: Exception) {
                    // 네트워크 오류나 Firestore 조회 실패 시 Room DB의 기본 정보만 사용
                    android.util.Log.w("ListDetailScreen", "Failed to get place details from Firestore, using Room DB data: ${e.message}")
                    PlaceDetails(
                        placeId = place.placeId,
                        name = place.name,
                        latitude = 0.0,
                        longitude = 0.0,
                        address = null,
                        country = null,
                        phoneNumber = null,
                        websiteUri = null,
                        openingHours = null,
                        photoUrl = null
                    )
                }
            }
            
            // placesDetails까지 모두 가져왔으니 로딩 상태 해제
            isLoading = false
        }
    }
    
    // 상태바 색상을 상단바와 동일하게 설정
    SetStatusBarColor(
        statusBarColor = SecondaryBackground,
        isLightStatusBars = true
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = TertiaryBackground,
            topBar = {
                // 커스텀 상단바: 좌측 뒤로가기, 중앙 리스트이름, 우측 편집 아이콘
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SecondaryBackground
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // 좌측 뒤로가기
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "뒤로가기"
                            )
                        }
                        
                        // 중앙 리스트 이름
                        Text(
                            text = listEntity?.name ?: "",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // 우측 편집 아이콘 + 공유 리스트인 경우 채팅 아이콘
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 편집 아이콘 - read 권한이 아닐 때만 표시
                            val currentUserRole = participantInfo?.currentUserRole
                            val canEdit = when {
                                // 공유 리스트인 경우 participantInfo가 로드될 때까지 대기
                                isFirestoreList == true -> {
                                    when (currentUserRole) {
                                        "owner", "edit" -> true
                                        "read" -> false
                                        null -> false // participantInfo 로드 전
                                        else -> false
                                    }
                                }
                                // 개인 리스트는 항상 편집 가능
                                else -> true
                            }
                            
                            if (canEdit) {
                                IconButton(
                                    onClick = {
                                        editedListName = listEntity?.name ?: ""
                                        showEditListNameDialog = true
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "리스트 이름 편집"
                                    )
                                }
                            }
                            
                            // 공유 리스트인 경우 채팅 아이콘
                            if (isFirestoreList == true) {
                                Box(
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            showChatDialog = true
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.chat),
                                            contentDescription = "채팅"
                                        )
                                    }
                                    // 읽지 않은 채팅 뱃지
                                    if (hasUnreadChat) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-7).dp, y = 7.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (listEntity == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("리스트를 찾을 수 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 최상단: 공유 리스트 / 개인 리스트 구분
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 24.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            // 리스트 타입 표시와 국가 태그
                            val isShared = participantInfo?.isShared == true
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isShared) "공유 리스트" else "개인 리스트",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccent
                                )
                                
                                // 국가 태그
                                if (listEntity?.country != null) {
                                    CountryTag(country = listEntity!!.country!!)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 개인 리스트 안내 또는 공유 리스트 정보
                            if (!isShared) {
                                // 개인 리스트 안내
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color(0xC1FFFFFF),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color.Gray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(24.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // 상단: 상태 뱃지와 초대코드 버튼
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 좌측: 진행 상태 텍스트와 상태 뱃지
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                StatusBadge(
                                                    status = listEntity!!.status,
                                                    onClick = {
                                                        scope.launch {
                                                            val nextStatus = when (listEntity!!.status) {
                                                                "준비중" -> "진행중"
                                                                "진행중" -> "완료"
                                                                "완료" -> "준비중"
                                                                else -> "준비중"
                                                            }
                                                            viewModel.updateListStatus(listId, nextStatus)
                                                            listEntity = listEntity!!.copy(status = nextStatus)
                                                        }
                                                    }
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ){
                                                    Text(
                                                        text = "진행 상태",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF333333)
                                                    )
                                                    Text(
                                                        text = "탭하여 변경 가능",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF666666),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }
                                            
                                            // 우측: 그룹 추가 버튼 (초대코드 발급)
                                            Box(
                                                modifier = Modifier
                                                    .background(color = PrimaryAccent, shape = RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        showInviteCodeDialog = true
                                                    }
                                                    .padding(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.group_add),
                                                    contentDescription = "그룹 추가",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        
                                        // 중간: 설명
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 5.dp)
                                        ) {
                                            Text(
                                                text = "나만 사용할 수 있는 리스트입니다.",
                                                fontSize = 14.sp,
                                                color = Color(0xFF333333),
                                                lineHeight = 20.sp
                                            )

                                            Text(
                                                text = "네트워크 연결과 무관하게 사용 가능합니다.",
                                                fontSize = 14.sp,
                                                color = Color(0xFF333333),
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 공유 리스트 정보
                                participantInfo?.let { info ->
                                    var isParticipantsExpanded by remember { mutableStateOf(false) }
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0xC1FFFFFF),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.Gray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // 상단: 상태 뱃지와 초대코드 버튼
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 좌측: 진행 상태 텍스트와 상태 뱃지
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                StatusBadge(
                                                    status = listEntity!!.status,
                                                    onClick = {
                                                        scope.launch {
                                                            val nextStatus = when (listEntity!!.status) {
                                                                "준비중" -> "진행중"
                                                                "진행중" -> "완료"
                                                                "완료" -> "준비중"
                                                                else -> "준비중"
                                                            }
                                                            viewModel.updateListStatus(listId, nextStatus)
                                                            listEntity = listEntity!!.copy(status = nextStatus)
                                                        }
                                                    }
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ){
                                                    Text(
                                                        text = "진행 상태",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF333333)
                                                    )
                                                    Text(
                                                        text = "탭하여 변경 가능",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF666666),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }
                                            
                                            // 우측: 그룹 추가 버튼 (초대코드 열람)
                                            Box(
                                                modifier = Modifier
                                                    .background(color = PrimaryAccent, shape = RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        scope.launch {
                                                            val code = viewModel.getInviteCode(listId)
                                                            if (code != null) {
                                                                inviteCode = code
                                                                showInviteCodeDisplayDialog = true
                                                            }
                                                        }
                                                    }
                                                    .padding(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.invitekey),
                                                    contentDescription = "그룹 추가",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        
                                        // 중간: 참여자 권한 표시
                                        val currentUserRole = info.currentUserRole ?: "read"
                                        val roleText = when (currentUserRole) {
                                            "owner" -> "edit"
                                            "edit" -> "edit"
                                            "read" -> "read"
                                            else -> "read"
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = Color(0xFF4F8A8B),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = roleText,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Text(
                                                text = "나의 권한",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF333333)
                                            )
                                        }
                                        
                                        // 권한 설명
                                        val right = when (currentUserRole) {
                                            "owner" -> "edit" // 소유자는 edit 권한과 동일
                                            else -> currentUserRole
                                        }
                                        
                                        if (right == "read" || right == "edit") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = Color(0x4363AEAF),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = Color.Gray.copy(alpha = 0.3f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (right == "read") {
                                                        "상품 구매 여부만 수정 가능"
                                                    } else {
                                                        "상품 추가, 상품 삭제 등 모든 수정 가능"
                                                    },
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF3A3A3A),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                        
                                        // 하단: 참여자 목록 (토글)
                                        if (info.participants.isNotEmpty()) {
                                            val currentUserId = info.currentUserId
                                            
                                            // 방장 찾기
                                            val owner = info.participants.find { it.role == "owner" }
                                            
                                            // 방장을 제외한 다른 참여자들
                                            val otherParticipants = info.participants.filter { 
                                                it.userId != owner?.userId
                                            }
                                            
                                            val totalMembers = info.participants.size
                                            
                                            // 참여자 목록
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 4.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    // 토글 버튼
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { isParticipantsExpanded = !isParticipantsExpanded },
                                                        horizontalArrangement = Arrangement.Start,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isParticipantsExpanded) 
                                                                Icons.Default.ExpandLess else 
                                                                Icons.Default.ExpandMore,
                                                            contentDescription = if (isParticipantsExpanded) "접기" else "펼치기",
                                                            modifier = Modifier.size(20.dp),
                                                            tint = Color(0xFF666666)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "멤버 (${totalMembers}명)",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF666666)
                                                        )
                                                    }
                                                    
                                                    // 펼쳐진 참여자 목록
                                                    if (isParticipantsExpanded) {
                                                        Column(
                                                            modifier = Modifier.padding(start = 28.dp),
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            // 방장 표시
                                                            owner?.let { ownerParticipant ->
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.Start,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Image(
                                                                        painter = painterResource(id = R.drawable.crown),
                                                                        contentDescription = "방장",
                                                                        modifier = Modifier.size(20.dp),
                                                                        colorFilter = ColorFilter.tint(Color(0xFFFF8C00))
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = ownerParticipant.name,
                                                                        fontSize = 16.sp,
                                                                        color = Color(0xFF333333),
                                                                        fontWeight = if (ownerParticipant.userId == currentUserId) FontWeight.Bold else FontWeight.Normal
                                                                    )
                                                                }
                                                            }
                                                            
                                                            // 다른 참여자들 표시
                                                            otherParticipants.forEach { participant ->
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.Start,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Person,
                                                                        contentDescription = "참여자",
                                                                        modifier = Modifier.size(20.dp),
                                                                        tint = Color(0xFF666666)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = participant.name,
                                                                        fontSize = 16.sp,
                                                                        color = Color(0xFF333333),
                                                                        fontWeight = if (participant.userId == currentUserId) FontWeight.Bold else FontWeight.Normal
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 상점 구분
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        SectionDivider(
                            title = "상점",
                            iconId = R.drawable.store,
                            verticalPadding = 16.dp,
                            onAddClick = onAddPlace
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    
                    // 상점 섹션
                    if (placesDetails.isNotEmpty()) {
                        item {
                            val lazyListState = rememberLazyListState()
                            val flingBehavior = rememberSnapFlingBehavior(
                                lazyListState = lazyListState
                            )
                            
                            LazyRow(
                                state = lazyListState,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
                                flingBehavior = flingBehavior
                            ) {
                                items(placesDetails.size) { index ->
                                    val place = placesDetails[index]
                                    // 현재 사용자의 권한 확인
                                    val currentUserRole = participantInfo?.currentUserRole
                                    val canEditPlace = when {
                                        // 공유 리스트인 경우 participantInfo가 로드될 때까지 대기
                                        isFirestoreList == true -> {
                                            when (currentUserRole) {
                                                "owner", "edit" -> true
                                                "read" -> false
                                                null -> false // participantInfo 로드 전
                                                else -> false
                                            }
                                        }
                                        // 개인 리스트는 항상 편집 가능
                                        else -> true
                                    }
                                    
                                    PlaceCard(
                                        place = place,
                                        totalPlacesCount = placesDetails.size,
                                        onMapClick = {
                                            // 구글맵 길찾기 연동
                                            // placeId는 실제 구글맵 검색과 호환 안 되는 경우가 많아,
                                            // 길찾기 시에는 위도/경도만 사용
                                            val uri = if (place.placeId.isNotEmpty()) {
                                                Uri.parse("https://www.google.com/maps/dir/?api=1&origin=current+location&destination=${place.latitude},${place.longitude}")
                                            } else if (place.latitude != 0.0 && place.longitude != 0.0) {
                                                Uri.parse("https://www.google.com/maps/dir/?api=1&origin=current+location&destination=${place.latitude},${place.longitude}")
                                            } else {
                                                null
                                            }
                                            
                                            uri?.let {
                                                val intent = Intent(Intent.ACTION_VIEW, it)
                                                context.startActivity(intent)
                                            }
                                        },
                                        onDeleteClick = if (canEditPlace) {
                                            {
                                                placeToDelete = place
                                                showDeletePlaceDialog = true
                                            }
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // 상점이 없을 때
                        item {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "상점이 없습니다.",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    }
                    
                    // 상품 구분
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        SectionDivider(
                            title = "상품",
                            iconId = R.drawable.bag,
                            verticalPadding = 16.dp,
                            onAddClick = onAddProduct
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // 상품 섹션
                    if (products.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 55.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "상품이 없습니다.",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        items(products) { product ->
                            // 현재 사용자의 권한 확인
                            val currentUserRole = participantInfo?.currentUserRole
                            val canEdit = when (currentUserRole) {
                                "owner", "edit" -> true
                                "read" -> false
                                else -> true // 개인 리스트는 항상 편집 가능
                            }
                            
                            ProductCard(
                                product = product,
                                onStatusClick = { newStatus ->
                                    scope.launch {
                                        viewModel.updateProductBoughtStatus(product.id, listId, newStatus)
                                    }
                                },
                                onEditClick = {
                                    onEditProduct(product.id, listId)
                                },
                                onImageClick = { imageIndex ->
                                    val images = product.imageUrls ?: emptyList()
                                    if (images.isNotEmpty()) {
                                        expandedImageIndex = Pair(imageIndex, images)
                                    }
                                },
                                canEdit = canEdit
                            )
                        }
                        
                        // 마지막 상품 하단에 여백 추가
                        item {
                            Spacer(modifier = Modifier.height(22.dp))
                        }
                    }
                }
            }
        }
        
        // 이미지 확대 오버레이
        expandedImageIndex?.let { (initialIndex, images) ->
            if (images.isNotEmpty()) {
                val pagerState = rememberPagerState(
                    initialPage = initialIndex,
                    pageCount = { images.size }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 상단 바 (뒤로가기, 인덱스)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 좌측 뒤로가기 버튼
                            IconButton(
                                onClick = { expandedImageIndex = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "뒤로가기",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // 중앙 인덱스 표시
                            Text(
                                text = "${pagerState.currentPage + 1} / ${images.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // 우측 빈 공간 (레이아웃 균형을 위해)
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                        
                        // 이미지 슬라이더
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(images[page])
                                                .build()
                                        ),
                                        contentDescription = "확대된 이미지",
                                        modifier = Modifier
                                            .fillMaxWidth(0.95f)
                                            .fillMaxHeight(0.9f),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 초대코드 발급 다이얼로그
        if (showInviteCodeDialog) {
            InviteCodeDialog(
                onDismiss = { showInviteCodeDialog = false },
                onGenerateInviteCode = { right, nickname ->
                    scope.launch {
                        val result = viewModel.generateInviteCode(listId, right, nickname)
                        result.onSuccess { code ->
                            inviteCode = code
                            showInviteCodeDialog = false
                            showInviteCodeDisplayDialog = true
                            
                            // 초대코드 발급 후 즉시 공유 리스트 UI로 전환
                            // Firestore 리스트 버전으로 변경 후 리스트 불러오기
                            isFirestoreList = true
                            listEntity = viewModel.getListDetail(listId)
                        }.onFailure { e ->
                            // 에러 처리
                        }
                    }
                }
            )
        }
        
        // 초대코드 표시 다이얼로그
        if (showInviteCodeDisplayDialog && inviteCode != null) {
            InviteCodeDisplayDialog(
                inviteCode = inviteCode!!, // !! - Not Null 보장
                onDismiss = {
                    showInviteCodeDisplayDialog = false
                    inviteCode = null // 메모리 정리 목적
                }
            )
        }
        
        // 채팅 다이얼로그
        if (showChatDialog && isFirestoreList == true) {
            // 채팅 팝업이 열릴 때 해당 리스트의 채팅 알림 모두 읽음 처리
            LaunchedEffect(showChatDialog) {
                if (showChatDialog) {
                    notificationViewModel.markChatNotificationsAsRead(listId)
                }
            }
            
            ChatDialog(
                listId = listId,
                onDismiss = { showChatDialog = false },
                viewModel = viewModel
            )
        }
        
        // 장소 삭제 확인 다이얼로그
        if (showDeletePlaceDialog && placeToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeletePlaceDialog = false
                    placeToDelete = null
                },
                title = {
                    Text("장소 삭제")
                },
                text = {
                    Text("정말로 ${placeToDelete?.name} 상점을 삭제하시겠습니까?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            placeToDelete?.let { place ->
                                scope.launch {
                                    val result = viewModel.removePlaceFromList(
                                        listId = listId,
                                        placeId = place.placeId,
                                        isFirestoreList = isFirestoreList == true
                                    )
                                    result.onSuccess {
                                        // 성공 시 listEntity 다시 로드 (placesDetails는 LaunchedEffect에서 자동 업데이트됨)
                                        listEntity = viewModel.getListDetail(listId)
                                    }
                                    showDeletePlaceDialog = false
                                    placeToDelete = null
                                }
                            }
                        }
                    ) {
                        Text("예", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeletePlaceDialog = false
                            placeToDelete = null
                        }
                    ) {
                        Text("아니오")
                    }
                }
            )
        }
        
        // 리스트 이름 편집 다이얼로그
        if (showEditListNameDialog) {
            AlertDialog(
                onDismissRequest = {
                    showEditListNameDialog = false
                    editedListName = ""
                },
                title = {
                    Text("리스트 이름 편집")
                },
                text = {
                    OutlinedTextField(
                        value = editedListName,
                        onValueChange = { editedListName = it },
                        label = { Text("리스트 이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editedListName.isNotBlank()) {
                                scope.launch {
                                    val result = viewModel.updateListName(
                                        listId = listId,
                                        newName = editedListName.trim(),
                                        isFirestoreList = isFirestoreList == true
                                    )
                                    result.onSuccess {
                                        // 성공 시 listEntity 다시 로드
                                        listEntity = viewModel.getListDetail(listId)
                                    }
                                    showEditListNameDialog = false
                                    editedListName = ""
                                }
                            }
                        },
                        enabled = editedListName.isNotBlank()
                    ) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditListNameDialog = false
                            editedListName = ""
                        }
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    onClick: () -> Unit
) {
    val backgroundColor = when(status) {
        "준비중" -> Color(0xFFFFA500)
        "진행중" -> Color(0xFF5DADE2)
        "완료" -> Color(0xFF7C9A52)
        else -> Color.Gray
    }
    
    Box(
        modifier = Modifier
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlaceCard(
    place: PlaceDetails,
    totalPlacesCount: Int,
    onMapClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // 장소가 하나일 때는 90%로 해서 꽉 차게,
    // 여러 개일 떄는 다음 장소 카드가 보일 수 있도록 85%로 설정하여 살짝 모자라게
    val cardWidth = screenWidth * if (totalPlacesCount == 1) 0.90f else 0.85f
    
    Card(
        modifier = Modifier
            .width(cardWidth)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 이미지가 있을 때만 표시
            if (!place.photoUrl.isNullOrEmpty()) {
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(place.photoUrl)
                        .build()
                )
                val imageSize = painter.intrinsicSize
                val isImageLoaded = painter.state is AsyncImagePainter.State.Success
                val aspectRatio = if (isImageLoaded && imageSize.width > 0f && imageSize.height > 0f) {
                    imageSize.width / imageSize.height
                } else {
                    4f / 3f
                }
                
                val imageWidth = if (aspectRatio > 1f) {
                    288.dp
                } else {
                    192.dp
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageWidth / aspectRatio)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // 장소 이름과 삭제 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = place.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 삭제 버튼
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "장소 삭제",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // 장소에 대한 하위 요소들 상하 간격을 조절하기 위한 Column
            Column{
                // 주소
                if (!place.address.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryAccent
                            )
                            Text(
                                text = place.address,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // 길찾기 버튼
                        IconButton(
                            onClick = onMapClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.map),
                                contentDescription = "길찾기",
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryAccent
                            )
                        }
                    }
                }
                
                // 전화번호
                if (!place.phoneNumber.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${place.phoneNumber}")
                            }
                            context.startActivity(intent)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryAccent
                        )
                        Text(
                            text = place.phoneNumber,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // 웹사이트
                if (!place.websiteUri.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(place.websiteUri)
                            }
                            context.startActivity(intent)
                        }
                            // 전화번호 - 웹사이트 사이 여백이 묘하게 좁아보여서 여백 통일시키기 위해 추가
                            .padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.link),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryAccent
                        )
                        Text(
                            text = place.websiteUri,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // 운영 시간
            if (!place.openingHours.isNullOrEmpty()) {
                var isExpanded by remember { mutableStateOf(false) }
                
                // 현재 시간 기준으로 해당 나라의 요일 계산
                val currentDayOfWeek = remember(place.country) {
                    val calendar = Calendar.getInstance()
                    // 기기 시간대를 이용해 요일 계산
                    // 해외로 나가면 기기 시간대 자체가 해외 기준으로 바뀌므로 문제 없이 사용 가능!
                    calendar.timeZone = TimeZone.getDefault()
                    calendar.get(Calendar.DAY_OF_WEEK)
                }
                
                // 요일 이름 매핑 (Calendar.DAY_OF_WEEK: 1=일요일, 2=월요일, ..., 7=토요일)
                val dayNames = listOf("일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일")
                val shortDayNames = listOf("일", "월", "화", "수", "목", "금", "토")
                val currentDayName = dayNames[currentDayOfWeek - 1]
                
                // 오늘의 운영시간 찾기
                val todayHours = place.openingHours.find { it.startsWith(currentDayName) }
                    ?.substringAfter(": ") ?: "정보 없음"
                
                // 전체 요일별 운영시간 파싱
                val parsedHours = place.openingHours.mapNotNull { hourText ->
                    val parts = hourText.split(": ", limit = 2)
                    if (parts.size == 2) {
                        val dayName = parts[0]
                        val hours = parts[1]
                        val dayIndex = dayNames.indexOf(dayName)
                        if (dayIndex >= 0) {
                            Triple(dayIndex, dayName, hours)
                        } else null
                    } else null
                }.sortedBy { it.first }
                
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // 상단: 시계 아이콘 + '운영 시간' 텍스트 + 토글 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.clock),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryAccent
                            )
                            Text(
                                text = "운영 시간",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // 토글 버튼
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "접기" else "펼치기",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                    
                    // 오늘의 운영시간 표시 (토글이 닫혀있을 때만)
                    if (!isExpanded) {
                        val todayIsClosed = todayHours.contains("휴무") || todayHours.contains("Closed") || todayHours.trim().isEmpty() || todayHours == "정보 없음"
                        val todayDayIndex = currentDayOfWeek - 1
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 요일 이름 (배경색 + border)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (todayIsClosed) 
                                            Color(0xFFFFE5E5) // 휴무일은 붉은 계열
                                        else 
                                            Color(0xFFE5F3FF), // 일반 요일은 파란 계열
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = PrimaryAccent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortDayNames[todayDayIndex],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (todayIsClosed) 
                                        Color(0xFFCC0000) // 휴무일은 붉은 계열
                                    else 
                                        Color(0xFF0066CC) // 일반 요일은 파란 계열
                                )
                            }
                            
                            // 운영 시간
                            Text(
                                text = if (todayIsClosed) "휴무" else todayHours,
                                fontSize = 14.sp,
                                color = if (todayIsClosed) Color(0xFFCC0000) else Color.Gray
                            )
                        }
                    }
                    
                    // 전체 요일 운영시간 (토글 열렸을 때만 표시)
                    if (isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            parsedHours.forEach { (dayIndex, dayName, hours) ->
                                val isClosed = hours.contains("휴무") || hours.contains("Closed") || hours.trim().isEmpty()
                                val isToday = dayIndex == currentDayOfWeek - 1
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 요일 이름 (배경색 + 오늘에 해당하는 요일에만 border 추가)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isClosed) 
                                                    Color(0xFFFFE5E5) // 휴무일은 붉은 계열
                                                else 
                                                    Color(0xFFE5F3FF), // 일반 요일은 파란 계열
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .then(
                                                if (isToday) {
                                                    Modifier.border(
                                                        width = 2.dp,
                                                        color = PrimaryAccent,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortDayNames[dayIndex],
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isClosed) 
                                                Color(0xFFCC0000) // 휴무일은 붉은 계열
                                            else 
                                                Color(0xFF0066CC) // 일반 요일은 파란 계열
                                        )
                                    }
                                    
                                    // 운영 시간
                                    Text(
                                        text = if (isClosed) "휴무" else hours,
                                        fontSize = if (isToday) 14.sp else 12.sp,
                                        color = if (isClosed) Color(0xFFCC0000) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ListProductEntity,
    onStatusClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onImageClick: (Int) -> Unit,
    canEdit: Boolean = true
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
                            .clickable { onImageClick(0) }
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onImageClick(page) }
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
            
            // 상품 박스 상단 영역 (체크박스, 수량, 상품 이름, 카테고리, 편집 버튼)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 좌측 - 체크박스 + 수량 + 상품 정보 (상품 이름, 카테고리)
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
                    
                    // 수량
                    if (product.quantity > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = PrimaryAccent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${product.quantity}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 24.sp
                                )
                                Text(
                                    text = "개",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 18.sp,
                                    // '개' 텍스트가 아래로 치우치는 현상이 발생해 하단 여백 추가
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        }
                    }
                    
                    // 상품 정보 (상품 이름, 카테고리)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // 상품 이름
                        Text(
                            text = product.productName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                        
                        // 카테고리
                        if (product.category.isNotEmpty()) {
                            Text(
                                text = product.category,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 우측 - 편집 버튼 (편집 권한이 있을 때만 표시)
                if (canEdit) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "편집",
                            tint = PrimaryAccent
                        )
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

@Composable
fun StatusCheckbox(
    bought: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(
                width = 2.dp,
                color = when (bought) {
                    "구매완료" -> Color(0xFF28A745)
                    "구매중" -> Color(0xFFFFC107)
                    else -> Color.LightGray // Gray가 나을까 LightGray가 나을까
                },
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (bought) {
            "구매전" -> {
                // 빈 체크박스 - 흰색 배경 + 검정색 테두리
            }
            "구매중" -> {
                // 삼각형 - 흰색 배경 + 노란색 테두리
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "구매중",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
            }
            "구매완료" -> {
                // 체크 - 흰색 배경 + 초록색 테두리
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "구매완료",
                    tint = Color(0xFF28A745),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SectionDivider(
    title: String,
    iconId: Int,
    verticalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    onAddClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SecondaryBackground)
            .padding(vertical = verticalPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF333333)
                )
            }
            
            // + 버튼
            if (onAddClick != null) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "추가",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}