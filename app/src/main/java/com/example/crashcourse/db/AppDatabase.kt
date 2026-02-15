package com.example.crashcourse.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 🏛️ Azura Tech Central Database (V.10.16 - Final Naming Alignment)
 * Menampung 10 Tabel Utama untuk Arsitektur 6-Pilar Multi-Tenant.
 * * Update log:
 * - Version 2 enabled for schema changes in UserEntity and MasterClassRoom.
 * - Aligned with 'current_user' table naming convention.
 */
@Database(
    entities = [
        UserEntity::class,       // Table: current_user
        FaceEntity::class,       // Table: students
        CheckInRecord::class,    // Table: attendance_records
        MasterClassRoom::class,  // Table: master_classes
        
        // 6-PILAR OPTION TABLES (Master Data)
        ClassOption::class,      
        SubClassOption::class,   
        GradeOption::class,      
        SubGradeOption::class,   
        ProgramOption::class,    
        RoleOption::class        
    ],
    version = 2, 
    exportSchema = false
)
@TypeConverters(Converters::class) 
abstract class AppDatabase : RoomDatabase() {

    // ==========================================
    // 1. CORE DAOs (The Bridges)
    // ==========================================
    abstract fun userDao(): UserDao
    abstract fun faceDao(): FaceDao
    abstract fun checkInRecordDao(): CheckInRecordDao
    abstract fun masterClassDao(): MasterClassDao
    
    // ==========================================
    // 2. 6-PILAR OPTION DAOs (Configuration Data)
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
         * Singleton pattern to prevent multiple database instances opening 
         * and causing data corruption.
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
                 * Selama tahap development, Room akan menghapus data lama dan 
                 * membuat ulang skema jika versi dinaikkan (V1 -> V2).
                 */
                .fallbackToDestructiveMigration() 
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}