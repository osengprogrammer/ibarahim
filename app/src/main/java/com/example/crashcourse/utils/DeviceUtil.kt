package com.example.crashcourse.util

import android.content.Context
import android.provider.Settings
import android.os.Build
import java.security.MessageDigest

object DeviceUtil {

    /**
     * Menghasilkan ID unik yang aman dengan menggabungkan:
     * Hardware Info + Android ID + Native ISO Key (C++) + SHA-256 Hashing
     */
    fun getUniqueDeviceId(context: Context): String {
        // 1. Ambil Android ID (Unik per perangkat/user)
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"

        // 2. Ambil Info Hardware
        val hardwareInfo = "${Build.MANUFACTURER}-${Build.MODEL}".uppercase()

        // 3. Ambil Bumbu Rahasia dari C++ (Native)
        // Ini yang bikin hacker pusing karena kuncinya tersembunyi di binary
        val secretIsoKey = try {
            NativeKeyStore.getIsoKey()
        } catch (e: Exception) {
            "FALLBACK_KEY_DO_NOT_USE_IN_PROD" 
        }

        // 4. Gabungkan semuanya menjadi satu string panjang
        val rawSeed = "${hardwareInfo}_${androidId}_$secretIsoKey".replace(" ", "_")

        // 5. Ubah menjadi Hash SHA-256 agar panjangnya seragam (64 karakter) dan aman
        return sha256(rawSeed)
    }

    /**
     * Fungsi Helper untuk merubah String menjadi Hash SHA-256
     */
    private fun sha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }.uppercase()
        } catch (e: Exception) {
            // Jika hashing gagal, balikkan string dengan karakter aman
            input.filter { it.isLetterOrDigit() }.uppercase()
        }
    }
}