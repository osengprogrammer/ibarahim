package com.example.crashcourse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.crashcourse.ui.*
import com.example.crashcourse.ui.checkin.CheckInRecordScreen
import com.example.crashcourse.ui.edit.EditUserScreen
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object CheckIn           : Screen("check_in")
    object RegistrationMenu  : Screen("registration_menu")
    object AddUser           : Screen("add_user")
    object BulkRegister      : Screen("bulk_register")
    object Manage            : Screen("manage_faces")
    object SingleUpload      : Screen("single_upload")
    
    object EditUser : Screen("edit_user/{studentId}") {
        fun createRoute(studentId: String) = "edit_user/$studentId"
    }

    object Options           : Screen("options_management")
    object CheckInRecord     : Screen("checkin_record")
    object Settings          : Screen("settings")
    object AdminDashboard    : Screen("admin_dashboard")
    object UserManagement    : Screen("user_management")
    
    // 🚀 INI YANG HILANG DAN BIKIN ERROR:
    object LiveMonitor       : Screen("live_monitor")
    
    // Tambahan untuk Edit Scope (User Management)
    object EditUserScope : Screen("edit_user_scope/{userId}") {
        fun createRoute(userId: String) = "edit_user_scope/$userId"
    }
}

fun NavGraphBuilder.addAppManagementGraph(
    navController: NavController,
    authState: AuthState.Active,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val isAdmin = authState.role == "ADMIN"

    // 1. Settings Screen
    composable(Screen.Settings.route) {
        SettingsScreen(
            onLogout = onLogout,
            onNavigateToMasterData = { navController.navigate(Screen.Options.route) },
            onNavigateToUserMan = { navController.navigate(Screen.UserManagement.route) }
        )
    }

    // 2. User Management
    composable(Screen.UserManagement.route) {
        if (isAdmin) {
            UserManagementScreen(
                onBack = { navController.popBackStack() },
                onEditUser = { userId -> 
                     navController.navigate(Screen.EditUserScope.createRoute(userId))
                }
            )
        } else {
            AccessDeniedScreen("Admin Only")
        }
    }
    
    // 3. Edit User Scope (Assign Kelas)
    composable(
        route = Screen.EditUserScope.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { backStackEntry ->
        if (isAdmin) {
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            // Pastikan EditUserScopeScreen sudah ada, atau gunakan placeholder sementara
            EditUserScopeScreen(userId = userId, onBack = { navController.popBackStack() })
        }
    }

    // 4. Master Data
    composable(Screen.Options.route) {
        if (isAdmin) {
            OptionsManagementScreen() 
        } else {
            AccessDeniedScreen("Admin Only")
        }
    }

    // 5. Riwayat Absensi
    composable(Screen.CheckInRecord.route) {
        CheckInRecordScreen(authState = authState)
    }

    // 6. Face Management
    composable(Screen.Manage.route) {
        FaceListScreen(
            authState = authState,
            onEditUser = { student ->
                navController.navigate(Screen.EditUser.createRoute(student.studentId))
            }
        )
    }

    // 7. Edit Student Profile
    composable(
        route = Screen.EditUser.route,
        arguments = listOf(navArgument("studentId") { type = NavType.StringType })
    ) { backStackEntry ->
        val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
        EditUserScreen(
            studentId = studentId,
            onNavigateBack = { navController.popBackStack() },
            onUserUpdated = { navController.popBackStack() }
        )
    }
    
    // 8. Live Monitor Route
    composable(Screen.LiveMonitor.route) {
        if (isAdmin) {
            LiveAttendanceScreen(onBack = { navController.popBackStack() })
        } else {
            AccessDeniedScreen("Admin Only")
        }
    }
}