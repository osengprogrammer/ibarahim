package com.example.crashcourse.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel 

// 🚀 UI IMPORTS
import com.example.crashcourse.ui.*
import com.example.crashcourse.ui.auth.*
import com.example.crashcourse.ui.add.*
import com.example.crashcourse.ui.edit.*
import com.example.crashcourse.ui.checkin.*
import com.example.crashcourse.ui.checkin.components.CheckInLoadingView
import com.example.crashcourse.ui.options.*
import com.example.crashcourse.ui.monitor.*
import com.example.crashcourse.ui.management.*
import com.example.crashcourse.ui.face.FaceListScreen 

// 🚀 VIEWMODELS & STATES
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel

private const val NAV_TAG = "AzuraNav"

/**
 * 🗺️ Azura Tech Navigation Graph (V.12.0 - Convergence Version)
 * Fokus: Mengarahkan Auth ke MainScreen (Container Bottom Nav).
 */
@Composable
fun NavGraph(
    navController: NavHostController, 
    authState: AuthState,
    authViewModel: AuthViewModel, 
    onLogoutRequest: () -> Unit
) {
    // 🎯 GLOBAL REDIRECT LOGIC
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Active -> {
                Log.i(NAV_TAG, "✅ User Active. Entering Main Container...")
                // Arahkan ke rute "Main" (Yang berisi Bottom Nav)
                navController.navigate("main_root") {
                    popUpTo(Screen.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is AuthState.LoggedOut -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> { /* Biarkan layar Checking/Loading menangani sisanya */ }
        }
    }

    NavHost(
        navController = navController, 
        startDestination = Screen.Login.route 
    ) {
        
        // ==========================================
        // 🔐 AUTHENTICATION FLOW
        // ==========================================
        
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                viewModel = authViewModel
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                viewModel = authViewModel 
            )
        }
        
        composable(Screen.StatusWaiting.route) { 
            StatusWaitingScreen(viewModel = authViewModel) 
        }

        // ==========================================
        // 🏠 THE MASTER CONTAINER (Bottom Nav)
        // ==========================================
        
        composable("main_root") {
            if (authState is AuthState.Active) {
                // ✅ Inilah kuncinya! MainScreen memegang Dashboard, Scanner, dan Profile.
                MainScreen(
                    authState = authState,
                    navController = navController,
                    onLogoutRequest = onLogoutRequest
                )
            } else {
                CheckInLoadingView() 
            }
        }

        // ==========================================
        // 🛠️ SUBSCREENS (Masih butuh NavController)
        // ==========================================
        
        // Rute ini tetap di sini karena dibuka sebagai "layar penuh" dari dashboard
        composable(Screen.History.route) {
            if (authState is AuthState.Active) {
                CheckInRecordScreen(
                    authState = authState, 
                    onNavigateBack = { navController.popBackStack() }
                ) 
            } else { CheckInLoadingView() } 
        }

        composable(Screen.UserManagement.route) {
            if (authState is AuthState.Active) {
                UserManagementScreen(
                    authState = authState, 
                    onBack = { navController.popBackStack() }, 
                    onEditScope = { id -> navController.navigate(Screen.EditUserScope.createRoute(id)) }
                )
            } else { CheckInLoadingView() }
        }

        composable(Screen.FaceList.route) {
            if (authState is AuthState.Active) {
                FaceListScreen(
                    faceVM = viewModel(),
                    onNavigateBack = { navController.popBackStack() }, 
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditUser.createRoute(id)) }
                )
            } else { CheckInLoadingView() }
        }

        composable(Screen.RegistrationMenu.route) {
            if (authState is AuthState.Active) {
                RegistrationMenuScreen(
                    onNavigateToSingleAdd = { navController.navigate(Screen.Add.route) }, 
                    onNavigateToBulk = { navController.navigate(Screen.Bulk.route) }, 
                    onNavigateToGallery = { navController.navigate(Screen.SingleUpload.route) }, 
                    onBack = { navController.popBackStack() } 
                )
            } else { CheckInLoadingView() }
        }
        
        composable(Screen.Add.route) { 
            AddUserScreen(
                onNavigateBack = { navController.popBackStack() }, 
                onUpdateSuccess = { navController.popBackStack() }
            ) 
        }

        composable(Screen.Bulk.route) { 
            BulkRegistrationScreen(onNavigateBack = { navController.popBackStack() }) 
        }
        
        composable(Screen.SingleUpload.route) { 
            SingleUploadScreen(
                onNavigateBack = { navController.popBackStack() }, 
                onUpdateSuccess = { navController.popBackStack() }
            ) 
        }

        composable(
            route = Screen.EditUserScope.route, 
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (authState is AuthState.Active) {
                EditUserScopeScreen(
                    userId = userId, 
                    authState = authState, 
                    onBack = { navController.popBackStack() }
                ) 
            } else { CheckInLoadingView() }
        }
        
        composable(
            route = Screen.EditUser.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            EditUserScreen(
                studentId = userId, 
                onNavigateBack = { navController.popBackStack() }, 
                onUpdateSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.MasterClass.route) { 
            MasterClassManagementScreen(onNavigateBack = { navController.popBackStack() }) 
        }
        composable(Screen.Options.route) { 
            OptionsManagementScreen(onNavigateBack = { navController.popBackStack() }) 
        }
        composable(Screen.LiveMonitor.route) { 
            LiveMonitorScreen(onBack = { navController.popBackStack() }) 
        }
    }
}