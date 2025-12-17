package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripcart.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcart.ui.components.AddItemDialog
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.example.tripcart.ui.components.JoinByInviteCodeDialog
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.ui.viewmodel.ListItemUiState
import com.example.tripcart.ui.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onNavigateToPlaceSearch: () -> Unit = {},
    onNavigateToListDetail: (String) -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    viewModel: ListViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val notificationState by notificationViewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "전체 리스트",
                onNotificationClick = onNavigateToNotification,
                onLogoClick = onNavigateToHome,
                onActionClick = {
                    showAddDialog = true
                },
                showActionButton = true,
                unreadNotificationCount = notificationState.unreadCount
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "list",
                onItemClick = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.lists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                        text = "리스트가 없습니다.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.lists) { listItem ->
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        var isOwner by remember { mutableStateOf<Boolean?>(null) }
                        
                        ListCard(
                            status = listItem.status,
                            listName = listItem.name,
                            country = listItem.country ?: "",
                            placeNames = listItem.places.map { it.name },
                            productCount = listItem.productCount,
                            onClick = {
                                onNavigateToListDetail(listItem.listId)
                            },
                            onDelete = {
                                if (listItem.isFromFirestore) {
                                    // 공유 리스트
                                    scope.launch {
                                        // owner 여부 확인
                                        isOwner = viewModel.isListOwner(listItem.listId)
                                        showDeleteDialog = true
                                    }
                                } else {
                                    // 개인 리스트
                                    isOwner = true
                                    showDeleteDialog = true
                                }
                            }
                        )
                        
                        // 삭제 확인 다이얼로그
                        if (showDeleteDialog) {
                            val (title, message, confirmText) = when {
                                // 개인 리스트인 경우
                                !listItem.isFromFirestore -> Triple(
                                    "리스트 삭제",
                                    "정말 ${listItem.name} 리스트를 삭제하시겠습니까?\n삭제된 리스트는 복구가 불가능합니다.",
                                    "삭제"
                                )
                                // 공유 리스트의 owner인 경우
                                listItem.isFromFirestore && isOwner == true -> Triple(
                                    "리스트 삭제",
                                    "정말 ${listItem.name} 리스트를 삭제하시겠습니까?\n모든 멤버에게서 리스트가 삭제됩니다.",
                                    "삭제"
                                )
                                // 공유 리스트의 멤버인 경우
                                listItem.isFromFirestore && isOwner == false -> Triple(
                                    "리스트 나가기",
                                    "정말 ${listItem.name} 리스트에서 나가시겠습니까?",
                                    "나가기"
                                )
                                // 기본값
                                else -> Triple(
                                    "리스트 삭제",
                                    "정말 ${listItem.name} 리스트를 삭제하시겠습니까?\n삭제된 리스트는 복구가 불가능합니다.",
                                    "삭제"
                                )
                            }
                            
                            AlertDialog(
                                title = {
                                    Text(title)
                                },
                                text = {
                                    Text(message)
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteList(listItem.listId, listItem.isFromFirestore)
                                            showDeleteDialog = false
                                        }
                                    ) {
                                        Text(confirmText, color = Color.Red)
                                    }
                                },
                                // 취소 버튼 클릭해서 닫을 때
                                dismissButton = {
                                    TextButton(
                                        onClick = { 
                                            showDeleteDialog = false
                                        }
                                    ) {
                                        Text("취소")
                                    }
                                },
                                // 뒤로가기 클릭 등 외부 요인으로 인해 닫힐 때
                                onDismissRequest = { 
                                    showDeleteDialog = false
                                }
                            )
                        }
                    }
                }
            }

            // 에러 메시지 표시
            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    // TODO: Snackbar 표시
                    viewModel.clearError()
                }
            }
        }
        
        // 추가하기 다이얼로그
        if (showAddDialog) {
            AddItemDialog(
                onDismiss = { showAddDialog = false },
                onAddProduct = {
                    showAddDialog = false
                    onAddClick()
                },
                onAddPlace = {
                    showAddDialog = false
                    onNavigateToPlaceSearch()
                },
                onJoinByInviteCode = {
                    showAddDialog = false
                    showJoinDialog = true
                }
            )
        }
        
        // 초대코드로 참여하기 다이얼로그
        if (showJoinDialog) {
            JoinByInviteCodeDialog(
                onDismiss = { showJoinDialog = false },
                onJoin = { inviteCode, nickname ->
                    scope.launch {
                        val result = viewModel.joinListByInviteCode(inviteCode, nickname)
                        result.onSuccess { listId ->
                            showJoinDialog = false
                            // 참여 성공 시 리스트 상세 화면으로 이동
                            Toast.makeText(context, "리스트에 참여했습니다.", Toast.LENGTH_SHORT).show()
                            onNavigateToListDetail(listId)
                        }.onFailure { e ->
                            // 에러 처리
                            val errorMessage = e.message ?: "리스트 참여에 실패했습니다."
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            showJoinDialog = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ListCard(
    status: String,
    listName: String,
    country: String,
    placeNames: List<String>,
    productCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = TertiaryBackground, // 카드 배경색
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusTag(status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = listName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (country.isNotEmpty()) {
                        CountryTag(country)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
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
                            text = if (placeNames.isNotEmpty()) {
                                placeNames.joinToString(", ")
                            } else {
                                "상점이 없습니다."
                            },
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (placeNames.isNotEmpty()) {
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
                                if (productCount == 0) Color.Gray else Color(0xFF333333)
                            )
                        )
                        if (productCount == 0) {
                            Text(
                                text = "상품이 존재하지 않습니다.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        } else {
                            Row {
                                Text(
                                    text = "${productCount}개",
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

@Composable
fun StatusTag(status: String) {
    val backgroundColor = when(status) {
        "준비중" -> Color(0xFFFFA500)
        "진행중" -> Color(0xFF5DADE2)
        "완료" -> Color(0xFF7C9A52)
        else -> Color.Gray
    }

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
fun CountryTag(country: String) {
    Box(
        modifier = Modifier
            .background(PrimaryBackground, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.country),
                contentDescription = "국가",
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = country,
                fontSize = 14.sp,
                color = Color(0xD2000000),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

