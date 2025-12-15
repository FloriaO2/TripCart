package com.example.tripcart.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripcart.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String = "진행 중인 리스트",
    onNotificationClick: () -> Unit = {},
    onLogoClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    showActionButton: Boolean = false,
    unreadNotificationCount: Int = 0
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 좌측 로고 이미지
            IconButton(
                onClick = onLogoClick,
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.CenterStart)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tripcart_border),
                    contentDescription = "로고",
                    modifier = Modifier.size(50.dp)
                )
            }
            
            // 중앙 텍스트
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // 우측 아이콘들
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // + 아이콘 버튼 (조건부 표시)
                if (showActionButton) {
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier.size(35.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "추가"
                        )
                    }
                }
                // 알림 아이콘 (뱃지 포함)
                Box(
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "알림"
                        )
                    }
                    // 읽지 않은 알림 뱃지
                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                // 뱃지가 알림 아이콘 우측 상단과 겹쳐지도록 수평 이동
                                .offset(x = (-5).dp, y = 5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }
                }
            }
        }
    }
}

