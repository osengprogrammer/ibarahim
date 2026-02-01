package com.example.crashcourse.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.crashcourse.ui.OptionsManagementScreen
import com.example.crashcourse.ui.OptionFormScreen
import com.example.crashcourse.ui.CheckInRecordScreen

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
    // Consistent naming for the record history
    object CheckInRecord     : Screen("checkin_record")
    object Debug             : Screen("debug")
}

/**
 * Extension function to add specialized management screens to the navigation graph.
 * This keeps your NavHost in MainScreen clean.
 */
fun NavGraphBuilder.addAppManagementGraph(navController: NavController) {
    // Options Management
    composable(Screen.Options.route) {
        OptionsManagementScreen(
            onNavigateToForm = { type ->
                navController.navigate(Screen.OptionForm.createRoute(type))
            }
        )
    }

    // Dynamic Option Forms (Class, Grade, Role, etc.)
    composable(Screen.OptionForm.route) { backStackEntry ->
        val type = backStackEntry.arguments?.getString("type") ?: ""
        OptionFormScreen(
            type = type,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Check-In History Records
    composable(Screen.CheckInRecord.route) {
        CheckInRecordScreen()
    }
}

/**
 * Helper button to navigate to options from other screens
 */
@Composable
fun OptionsManagementButton(navController: NavController) {
    Button(
        onClick = { navController.navigate(Screen.Options.route) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Manage Settings & Options")
    }
}