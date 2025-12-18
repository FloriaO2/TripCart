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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.tripcart.R
import com.example.tripcart.data.local.entity.ListProductEntity
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    product: ListProductEntity,
    listId: String,
    onBack: () -> Unit,
    onProductUpdated: () -> Unit,
    listViewModel: ListViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 입력 상태를 상품 정보로 초기화
    var productImages by remember { mutableStateOf<List<Uri>>(emptyList()) } // 새로 추가한 이미지 URI
    var existingImageUrls by remember { mutableStateOf(product.imageUrls ?: emptyList()) } // 기존 이미지 URL
    var productName by remember { mutableStateOf(product.productName) }
    var productMemo by remember { mutableStateOf(product.note ?: "") }
    var quantity by remember { mutableStateOf(product.quantity) }
    var selectedCategory by remember { mutableStateOf(product.category) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
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
                        "상품 편집하기",
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
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isDeleting // 이미 삭제 중일 땐 삭제 버튼 비활성화
                                              // - enable이 false면 비활성화     
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SecondaryBackground,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
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
                        // 기존 이미지 URL들
                        items(existingImageUrls) { imageUrl ->
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
                                // 기존 이미지도 삭제 가능
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
                                            existingImageUrls = existingImageUrls.filter { it != imageUrl }
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
                        // 새로 추가한 이미지들
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
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
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
                        value = selectedCategory,
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDialog = true },
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
                            IconButton(
                                onClick = { showCategoryDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "카테고리 선택"
                                )
                            }
                        }
                    )
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
                            modifier = Modifier.size(40.dp)
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
            
            // 저장 버튼
            Button(
                onClick = {
                    if (productName.isBlank() || selectedCategory.isEmpty()) {
                        return@Button
                    }
                    
                    isSaving = true
                    scope.launch {
                        try {
                            // 새로 추가한 이미지 업로드
                            val newImageUrls = if (productImages.isNotEmpty()) {
                                productViewModel.uploadImagesToList(productImages, listId)
                            } else {
                                emptyList()
                            }
                            
                            // 최종 이미지 URL 리스트 (기존 이미지 + 새로 업로드한 이미지)
                            val finalImageUrls = existingImageUrls + newImageUrls
                            
                            // 상품 정보 업데이트
                            val result = listViewModel.updateProductInfo(
                                productId = product.id,
                                listId = listId,
                                productName = productName,
                                category = selectedCategory,
                                quantity = quantity,
                                note = if (productMemo.isNotBlank()) productMemo else null,
                                imageUrls = if (finalImageUrls.isNotEmpty()) finalImageUrls else null
                            )
                            
                            if (result.isSuccess) {
                                onProductUpdated()
                            } else {
                                // 에러 처리
                                isSaving = false
                            }
                        } catch (e: Exception) {
                            // 에러 처리
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving && productName.isNotBlank() && selectedCategory.isNotEmpty() && quantity > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBackground
                )
            ) {
                if (isSaving) {
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
        
        // 삭제 확인 다이얼로그
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { 
                    if (!isDeleting) {
                        showDeleteDialog = false
                    }
                },
                title = {
                    Text(
                        text = "상품 삭제",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "정말로 이 상품을 삭제하시겠습니까?",
                        fontSize = 16.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isDeleting = true
                            scope.launch {
                                try {
                                    val result = listViewModel.deleteProductFromList(product.id, listId)
                                    if (result.isSuccess) {
                                        onProductUpdated()
                                    } else {
                                        isDeleting = false
                                        showDeleteDialog = false
                                    }
                                } catch (e: Exception) {
                                    isDeleting = false
                                    showDeleteDialog = false
                                }
                            }
                        },
                        enabled = !isDeleting // 이미 삭제 중일 땐 삭제 버튼 비활성화
                                              // - enable이 false면 비활성화
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "예",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting // 이미 삭제 중일 땐 삭제 버튼 비활성화
                                              // - enable이 false면 비활성화
                    ) {
                        Text(
                            text = "아니오",
                            color = Color.Gray
                        )
                    }
                }
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

