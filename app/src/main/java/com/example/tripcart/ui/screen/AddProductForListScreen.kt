package com.example.tripcart.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.ProductViewModel
import com.example.tripcart.util.SetStatusBarColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductForListScreen(
    listId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    productViewModel: ProductViewModel = viewModel(),
    listViewModel: com.example.tripcart.ui.viewmodel.ListViewModel = viewModel()
) {
    val uiState = productViewModel.uiState.collectAsState().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isProcessing by remember { mutableStateOf(false) }
    
    // ViewModel에서 저장된 draftProduct 불러오기
    val draftProduct = uiState.draftProduct
    
    // 입력 상태 - ViewModel의 draftProduct로 초기화
    var productImages by remember { mutableStateOf(emptyList<Uri>()) }
    var productName by remember { mutableStateOf("") }
    var productMemo by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isPublic by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var isFromFirestore by remember { mutableStateOf(false) }
    var firestoreImageUrls by remember { mutableStateOf(emptyList<String>()) }
    var firestoreProductId by remember { mutableStateOf<String?>(null) }
    
    // 검색 관련 상태
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    
    // 화면 진입 시 해당 리스트를 선택 상태로 설정 및 이전 입력 기록 초기화
    LaunchedEffect(Unit) {
        listViewModel.clearAllSelections()
        listViewModel.toggleListSelection(listId)
        productViewModel.clearDraftProduct() // 이전 입력 기록 초기화
        
        // 모든 로컬 상태 변수도 직접 초기화
        productImages = emptyList()
        productName = ""
        productMemo = ""
        quantity = 1
        selectedCategory = null
        isPublic = false
        isFromFirestore = false
        firestoreImageUrls = emptyList()
        firestoreProductId = null
        searchQuery = ""
        showSearchResults = false
    }
    
    // draftProduct가 변경되면 입력 필드 업데이트
    // saveDraftProduct를 이용해
    // AddProductToListScreen에서 AddProductScreen으로 이동할 때 상품 정보 유지
    // - saveDraftProduct의 기본값 설정때문에 isFromFirestore가 false로 리셋되는 문제가 발생했는데
    //   saveDraftProduct에서 새롭게 추가한 필드들도 저장하게끔 변경함으로써 해결
    LaunchedEffect(draftProduct) {
        // 상품을 firestore에서 불러오는 경우
        if (draftProduct.isFromFirestore) {
            if (isFromFirestore != true ||
                productName != draftProduct.productName ||
                selectedCategory != draftProduct.selectedCategory ||
                firestoreImageUrls != draftProduct.firestoreImageUrls ||
                firestoreProductId != draftProduct.firestoreProductId) {
                // 실제로 불러오기
                productName = draftProduct.productName
                selectedCategory = draftProduct.selectedCategory
                isFromFirestore = true
                firestoreImageUrls = draftProduct.firestoreImageUrls
                firestoreProductId = draftProduct.firestoreProductId
                isPublic = false // 불러온 상품은 공개 불가
                // 불러온 이미지 URL들을 productImages에 추가하지 않고 기존 url 재탕
            }
        } else {
            // 직접 입력하는 경우
            if (productImages != draftProduct.productImages ||
                productName != draftProduct.productName ||
                productMemo != draftProduct.productMemo ||
                quantity != draftProduct.quantity ||
                selectedCategory != draftProduct.selectedCategory ||
                isPublic != draftProduct.isPublic ||
                isFromFirestore != false ||
                firestoreImageUrls != draftProduct.firestoreImageUrls ||
                firestoreProductId != draftProduct.firestoreProductId) {
                productImages = draftProduct.productImages
                productName = draftProduct.productName
                productMemo = draftProduct.productMemo
                quantity = draftProduct.quantity
                selectedCategory = draftProduct.selectedCategory
                isPublic = draftProduct.isPublic
                isFromFirestore = false // 직접 입력하는 경우 false로 설정
                firestoreImageUrls = draftProduct.firestoreImageUrls
                firestoreProductId = draftProduct.firestoreProductId
            }
        }
    }
    
    // 화면 진입 시 전체 상품 로드
    LaunchedEffect(Unit) {
        productViewModel.loadAllProducts(showLoading = false)
    }
    
    // 상품 이름을 통해 검색 실행
    LaunchedEffect(searchQuery) {
        // 불러온 상품이면 검색 결과 팝업이 뜨지 않도록 !isFromFirestore 조건 추가
        if (searchQuery.isNotBlank() && !isFromFirestore) { // 한 글자도 검색 가능
            delay(300)
            productViewModel.searchProducts(searchQuery)
            showSearchResults = true
        } else {
            productViewModel.clearSearchResults()
            showSearchResults = false
        }
    }
    
    LaunchedEffect(productName) {
        // 불러온 상품이 아닐 경우 상품 이름 변경 시 검색어도 함께 업데이트
        if (!isFromFirestore && searchQuery != productName) {
            searchQuery = productName
        }
    }
    
    // ViewModel에 입력 값 저장
    // isFromFirestore 리셋 문제를 해결하기 위해
    // isFromFirestore, firestoreImageUrls, firestoreProductId 필드도 저장할 수 있게 수정
    LaunchedEffect(productImages, productName, productMemo, quantity, selectedCategory, isPublic, isFromFirestore, firestoreImageUrls, firestoreProductId) {
        val currentDraft = com.example.tripcart.ui.viewmodel.DraftProduct(
            productImages = productImages,
            productName = productName,
            productMemo = productMemo,
            quantity = quantity,
            selectedCategory = selectedCategory,
            isPublic = isPublic,
            isFromFirestore = isFromFirestore,
            firestoreProductId = firestoreProductId,
            firestoreImageUrls = firestoreImageUrls
        )
        
        // 현재 draftProduct와 입력 값이 다를 때만 ViewModel에 저장 - 무한 루프 방지!
        if (currentDraft != draftProduct) {
            productViewModel.saveDraftProduct(currentDraft)
        }
    }
    
    // 공개 여부 활성화 조건 체크
    val canSetPublic = !isFromFirestore &&            // 불러온 상품은 공개 불가
                       productImages.isNotEmpty() && 
                       productName.isNotBlank() && 
                       selectedCategory != null
    
    // 이미지 선택 Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        productImages = productImages + uris
    }
    
    // 상태바 색상을 노란 계열로 설정
    SetStatusBarColor(
        statusBarColor = SecondaryBackground,
        isLightStatusBars = true
    )
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "상품 추가하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(24.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SecondaryBackground,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 스크롤 가능
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 상품 사진
                Column {
                    Text(
                        text = "상품 사진",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Firestore에서 불러온 이미지들
                        items(firestoreImageUrls) { imageUrl ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f))
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "상품 사진",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // 삭제 불가 - X 버튼 없음
                            }
                        }
                        // 사용자가 추가한 이미지들
                        items(productImages) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(context)
                                            .data(uri)
                                            .build()
                                    ),
                                    contentDescription = "상품 사진",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(
                                            color = Color.White.copy(alpha = 0.8f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            productImages = productImages.filter { it != uri }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "삭제",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        // 사진 추가 버튼
                        item {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        imagePickerLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "사진 추가",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                // 상품 이름 (필수)
                Column {
                    Text(
                        text = if (isFromFirestore) "상품 이름 (변경 불가)" else "상품 이름 *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { 
                            if (!isFromFirestore) {
                                productName = it
                                searchQuery = it    // 검색어도 함께 업데이트
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("상품 이름을 입력하세요") },
                        singleLine = true,
                        enabled = !isFromFirestore,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledPlaceholderColor = Color.Gray
                        ),
                        trailingIcon = {
                            if (isFromFirestore) {
                                IconButton(
                                    onClick = {
                                        // 검색으로 불러온 상품 상태 완전 리셋
                                        isFromFirestore = false
                                        productName = ""
                                        productMemo = ""
                                        quantity = 1
                                        productImages = emptyList()
                                        selectedCategory = null
                                        firestoreImageUrls = emptyList()
                                        firestoreProductId = null
                                        isPublic = false
                                        searchQuery = ""
                                        showSearchResults = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "입력 상태 리셋",
                                        tint = PrimaryAccent
                                    )
                                }
                            }
                        }
                    )
                    
                    // 검색 결과 표시
                    if (showSearchResults && !isFromFirestore) {
                        // 결과가 존재할 때만 표시
                        if (uiState.searchResults.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .heightIn(max = 300.dp), // 테스트용
                                                             // 추후 DB 쌓이면 높이 제한 없애야 할 수도 있음
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.searchResults) { searchedProduct ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // 상품 선택 시 불러오기
                                                    productViewModel.loadProductFromFirestore(searchedProduct.productId)
                                                    searchQuery = ""
                                                    showSearchResults = false
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // 상품 사진
                                            if (searchedProduct.imageUrls.isNotEmpty()) {
                                                AsyncImage(
                                                    model = searchedProduct.imageUrls.first(),
                                                    contentDescription = "상품 사진",
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Gray.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "이미지 없음",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                            
                                            // 상품 이름과 카테고리
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = searchedProduct.productName,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    lineHeight = 16.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = searchedProduct.category,
                                                    fontSize = 14.sp,
                                                    color = Color.Gray,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 상품 품목 (필수)
                Column {
                    Text(
                        text = if (isFromFirestore) "상품 품목 (변경 불가)" else "상품 품목 *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = selectedCategory ?: "",
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (!isFromFirestore) {
                                    showCategoryDialog = true 
                                }
                            },
                        placeholder = { Text("카테고리") },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledPlaceholderColor = Color.Gray
                        ),
                        trailingIcon = {
                            if (!isFromFirestore) {
                                IconButton(
                                    onClick = { showCategoryDialog = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "카테고리 선택" )
                                }
                            }
                        }
                    )
                }

                // 상품 공개 여부
                Column {
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "상품 공개 여부",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                !canSetPublic -> "(비활성화)"
                                isPublic -> "(공개)"
                                else -> "(비공개)"
                            },
                            fontSize = 14.sp,
                            color = when {
                                !canSetPublic -> Color.Gray
                                isPublic -> PrimaryAccent
                                else -> Color(0xFFD32F2F) // 붉은 계열 색상
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // 스위치
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                    ) {
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            enabled = canSetPublic,
                            colors = SwitchDefaults.colors(
                                // 활성화 상태 - 공개 (checked)
                                checkedTrackColor = PrimaryAccent,
                                checkedThumbColor = Color.White,
                                checkedBorderColor = PrimaryAccent,
                                // 활성화 상태 - 비공개 (unchecked)
                                uncheckedTrackColor = Color(0xFFD32F2F), // 붉은 계열
                                uncheckedThumbColor = Color.White,
                                uncheckedBorderColor = Color(0xFFD32F2F),
                                disabledCheckedTrackColor = Color.Gray,
                                disabledUncheckedTrackColor = Color.Gray,
                                disabledCheckedThumbColor = Color.White,
                                disabledUncheckedThumbColor = Color.White
                            )
                        )
                    }
                    
                    // 상세 설명 (스위치 아래 설명 박스 부분)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TertiaryBackground, RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = SecondaryBackground,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        when {
                            isFromFirestore -> {
                                Text(
                                    text = "이미 불러온 상품은 서버에 재업로드할 수 없습니다.",
                                    fontSize = 14.sp,
                                    color = PrimaryAccent
                                )
                            }
                            !canSetPublic -> {
                                val annotatedText = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("상품 사진, 상품 이름, 상품 품목")
                                    }
                                    append("을 ")
                                    withStyle(style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F) // 붉은 계열
                                    )) {
                                        append("모두")
                                    }
                                    append(" 입력한 상품만 공개 여부를 선택할 수 있습니다.")
                                }
                                Text(
                                    text = annotatedText,
                                    fontSize = 14.sp,
                                    color = PrimaryAccent
                                )
                            }
                            isPublic -> {
                                // 공개 상태는 두 줄로 나누어 표시
                                Column {
                                    val firstLineText = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("모든 사용자들")
                                        }
                                        append("이 ")
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("내가 등록한 상품 정보")
                                        }
                                        append("에 접근할 수 있습니다.")
                                    }
                                    Text(
                                        text = firstLineText,
                                        fontSize = 14.sp,
                                        color = PrimaryAccent
                                    )
                                    Text(
                                        text = "(※ 상품 사진, 상품 이름, 상품 품목에만 접근할 수 있습니다.)",
                                        fontSize = 12.sp,
                                        color = PrimaryAccent
                                    )
                                }
                            }
                            else -> {
                                val annotatedText = buildAnnotatedString {
                                    append("이 상품의 정보에 ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("나만")
                                    }
                                    append(" 접근할 수 있습니다.")
                                }
                                Text(
                                    text = annotatedText,
                                    fontSize = 14.sp,
                                    color = PrimaryAccent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(0.dp))

                // 구분선
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
                
                // 구매할 수량 (필수)
                Column {
                    Text(
                        text = "구매할 수량 *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(end = 180.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // - 버튼
                        IconButton(
                            onClick = {
                                if (quantity > 1) {
                                    quantity--
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                        ) {
                            Text(
                                text = "-",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // 수량 표시
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$quantity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // + 버튼
                        IconButton(
                            onClick = {
                                quantity++
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "증가",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 상품 메모
                Column {
                    Text(
                        text = "상품 메모",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = productMemo,
                        onValueChange = { productMemo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("상품에 대해 설명해주세요") },
                        minLines = 1,
                        maxLines = 5,
                        singleLine = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))
            }
            
            // 확인 버튼 - 바로 리스트에 추가
            Button(
                onClick = {
                    if (productName.isBlank() || selectedCategory == null) {
                        return@Button
                    }
                    
                    scope.launch {
                        isProcessing = true
                        try {
                            // 이미지 업로드 처리
                            val uploadedImageUrls = if (isFromFirestore) {
                                // Firestore에서 불러온 이미지 URL 사용
                                firestoreImageUrls
                            } else if (productImages.isNotEmpty()) {
                                // 새로 추가한 이미지 업로드
                                productViewModel.uploadImagesForProduct(productImages, isPublic)
                            } else {
                                null
                            }
                            
                            // 공개 상품인 경우 Firestore products 컬렉션에 저장
                            var finalProductId = firestoreProductId
                            if (isPublic && uploadedImageUrls != null && uploadedImageUrls.isNotEmpty() && !isFromFirestore) {
                                try {
                                    val firestoreProductId = productViewModel.savePublicProductToFirestore(
                                        productName = productName,
                                        category = selectedCategory!!,
                                        imageUrls = uploadedImageUrls
                                    )
                                    if (firestoreProductId != null) {
                                        finalProductId = firestoreProductId
                                    }
                                } catch (e: Exception) {
                                    // Firestore 저장 실패 시 무시
                                }
                            }
                            
                            // ProductDetails 생성
                            val productDetails = ProductDetails(
                                id = UUID.randomUUID().toString(),
                                productName = productName,
                                category = selectedCategory!!,
                                imageUrls = uploadedImageUrls,
                                imageUris = if (isFromFirestore) null else productImages,
                                quantity = quantity,
                                note = productMemo.ifBlank { null },
                                productId = finalProductId,
                                isPublic = isPublic
                            )
                            
                            // 리스트에 상품 추가
                            val result = listViewModel.addProductToSelectedLists(
                                productDetails,
                                imageUrls = uploadedImageUrls
                            )
                            
                            if (result.isSuccess) {
                                listViewModel.setSuccessMessage("상품이 추가되었습니다.")
                                // 상품 추가 성공 후 draftProduct 초기화
                                productViewModel.clearDraftProduct()
                                onComplete()
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = result.exceptionOrNull()?.message ?: "오류가 발생했습니다.",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "오류가 발생했습니다: ${e.message}",
                                duration = SnackbarDuration.Long
                            )
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isProcessing && productName.isNotBlank() && selectedCategory != null && quantity > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBackground,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.White
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "저장",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 카테고리 선택 다이얼로그
        if (showCategoryDialog) {
            CategorySelectionDialog(
                categories = PRODUCT_CATEGORIES,
                onCategorySelected = { category ->
                    selectedCategory = category
                    showCategoryDialog = false
                },
                onDismiss = { showCategoryDialog = false }
            )
        }
    }
}

@Composable
private fun CategorySelectionDialog(
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "카테고리 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                categories.forEach { category ->
                    TextButton(
                        onClick = { onCategorySelected(category) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = category,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

