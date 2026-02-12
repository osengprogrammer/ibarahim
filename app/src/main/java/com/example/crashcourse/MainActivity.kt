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
import androidx.navigation.compose.rememberNavController
import com.example.crashcourse.navigation.NavGraph
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    // Instance utama ViewModel yang mengontrol seluruh aplikasi
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            CrashcourseTheme {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // FIX: Teruskan authViewModel ke NavGraph
                        NavGraph(
                            navController = navController,
                            authState = authState,
                            viewModel = authViewModel, // <--- TAMBAHAN PENTING
                            onLogoutRequest = { 
                                authViewModel.logout() 
                            }
                        )

                        // 🔍 Overlay Loading
                        if (authState is AuthState.Checking) {
                            LoadingScreen("Memverifikasi Sesi Azura...")
                        }
                    }
                }
            }
        }
    }
}

// ... LoadingScreen tetap sama ...
@Composable
fun LoadingScreen(message: String = "Menghubungkan ke Azura Cloud...") {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}