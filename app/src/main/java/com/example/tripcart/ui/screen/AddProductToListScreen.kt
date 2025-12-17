package com.example.tripcart.ui.screen

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.ui.viewmodel.ListItemUiState
import com.example.tripcart.ui.viewmodel.ProductViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch

// 상품 정보 데이터 클래스
data class ProductDetails(
    val id: String,
    val productName: String,
    val category: String,
    val imageUrls: List<String>?,
    val imageUris: List<android.net.Uri>? = null,
    val quantity: Int,
    val note: String?,
    val productId: String? = null, // products 컬렉션에서 불러온 ID (랭킹 반영용, null 가능)
    val isPublic: Boolean = false // 상품 공개 여부 (공개면 public 폴더, 비공개면 user 폴더에 저장)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductToListScreen(
    productDetails: ProductDetails,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    listViewModel: ListViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel()
) {
    val uiState = listViewModel.uiState.collectAsState().value
    val scope = rememberCoroutineScope()
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 화면 진입 시 선택 상태 초기화
    // - 상점 추가와 상품 추가를 왔다갔다할 때 리스트 선택 상태가 공유되는 것을 방지
    LaunchedEffect(Unit) {
        listViewModel.clearAllSelections()
    }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "리스트에 상품 추가하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val selectedLists = uiState.lists.filter { it.isSelected }
                                
                                if (selectedLists.isNotEmpty()) {
                                    // 이미지 업로드 처리 (imageUris가 있는 경우)
                                    // 공개/비공개에 따라 경로 선택: 공개면 public, 비공개면 user 경로
                                    val uploadedImageUrls = if (productDetails.imageUris != null && productDetails.imageUris.isNotEmpty()) {
                                        try {
                                            // 공개/비공개에 따라 이미지 업로드
                                            val imageUrls = productViewModel.uploadImagesForProduct(
                                                productDetails.imageUris,
                                                isPublic = productDetails.isPublic
                                            )
                                            
                                            imageUrls
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar(
                                                message = "이미지 업로드 실패: ${e.message}",
                                                duration = SnackbarDuration.Long
                                            )
                                            isProcessing = false
                                            return@launch
                                        }
                                    } else {
                                        // 이미 업로드된 이미지 URL 사용
                                        productDetails.imageUrls
                                    }
                                    
                                    // 공개 상품인 경우 Firestore products 컬렉션에 저장
                                    var finalProductId = productDetails.productId
                                    if (productDetails.isPublic && uploadedImageUrls != null && uploadedImageUrls.isNotEmpty()) {
                                        try {
                                            val firestoreProductId = productViewModel.savePublicProductToFirestore(
                                                productName = productDetails.productName,
                                                category = productDetails.category,
                                                imageUrls = uploadedImageUrls
                                            )
                                            if (firestoreProductId != null) {
                                                finalProductId = firestoreProductId
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("AddProductToListScreen", "Failed to save public product to Firestore", e)
                                            // Firestore 저장 실패해도 리스트 추가는 계속 진행
                                        }
                                    }
                                    
                                    // 업데이트된 productDetails로 리스트에 상품 추가
                                    val updatedProductDetails = if (finalProductId != productDetails.productId) {
                                        productDetails.copy(productId = finalProductId)
                                    } else {
                                        productDetails
                                    }
                                    
                                    val result = listViewModel.addProductToSelectedLists(
                                        updatedProductDetails,
                                        imageUrls = uploadedImageUrls
                                    )
                                    
                                    if (result.isSuccess) {
                                        // 성공 메시지를 ListViewModel에 설정 (ListScreen에서 표시)
                                        listViewModel.setSuccessMessage("상품이 추가되었습니다.")
                                        // 화면 닫기
                                        onComplete()
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = result.exceptionOrNull()?.message ?: "오류가 발생했습니다.",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                } else {
                                    // 리스트를 선택하지 않았으면 그냥 돌아가기
                                    onComplete()
                                }
                                isProcessing = false
                            }
                        },
                        // 리스트를 선택하지 않아도 완료 버튼을 누를 수 있도록 보완
                        enabled = !isProcessing
                    ) {
                        Text(
                            "완료",
                            fontWeight = FontWeight.Bold,
                            color = if (isProcessing) Color.Gray else PrimaryAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SecondaryBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 선택한 상품 정보 표시
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        width = 2.dp,
                        color = Color(0x4F000000),
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 이미지가 있을 때만 상단에 표시
                    // imageUris가 있으면 Uri 사용, 없으면 imageUrls 사용
                    val imageUris = productDetails.imageUris ?: emptyList()
                    val imageUrls = productDetails.imageUrls ?: emptyList()
                    val hasImages = imageUris.isNotEmpty() || imageUrls.isNotEmpty()
                    val context = LocalContext.current
                    
                    if (hasImages) {
                        val imageCount = if (imageUris.isNotEmpty()) imageUris.size else imageUrls.size
                        
                        if (imageCount == 1) {
                            // 이미지가 한 장일 때
                            val imageData = if (imageUris.isNotEmpty()) imageUris[0] else imageUrls[0]
                            val painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(imageData)
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
                                pageCount = { imageCount }
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
                                        val imageData = if (imageUris.isNotEmpty()) imageUris[page] else imageUrls[page]
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                ImageRequest.Builder(context)
                                                    .data(imageData)
                                                    .build()
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                
                                // 페이지 인디케이터 (하단에 이미지 index 보여주는 점)
                                if (imageCount > 1) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(imageCount) { index ->
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
                    
                    // 상품 이름
                    Text(
                        text = productDetails.productName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    
                    // 구매 수량, 카테고리
                    if (productDetails.quantity > 0 || productDetails.category.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 구매 수량
                            if (productDetails.quantity > 0) {
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
                                            text = "${productDetails.quantity}",
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
                            if (productDetails.category.isNotEmpty()) {
                                Text(
                                    text = productDetails.category,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                    
                    // 메모
                    if (!productDetails.note.isNullOrEmpty()) {
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
                                text = productDetails.note!!,
                                fontSize = 14.sp,
                                color = Color.Black.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            
            // 구분선
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0),
                thickness = 1.dp
            )
            
            // 구분선 아래 영역
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 새 리스트 만들기 버튼
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        NewListCardForProduct(
                            onClick = {
                                if (!isProcessing) {
                                    showCreateListDialog = true
                                }
                            }
                        )
                    }
                    
                    // 리스트 목록
                    if (uiState.lists.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            // read 권한 리스트 필터링 (read 권한 리스트는 표시하지 않음)
                            val filteredLists = uiState.lists.filter { listItem ->
                                !(listItem.isFromFirestore && listItem.userRole == "read")
                            }
                            
                            // 아래 순서대로 리스트 정렬:
                            // 1. 장소와 상품이 하나도 없는 새 리스트
                            // 2. 그 외 모든 리스트
                            val sortedLists = filteredLists.sortedWith(compareBy<ListItemUiState> { listItem ->
                                // 빈 리스트인지 확인
                                val isEmpty = listItem.places.isEmpty() && listItem.productCount == 0
                                if (isEmpty) 0 else 1 // 빈 리스트가 먼저
                                // 후순위인 '그 외 모든 리스트'는 가장 마지막에 배치
                            })
                            
                            items(sortedLists) { listItem ->
                                SelectableListItemCardForProduct(
                                    listItem = listItem,
                                    productDetails = productDetails,
                                    isForcedChecked = false,
                                    onToggleSelection = {
                                        if (!isProcessing) {
                                            listViewModel.toggleListSelection(listItem.listId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // 로딩 오버레이
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryAccent
                        )
                    }
                }
            }
        }
        
        // 새 리스트 만들기 다이얼로그
        if (showCreateListDialog) {
            AlertDialog(
                onDismissRequest = { showCreateListDialog = false },
                title = {
                    Text(
                        "새 리스트 만들기",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("리스트 이름") },
                        placeholder = { Text("예: 여행") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val listName = newListName.ifBlank { "새 리스트" }
                                val result = listViewModel.createNewList(listName)
                                
                                if (result.isSuccess) {
                                    showCreateListDialog = false
                                    newListName = ""
                                    isProcessing = false
                                    // 새로 생성한 리스트는 자동으로 선택 상태가 되고 상품이 추가됨
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = result.exceptionOrNull()?.message ?: "오류가 발생했습니다.",
                                        duration = SnackbarDuration.Long
                                    )
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = !isProcessing
                    ) {
                        Text("생성")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateListDialog = false },
                        enabled = !isProcessing
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
private fun NewListCardForProduct(
    onClick: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "새 리스트 만들기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryAccent
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun SelectableListItemCardForProduct(
    listItem: ListItemUiState,
    productDetails: ProductDetails,
    isForcedChecked: Boolean,
    onToggleSelection: () -> Unit
) {
    val isProductInList = listItem.listId.let { listId ->
        // 이 함수는 suspend 함수이므로 LaunchedEffect에서 호출해야 하지만,
        // 여기서는 이미 계산된 값을 사용
        isForcedChecked
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 그림자용 Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(
                    color = Color(0x55000000),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // 실제 카드
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = when {
                isForcedChecked -> Color(0xFFFFE0B2) // 이미 들어있는 리스트는 연한 주황색 배경
                listItem.isSelected -> Color(0xFFFFD080) // 선택된 리스트는 노란색 배경
                else -> TertiaryBackground // 기본 배경
            },
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !isForcedChecked,
                            onClick = onToggleSelection
                        )
                ) {
                    // 체크박스
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = listItem.isSelected || isForcedChecked,
                            onCheckedChange = { 
                                if (!isForcedChecked) {
                                    onToggleSelection()
                                }
                            },
                            enabled = !isForcedChecked,
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = SecondaryBackground,
                                uncheckedColor = SecondaryBackground,
                                checkmarkColor = PrimaryAccent
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(5.dp))
                    
                    StatusTag(listItem.status)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = listItem.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = when {
                            isForcedChecked -> Color(0xFF666666)
                            else -> Color(0xFF333333)
                        }
                    )
                    
                    if (listItem.country != null && listItem.country.isNotEmpty()) {
                        CountryTag(listItem.country)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.store),
                            contentDescription = "상점",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (listItem.places.isNotEmpty()) {
                                listItem.places.map { it.name }.joinToString(", ")
                            } else {
                                "상점이 없습니다."
                            },
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = if (listItem.places.isNotEmpty()) {
                                Color(0xFF333333)
                            } else {
                                Color.Gray
                            }
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bag),
                            contentDescription = "상품",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(
                                if (listItem.productCount == 0) Color.Gray else Color(0xFF333333)
                            )
                        )
                        if (listItem.productCount == 0) {
                            Text(
                                text = "상품이 존재하지 않습니다.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        } else {
                            Row {
                                Text(
                                    text = "${listItem.productCount}개",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                                Text(
                                    text = "의 상품이 있습니다.",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
