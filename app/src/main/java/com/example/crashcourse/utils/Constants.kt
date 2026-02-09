package com.example.crashcourse.utils

/**
 * 🛡️ CENTRALIZED CONSTANTS
 * Satu tempat untuk mengatur semua nama koleksi dan konfigurasi statis.
 * Jika nanti nama database berubah, cukup ubah di sini saja.
 */
object Constants {
    // --- FIRESTORE COLLECTIONS ---
    const val COLL_USERS = "users"
    const val COLL_STUDENTS = "students"
    const val COLL_ATTENDANCE = "attendance" // Standardisasi nama koleksi absensi
    
    // --- MASTER DATA COLLECTIONS ---
    const val COLL_OPT_CLASSES = "options_classes"
    const val COLL_OPT_SUBCLASSES = "options_subclasses"
    const val COLL_OPT_GRADES = "options_grades"
    const val COLL_OPT_SUBGRADES = "options_subgrades"
    const val COLL_OPT_PROGRAMS = "options_programs"
    const val COLL_OPT_ROLES = "options_roles"
    const val COLL_OPT_OTHERS = "options_others"

    // --- FIELD NAMES (Opsional, untuk mencegah typo field kritis) ---
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_LAST_UPDATED = "last_updated"
    const val FIELD_CLASS_NAME = "className"
    const val FIELD_STUDENT_ID = "studentId"

    // TAMBAHKAN INI:
    const val ROLE_ADMIN = "ADMIN"
    const val ROLE_USER = "USER"
    const val STATUS_PRESENT = "PRESENT"
}