package com.example.tripcart.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground

@Composable
fun InviteCodeDisplayDialog(
    inviteCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCopyToast by remember { mutableStateOf(false) }
    
    LaunchedEffect(showCopyToast) {
        if (showCopyToast) {
            kotlinx.coroutines.delay(2000)
            showCopyToast = false
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "초대코드 사용 방법",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "'초대코드로 참여하기'에 초대코드를 입력하면 리스트에 참여할 수 있습니다.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 초대코드 표시 박스
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = PrimaryBackground,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inviteCode,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAccent,
                        letterSpacing = 2.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 복사 버튼과 공유 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 복사 버튼
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = PrimaryAccent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .clickable {
                                // 실제 복사가 가능하도록 Android 시스템 서비스 연동
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("초대코드", inviteCode)
                                clipboard.setPrimaryClip(clip) // 실제 복사
                                showCopyToast = true
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "복사",
                                modifier = Modifier.size(20.dp),
                                tint = PrimaryAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "복사",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                        }
                    }
                    
                    // 공유 버튼
                    Button(
                        onClick = {
                            // Intent 객체를 써야 다른 앱에 데이터 전달 가능
                            // ACTION_SEND 사용해서 텍스트 공유 구현!
                            // ACTION_DIAL(전화), ACTION_IMAGE_CAPTURE(사진 촬영) 등 다양하게 쓸 수 있다고 함
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain" // 텍스트 타입으로 공유하겠다!
                                putExtra(Intent.EXTRA_TEXT, "리스트 초대코드: $inviteCode") // 이런 텍스트를 공유하겠다!
                            }
                            // createChooser 이용해서 공유할 앱 선택하는 다이얼로그 띄움
                            context.startActivity(Intent.createChooser(shareIntent, "초대코드 공유"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent
                        ),
                        // 내부 여백 (버튼 자체 크기는 유지한 상태로 그 내부를 변경)
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "공유",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "공유",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                // 복사 완료 토스트 알림
                if (showCopyToast) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "초대코드가 복사되었습니다.",
                        fontSize = 14.sp,
                        color = PrimaryAccent
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 닫기 버튼
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "닫기",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

