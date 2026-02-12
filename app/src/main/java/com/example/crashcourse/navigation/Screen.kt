package com.example.crashcourse.navigation

/**
 * 🗺️ Azura Tech Route Definition
 * Single Source of Truth untuk semua alamat layar di aplikasi.
 */
sealed class Screen(val route: String) {
    // --- 🔐 AUTH ---
    object Login : Screen("login")
    object Register : Screen("register")
    object StatusWaiting : Screen("status_waiting")

    // --- 🏠 MAIN & USER ---
    object Main : Screen("main")
    object CheckIn : Screen("check_in")
    object History : Screen("history")
    object Profile : Screen("profile") // 🚀 TAMBAHKAN INI

    // --- 🏛️ ADMIN DASHBOARD ---
    object Admin : Screen("admin")
    object LiveMonitor : Screen("live_monitor")
    object MasterClass : Screen("master_class")
    object Options : Screen("options")
    object UserManagement : Screen("user_management") // Daftar Email Staff

    // --- 📝 REGISTRATION (STUDENT) ---
    object RegistrationMenu : Screen("registration_menu")
    object Add : Screen("add_user")
    object Bulk : Screen("bulk_registration")
    object SingleUpload : Screen("single_upload")
    object FaceList : Screen("face_list") // Database Wajah

    // --- 🛠️ DYNAMIC ROUTES (With Parameters) ---
    
    // Edit Data Murid/Siswa
    object EditUser : Screen("edit_user/{userId}") {
        fun createRoute(userId: String) = "edit_user/$userId"
    }

    // Edit Pagar Akses/Otoritas Guru 🚀 TAMBAHKAN INI
    object EditUserScope : Screen("edit_user_scope/{userId}") {
        fun createRoute(userId: String) = "edit_user_scope/$userId"
    }
}