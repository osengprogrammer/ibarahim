package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.rombel.FirestoreRombel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 🏗️ Master Class ViewModel (V.10.18 - Final Refactored)
 * Mengelola logic untuk Unit Rakitan (Rombel) berbasis 6-Pilar.
 * Kompatibel dengan skema Multi-Tenant schoolId.
 */
class MasterClassViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val masterClassDao = db.masterClassDao()
    private val userDao = db.userDao()

    // Mengamati ID Sekolah yang sedang aktif
    private val _schoolId = MutableStateFlow<String?>(null)

    // 📊 REAKTIF: List MasterClass yang sudah di-join dengan nama kategori asli
    @OptIn(ExperimentalCoroutinesApi::class)
    val masterClassesWithNames: StateFlow<List<MasterClassWithNames>> = _schoolId
        .filterNotNull()
        .flatMapLatest { sid ->
            // Menggunakan method baru di DAO yang sudah kita perbaiki
            masterClassDao.getAllMasterClassesWithNamesFlow(sid)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Observer sesi user secara otomatis
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getCurrentUserFlow().collect { user ->
                val sid = user?.schoolId
                _schoolId.value = sid
                
                if (sid != null) {
                    // Jalankan sinkronisasi otomatis saat sekolah terdeteksi
                    syncFromCloud(sid)
                }
            }
        }
    }

    /**
     * 🚀 Simpan Unit Rakitan Baru (Local + Cloud)
     */
    fun addMasterClassFull(
        name: String,
        cId: Int, scId: Int, gId: Int, sgId: Int, pId: Int, rId: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sid = _schoolId.value ?: return@launch
            
            // 🔥 FIXED: Constructor MasterClassRoom sekarang pakai 'schoolId'
            val newClass = MasterClassRoom(
                classId = 0, // Akan diganti oleh generated ID atau Cloud ID
                schoolId = sid, 
                className = name,
                gradeId = gId,
                classOptionId = cId,
                programId = pId,
                subClassId = scId,
                subGradeId = sgId,
                roleId = rId
            )

            // 1. Simpan Lokal untuk mendapatkan ID
            val generatedId = masterClassDao.insertClass(newClass)
            val classWithId = newClass.copy(classId = generatedId.toInt())

            try {
                // 2. Kirim ke Firestore (Gunakan data yang sudah ada ID-nya)
                FirestoreRombel.saveMasterClass(classWithId)
                Log.d("MasterClassVM", "✅ Unit Synced to Cloud: $name")
            } catch (e: Exception) {
                Log.e("MasterClassVM", "❌ Cloud Save Failed", e)
            }
        }
    }

    /**
     * 🗑️ Delete Class (Local + Cloud)
     */
    fun deleteClass(item: MasterClassWithNames) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 🔥 FIXED: Parameter sesuai signature baru FirestoreRombel
                FirestoreRombel.deleteMasterClass(
                    schoolId = item.schoolId, 
                    classId = item.classId
                )
                
                // Hapus dari Local Room (Gunakan Entity)
                val entityToDelete = MasterClassRoom(
                    classId = item.classId,
                    schoolId = item.schoolId,
                    className = item.className ?: "",
                    gradeId = 0, classOptionId = 0, programId = 0,
                    subClassId = 0, subGradeId = 0, roleId = 0
                )
                masterClassDao.deleteClass(entityToDelete)
                
                Log.d("MasterClassVM", "🗑️ Deleted: ${item.className}")
            } catch (e: Exception) {
                Log.e("MasterClassVM", "❌ Delete failed", e)
            }
        }
    }

    /**
     * 🔄 Sinkronisasi Data dari Cloud
     */
    fun syncFromCloud(schoolId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cloudData = FirestoreRombel.fetchMasterClasses(schoolId)
                if (cloudData.isNotEmpty()) {
                    masterClassDao.insertAll(cloudData)
                    Log.d("MasterClassVM", "📥 ${cloudData.size} units synced from cloud.")
                }
            } catch (e: Exception) {
                Log.e("MasterClassVM", "❌ Sync failed", e)
            }
        }
    }
}