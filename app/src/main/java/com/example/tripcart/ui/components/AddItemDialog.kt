package com.example.tripcart.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddProduct: () -> Unit,
    onAddPlace: () -> Unit
) {
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
                    text = "추가하기",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 상품 추가하기, 상점 추가하기 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 상품 추가하기 버튼
                    val productInteractionSource = remember { MutableInteractionSource() }
                    val isProductPressed = productInteractionSource.collectIsPressedAsState().value
                    
                    Button(
                        onClick = {
                            onAddProduct()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProductPressed) Color(0xFFE5B85A) else PrimaryBackground
                        ),
                        // pressed 상태 감지
                        interactionSource = productInteractionSource
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "상품",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                            Text(
                                text = "추가하기",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0x70000000)
                            )
                        }
                    }
                    
                    // 상점 추가하기 버튼
                    val placeInteractionSource = remember { MutableInteractionSource() }
                    val isPlacePressed = placeInteractionSource.collectIsPressedAsState().value
                    
                    Button(
                        onClick = {
                            onAddPlace()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlacePressed) Color(0xFFE5B85A) else PrimaryBackground
                        ),
                        // pressed 상태 감지
                        interactionSource = placeInteractionSource
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "상점",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                            Text(
                                text = "추가하기",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0x70000000)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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

