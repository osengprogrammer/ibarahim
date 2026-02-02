package com.example.crashcourse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.crashcourse.navigation.Screen
import com.example.crashcourse.ui.add.AddUserScreen
import com.example.crashcourse.ui.add.BulkRegistrationScreen
import com.example.crashcourse.ui.edit.EditUserScreen
import com.example.crashcourse.viewmodel.FaceViewModel
import com.example.crashcourse.viewmodel.LicenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(licenseViewModel: LicenseViewModel) {
    val navController = rememberNavController()
    var useBackCamera by remember { mutableStateOf(false) }

    // Create a shared ViewModel instance for all screens
    val sharedFaceViewModel: FaceViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomNav(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.RegistrationMenu.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // 1. Check In Screen
            composable(Screen.CheckIn.route) {
                CheckInScreen(useBackCamera = useBackCamera)
            }

            // 2. Registration Menu
            composable(Screen.RegistrationMenu.route) {
                RegistrationMenuScreen(
                    onNavigateToBulkRegister = {
                        navController.navigate(Screen.BulkRegister.route)
                    },
                    onNavigateToAddUser = {
                        navController.navigate(Screen.AddUser.route)
                    }
                )
            }

            // 3. Add User Screen
            composable(Screen.AddUser.route) {
                AddUserScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onUserAdded = { /* Refresh or update list if needed */ },
                    viewModel = sharedFaceViewModel
                )
            }

            // 4. Bulk Register Screen
            composable(Screen.BulkRegister.route) {
                BulkRegistrationScreen(
                    faceViewModel = sharedFaceViewModel
                )
            }

            // 5. Manage / Face List Screen
            composable(Screen.Manage.route) {
                FaceListScreen(
                    viewModel = sharedFaceViewModel,
                    onEditUser = { user ->
                        navController.navigate(Screen.EditUser.createRoute(user.studentId))
                    }
                )
            }

            // 6. Edit User Screen
            composable(Screen.EditUser.route) { backStackEntry ->
                val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                EditUserScreen(
                    studentId = studentId,
                    useBackCamera = useBackCamera,
                    onNavigateBack = { navController.popBackStack() },
                    onUserUpdated = { /* Refresh or update list if needed */ },
                    faceViewModel = sharedFaceViewModel
                )
            }
            
            // 7. OPTIONS / SETTINGS SCREEN (UPDATED)
            composable(Screen.Options.route) {
                // ✅ CHANGED: Calls SettingsScreen instead of OptionsManagementScreen
                SettingsScreen(
                    licenseViewModel = licenseViewModel, 
                    onNavigateToForm = { type ->
                        navController.navigate(Screen.OptionForm.createRoute(type))
                    }
                )
            }
            
            // 8. Option Form (Data Master)
            composable(
                route = Screen.OptionForm.route,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: return@composable
                OptionFormScreen(
                    type = type,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 9. Records Screen
            composable(Screen.CheckInRecord.route) {
                CheckInRecordScreen()
            }

            // 10. Debug Screen
            composable(Screen.Debug.route) {
                DebugScreen(viewModel = sharedFaceViewModel)
            }
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController) {
    val items = listOf(
        Screen.CheckIn  to "Check In",
        Screen.RegistrationMenu to "Register",
        Screen.Manage   to "Manage",
        Screen.CheckInRecord to "Records",
        Screen.Options  to "Options"
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { (screen, label) ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.CheckIn  -> Icons.Default.Person
                            Screen.RegistrationMenu -> Icons.Default.PersonAdd
                            Screen.Manage   -> Icons.AutoMirrored.Filled.List
                            Screen.CheckInRecord -> Icons.Default.History
                            Screen.Options  -> Icons.Default.Settings
                            Screen.Debug -> Icons.Default.BugReport
                            else -> Icons.Default.Person
                        },
                        contentDescription = label
                    )
                },
                label = { Text(label) }
            )
        }
    }
}