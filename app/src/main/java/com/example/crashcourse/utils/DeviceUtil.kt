package com.example.crashcourse.util

import android.content.Context
import android.provider.Settings
import android.os.Build

object DeviceUtil {
    fun getUniqueDeviceId(context: Context): String {
        // Ambil Android ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        // Gabung dengan Model HP biar keren (ex: SAMSUNG-A50_837281)
        val hardwareInfo = "${Build.MANUFACTURER}-${Build.MODEL}"
        return "${hardwareInfo.uppercase()}_$androidId".replace(" ", "_")
    }
}