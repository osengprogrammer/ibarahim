package com.example.crashcourse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.crashcourse.ml.FaceRecognizer
import com.example.crashcourse.ui.MainScreen
import com.example.crashcourse.ui.auth.AuthScreen
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel
import androidx.compose.material.icons.filled.Warning // Untuk StatusWaitingScreen
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

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
                // Pantau status autentikasi
                val authState by authViewModel.authState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = authState) {
                        is AuthState.Active -> {
                            // ✅ Kirim state 'state' (yang sudah di-cast otomatis jadi Active) ke MainScreen
                            MainScreen(
                                authState = state, 
                                onLogout = { authViewModel.logout() }
                            )
                        }
                        
                        is AuthState.StatusWaiting -> {
                            // ⏳ Tampilkan layar tunggu (Pending/Banned/Expired)
                            StatusWaitingScreen(
                                message = state.message,
                                onLogout = { authViewModel.logout() }
                            )
                        }

                        is AuthState.Loading, is AuthState.Checking -> {
                            // 🔄 Layar Loading saat cek database
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        else -> {
                            // ⛔ LoggedOut atau Error: Tampilkan layar Login/Register
                            AuthScreen(viewModel = authViewModel)
                        }
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

/**
 * Layar khusus untuk menangani status PENDING, BANNED, atau EXPIRED.
 * Agar user tidak bisa masuk ke menu utama tapi tetap bisa Logout.
 */
@Composable
fun StatusWaitingScreen(message: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
           imageVector = androidx.compose.material.icons.Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout) {
            Text("Kembali ke Login / Ganti Akun")
        }
    }
}