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
 * 🏗️ Master Class ViewModel
 * Mengelola logic untuk Unit Rakitan (Rombel) berbasis 6-Pilar.
 * Fixed: Updated delete logic to match FirestoreRombel's composite ID.
 */
class MasterClassViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val masterClassDao = db.masterClassDao()
    private val userDao = db.userDao()

    private val _sekolahId = MutableStateFlow<String?>(null)

    // 📊 REAKTIF: List MasterClass yang sudah di-join dengan nama kategori
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
        viewModelScope.launch(Dispatchers.IO) {
            userDao.getCurrentUserFlow().collect { user ->
                _sekolahId.value = user?.sekolahId
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
        cId: Int, scId: Int, gId: Int, sgId: Int, pId: Int, rId: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sid = _sekolahId.value ?: return@launch
            
            val newClass = MasterClassRoom(
                classId = 0, 
                sekolahId = sid,
                className = name,
                gradeId = gId,
                classOptionId = cId,
                programId = pId,
                subClassId = scId,
                subGradeId = sgId,
                roleId = rId
            )

            val generatedId = masterClassDao.insertClass(newClass)
            val classWithId = newClass.copy(classId = generatedId.toInt())

            try {
                FirestoreRombel.saveMasterClass(classWithId)
                Log.d("MasterClassVM", "✅ Saved to Cloud: $name")
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
                // 🚀 THE FIX: Kirim SECOLAH_ID dan CLASS_ID sesuai signature baru
                FirestoreRombel.deleteMasterClass(
                    sekolahId = item.sekolahId, // Argumen 1 (String)
                    classId = item.classId      // Argumen 2 (Int)
                )
                
                // Hapus dari Local Room
                val entityToDelete = MasterClassRoom(
                    classId = item.classId,
                    sekolahId = item.sekolahId,
                    className = item.className ?: "",
                    gradeId = 0, classOptionId = 0, programId = 0,
                    subClassId = 0, subGradeId = 0, roleId = 0
                )
                masterClassDao.deleteClass(entityToDelete)
                
                Log.d("MasterClassVM", "🗑️ Deleted from Cloud & Local: ${item.className}")
            } catch (e: Exception) {
                Log.e("MasterClassVM", "❌ Delete failed", e)
            }
        }
    }

    /**
     * 🔄 Sinkronisasi Data dari Cloud
     */
    fun syncFromCloud(sekolahId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cloudData = FirestoreRombel.fetchMasterClasses(sekolahId)
                if (cloudData.isNotEmpty()) {
                    masterClassDao.insertAll(cloudData)
                }
            } catch (e: Exception) {
                Log.e("MasterClassVM", "❌ Sync failed", e)
            }
        }
    }
}