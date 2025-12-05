package com.example.tripcart.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tripcart.ui.theme.PrimaryAccent

@Composable
fun JoinByInviteCodeDialog(
    onDismiss: () -> Unit,
    onJoin: (String, String) -> Unit // inviteCode, nickname
) {
    var inviteCodeText by remember { mutableStateOf(TextFieldValue("")) }
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
                    text = "초대코드로 참여하기",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 26.dp)
                )
                
                OutlinedTextField(
                    value = inviteCodeText,
                    onValueChange = { inviteCodeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "초대코드 입력",
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "이 리스트에서 사용할 닉네임",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 참여하기 버튼
                Button(
                    onClick = {
                        if (inviteCodeText.text.isNotBlank() && nickname.isNotBlank()) {
                            onJoin(inviteCodeText.text.trim().uppercase(), nickname.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inviteCodeText.text.isNotBlank() && nickname.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAccent
                    )
                ) {
                    Text(
                        text = "참여하기",
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

