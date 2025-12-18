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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
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
import com.example.tripcart.ui.viewmodel.PlaceViewModel
import com.example.tripcart.ui.viewmodel.PlaceDetails
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceToListScreen(
    placeDetails: PlaceDetails,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    listViewModel: ListViewModel = viewModel(),
    placeViewModel: PlaceViewModel = viewModel()
) {
    val uiState = listViewModel.uiState.collectAsState().value
    val scope = rememberCoroutineScope()
    var showCreateListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 화면 진입 시 선택 상태 초기화
    // - 상품 추가와 상점 추가를 왔다갔다할 때 리스트 선택 상태가 공유되는 것을 방지
    LaunchedEffect(Unit) {
        listViewModel.clearAllSelections()
    }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "리스트에 장소 추가하기",
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
                    TextButton(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val selectedLists = uiState.lists.filter { it.isSelected }
                                
                                if (selectedLists.isNotEmpty()) {
                                    val result = listViewModel.addPlaceToSelectedLists(placeDetails)
                                    
                                    if (result.isSuccess) {
                                        // 성공 메시지를 ListViewModel에 설정 (ListScreen에서 표시)
                                        listViewModel.setSuccessMessage("장소가 추가되었습니다.")
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
                .padding(paddingValues)
        ) {
            // 선택한 장소 정보 표시
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
                    // 국가 표시
                    placeDetails.country?.let { country ->
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
                                Image(
                                    painter = painterResource(id = R.drawable.country),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                        .padding(end = 6.dp),
                                    colorFilter = ColorFilter.tint(Color(0xFF333333))
                                )
                                Text(
                                    text = country,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                            }
                        }
                    }
                    
                    // 장소 이름
                    Text(
                        text = placeDetails.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 주소
                    placeDetails.address?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PrimaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // 구분선 (선택한 장소 정보 영역 바로 아래)
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
                        NewListCard(
                            onClick = {
                                if (!isProcessing) {
                                    showCreateListDialog = true
                                }
                            }
                        )
                    }
                    
                    // 리스트 목록
                    if (uiState.lists.isEmpty()) {
                        // 리스트가 없으면 빈 공간만 표시
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
                            // 1. 이미 해당 장소가 목록에 들어가있는 리스트
                            // 2. 장소와 상품이 하나도 없는 새 리스트
                            // 3. 현재 선택한 장소를 추가할 수 있는 리스트
                            // 4. 현재 선택한 장소를 아예 추가하지 못하는 리스트
                            val sortedLists = filteredLists.sortedWith(compareBy<ListItemUiState> { listItem ->
                                // 0 들어가있는게 우선 순위
                                val isAlreadyInList = listItem.places.any { it.placeId == placeDetails.placeId }
                                if (isAlreadyInList) 0 else 1 // 이미 들어있는 리스트가 먼저
                            }.thenBy { listItem ->
                                // 빈 리스트인지 확인
                                val isEmpty = listItem.places.isEmpty() && listItem.productCount == 0
                                if (isEmpty) 0 else 1 // 빈 리스트가 그 다음
                            }.thenBy { listItem ->
                                // 현재 선택한 장소를 추가할 수 있는지 확인
                                val isSelectable = when {
                                    listItem.country == null -> true // 비어있는 리스트
                                    listItem.country == placeDetails.country -> true // 같은 국가
                                    else -> false // 다른 국가
                                }
                                val isAlreadyInList = listItem.places.any { it.placeId == placeDetails.placeId }
                                if (isSelectable && !isAlreadyInList) 0 else 1 // 추가 가능한 리스트가 그 다음
                                // 모든 기준에서 후순위인 '현재 선택한 장소를 추가할 수 없는 리스트'는
                                // 가장 마지막에 배치
                            })
                            
                            items(sortedLists) { listItem ->
                                SelectableListItemCard(
                                    listItem = listItem,
                                    placeDetails = placeDetails,
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
                containerColor = Color.White,
                title = {
                    Text(
                        "새 리스트 만들기",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("리스트 이름") },
                        placeholder = { Text("예: 여행") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
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
                                    // 팝업 바로 닫기
                                    showCreateListDialog = false
                                    newListName = ""
                                    isProcessing = false
                                    // 새로 생성한 리스트는 자동으로 선택 상태가 됨
                                } else {
                                    // 오류 발생 시에만 토스트 표시
                                    snackbarHostState.showSnackbar(
                                        message = result.exceptionOrNull()?.message ?: "오류가 발생했습니다.",
                                        duration = SnackbarDuration.Long
                                    )
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = PrimaryAccent,
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text("생성", color = if (!isProcessing) PrimaryAccent else Color.Gray)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateListDialog = false },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Gray,
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
private fun NewListCard(
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
private fun SelectableListItemCard(
    listItem: ListItemUiState,
    placeDetails: PlaceDetails,
    onToggleSelection: () -> Unit
) {
    // 이미 장소가 들어있는지 확인
    val isAlreadyInList = listItem.places.any { it.placeId == placeDetails.placeId }
    
    // 국가 검증: 리스트에 이미 장소가 있고 다른 국가면 선택 불가
    val isSelectableByCountry = when {
        listItem.country == null -> true // 리스트에 장소가 없으면 선택 가능
        listItem.country == placeDetails.country -> true // 같은 국가면 선택 가능
        else -> false // 다른 국가면 선택 불가
    }
    
    // 최종 선택 가능 여부: 국가가 맞고, 이미 들어있지 않아야 함
    val isSelectable = isSelectableByCountry && !isAlreadyInList
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 그림자용 Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp) // 우하향 이동
                .background(
                    color = Color(0x55000000), // 반투명 검정
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // 실제 카드
        // 상태에 따른 테두리 색상
        val borderColor = when(listItem.status) {
            "준비중" -> Color(0xFFFFA500) // 주황색
            "진행중" -> Color(0xFF5DADE2) // 파란색
            "완료" -> Color(0xFF7C9A52) // 초록색
            else -> Color.Gray
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
            color = when {
                isAlreadyInList -> Color(0xFFFFE0B2) // 이미 들어있는 리스트는 연한 주황색 배경
                !isSelectable -> Color(0xFFE0E0E0) // 비활성화된 블럭은 회색 배경
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
                            enabled = isSelectable,
                            onClick = onToggleSelection
                        )
                ) {
                    // 체크박스
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = listItem.isSelected || isAlreadyInList, // 이미 들어있는 리스트는 체크된 상태로 표시
                            onCheckedChange = { 
                                if (isSelectable) {
                                    onToggleSelection()
                                }
                            },
                            enabled = isSelectable, // 이미 들어있는 리스트는 비활성화
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = SecondaryBackground,
                                uncheckedColor = SecondaryBackground,
                                checkmarkColor = PrimaryAccent
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(5.dp))
                    
                    if (isSelectable || isAlreadyInList) {
                        StatusTag(listItem.status)
                    } else {
                        DisabledStatusTag(listItem.status)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = listItem.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = when {
                            isAlreadyInList -> Color(0xFF666666) // 이미 들어있는 리스트는 중간 회색
                            isSelectable -> Color(0xFF333333)
                            else -> Color(0xFF999999)
                        }
                    )
                    
                    if (listItem.country != null && listItem.country.isNotEmpty()) {
                        if (isSelectable || isAlreadyInList) {
                            CountryTag(listItem.country, listItem.isFromFirestore)
                        } else {
                            DisabledCountryTag(listItem.country)
                        }
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
                            color = when {
                                !isSelectable && !isAlreadyInList -> Color(0xFF999999)
                                listItem.places.isEmpty() -> Color.Gray
                                else -> Color(0xFF333333)
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
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                when {
                                    !isSelectable && !isAlreadyInList -> Color(0xFFCCCCCC)
                                    listItem.productCount == 0 -> Color.Gray
                                    else -> Color(0xFF333333)
                                }
                            )
                        )
                        if (listItem.productCount == 0) {
                            Text(
                                text = "상품이 존재하지 않습니다.",
                                fontSize = 13.sp,
                                color = when {
                                    !isSelectable && !isAlreadyInList -> Color(0xFFCCCCCC)
                                    else -> Color.Gray
                                }
                            )
                        } else {
                            val textColor = when {
                                !isSelectable && !isAlreadyInList -> Color(0xFFCCCCCC)
                                else -> Color(0xFF333333)
                            }
                            Row {
                                Text(
                                    text = "${listItem.productCount}개",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
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

@Composable
private fun DisabledStatusTag(status: String) {
    val backgroundColor = Color(0xFF9E9E9E) // 회색 배경
    
    Box(
        modifier = Modifier
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DisabledCountryTag(country: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFBDBDBD), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.country),
                contentDescription = "국가",
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(Color(0xFF757575))
            )
            Text(
                text = country,
                fontSize = 14.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

