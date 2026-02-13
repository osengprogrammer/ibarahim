package com.example.crashcourse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel 

// 🚀 CORE SCREENS & COMPONENTS
import com.example.crashcourse.ui.*
import com.example.crashcourse.ui.auth.*
import com.example.crashcourse.ui.add.*
import com.example.crashcourse.ui.edit.*
import com.example.crashcourse.ui.checkin.*
import com.example.crashcourse.ui.options.*
import com.example.crashcourse.ui.monitor.*
import com.example.crashcourse.ui.management.*
import com.example.crashcourse.ui.face.*
import com.example.crashcourse.LoadingScreen 

// 🚀 VIEWMODELS & STATES
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel
import com.example.crashcourse.viewmodel.RegisterViewModel

@Composable
fun NavGraph(
    navController: NavHostController, 
    authState: AuthState,
    viewModel: AuthViewModel, 
    onLogoutRequest: () -> Unit
) {
    // 🎯 1. CENTRALIZED NAVIGATION OBSERVER
    // Mengatur aliran navigasi global agar tidak terjadi loop/refresh terus-menerus
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Active -> {
                // Hanya pindah ke Main jika saat ini masih di layar Login/Register
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Screen.Login.route || currentRoute == Screen.Register.route) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.LoggedOut -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.StatusWaiting -> {
                // Hanya pindah jika belum di layar Waiting
                if (navController.currentBackStackEntry?.destination?.route != Screen.StatusWaiting.route) {
                    navController.navigate(Screen.StatusWaiting.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            else -> {} 
        }
    }

    // 🎯 2. SETUP NAVHOST
    NavHost(
        navController = navController, 
        startDestination = Screen.Login.route 
    ) {
        
        // ==========================================
        // 🔐 AUTH ROUTES
        // ==========================================
        
        composable(Screen.Login.route) {
            // ✅ FIX: Hapus LaunchedEffect internal agar tidak berantem dengan Global Observer
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                viewModel = viewModel
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                viewModel = viewModel 
            )
        }
        
        composable(Screen.StatusWaiting.route) { 
            StatusWaitingScreen(viewModel = viewModel) 
        }

        // ==========================================
        // 🏠 ACTIVE ROUTES (ADMIN & STAFF)
        // ==========================================
        
        composable(Screen.Main.route) {
            when (authState) {
                is AuthState.Active -> {
                    MainScreen(
                        authState = authState,
                        onNavigateToCheckIn = { sessionName -> 
                            navController.navigate("checkin_screen/$sessionName") 
                        },
                        onNavigateToAdmin = { 
                            // Proteksi tambahan: hanya navigasi jika role ADMIN
                            if (authState.role == "ADMIN") {
                                navController.navigate(Screen.Admin.route) 
                            }
                        },
                        onLogoutRequest = onLogoutRequest
                    )
                }
                is AuthState.Loading -> LoadingScreen(authState.message)
                else -> LoadingScreen("Mempersiapkan data sesi...")
            }
        }

        composable(Screen.Profile.route) { 
            if (authState is AuthState.Active) {
                ProfileScreen(authState, { onLogoutRequest() }, { navController.popBackStack() })
            } else { LoadingScreen() }
        }

        composable(
            route = "checkin_screen/{sessionName}",
            arguments = listOf(navArgument("sessionName") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionName = backStackEntry.arguments?.getString("sessionName") ?: "General"
            CheckInScreen(
                useBackCamera = true,
                activeSession = sessionName
            )
        }
        
        // ==========================================
        // ⚙️ MANAGEMENT ROUTES (DIBATASI JIKA PERLU)
        // ==========================================

        composable(Screen.Admin.route) { 
            if (authState is AuthState.Active) AdminDashboardScreen(navController, authState) else LoadingScreen() 
        }
        
        composable(Screen.Options.route) { 
            if (authState is AuthState.Active) OptionsManagementScreen({ navController.popBackStack() }) else LoadingScreen() 
        }

        composable(Screen.MasterClass.route) { 
            if (authState is AuthState.Active) MasterClassManagementScreen({ navController.popBackStack() }) else LoadingScreen() 
        }

        composable(Screen.LiveMonitor.route) { 
            if (authState is AuthState.Active) LiveMonitorScreen({ navController.popBackStack() }) else LoadingScreen() 
        }

        composable(Screen.History.route) {
            if (authState is AuthState.Active) CheckInRecordScreen(authState, { navController.popBackStack() }) else LoadingScreen() 
        }

        composable(Screen.FaceList.route) {
            if (authState is AuthState.Active) FaceListScreen({ navController.popBackStack() }, { id -> navController.navigate(Screen.EditUser.createRoute(id)) }) else LoadingScreen() 
        }

        composable(Screen.RegistrationMenu.route) {
            if (authState is AuthState.Active) RegistrationMenuScreen({ navController.navigate(Screen.Add.route) }, { navController.navigate(Screen.Bulk.route) }, { navController.navigate(Screen.SingleUpload.route) }, { navController.popBackStack() }) else LoadingScreen() 
        }
        
        composable(Screen.Add.route) { AddUserScreen({ navController.popBackStack() }, { navController.popBackStack() }) }

        composable(Screen.Bulk.route) { 
            val registerViewModel: RegisterViewModel = viewModel()
            BulkRegistrationScreen(registerViewModel, { navController.popBackStack() }) 
        }
        
        composable(Screen.SingleUpload.route) { SingleUploadScreen({ navController.popBackStack() }, { navController.popBackStack() }) }

        composable(Screen.UserManagement.route) {
            if (authState is AuthState.Active) UserManagementScreen(authState, { navController.popBackStack() }, { id -> navController.navigate(Screen.EditUserScope.createRoute(id)) }) else LoadingScreen() 
        }

        composable(
            route = Screen.EditUserScope.route, 
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (authState is AuthState.Active) EditUserScopeScreen(userId, authState, { navController.popBackStack() }) else LoadingScreen() 
        }
        
        composable(
            route = Screen.EditUser.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType; nullable = false })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            EditUserScreen(userId, { navController.popBackStack() }, { navController.popBackStack() })
        }
    }
}