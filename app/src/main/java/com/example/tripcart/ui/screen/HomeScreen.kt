package com.example.tripcart.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripcart.R
import androidx.compose.ui.graphics.Color
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "진행 중인 리스트",
                onNotificationClick = {
                    // TODO: 알림 기능 구현
                },
                onLogoClick = onNavigateToHome
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "active_list",
                onItemClick = { route ->
                    onNavigateToRoute(route)
                }
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
                text = "테스트용"
            )
        }
    }
}

