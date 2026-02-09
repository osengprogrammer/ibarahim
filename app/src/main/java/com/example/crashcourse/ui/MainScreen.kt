package com.example.crashcourse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

// --- IMPORT INTERNAL ---
import com.example.crashcourse.navigation.Screen
import com.example.crashcourse.navigation.addAppManagementGraph
import com.example.crashcourse.ui.add.AddUserScreen
import com.example.crashcourse.ui.add.BulkRegistrationScreen
import com.example.crashcourse.ui.add.SingleUploadScreen
import com.example.crashcourse.ui.checkin.CheckInScreen
import com.example.crashcourse.ui.menu.RegistrationMenuScreen
import com.example.crashcourse.ui.AdminDashboardScreen
import com.example.crashcourse.ui.LiveAttendanceScreen

import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.ui.theme.AzuraPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authState: AuthState.Active,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val useBackCamera by remember { mutableStateOf(false) }
    
    // Shared ViewModels agar data pendaftaran wajah konsisten
    val sharedFaceViewModel: FaceViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel() 

    val isAdmin = authState.role == "ADMIN"

    Scaffold(
        bottomBar = { 
            BottomNav(navController, authState.role) 
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.CheckIn.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { androidx.compose.animation.fadeIn() },
            exitTransition = { androidx.compose.animation.fadeOut() }
        ) {
            // ==========================================
            // 1. UTAMA (ABSENSI)
            // ==========================================
            composable(Screen.CheckIn.route) {
                CheckInScreen(useBackCamera = useBackCamera)
            }

            // ==========================================
            // 2. ADMIN & MONITORING
            // ==========================================
            
            composable(Screen.AdminDashboard.route) {
                if (isAdmin) {
                    AdminDashboardScreen(navController = navController)
                } else {
                    AccessDeniedScreen()
                }
            }

            composable(Screen.LiveMonitor.route) {
                if (isAdmin) {
                    LiveAttendanceScreen(onBack = { navController.popBackStack() })
                } else {
                    AccessDeniedScreen()
                }
            }

            // ==========================================
            // 3. REGISTRASI SISWA (FLOW)
            // ==========================================
            
            composable(Screen.RegistrationMenu.route) {
                if (isAdmin) {
                    RegistrationMenuScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddUser = { navController.navigate(Screen.AddUser.route) },
                        onNavigateToBulkRegister = { navController.navigate(Screen.BulkRegister.route) },
                        onNavigateToSingleUpload = { navController.navigate(Screen.SingleUpload.route) }
                    )
                } else {
                    AccessDeniedScreen()
                }
            }

            composable(Screen.AddUser.route) {
                if (isAdmin) {
                    AddUserScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onUserAdded = { navController.popBackStack() },
                        viewModel = sharedFaceViewModel
                    )
                }
            }

            composable(Screen.BulkRegister.route) {
                if (isAdmin) {
                    BulkRegistrationScreen(
                        bulkViewModel = viewModel(),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.SingleUpload.route) {
                if (isAdmin) {
                    SingleUploadScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onUserAdded = { navController.popBackStack() },
                        viewModel = sharedFaceViewModel
                    )
                }
            }

            // ==========================================
            // 4. GRAPH MODULAR (Manajemen & Pengaturan)
            // ==========================================
            addAppManagementGraph(
                navController = navController,
                authState = authState,
                authViewModel = authViewModel,
                onLogout = onLogout
            )
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController, role: String) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = remember(role) {
        mutableListOf<Pair<Screen, String>>().apply {
            add(Screen.CheckIn to "Absensi")
            if (role == "ADMIN" || role == "SUPERVISOR") {
                add(Screen.CheckInRecord to "Riwayat")
            }
            if (role == "ADMIN") {
                add(Screen.AdminDashboard to "Admin")
            }
            add(Screen.Settings to "Pengaturan")
        }
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        items.forEach { (screen, label) ->
            // Cek hierarki rute agar icon tetap menyala di sub-menu
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true ||
                           (screen == Screen.AdminDashboard && currentDestination?.route?.contains("registration") == true)
            
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true 
                        }
                        launchSingleTop = true
                        restoreState = true 
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.CheckIn -> Icons.Default.Fingerprint
                            Screen.CheckInRecord -> Icons.Default.History
                            Screen.AdminDashboard -> Icons.Default.AdminPanelSettings
                            Screen.Settings -> Icons.Default.Settings
                            else -> Icons.Default.Circle
                        },
                        contentDescription = label
                    )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AzuraPrimary,
                    selectedTextColor = AzuraPrimary,
                    indicatorColor = AzuraPrimary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

/**
 * Komponen UI untuk menampilkan pesan jika akses ditolak.
 * Dibuat publik agar bisa digunakan di file navigasi (addAppManagementGraph).
 */
@Composable
fun AccessDeniedScreen(message: String = "Akses Terbatas: Hanya Admin.") {
    Box(
        modifier = Modifier.fillMaxSize(), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp), 
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message, 
                color = MaterialTheme.colorScheme.error, 
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}