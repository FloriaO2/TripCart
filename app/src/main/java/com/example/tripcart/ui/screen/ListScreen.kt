package com.example.tripcart.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tripcart.ui.components.AddItemDialog
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onNavigateToPlaceSearch: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "전체 리스트",
                onNotificationClick = {
                    // TODO: 알림 기능 구현
                },
                onLogoClick = onNavigateToHome,
                onActionClick = {
                    showAddDialog = true
                },
                showActionButton = true
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
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "전체 리스트 화면"
            )
        }
        
        // 추가하기 다이얼로그
        if (showAddDialog) {
            AddItemDialog(
                onDismiss = { showAddDialog = false },
                onAddProduct = {
                    // TODO: 상품 추가하기 화면으로 이동
                    onAddClick()
                },
                onAddPlace = {
                    showAddDialog = false
                    onNavigateToPlaceSearch()
                }
            )
        }
    }
}

