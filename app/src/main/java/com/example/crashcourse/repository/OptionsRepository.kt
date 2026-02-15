package com.example.crashcourse.repository

import android.app.Application
import android.content.Context
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.options.FirestoreOptions
import com.example.crashcourse.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 🛠️ OptionsRepository (Optimized for Instant UI)
 * Mengelola data Master (Pilar 6) antara Room Database dan Firestore.
 */
class OptionsRepository(application: Application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = application.getSharedPreferences("azura_sync_prefs", Context.MODE_PRIVATE)

    // ==========================================
    // 1. 🌊 DATA FLOWS (REAL-TIME LOCAL)
    // ==========================================
    fun getGradeOptions(): Flow<List<GradeOption>> = db.gradeOptionDao().getAll()
    fun getProgramOptions(): Flow<List<ProgramOption>> = db.programOptionDao().getAll()
    fun getSubClassOptions(): Flow<List<SubClassOption>> = db.subClassOptionDao().getAll()
    fun getClassOptions(): Flow<List<ClassOption>> = db.classOptionDao().getAll()
    fun getSubGradeOptions(): Flow<List<SubGradeOption>> = db.subGradeOptionDao().getAll()
    fun getRoleOptions(): Flow<List<RoleOption>> = db.roleOptionDao().getAll()

    // ==========================================
    // 2. 📥 WRITE OPERATIONS (LOCAL FIRST)
    // ==========================================

    /**
     * 🚀 INSERT LOCALLY
     * Digunakan oleh ViewModel untuk memberikan feedback instan di UI.
     */
    suspend fun insertLocally(option: Any) = withContext(Dispatchers.IO) {
        when (option) {
            is ClassOption -> db.classOptionDao().insert(option)
            is SubClassOption -> db.subClassOptionDao().insert(option)
            is GradeOption -> db.gradeOptionDao().insert(option)
            is SubGradeOption -> db.subGradeOptionDao().insert(option)
            is ProgramOption -> db.programOptionDao().insert(option)
            is RoleOption -> db.roleOptionDao().insert(option)
        }
    }

    /**
     * 🗑️ DELETE LOCALLY
     */
    suspend fun deleteOptionLocally(option: Any) = withContext(Dispatchers.IO) {
        when (option) {
            is ClassOption -> db.classOptionDao().delete(option)
            is SubClassOption -> db.subClassOptionDao().delete(option)
            is GradeOption -> db.gradeOptionDao().delete(option)
            is SubGradeOption -> db.subGradeOptionDao().delete(option)
            is ProgramOption -> db.programOptionDao().delete(option)
            is RoleOption -> db.roleOptionDao().delete(option)
        }
    }

    // ==========================================
    // 3. ☁️ CLOUD SYNC OPERATIONS
    // ==========================================

    /**
     * 🚀 SAVE TO CLOUD
     * Mengirim data ke Firestore.
     */
    suspend fun saveOptionToCloud(collectionName: String, id: Int, data: Map<String, Any>) = withContext(Dispatchers.IO) {
        FirestoreOptions.saveOption(collectionName, id, data)
    }

    /**
     * 🔥 INCREMENTAL SYNC
     * Menarik data terbaru dari Firestore sejak sinkronisasi terakhir.
     */
    suspend fun syncAllFromCloud() = withContext(Dispatchers.IO) {
        val lastSync = prefs.getLong("last_options_sync", 0L)
        val types = listOf("Class", "SubClass", "Grade", "SubGrade", "Program", "Role")
        
        types.forEach { type ->
            val collectionName = getCollectionName(type)
            val updates = FirestoreOptions.fetchOptionsUpdates(collectionName, lastSync)
            if (updates.isNotEmpty()) {
                processAndSave(type, updates)
            }
        }
        prefs.edit().putLong("last_options_sync", System.currentTimeMillis()).apply()
    }

    /**
     * 📥 PROCESS & SAVE (Mapping logic)
     */
    suspend fun processAndSave(type: String, dataList: List<Map<String, Any>>) = withContext(Dispatchers.IO) {
        when (type) {
            "Class" -> db.classOptionDao().insertAll(dataList.map {
                ClassOption(it["id"].toString().toIntOrNull() ?: 0, it["name"].toString(), it["order"].toString().toIntOrNull() ?: 0)
            })
            "SubClass" -> db.subClassOptionDao().insertAll(dataList.map {
                SubClassOption(it["id"].toString().toIntOrNull() ?: 0, it["name"].toString(), it["parentId"].toString().toIntOrNull() ?: 0, it["order"].toString().toIntOrNull() ?: 0)
            })
            "Grade" -> db.gradeOptionDao().insertAll(dataList.map {
                GradeOption(it["id"].toString().toIntOrNull() ?: 0, it["name"].toString(), it["order"].toString().toIntOrNull() ?: 0)
            })
            "SubGrade" -> db.subGradeOptionDao().insertAll(dataList.map {
                SubGradeOption(it["id"].toString().toIntOrNull() ?: 0, it["name"].toString(), it["parentId"].toString().toIntOrNull() ?: 0, it["order"].toString().toIntOrNull() ?: 0)
            })
            "Program" -> db.programOptionDao().insertAll(dataList.map {
                ProgramOption(it["id"].toString().toIntOrNull() ?: 0, it["name"].toString(), it["order"].toString().toIntOrNull() ?: 0)
            })
            "Role" -> db.roleOptionDao().insertAll(dataList.map {
                RoleOption(it["id"].toString().toIntOrNull() ?: 0, it["name"].toString(), it["order"].toString().toIntOrNull() ?: 0)
            })
        }
    }

    // ==========================================
    // 4. 🛠️ HELPERS
    // ==========================================

    fun getCollectionName(type: String): String = when(type) {
        "Class" -> Constants.COLL_OPT_CLASSES
        "SubClass" -> Constants.COLL_OPT_SUBCLASSES
        "Grade" -> Constants.COLL_OPT_GRADES
        "SubGrade" -> Constants.COLL_OPT_SUBGRADES
        "Program" -> Constants.COLL_OPT_PROGRAMS
        "Role" -> Constants.COLL_OPT_ROLES
        else -> "others"
    }
}