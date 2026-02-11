package com.example.crashcourse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// 🚀 CORE SCREENS
import com.example.crashcourse.ui.MainScreen
import com.example.crashcourse.ui.AdminDashboardScreen
import com.example.crashcourse.ui.auth.LoginScreen
import com.example.crashcourse.ui.auth.RegisterScreen
import com.example.crashcourse.ui.auth.StatusWaitingScreen
import com.example.crashcourse.ui.add.AddUserScreen
import com.example.crashcourse.ui.add.RegistrationMenuScreen
import com.example.crashcourse.ui.add.SingleUploadScreen
import com.example.crashcourse.ui.add.BulkRegistrationScreen
import com.example.crashcourse.ui.edit.EditUserScreen
import com.example.crashcourse.ui.checkin.CheckInScreen
import com.example.crashcourse.ui.options.OptionsManagementScreen
import com.example.crashcourse.ui.options.MasterClassManagementScreen
import com.example.crashcourse.ui.monitor.LiveMonitorScreen 
import com.example.crashcourse.ui.management.UserManagementScreen
import com.example.crashcourse.ui.face.FaceListScreen // ✅ IMPORT WAJIB

// 🚀 VIEWMODELS & STATES
import com.example.crashcourse.viewmodel.AuthState

/**
 * 🗺️ Azura Tech Central NavGraph
 * Menghubungkan semua rute (String) ke Layar (Composable).
 */
@Composable
fun NavGraph(
    navController: NavHostController, 
    authState: AuthState
) {
    // Tentukan layar awal berdasarkan status login
    val startDest = when (authState) {
        is AuthState.Active -> Screen.Main.route
        is AuthState.StatusWaiting -> Screen.StatusWaiting.route
        else -> Screen.Login.route
    }

    

    NavHost(navController = navController, startDestination = startDest) {
        
        // ==========================================
        // 🔐 1. AUTHENTICATION ROUTES
        // ==========================================
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { 
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        
        composable(Screen.StatusWaiting.route) { 
            StatusWaitingScreen() 
        }

        // ==========================================
        // 🏠 2. ACTIVE SESSION ROUTES (Hanya jika Login)
        // ==========================================
        if (authState is AuthState.Active) {
            
            composable(Screen.Main.route) {
                MainScreen(
                    authState = authState,
                    onNavigateToCheckIn = { navController.navigate(Screen.CheckIn.route) },
                    onNavigateToAdmin = { navController.navigate(Screen.Admin.route) }
                )
            }

            composable(Screen.CheckIn.route) { 
                CheckInScreen(useBackCamera = true) 
            }
            
            composable(Screen.Admin.route) { 
                AdminDashboardScreen(navController = navController) 
            }
            
            composable(Screen.Options.route) { 
                OptionsManagementScreen(onNavigateBack = { navController.popBackStack() }) 
            }

            composable(Screen.MasterClass.route) { 
                MasterClassManagementScreen(onNavigateBack = { navController.popBackStack() }) 
            }

            composable(Screen.LiveMonitor.route) { 
                LiveMonitorScreen(onBack = { navController.popBackStack() }) 
            }

            // 🚀 FACE MANAGEMENT (Rute yang tadi Error)
            composable(Screen.FaceList.route) {
                FaceListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> 
                        navController.navigate(Screen.EditUser.createRoute(id)) 
                    }
                )
            }

            // --- 📝 REGISTRATION FLOW ---
            composable(Screen.RegistrationMenu.route) {
                RegistrationMenuScreen(
                    onNavigateToSingleAdd = { navController.navigate(Screen.Add.route) },
                    onNavigateToBulk = { navController.navigate(Screen.Bulk.route) },
                    onNavigateToGallery = { navController.navigate(Screen.SingleUpload.route) },
                    onBack = { navController.popBackStack() }
                )
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

            // --- 👥 USER MANAGEMENT ---
            composable(Screen.UserManagement.route) {
                UserManagementScreen(
                    onBack = { navController.popBackStack() },
                    onEditUser = { id -> navController.navigate(Screen.EditUser.createRoute(id)) }
                )
            }
            
            // --- ✏️ DYNAMIC EDIT SCREEN ---
            composable(
                route = Screen.EditUser.route,
                arguments = listOf(
                    navArgument("userId") { 
                        type = NavType.StringType 
                        nullable = false
                    }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                EditUserScreen(
                    studentId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onUpdateSuccess = { navController.popBackStack() }
                )
            }
        }
    }
}