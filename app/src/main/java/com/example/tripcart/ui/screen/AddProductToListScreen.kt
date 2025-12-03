package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch

// 상품 정보 데이터 클래스
data class ProductDetails(
    val id: String,
    val productName: String,
    val category: String,
    val imageUrls: List<String>?,
    val quantity: Int,
    val note: String?,
    val productId: String? = null // products 컬렉션에서 불러온 ID (랭킹 반영용, null 가능)
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
    
    // 상품이 이미 들어있는 리스트 ID들을 추적
    var productInListIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 상품이 이미 리스트에 있는지 확인하여 자동 체크
    LaunchedEffect(productDetails.id, uiState.lists) {
        val existingListIds = mutableSetOf<String>()
        uiState.lists.forEach { listItem ->
            // 상품이 이미 리스트에 있는지 확인 (suspend 함수 사용)
            val productExists = listViewModel.productExistsInListSuspend(productDetails.id, listItem.listId)
            if (productExists) {
                existingListIds.add(listItem.listId)
                if (!listItem.isSelected) {
                    listViewModel.selectList(listItem.listId)
                }
            }
        }
        productInListIds = existingListIds
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
                                
                                if (selectedLists.isEmpty()) {
                                    snackbarHostState.showSnackbar(
                                        message = "리스트를 선택해주세요",
                                        duration = SnackbarDuration.Short
                                    )
                                    isProcessing = false
                                    return@launch
                                }
                                
                                val result = listViewModel.addProductToSelectedLists(productDetails)
                                
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar(
                                        message = "상품이 추가되었습니다",
                                        duration = SnackbarDuration.Short
                                    )
                                    // 잠시 후 화면 닫기
                                    kotlinx.coroutines.delay(1000)
                                    onComplete()
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = result.exceptionOrNull()?.message ?: "오류가 발생했습니다",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing && uiState.lists.any { it.isSelected }
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "완료",
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.lists.any { it.isSelected }) {
                                    PrimaryAccent
                                } else {
                                    Color.Gray
                                }
                            )
                        }
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
                        .padding(16.dp)
                ) {
                    // 카테고리 표시
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBackground,
                        modifier = Modifier
                            .wrapContentWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = productDetails.category,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                    
                    // 상품 이름
                    Text(
                        text = productDetails.productName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 구매 수량
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bag),
                            contentDescription = "상품",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(PrimaryAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "구매 수량: ${productDetails.quantity}개",
                            fontSize = 14.sp,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // 메모
                    productDetails.note?.takeIf { it.isNotBlank() }?.let { note ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "메모: $note",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
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
            
            // 새 리스트 만들기 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                NewListCardForProduct(
                    onClick = {
                        showCreateListDialog = true
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
                    // 상품이 이미 들어있는 리스트들 (상단, 강제 체크)
                    val alreadyInLists = uiState.lists.filter { listItem ->
                        productInListIds.contains(listItem.listId)
                    }
                    
                    // 선택 가능한 리스트들 (하단)
                    val selectableLists = uiState.lists.filter { listItem ->
                        !productInListIds.contains(listItem.listId)
                    }
                    
                    // 이미 상품이 들어있는 리스트들 (상단)
                    items(alreadyInLists) { listItem ->
                        SelectableListItemCardForProduct(
                            listItem = listItem,
                            productDetails = productDetails,
                            isForcedChecked = true,
                            onToggleSelection = {
                                // 강제 체크된 리스트는 해제 불가
                            }
                        )
                    }
                    
                    // 선택 가능한 리스트들 (하단)
                    items(selectableLists) { listItem ->
                        SelectableListItemCardForProduct(
                            listItem = listItem,
                            productDetails = productDetails,
                            isForcedChecked = false,
                            onToggleSelection = {
                                listViewModel.toggleListSelection(listItem.listId)
                            }
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
                                val result = listViewModel.createNewListForProduct(productDetails, listName)
                                
                                if (result.isSuccess) {
                                    showCreateListDialog = false
                                    newListName = ""
                                    isProcessing = false
                                    // 새로 생성한 리스트는 자동으로 선택 상태가 되고 상품이 추가됨
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = result.exceptionOrNull()?.message ?: "오류가 발생했습니다",
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
                                "상점이 없습니다"
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
                                text = "상품이 존재하지 않습니다",
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
                                    text = "의 상품이 있습니다",
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
