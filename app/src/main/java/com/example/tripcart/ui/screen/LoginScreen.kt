package com.example.tripcart.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel as viewModelCompose
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModelCompose(),
    onSignInSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Google Sign-In 결과를 처리하는 Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            viewModel.handleSignInResult(task)
        }
    }
    
    // 로그인 성공 시 콜백 호출
    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            onSignInSuccess()
        }
    }
    
    // 에러 메시지 표시
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryBackground)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "발걸음이 닿는 곳마다,",
                                fontSize = 18.sp,
                                fontWeight = FontWeight(600),
                                color = Color(0xCE023694),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.End)
                        ) {
                            Text(
                                text = "나만의 쇼핑 메이트",
                                fontSize = 22.sp,
                                fontWeight = FontWeight(800),
                                color = PrimaryAccent.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .background(
                                        color = Color(0x2FFFFFFF),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,  // 왼쪽 상단
                                            bottomEnd = 16.dp, // 오른쪽 하단
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Logo 이미지
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "TripCart Logo",
                        modifier = Modifier
                            .size(300.dp)
                            .padding(bottom = 35.dp)
                    )
                    
                    // Google 로그인 버튼
                    Button(
                        onClick = {
                            try {
                                val signInIntent = viewModel.getSignInIntent()
                                signInLauncher.launch(signInIntent)
                            } catch (e: Exception) {
                                // getSignInIntent 실패
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B9A8),
                            contentColor = Color(0xFFFFFFFF),
                            // 로딩 중에도 버튼 색상 유지시키기 위해 disabled 내용 따로 지정 ..
                            disabledContainerColor = Color(0xFF00B9A8),
                            disabledContentColor = Color(0xFFFFFFFF)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFFFFFFF),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Google 로고
                                Image(
                                    painter = painterResource(id = R.drawable.google_logo_2),
                                    contentDescription = "Google Logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(end = 10.dp)
                                )
                                Text(
                                    text = "구글로 로그인하기",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

