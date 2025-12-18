package com.example.tripcart.util

import android.app.Activity
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 상태바 색상 설정
@Composable
fun SetStatusBarColor(
    statusBarColor: androidx.compose.ui.graphics.Color,
    isLightStatusBars: Boolean = true
) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    
    // Composable이 화면에 나타나면 실행
    DisposableEffect(statusBarColor, isLightStatusBars) {
        // 시스템 UI 제어를 위한 컨트롤러
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        
        window.statusBarColor = statusBarColor.toArgb()
        windowInsetsController.isAppearanceLightStatusBars = isLightStatusBars
        
        onDispose {
            // 복원 할 필요가 없어서 일단 주석 처리
        }
    }
}

