package com.example.tripcart.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object EnvUtils {
    /*
     * MAPS_API_KEY를 AndroidManifest의 메타데이터(.env)에서 읽어오며,
     * 빌드 시 .env 파일에서 manifestPlaceholders로 주입된 값을 사용.
     * (소스는 .env 파일이지만, 런타임에는 AndroidManifest에서 읽음.)
     */
    fun getMapsApiKey(context: Context): String {
        return try {
            val ai: ApplicationInfo = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            bundle?.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            android.util.Log.e("EnvUtils", "Failed to read API key from manifest", e)
            ""
        }
    }
}

