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
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.ui.viewmodel.NotificationViewModel
import com.example.tripcart.util.SetStatusBarColor
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToListDetail: (String) -> Unit = {},
    viewModel: ListViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notificationState by notificationViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 진행중인 리스트만 필터링
    val activeLists = uiState.lists.filter { it.status == "진행중" }
    
    // 상태바 색상을 상단바와 동일하게 설정
    SetStatusBarColor(
        statusBarColor = Color.White,
        isLightStatusBars = true
    )
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "진행 중인 리스트",
                onNotificationClick = onNavigateToNotification,
                onLogoClick = onNavigateToHome,
                unreadNotificationCount = notificationState.unreadCount
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "active_list",
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
            } else if (activeLists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "진행 중인 리스트가 없습니다.",
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
                    items(activeLists) { listItem ->
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
                            },
                            // 개인 리스트, 공유 리스트 구분해서 아이콘 띄워주기 위해 관련 데이터 전달
                            isFromFirestore = listItem.isFromFirestore
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
    }
}

