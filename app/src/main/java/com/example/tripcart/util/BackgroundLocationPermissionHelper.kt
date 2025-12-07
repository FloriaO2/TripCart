package com.example.tripcart.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BackgroundLocationPermissionHelper {
    
    // 백그라운드 위치 권한이 허용되었는지 확인
    fun isBackgroundLocationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 이상일 때
            // ACCESS_BACKGROUND_LOCATION 권한 확인
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 9 이하는 기본적으로 백그라운드 위치 권한이 있음
            true
        }
    }
    
    // 백그라운드 위치 권한이 필요한지 확인 (Android 10 이상)
    fun isBackgroundLocationPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // Android 10 이상일 때
    }
}