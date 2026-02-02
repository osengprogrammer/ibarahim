package com.example.crashcourse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // ✅ Added to access the ViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.crashcourse.ml.FaceRecognizer
import com.example.crashcourse.ui.MainScreen
import com.example.crashcourse.ui.auth.LicenseScreen
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.viewmodel.LicenseState
import com.example.crashcourse.viewmodel.LicenseViewModel

class MainActivity : ComponentActivity() {

    // 1. Initialize the License ViewModel (The "Security Guard")
    private val licenseViewModel: LicenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        try {
            FaceRecognizer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            CrashcourseTheme {
                val licenseState by licenseViewModel.licenseState.collectAsState()

                when (licenseState) {
                    is LicenseState.Valid -> {
                        // ✅ UPDATE PENTING DI SINI:
                        // Kita oper licenseViewModel ke dalam MainScreen
                        // (Pastikan ui/MainScreen.kt Anda sudah diupdate untuk menerima parameter ini)
                        MainScreen(licenseViewModel = licenseViewModel)
                    }
                    else -> {
                        // ⛔ LOCKED: Show the License Input Screen
                        LicenseScreen(viewModel = licenseViewModel)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FaceRecognizer.close()
    }
}