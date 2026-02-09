package com.example.crashcourse

import android.app.Application
import com.example.crashcourse.ml.FaceRecognizer

class AzuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 🚀 Inisialisasi SEKALI SAJA seumur hidup aplikasi
        try {
            FaceRecognizer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Tidak perlu onDestroy, OS yang akan mematikan memori saat app ditutup paksa
}