package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.repository.OptionsRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 👨‍💻 OptionsViewModel
 * Menangani logika "Kamus Master" (6-Pilar).
 * Menggunakan strategi Local-First: Simpan ke HP dulu, baru ke Cloud.
 */
class OptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OptionsRepository(application)
    private val listeners = mutableListOf<ListenerRegistration>()

    // --- 📊 DATA FLOWS (Observing Local Room DB) ---
    val gradeOptions = repository.getGradeOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val programOptions = repository.getProgramOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subClassOptions = repository.getSubClassOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classOptions = repository.getClassOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subGradeOptions = repository.getSubGradeOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val roleOptions = repository.getRoleOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    init {
        // Jalankan sinkronisasi awal saat screen dibuka
        syncAllFromCloud()
    }

    /**
     * ➕ ADD OPTION (Local-First)
     */
    fun addOption(type: String, name: String, order: Int, parentId: Int? = null) {
        viewModelScope.launch {
            val newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            
            // 1. Buat Entity untuk disimpan ke Room secara instan
            val entity: Any? = when (type) {
                "Class" -> ClassOption(newId, name, order)
                "SubClass" -> SubClassOption(newId, name, parentId ?: 0, order)
                "Grade" -> GradeOption(newId, name, order)
                "SubGrade" -> SubGradeOption(newId, name, parentId ?: 0, order)
                "Program" -> ProgramOption(newId, name, order)
                "Role" -> RoleOption(newId, name, order)
                else -> null
            }

            // 2. Simpan ke Local agar UI langsung update (Optimistic UI)
            entity?.let { repository.insertLocally(it) }

            // 3. Simpan ke Cloud (Background Sync)
            val collectionName = repository.getCollectionName(type)
            val data = hashMapOf(
                "id" to newId,
                "name" to name,
                "order" to order,
                "parentId" to (parentId ?: 0)
            )
            repository.saveOptionToCloud(collectionName, newId, data)
        }
    }

    /**
     * 🆙 UPDATE OPTION (Local-First)
     */
    fun updateOption(type: String, option: Any, name: String, order: Int, parentId: Int? = null) {
        viewModelScope.launch {
            val id = getOptionId(option)
            
            // 1. Update Room secara instan
            val updatedEntity: Any? = when (option) {
                is ClassOption -> option.copy(name = name, displayOrder = order)
                is SubClassOption -> option.copy(name = name, parentClassId = parentId ?: 0, displayOrder = order)
                is GradeOption -> option.copy(name = name, displayOrder = order)
                is SubGradeOption -> option.copy(name = name, parentGradeId = parentId ?: 0, displayOrder = order)
                is ProgramOption -> option.copy(name = name, displayOrder = order)
                is RoleOption -> option.copy(name = name, displayOrder = order)
                else -> null
            }
            
            updatedEntity?.let { repository.insertLocally(it) }

            // 2. Update Cloud
            val collectionName = repository.getCollectionName(type)
            val data = hashMapOf(
                "id" to id,
                "name" to name,
                "order" to order,
                "parentId" to (parentId ?: 0)
            )
            repository.saveOptionToCloud(collectionName, id, data)
        }
    }

    /**
     * ☁️ SYNC MANUAL
     */
    fun syncAllFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncAllFromCloud()
            _isSyncing.value = false
        }
    }

    /**
     * 🗑️ DELETE OPTION
     */
    fun deleteOption(type: String, option: Any) {
        viewModelScope.launch {
            try {
                val collectionName = repository.getCollectionName(type)
                val id = getOptionId(option)
                
                // Hapus di Cloud & Local
                com.example.crashcourse.firestore.options.FirestoreOptions.deleteOption(collectionName, id)
                repository.deleteOptionLocally(option)
            } catch (e: Exception) { 
                // Handle error silentry
            }
        }
    }

    /**
     * 🛠️ INTERNAL HELPER
     */
    private fun getOptionId(option: Any): Int = when (option) {
        is ClassOption -> option.id
        is SubClassOption -> option.id
        is GradeOption -> option.id
        is SubGradeOption -> option.id
        is ProgramOption -> option.id
        is RoleOption -> option.id
        else -> 0
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}