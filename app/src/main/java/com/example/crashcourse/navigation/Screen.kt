package com.example.crashcourse.navigation

sealed class Screen(val route: String) {
    // --- 🔐 AUTH ---
    object Login : Screen("login")
    object Register : Screen("register")
    object StatusWaiting : Screen("status_waiting")

    // --- 🏠 MAIN ---
    object Main : Screen("main")
    object CheckIn : Screen("check_in")
    object History : Screen("history")
    object Settings : Screen("settings")

    // --- 🏛️ ADMIN ---
    object Admin : Screen("admin")
    object LiveMonitor : Screen("live_monitor")
    object MasterClass : Screen("master_class")
    object Options : Screen("options")
    object UserManagement : Screen("user_management")

    // --- 📝 REGISTRATION ---
    object RegistrationMenu : Screen("registration_menu")
    object Add : Screen("add_user")
    object Bulk : Screen("bulk_registration")
    object SingleUpload : Screen("single_upload")

    // --- 🛠️ DYNAMIC ROUTES ---
    object EditUser : Screen("edit_user/{userId}") {
        fun createRoute(userId: String) = "edit_user/$userId"
    }
    object FaceList : Screen("face_list") // 👈 TAMBAHKAN INI
}