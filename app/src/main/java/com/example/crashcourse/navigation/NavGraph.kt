package com.example.crashcourse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel 

// 🚀 CORE SCREENS
import com.example.crashcourse.ui.MainScreen
import com.example.crashcourse.ui.AdminDashboardScreen
import com.example.crashcourse.ui.ProfileScreen
import com.example.crashcourse.ui.auth.LoginScreen
import com.example.crashcourse.ui.auth.RegisterScreen
import com.example.crashcourse.ui.auth.StatusWaitingScreen
import com.example.crashcourse.ui.add.AddUserScreen
import com.example.crashcourse.ui.add.RegistrationMenuScreen
import com.example.crashcourse.ui.add.SingleUploadScreen
import com.example.crashcourse.ui.add.BulkRegistrationScreen
import com.example.crashcourse.ui.edit.EditUserScreen
import com.example.crashcourse.ui.checkin.CheckInScreen
import com.example.crashcourse.ui.checkin.CheckInRecordScreen
import com.example.crashcourse.ui.options.OptionsManagementScreen
import com.example.crashcourse.ui.options.MasterClassManagementScreen
import com.example.crashcourse.ui.monitor.LiveMonitorScreen 
import com.example.crashcourse.ui.management.UserManagementScreen
import com.example.crashcourse.ui.face.FaceListScreen
import com.example.crashcourse.ui.EditUserScopeScreen 
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
    // 🎯 1. GLOBAL SECURITY OBSERVER
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.LoggedOut -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.StatusWaiting -> {
                navController.navigate(Screen.StatusWaiting.route) {
                    popUpTo(0)
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
            LaunchedEffect(authState) {
                if (authState is AuthState.Active) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            if (authState is AuthState.Active) {
                LoadingScreen("Mengalihkan ke Dashboard...")
            } else {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    viewModel = viewModel
                )
            }
        }
        
        composable(Screen.Register.route) {
            LaunchedEffect(authState) {
                if (authState is AuthState.Active) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            }

            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                viewModel = viewModel 
            )
        }
        
        composable(Screen.StatusWaiting.route) { 
            StatusWaitingScreen(viewModel = viewModel) 
        }

        // ==========================================
        // 🏠 ACTIVE ROUTES
        // ==========================================
        
        composable(Screen.Main.route) {
            when (authState) {
                is AuthState.Active -> {
                    MainScreen(
                        authState = authState,
                        // ✅ MODIFIKASI: Sekarang navigasi CheckIn butuh konteks sesi
                        onNavigateToCheckIn = { 
                            // Jika di Main sudah ada pilihan kelas, kirim namanya
                            // Untuk sementara default "General" atau rute dinamis
                            navController.navigate("checkin_screen/General Session") 
                        },
                        onNavigateToAdmin = { navController.navigate(Screen.Admin.route) },
                        onLogoutRequest = onLogoutRequest
                    )
                }
                is AuthState.Loading -> LoadingScreen(authState.message)
                is AuthState.Checking -> LoadingScreen("Memverifikasi data...")
                else -> LoadingScreen("Sinkronisasi Cloud...")
            }
        }

        composable(Screen.Profile.route) { 
            if (authState is AuthState.Active) {
                ProfileScreen(authState, { onLogoutRequest() }, { navController.popBackStack() })
            } else { LoadingScreen() }
        }

        // ✅ FIX: CHECK-IN DENGAN ARGUMENT (MANY-TO-MANY)
        composable(
            route = "checkin_screen/{sessionName}",
            arguments = listOf(navArgument("sessionName") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionName = backStackEntry.arguments?.getString("sessionName") ?: "General"
            CheckInScreen(
                useBackCamera = true,
                activeSession = sessionName // 🚀 Kirim ke Screen
            )
        }
        
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