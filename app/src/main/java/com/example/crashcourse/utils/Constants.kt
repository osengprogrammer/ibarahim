package com.example.crashcourse.utils

object Constants {

    // ==========================================
    // 📂 CORE COLLECTIONS
    // ==========================================
    const val COLL_USERS = "users"
    const val COLL_STUDENTS = "students"
    const val COLL_ATTENDANCE = "attendance_records"
    const val COLL_MASTER_CLASSES = "master_classes"

    // ==========================================
    // ⚙️ MASTER DATA OPTIONS (6-PILAR)
    // ==========================================
    const val COLL_OPT_CLASSES = "options_classes"
    const val COLL_OPT_SUBCLASSES = "options_sub_classes"
    const val COLL_OPT_GRADES = "options_grades"
    const val COLL_OPT_SUBGRADES = "options_sub_grades"
    const val COLL_OPT_PROGRAMS = "options_programs"
    const val COLL_OPT_ROLES = "options_roles"

    // ==========================================
    // 👤 ROLES
    // ==========================================
    const val ROLE_ADMIN = "ADMIN"
    const val ROLE_USER = "USER"

    // ==========================================
    // 🧱 6-PILAR FIELD KEYS
    // ==========================================
    const val PILLAR_CLASS = "class"
    const val PILLAR_SUB_CLASS = "subClass"
    const val PILLAR_GRADE = "grade"
    const val PILLAR_SUB_GRADE = "subGrade"
    const val PILLAR_PROGRAM = "program"

    // ==========================================
    // 🔑 GENERIC FIELD NAMES
    // ==========================================
    const val KEY_ID = "id"
    const val KEY_NAME = "name"
    const val KEY_SEKOLAH_ID = "sekolahId"

    // ==========================================
    // 🕒 TIMESTAMP FIELDS
    // ==========================================
    const val FIELD_TIMESTAMP = "timestamp"      // Firestore Timestamp
    const val FIELD_DATE = "date"                // yyyy-MM-dd (String)
    const val KEY_TIMESTAMP = "timestamp"        // unified meaning

    // ==========================================
    // 🧬 STUDENT / FACE FIELDS
    // ==========================================
    const val FIELD_STUDENT_ID = "studentId"
    const val FIELD_EMBEDDING = "embedding"
    const val FIELD_PHOTO_URL = "photoUrl"
    const val FIELD_PHOTO_PATH = "photoPath"
    const val FIELD_FIRESTORE_ID = "firestoreId"
    const val FIELD_ROLE = "role"                // ✅ ADDED

    // ==========================================
    // 📌 ATTENDANCE FIELDS
    // ==========================================
    const val FIELD_STATUS = "status"
    const val FIELD_VERIFIED = "verified"

    // ==========================================
    // 📊 STATUS VALUES
    // ==========================================
    const val STATUS_PRESENT = "PRESENT"
    const val STATUS_ABSENT = "ABSENT"
}
