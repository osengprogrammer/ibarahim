package com.example.crashcourse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.crashcourse.ui.MainScreen
import com.example.crashcourse.ui.auth.AuthScreen
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🚀 Setup Fullscreen (Immersive Mode)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // ❌ FaceRecognizer.initialize JANGAN DI SINI LAGI (Sudah di AzuraApplication)

        setContent {
            CrashcourseTheme {
                // Observasi Auth State
                val authState by authViewModel.authState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = authState) {
                        is AuthState.Active -> {
                            MainScreen(
                                authState = state,
                                onLogout = { authViewModel.logout() }
                            )
                        }

                        is AuthState.StatusWaiting -> {
                            StatusWaitingScreen(
                                message = state.message,
                                onLogout = { authViewModel.logout() }
                            )
                        }

                        is AuthState.Loading, is AuthState.Checking -> {
                            LoadingScreen()
                        }

                        else -> {
                            AuthScreen(viewModel = authViewModel)
                        }
                    }
                }
            }
        }
    }
    
    // ❌ FaceRecognizer.close() JANGAN DI DESTROY. 
    // Biarkan tetap hidup selama aplikasi di RAM, atau handle di Application class.
}

// --- KOMPONEN UI PENDUKUNG ---

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Memuat Data...", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StatusWaitingScreen(message: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning, // ✅ Import sudah dirapikan
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Status Akun Bermasalah",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Keluar / Ganti Akun")
        }
    }
}