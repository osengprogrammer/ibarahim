package com.example.crashcourse.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * 👤 UserEntity (V.10.18 - Unified Identity Edition)
 * Mewakili sesi user aktif yang tersimpan di Room Database.
 * Menggunakan tabel 'current_user' untuk memastikan hanya ada satu sesi aktif.
 * * NOTE: Kita menghapus 'schoolId' dan menggantinya dengan 'schoolId' 
 * agar sinkron dengan identitas permanen di Firestore dan data Siswa.
 */
@Entity(tableName = "current_user")
@TypeConverters(Converters::class) // Wajib untuk menangani List<String>
data class UserEntity(
    @PrimaryKey
    val uid: String, // Firebase UID sebagai kunci utama
    
    val name: String,
    val role: String,

    // --- 🏫 IDENTITAS SEKOLAH ---
    /**
     * schoolId: Satu-satunya sumber kebenaran.
     * Tidak perlu 'schoolId' karena bagi Guru/Admin, ID ini adalah identitas tetap.
     * Digunakan langsung untuk men-stempel data Siswa saat registrasi.
     */
    val schoolId: String, 

    // --- ✅ STATUS & SECURITY ---
    /**
     * Menggunakan Boolean agar logika 'isActive' tidak mudah stuck karena typo String.
     */
    val isActive: Boolean = false,

    /**
     * Masa aktif lisensi sekolah.
     */
    val expiryMillis: Long = 0L,

    // --- 📚 DAFTAR AKSES (SOFT CONTEXT) ---
    /**
     * Daftar kelas yang boleh diakses/diajar oleh user ini.
     * Nilai activeClassName/Subject sebaiknya dikelola di ViewModel (State),
     * bukan di Database, agar database tetap bersih.
     */
    val assignedClasses: List<String> = emptyList(),
    val assignedSubjects: List<String> = emptyList(),

    val lastSync: Long = System.currentTimeMillis()
)