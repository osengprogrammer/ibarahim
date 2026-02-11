package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.rombel.FirestoreRombel // ✅ NEW IMPORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 🏗️ Master Class ViewModel
 * Mengelola logic untuk Unit Rakitan (Rombel/Unit Kerja) berbasis 6-Pilar.
 * Syncs: Room (Local) <-> Firestore (Cloud) via FirestoreRombel
 */
class MasterClassViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val masterClassDao = db.masterClassDao()
    private val userDao = db.userDao()

    private val _sekolahId = MutableStateFlow<String?>(null)

    // 📊 REAKTIF: Data otomatis terupdate saat sekolahId tersedia atau data di DB berubah
    @OptIn(ExperimentalCoroutinesApi::class)
    val masterClassesWithNames: StateFlow<List<MasterClassWithNames>> = _sekolahId
        .filterNotNull()
        .flatMapLatest { sid ->
            masterClassDao.getAllMasterClassesWithNamesFlow(sid)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // 1. Ambil sekolahId dari user
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getCurrentUserFlow().collect { user ->
                _sekolahId.value = user?.sekolahId
                // 2. Trigger Sync saat user terdeteksi
                if (user?.sekolahId != null) {
                    syncFromCloud(user.sekolahId!!)
                }
            }
        }
    }

    /**
     * 🚀 Simpan Unit Rakitan Baru (Local + Cloud)
     */
    fun addMasterClassFull(
        name: String,
        cId: Int,
        scId: Int,
        gId: Int,
        sgId: Int,
        pId: Int,
        rId: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sid = _sekolahId.value ?: return@launch
            
            // A. Siapkan Data
            val newClass = MasterClassRoom(
                classId = 0, // 0 = Auto Generate di Room
                sekolahId = sid,
                className = name,
                gradeId = gId,
                classOptionId = cId,
                programId = pId,
                subClassId = scId,
                subGradeId = sgId,
                roleId = rId
            )

            // B. Simpan ke Local (Room) & Ambil ID yang digenerate
            val generatedId = masterClassDao.insertClass(newClass)

            // C. Update Object dengan ID baru untuk Cloud
            val classWithId = newClass.copy(classId = generatedId.toInt())

            // D. Simpan ke Firestore via FirestoreRombel
            try {
                FirestoreRombel.saveMasterClass(classWithId)
                Log.d("MasterClassVM", "✅ Saved to Cloud: $name (ID: $generatedId)")
            } catch (e: Exception) {
                Log.e("MasterClassVM", "❌ Failed to save to cloud", e)
            }
        }
    }

    /**
     * 🗑️ Delete Class (Local + Cloud)
     */
    fun deleteClass(item: MasterClassWithNames) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // A. Hapus dari Cloud dulu (Optimistic / Parallel)
                FirestoreRombel.deleteMasterClass(item.classId)
                
                // B. Hapus dari Local Room
                val entityToDelete = MasterClassRoom(
                    classId = item.classId,
                    sekolahId = item.sekolahId,
                    className = item.className,
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
     * 🔄 Sync Manual / Auto
     */
    fun syncFromCloud(sekolahId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cloudData = FirestoreRombel.fetchMasterClasses(sekolahId)
                if (cloudData.isNotEmpty()) {
                    masterClassDao.insertAll(cloudData)
                    Log.d("MasterClassVM", "🔄 Synced ${cloudData.size} classes")
                }
            } catch (e: Exception) {
                Log.e("MasterClassVM", "❌ Sync failed", e)
            }
        }
    }
}