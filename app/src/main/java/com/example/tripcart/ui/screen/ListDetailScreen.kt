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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.example.tripcart.R
import androidx.compose.ui.res.painterResource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tripcart.ui.components.InviteCodeDialog
import com.example.tripcart.ui.components.InviteCodeDisplayDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: String,
    onBack: () -> Unit,
    onEditList: () -> Unit = {},
    onEditProduct: (String, String) -> Unit = { _, _ -> }, // productId, listId
    onGroupAddClick: () -> Unit = {}, // 그룹 추가 버튼 클릭
    viewModel: ListViewModel = viewModel()
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
    
    // 리스트 정보 로드
    LaunchedEffect(listId) {
        isLoading = true
        
        // Firestore에 리스트가 있는지 확인
        isFirestoreList = viewModel.hasInviteCode(listId)
        
        // 리스트 정보 가져오기
        listEntity = viewModel.getListDetail(listId)
        
        // 참여자 정보 가져오기 (공유 리스트일 때만)
        if (isFirestoreList == true) {
            viewModel.getListParticipants(listId).onSuccess {
                participantInfo = it
            }
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
                        
                        // 우측 편집 아이콘
                        IconButton(
                            onClick = onEditList,
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "편집"
                            )
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
                    Text("리스트를 찾을 수 없습니다", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                        // 상단: 상태 태그
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(0.dp),
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
                                                        color = Color(0xFF666666)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // 중간: 설명
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "나만 사용할 수 있는 리스트입니다.",
                                                fontSize = 14.sp,
                                                color = Color(0xFF333333),
                                                lineHeight = 20.sp
                                            )

                                            Text(
                                                text = "네트워크 연결과 무관하게 사용 가능합니다.",
                                                fontSize = 14.sp,
                                                color = Color(0xFF666666),
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
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(0.dp)
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
                                                        color = Color(0xFF666666)
                                                    )
                                                }
                                            }
                                            
                                            // 우측: 그룹 추가 버튼
                                            Box(
                                                modifier = Modifier
                                                    .background(color = PrimaryAccent, shape = RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        scope.launch {
                                                            // 초대코드가 이미 발급되었는지 확인
                                                            val hasCode = viewModel.hasInviteCode(listId)
                                                            if (hasCode) {
                                                                // 이미 발급된 경우 초대코드 표시
                                                                val code = viewModel.getInviteCode(listId)
                                                                if (code != null) {
                                                                    inviteCode = code
                                                                    showInviteCodeDisplayDialog = true
                                                                }
                                                            } else {
                                                                // 아직 발급되지 않은 경우 발급 다이얼로그 표시
                                                                showInviteCodeDialog = true
                                                            }
                                                        }
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
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    
                    
                    // 장소 섹션
                    if (placesDetails.isNotEmpty()) {
                        /*
                        item {
                            Text(
                                text = "장소",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 30.dp, vertical = 8.dp)
                            )
                        }
                        */
                        
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
                                    PlaceCard(
                                        place = place,
                                        onMapClick = {
                                            // 구글맵 길찾기 연동
                                            // placeId를 우선 사용, 불가능하면 위도/경도 사용
                                            val uri = if (place.placeId.isNotEmpty()) {
                                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=place_id:${place.placeId}")
                                            } else if (place.latitude != 0.0 && place.longitude != 0.0) {
                                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}")
                                            } else {
                                                null
                                            }
                                            
                                            uri?.let {
                                                val intent = Intent(Intent.ACTION_VIEW, it)
                                                context.startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // 구분선
                    if (placesDetails.isNotEmpty() && products.isNotEmpty()) {
                        item {
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color.Gray.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                        }
                    }
                    
                    // 상품 섹션
                    item {
                        Text(
                            text = "상품",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 30.dp, vertical = 8.dp)
                        )
                    }
                    
                    if (products.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "상품이 없습니다",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        items(products) { product ->
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
                                }
                            )
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
    }
}

@Composable
fun StatusBadge(
    status: String,
    onClick: () -> Unit
) {
    val backgroundColor = when(status) {
        "준비중" -> Color(0xFFFFA500)
        "진행중" -> Color(0xFF28A745)
        "완료" -> Color(0xFF555555)
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
    onMapClick: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth * 0.85f // 화면 너비의 85%로 설정하여 살짝 모자라게
    
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            
            // 장소 이름
            Text(
                text = place.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // 주소
            if (!place.address.isNullOrEmpty()) {
                Row(
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
            }
            
            // 전화번호
            if (!place.phoneNumber.isNullOrEmpty()) {
                Row(
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
            
            // 운영 시간
            if (!place.openingHours.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "운영 시간",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    place.openingHours.take(3).forEach { hour ->
                        Text(
                            text = hour,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // 지도 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onMapClick,
                    modifier = Modifier.size(40.dp)
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
    }
}

@Composable
fun ProductCard(
    product: ListProductEntity,
    onStatusClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onImageClick: (Int) -> Unit
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
            
            // 상품 박스 상단 영역 (체크박스, 상품 이름, 수량, 카테고리, 편집 버튼)
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
                                                // '개' 텍스트가 아래로 치우치는 현상이 발생해 하단 여백 추가
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

                Spacer(modifier = Modifier.width(8.dp))

                // 우측 - 편집 버튼
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