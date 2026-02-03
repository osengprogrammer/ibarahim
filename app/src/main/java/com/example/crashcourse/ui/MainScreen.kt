package com.example.crashcourse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.crashcourse.navigation.Screen
import com.example.crashcourse.navigation.addAppManagementGraph
import com.example.crashcourse.ui.add.AddUserScreen
import com.example.crashcourse.ui.add.BulkRegistrationScreen
import com.example.crashcourse.ui.edit.EditUserScreen
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.FaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authState: AuthState.Active,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var useBackCamera by remember { mutableStateOf(false) }
    
    val sharedFaceViewModel: FaceViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel() 

    val isAdmin = authState.role == "ADMIN"
    // Guru (USER/SUPERVISOR) sekarang diizinkan melihat list murid
    val canViewStudentList = authState.role == "ADMIN" || authState.role == "USER" || authState.role == "SUPERVISOR"

    Scaffold(
        bottomBar = { 
            BottomNav(navController, authState.role) 
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.CheckIn.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // 1. Check In (Absensi)
            composable(Screen.CheckIn.route) {
                CheckInScreen(useBackCamera = useBackCamera)
            }

            // 2. Admin Dashboard (Konfigurasi Sistem)
            composable(Screen.AdminDashboard.route) {
                if (isAdmin) {
                    AdminDashboardScreen(navController)
                } else {
                    AccessDeniedScreen("Hanya Admin yang dapat mengakses Dashboard.")
                }
            }

            // 3. Live Monitoring
            composable(Screen.LiveMonitor.route) {
                if (isAdmin) {
                    LiveAttendanceScreen(
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    AccessDeniedScreen()
                }
            }

            // 4. Registration Menu (Menu Pilihan Tambah Murid)
            composable(Screen.RegistrationMenu.route) {
                if (isAdmin) {
                    RegistrationMenuScreen(
                        onNavigateToBulkRegister = { navController.navigate(Screen.BulkRegister.route) },
                        onNavigateToAddUser = { navController.navigate(Screen.AddUser.route) }
                    )
                } else {
                    AccessDeniedScreen("Pendaftaran murid hanya dilakukan oleh Admin.")
                }
            }

            // 5. Single Add User
            composable(Screen.AddUser.route) {
                if (isAdmin) {
                    AddUserScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onUserAdded = { },
                        viewModel = sharedFaceViewModel
                    )
                }
            }

            // 6. Bulk Register
            composable(Screen.BulkRegister.route) {
                if (isAdmin) {
                    BulkRegistrationScreen(faceViewModel = sharedFaceViewModel)
                }
            }

            // 7. Manage Faces (Daftar Murid) 🔥 UPDATED
            composable(Screen.Manage.route) {
                if (canViewStudentList) { 
                    FaceListScreen(
                        authState = authState, 
                        viewModel = sharedFaceViewModel,
                        onEditUser = { user ->
                            // PROTEKSI: Navigasi ke Edit hanya jalan jika Role adalah ADMIN
                            if (isAdmin) {
                                navController.navigate(Screen.EditUser.createRoute(user.studentId))
                            }
                        }
                    )
                } else {
                    AccessDeniedScreen()
                }
            }

            // 8. Edit User (Halaman Edit Detail & Foto)
            composable(Screen.EditUser.route) { backStackEntry ->
                val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                if (isAdmin) {
                    EditUserScreen(
                        studentId = studentId,
                        useBackCamera = useBackCamera,
                        onNavigateBack = { navController.popBackStack() },
                        onUserUpdated = { },
                        faceViewModel = sharedFaceViewModel
                    )
                } else {
                    AccessDeniedScreen("Anda tidak memiliki izin untuk mengedit data biometrik.")
                }
            }

            // 9. Management Graph (Profile, Records, Options)
            addAppManagementGraph(
                navController = navController,
                authState = authState,
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController, role: String) {
    val items = remember(role) {
        val list = mutableListOf<Pair<Screen, String>>(
            Screen.CheckIn to "Absensi"
        )
        
        // Riwayat Absensi untuk Admin & Supervisor
        if (role == "SUPERVISOR" || role == "ADMIN") {
            list.add(Screen.CheckInRecord to "Riwayat")
        }

        // Daftar Murid dimunculkan untuk Guru agar bisa monitoring
        if (role == "USER" || role == "SUPERVISOR" || role == "ADMIN") {
            list.add(Screen.Manage to "Murid")
        }

        if (role == "ADMIN") {
            list.add(Screen.AdminDashboard to "Admin")
        }
        
        list.add(Screen.Profile to "Profil")
        list
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { (screen, label) ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.CheckIn -> Icons.Default.Fingerprint
                            Screen.CheckInRecord -> Icons.Default.History
                            Screen.Manage -> Icons.Default.People // Ikon Murid
                            Screen.AdminDashboard -> Icons.Default.AdminPanelSettings
                            Screen.Profile -> Icons.Default.AccountCircle
                            else -> Icons.Default.Circle
                        },
                        contentDescription = label
                    )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun AccessDeniedScreen(message: String = "Akses Terbatas.") {
    Box(
        modifier = Modifier.fillMaxSize(), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message, 
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}