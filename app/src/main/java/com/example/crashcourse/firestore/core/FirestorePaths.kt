package com.example.crashcourse.firestore.core

/**
 * 🛣️ FirestorePaths
 * Menyimpan nama-nama koleksi Firestore.
 * Diupdate untuk mendukung 'MASTER_CLASSES' sesuai kode Rombel & UserScope.
 */
object FirestorePaths {
    
    // --- Root Collections ---
    const val USERS = "users"
    const val SCHOOLS = "schools"

    // --- Sub Collections (Inside Schools) ---
    const val STUDENTS = "students"
    
    // 🔥 FIXED: Menggunakan nama MASTER_CLASSES agar sesuai dengan FirestoreRombel.kt
    const val MASTER_CLASSES = "classes"       
    
    // 🔥 ANTISIPASI: Kemungkinan besar kode Anda juga butuh ini
    const val MASTER_SUBJECTS = "subjects"
    
    const val ATTENDANCE = "attendance_records" // Sesuai dengan nama tabel Room Anda
    
    // --- Storage Paths (Optional) ---
    const val STORAGE_FACES = "face_embeddings"
}