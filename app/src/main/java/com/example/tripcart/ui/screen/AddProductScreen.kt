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
import kotlinx.coroutines.delay
import java.util.UUID

// 상품 카테고리 목록
val PRODUCT_CATEGORIES = listOf(
    "식품 / 먹거리",
    "의류 / 패션",
    "화장품 / 뷰티",
    "액세서리 / 패션잡화",
    "생활용품",
    "문구 / 기념품",
    "전자제품",
    "건강 / 웰니스",
    "기타"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    onProductSaved: (com.example.tripcart.ui.screen.ProductDetails) -> Unit,
    viewModel: ProductViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current
    
    // ViewModel에서 저장된 draftProduct 불러오기
    val draftProduct = uiState.draftProduct
    
    // 입력 상태 - ViewModel의 draftProduct로 초기화
    var productImages by remember { mutableStateOf(draftProduct.productImages) }
    var productName by remember { mutableStateOf(draftProduct.productName) }
    var productMemo by remember { mutableStateOf(draftProduct.productMemo) }
    var quantity by remember { mutableStateOf(draftProduct.quantity) }
    var selectedCategory by remember { mutableStateOf(draftProduct.selectedCategory) }
    var isPublic by remember { mutableStateOf(draftProduct.isPublic) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var isFromFirestore by remember { mutableStateOf(draftProduct.isFromFirestore) }
    var firestoreImageUrls by remember { mutableStateOf(draftProduct.firestoreImageUrls) }
    var firestoreProductId by remember { mutableStateOf(draftProduct.firestoreProductId) }
    
    // 검색 관련 상태
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    
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
                isPublic != draftProduct.isPublic) {
                productImages = draftProduct.productImages
                productName = draftProduct.productName
                productMemo = draftProduct.productMemo
                quantity = draftProduct.quantity
                selectedCategory = draftProduct.selectedCategory
                isPublic = draftProduct.isPublic
                isFromFirestore = false // 직접 입력하는 경우 false로 설정
            }
        }
    }
    
    // 상품 이름을 통해 검색 실행
    LaunchedEffect(searchQuery) {
        // 불러온 상품이면 검색 결과 팝업이 뜨지 않도록 !isFromFirestore 조건 추가
        if (searchQuery.isNotBlank() && !isFromFirestore) { // 한 글자도 검색 가능
            delay(300)
            viewModel.searchProducts(searchQuery)
            showSearchResults = true
        } else {
            viewModel.clearSearchResults()
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
            viewModel.saveDraftProduct(currentDraft)
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
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "상품 추가하기",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SecondaryBackground
                )
            )
        }
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
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledPlaceholderColor = Color.Gray
                        )
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
                                                    viewModel.loadProductFromFirestore(searchedProduct.productId)
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
                                // 활성화 상태 - 비공개 (unchecked)
                                uncheckedTrackColor = Color(0xFFD32F2F), // 붉은 계열
                                uncheckedThumbColor = Color.White,
                                // 비활성화 상태면 회색으로 바꿔서 직관적으로 표현
                                disabledUncheckedTrackColor = Color.Gray.copy(alpha = 0.4f),
                                disabledCheckedTrackColor = Color.Gray.copy(alpha = 0.4f),
                                disabledUncheckedThumbColor = Color.Gray,
                                disabledCheckedThumbColor = Color.LightGray,
                            )
                        )
                    }
                    
                    // 상세 설명 (스위치 아래 설명 박스 부분)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = SecondaryBackground,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(TertiaryBackground)
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
                        singleLine = false
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))
            }
            
            // 저장 버튼
            Button(
                onClick = {
                    // 상품 이름, 상품 품목, 구매할 수량이 모두 입력되어야만 버튼 활성화
                    if (productName.isBlank()) {
                        return@Button
                    }
                    if (selectedCategory == null) {
                        return@Button
                    }
                    
                    // 불러온 상품인 경우 특별 처리
                    if (isFromFirestore) {
                        firestoreProductId?.let { productId ->
                            // 불러온 사진과 사용자가 추가한 사진 분리 및 productId 업데이트 등의 이유로
                            // 불러온 상품은 별도 함수로 처리
                            viewModel.saveProductFromFirestore(
                                productName = productName,
                                productMemo = productMemo,
                                quantity = quantity,
                                category = selectedCategory!!,
                                existingImageUrls = firestoreImageUrls,
                                newImageUris = productImages,
                                firestoreProductId = productId
                            )
                        }
                    } else {
                        viewModel.saveProduct(
                            productName = productName,
                            productMemo = productMemo,
                            quantity = quantity,
                            category = selectedCategory!!,
                            imageUris = productImages,
                            isPublic = isPublic
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving && productName.isNotBlank() && selectedCategory != null && quantity > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBackground
                )
            ) {
                if (uiState.isSaving) {
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
        
        // 저장 성공 시 상품 정보를 전달하고 AddProductToListScreen으로 이동
        // saveSuccess가 true이고 hasNavigatedToAddToList가 false일 때만 실행
        LaunchedEffect(uiState.saveSuccess, uiState.savedProduct) {
            // saveSuccess가 true이고, savedProduct가 있고, 아직 네비게이션하지 않았을 때만 실행
            if (uiState.saveSuccess && uiState.savedProduct != null && !uiState.hasNavigatedToAddToList) {
                // 플래그를 먼저 설정하여 재트리거 방지
                viewModel.setHasNavigatedToAddToList(true)
                // 짧은 딜레이로 UI 업데이트 후 네비게이션
                kotlinx.coroutines.delay(100)
                val savedProduct = uiState.savedProduct!!
                // clearSuccess()는 AddProductToListScreen에서 완료 후 호출하도록 함
                // savedProduct를 유지하기 위해 여기서는 호출하지 않음
                onProductSaved(savedProduct)
            }
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

