package com.example.crashcourse.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 🏛️ Azura Tech Central Database
 * Menampung 10 Tabel Utama untuk Arsitektur 6-Pilar.
 * * Database ini menggunakan pola Singleton dan Destructive Migration 
 * untuk memudahkan sinkronisasi data dari Firestore ke lokal.
 */
@Database(
    entities = [
        UserEntity::class,       // Sesi Login User
        FaceEntity::class,       // Data Biometrik & Profil
        CheckInRecord::class,    // Log Absensi
        MasterClassRoom::class,  // Unit Rakitan (Rombel)
        ClassOption::class,      // Pillar 1: Departemen
        SubClassOption::class,   // Pillar 2: Sub-Unit
        GradeOption::class,      // Pillar 3: Jenjang
        SubGradeOption::class,   // Pillar 4: Periode
        ProgramOption::class,    // Pillar 5: Jurusan
        RoleOption::class        // Pillar 6: Jabatan
    ],
    version = 1, // Reset ke 1 untuk Arsitektur Baru
    exportSchema = false
)
@TypeConverters(Converters::class) // 🚀 Wajib: Mengolah LocalDateTime, FloatArray, & List<String>
abstract class AppDatabase : RoomDatabase() {

    // ==========================================
    // 1. CORE DAOs
    // ==========================================
    abstract fun userDao(): UserDao
    abstract fun faceDao(): FaceDao
    abstract fun checkInRecordDao(): CheckInRecordDao
    abstract fun masterClassDao(): MasterClassDao
    
    // ==========================================
    // 2. 6-PILAR OPTION DAOs (Master Data)
    // ==========================================
    abstract fun classOptionDao(): ClassOptionDao
    abstract fun subClassOptionDao(): SubClassOptionDao
    abstract fun gradeOptionDao(): GradeOptionDao
    abstract fun subGradeOptionDao(): SubGradeOptionDao
    abstract fun programOptionDao(): ProgramOptionDao
    abstract fun roleOptionDao(): RoleOptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Singleton Instance untuk mencegah kebocoran memory 
         * dan konflik akses database antar Thread.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "azura_attendance_db"
                )
                /**
                 * ⚠️ Destructive Migration: 
                 * Jika skema tabel berubah, database lama akan dihapus dan dibuat baru.
                 * Sangat efektif untuk fase development agar tidak pusing migrasi manual.
                 */
                .fallbackToDestructiveMigration() 
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Logika Pre-populate data awal bisa diletakkan di sini
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}