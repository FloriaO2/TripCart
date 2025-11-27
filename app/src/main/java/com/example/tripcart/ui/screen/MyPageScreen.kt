package com.example.tripcart.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onSignOut: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val user = FirebaseAuth.getInstance().currentUser
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "마이페이지",
                onNotificationClick = {
                    // TODO: 알림 기능 구현
                },
                onLogoClick = onNavigateToHome
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "my_page",
                onItemClick = { route ->
                    onNavigateToRoute(route)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 사용자 정보
            user?.let {
                it.displayName?.let { name ->
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                it.email?.let { email ->
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 로그아웃 버튼
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC3545),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "로그아웃",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "로그아웃",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

