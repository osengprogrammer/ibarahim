package com.example.crashcourse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.crashcourse.ml.FaceRecognizer
import com.example.crashcourse.ui.MainScreen
import com.example.crashcourse.ui.auth.AuthScreen // ✅ AuthScreen Baru
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel // ✅ AuthViewModel Baru

class MainActivity : ComponentActivity() {

    // 1. Inisialisasi AuthViewModel sebagai pusat kendali akses
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi tampilan penuh (Edge-to-Edge)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // 2. Inisialisasi Engine AI Wajah
        try {
            FaceRecognizer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            CrashcourseTheme {
                // 3. Pantau status autentikasi secara Real-time
                val authState by authViewModel.authState.collectAsState()

                

                when (authState) {
                    is AuthState.Active -> {
                        // ✅ STATUS AKTIF: Tampilkan Aplikasi Utama
                        // Kita berikan fungsi logout agar bisa dipanggil dari dalam Settings
                        MainScreen(
                            onLogout = { authViewModel.logout() }
                        )
                    }
                    else -> {
                        // ⛔ BELUM LOGIN / PENDING / ERROR: Tampilkan AuthScreen
                        AuthScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Bersihkan resource AI saat aplikasi ditutup
        FaceRecognizer.close()
    }
}