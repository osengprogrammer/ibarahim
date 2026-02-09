package com.example.crashcourse.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.crashcourse.utils.Converters

@Database(
    entities = [
        ClassOption::class,
        SubClassOption::class,
        GradeOption::class,
        SubGradeOption::class,
        ProgramOption::class,
        RoleOption::class,
        FaceEntity::class,
        CheckInRecord::class
    ],
    version = 7, // ✅ Versi Terbaru
    exportSchema = false
)
@TypeConverters(Converters::class) // ✅ Memanggil file Converters.kt di atas
abstract class AppDatabase : RoomDatabase() {
    abstract fun faceDao(): FaceDao
    abstract fun checkInRecordDao(): CheckInRecordDao
    
    // DAO Options
    abstract fun classOptionDao(): ClassOptionDao
    abstract fun subClassOptionDao(): SubClassOptionDao
    abstract fun gradeOptionDao(): GradeOptionDao
    abstract fun subGradeOptionDao(): SubGradeOptionDao
    abstract fun programOptionDao(): ProgramOptionDao
    abstract fun roleOptionDao(): RoleOptionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "face_db"
                )
                .fallbackToDestructiveMigration() // Reset DB otomatis kalau ada perubahan
                .build().also { INSTANCE = it }
            }
    }
}