package com.example.tripcart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground

@Composable
fun InviteCodeDialog(
    onDismiss: () -> Unit,
    onGenerateInviteCode: (String, String) -> Unit // right: "read" or "edit", nickname: String
) {
    var selectedRight by remember { mutableStateOf<String?>(null) }
    var nickname by remember { mutableStateOf("") }
    
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
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "공유 리스트로 전환",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = " 초대코드를 이용해 다른 유저들이 이 리스트에 접근할 수 있습니다.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Text(
                    text = " 대신, 네트워크 없는 환경에서의 리스트 편집에 제한이 있을 수 있습니다.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Text(
                    text = "이 리스트에서 사용할 닉네임",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    placeholder = {
                        Text(
                            text = "닉네임을 입력하세요",
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Text(
                    text = "참여자 권한",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // read 권한 선택 카드
                PermissionCard(
                    title = "read",
                    description = "상품 구매 여부만 수정 가능",
                    isSelected = selectedRight == "read",
                    onClick = { selectedRight = "read" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                // edit 권한 선택 카드
                PermissionCard(
                    title = "edit",
                    description = "모든 수정 가능",
                    isSelected = selectedRight == "edit",
                    onClick = { selectedRight = "edit" },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(25.dp))
                
                // 초대코드 발급받기 버튼
                Button(
                    onClick = {
                        selectedRight?.let { right ->
                            if (nickname.isNotBlank()) {
                                onGenerateInviteCode(right, nickname.trim())
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedRight != null && nickname.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAccent
                    )
                ) {
                    Text(
                        text = "초대코드 발급받기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 취소 버튼
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "취소",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// read, edit 권한 선택 카드
@Composable
fun PermissionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = if (isSelected) {
                    PrimaryAccent.copy(alpha = 0.1f)
                } else {
                    Color.White
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    PrimaryAccent
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) PrimaryAccent else Color.Black
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "선택됨",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

