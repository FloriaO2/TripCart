package com.example.tripcart.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.ProductViewModel

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
    
    // 입력 상태
    var productImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var productName by remember { mutableStateOf("") }
    var productMemo by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isPublic by remember { mutableStateOf(false) } // 기본값: 비공개
    var showCategoryDialog by remember { mutableStateOf(false) }
    
    // 공개 여부 활성화 조건 체크
    val canSetPublic = productImages.isNotEmpty() && 
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
                        text = "상품 이름 *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("상품 이름을 입력하세요") },
                        singleLine = true
                    )
                }
                
                // 상품 품목 (필수)
                Column {
                    Text(
                        text = "상품 품목 *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = selectedCategory ?: "",
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDialog = true },
                        placeholder = { Text("카테고리") },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledPlaceholderColor = Color.Gray
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = { showCategoryDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "카테고리 선택" )
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
                    
                    viewModel.saveProduct(
                        productName = productName,
                        productMemo = productMemo,
                        quantity = quantity,
                        category = selectedCategory!!,
                        imageUris = productImages,
                        isPublic = isPublic
                    )
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
        LaunchedEffect(uiState.saveSuccess, uiState.savedProduct) {
            if (uiState.saveSuccess && uiState.savedProduct != null) {
                kotlinx.coroutines.delay(500)
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

