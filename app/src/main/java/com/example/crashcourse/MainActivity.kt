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
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.crashcourse.navigation.NavGraph
import com.example.crashcourse.navigation.Screen
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel

@Composable
fun LoadingScreen(message: String = "Menghubungkan ke Azura Cloud...") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrashcourseTheme {
                val authState by authViewModel.authState.collectAsState()
                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (authState is AuthState.Checking) {
                        LoadingScreen()
                    } else {
                        // 🚀 FIXED: 'startDestination' removed as it's handled inside NavGraph
                        NavGraph(
                            navController = navController,
                            authState = authState
                        )
                    }
                }
            }
        }
    }
}