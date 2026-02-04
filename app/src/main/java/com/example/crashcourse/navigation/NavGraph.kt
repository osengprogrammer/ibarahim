package com.example.crashcourse.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
// Import UI secara eksplisit agar tidak ada "Unresolved Reference"
import com.example.crashcourse.ui.*
import com.example.crashcourse.viewmodel.AuthState
import com.example.crashcourse.viewmodel.AuthViewModel
// Pastikan baris ini ada dan benar alamatnya
import com.example.crashcourse.ui.checkin.CheckInRecordScreen

sealed class Screen(val route: String) {
    object CheckIn           : Screen("check_in")
    object RegistrationMenu  : Screen("registration_menu")
    object AddUser           : Screen("add_user")
    object BulkRegister      : Screen("bulk_register")
    object FaceManualCapture : Screen("face_manual_capture")
    object ManualRegistration: Screen("manual_registration")
    object Manage            : Screen("manage_faces")
    object EditUser : Screen("edit_user/{studentId}") {
        fun createRoute(studentId: String) = "edit_user/$studentId"
    }
    object Options           : Screen("options_management")
    object OptionForm : Screen("option_form/{type}") {
        fun createRoute(type: String) = "option_form/$type"
    }
    object CheckInRecord     : Screen("checkin_record")
    object Debug             : Screen("debug")
    object Profile           : Screen("profile")
    
    // 🔥 Admin & Monitoring Routes
    object AdminDashboard    : Screen("admin_dashboard")
    object LiveMonitor       : Screen("live_monitor")
    object UserManagement    : Screen("user_management")
    object EditUserScope : Screen("edit_user_scope/{userId}") {
        fun createRoute(userId: String) = "edit_user_scope/$userId"
    }
}

/**
 * Extension function untuk menangani navigasi management & profile.
 * Terintegrasi dengan sistem Role (Admin, Supervisor, User).
 */
fun NavGraphBuilder.addAppManagementGraph(
    navController: NavController,
    authState: AuthState.Active,
    authViewModel: AuthViewModel
) {
    val isAdmin = authState.role == "ADMIN"
    val isSupervisor = authState.role == "SUPERVISOR" || isAdmin

    // 1. Profile Screen
    composable(Screen.Profile.route) {
        ProfileScreen(
            authState = authState,
            onLogout = { authViewModel.logout() },
            onInviteStaff = { email, pass ->
                authViewModel.inviteStaff(email, pass)
            }
        )
    }

    // 2. User Management (Daftar Guru/Staff)
    composable(Screen.UserManagement.route) {
        if (isAdmin) {
            UserManagementScreen(
                onBack = { navController.popBackStack() },
                onEditUser = { userId -> 
                    navController.navigate(Screen.EditUserScope.createRoute(userId)) 
                }
            )
        } else {
            AccessDeniedScreen("Hanya Admin yang bisa mengelola staff.")
        }
    }

    // 3. Edit User Scope (Assign Kelas/Role)
    composable(
        route = Screen.EditUserScope.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { backStackEntry ->
        if (isAdmin) {
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            EditUserScopeScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        } else {
            AccessDeniedScreen()
        }
    }

    // 4. Options Management (Master Data)
    composable(Screen.Options.route) {
        if (isAdmin) {
            OptionsManagementScreen() 
        } else {
            AccessDeniedScreen("Hanya Admin yang bisa mengelola data master.")
        }
    }

    // 5. Dynamic Option Forms
    composable(
        route = Screen.OptionForm.route,
        arguments = listOf(navArgument("type") { type = NavType.StringType })
    ) { backStackEntry ->
        if (isAdmin) {
            val type = backStackEntry.arguments?.getString("type") ?: ""
            OptionFormScreen(
                type = type,
                onNavigateBack = { navController.popBackStack() }
            )
        } else {
            AccessDeniedScreen()
        }
    }

    // 6. Check-In History Records
    composable(Screen.CheckInRecord.route) {
        if (isSupervisor) {
            CheckInRecordScreen(
                authState = authState
            )
        } else {
            AccessDeniedScreen("Riwayat hanya bisa diakses oleh Supervisor.")
        }
    }

    // 7. Face Management (Manage Faces)
    composable(Screen.Manage.route) {
        // ✅ TETAP MENGGUNAKAN FaceListScreen DENGAN authState
        FaceListScreen(
            authState = authState,
            onEditUser = { student ->
                navController.navigate(Screen.EditUser.createRoute(student.studentId))
            }
        )
    }

    // Di dalam NavGraph.kt -> fun addAppManagementGraph
// 🔥 TAMBAHKAN RUTE INI:
composable(Screen.LiveMonitor.route) {
    LiveAttendanceScreen(
        onBack = { navController.popBackStack() }
    )
}
}

@Composable
fun OptionsManagementButton(navController: NavController) {
    Button(
        onClick = { navController.navigate(Screen.Options.route) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Manage Settings & Options")
    }
}