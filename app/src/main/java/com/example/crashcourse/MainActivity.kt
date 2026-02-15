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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.navigation.NavGraph
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * 🏛️ MainActivity (V.12.0 - Monisme Release)
 * Authority: Root Controller untuk Azura Attendance.
 * Inovasi: Cold-Start FaceCache Warmup & Reactive Auth Bridge.
 */
class MainActivity : ComponentActivity() {
    
    // AuthViewModel sebagai Single Source of Truth untuk status login
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // 🔥 WARM-UP MEMORY (Monisme Strategy)
        // Kita tidak menunggu user masuk ke Scanner. 
        // Begitu aplikasi diklik, kita langsung siapkan FaceCache di RAM.
        lifecycleScope.launch {
            FaceCache.ensureLoaded(applicationContext)
        }
        
        setContent {
            CrashcourseTheme {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // 🧭 ROOT NAVIGATION ENGINE
                        // Menangani perpindahan antara Login -> MainScreen (Dashboard+Scanner)
                        NavGraph(
                            navController = navController,
                            authState = authState,
                            authViewModel = authViewModel, 
                            onLogoutRequest = { 
                                // Saat logout, bersihkan RAM biometrik untuk keamanan (Privacy First)
                                authViewModel.logout()
                                FaceCache.clear() 
                            }
                        )

                        // 🔍 INITIALIZING OVERLAY
                        // Tampil saat AuthViewModel sedang mengecek Firebase Session (Persistent Login)
                        if (authState is AuthState.Checking) {
                            LoadingScreen("Sinkronisasi Sesi Azura...")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 🎨 GLOBAL UI COMPONENTS
// ==========================================

/**
 * Layar Loading Global dengan Identitas Visual Azura Tech.
 */
@Composable
fun LoadingScreen(message: String = "Mohon Tunggu...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary, 
                strokeWidth = 4.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Banyuwangi Digital Solution", 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}