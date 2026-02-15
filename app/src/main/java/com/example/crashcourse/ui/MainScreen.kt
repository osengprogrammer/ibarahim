package com.example.crashcourse.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.crashcourse.ui.checkin.CheckInScreen
import com.example.crashcourse.ui.profile.ProfileScreen // ✅ FIX IMPORT
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.RecognitionViewModel

@Composable
fun MainScreen(
    authState: AuthState.Active,
    navController: NavHostController,
    recognitionViewModel: RecognitionViewModel = viewModel(),
    onLogoutRequest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default Scanner

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, "Menu") },
                    label = { Text("Menu") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Face, "Scanner") },
                    label = { Text("Scanner") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, "Profil") },
                    label = { Text("Profil") }
                )
            }
        }
    ) { innerPadding ->
        
        Crossfade(
            targetState = selectedTab, 
            modifier = Modifier.padding(innerPadding),
            label = "MainCrossfade"
        ) { index ->
            when (index) {
                0 -> AdminDashboardScreen(
                    navController = navController,
                    authState = authState,
                    recognitionVM = recognitionViewModel,
                    onSwitchToScanner = { selectedTab = 1 }
                )
                
                1 -> CheckInScreen(
                    useBackCamera = false,
                    recognitionViewModel = recognitionViewModel
                )
                
                2 -> ProfileScreen(
                    authState = authState,
                    onLogout = onLogoutRequest
                    // ✅ FIX: Hapus onNavigateBack karena tidak dibutuhkan di Tab
                )
            }
        }
    }
}