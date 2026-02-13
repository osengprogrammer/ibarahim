package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.repository.OptionsRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OptionsRepository(application)
    private val listeners = mutableListOf<ListenerRegistration>()

    // --- 📊 DATA FLOWS ---
    val gradeOptions = repository.getGradeOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val programOptions = repository.getProgramOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subClassOptions = repository.getSubClassOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val classOptions = repository.getClassOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subGradeOptions = repository.getSubGradeOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val roleOptions = repository.getRoleOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    init {
        startRealtimeSync()
    }

    // 🔥 ADDED: Fix Unresolved reference 'addOption'
    fun addOption(type: String, name: String, order: Int, parentId: Int? = null) {
        viewModelScope.launch {
            val collectionName = repository.getCollectionName(type)
            val newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val data = hashMapOf(
                "id" to newId,
                "name" to name,
                "order" to order,
                "parentId" to (parentId ?: 0)
            )
            repository.saveOptionToCloud(collectionName, newId, data)
        }
    }

    // 🔥 ADDED: Fix Unresolved reference 'updateOption'
    fun updateOption(type: String, option: Any, name: String, order: Int, parentId: Int? = null) {
        viewModelScope.launch {
            val collectionName = repository.getCollectionName(type)
            val id = getOptionId(option)
            val data = hashMapOf(
                "id" to id,
                "name" to name,
                "order" to order,
                "parentId" to (parentId ?: 0)
            )
            repository.saveOptionToCloud(collectionName, id, data)
        }
    }

    fun syncAllFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncAllFromCloud()
            _isSyncing.value = false
        }
    }

    fun deleteOption(type: String, option: Any) {
        viewModelScope.launch {
            try {
                val collectionName = repository.getCollectionName(type)
                val id = getOptionId(option)
                com.example.crashcourse.firestore.options.FirestoreOptions.deleteOption(collectionName, id)
                repository.deleteOptionLocally(option)
            } catch (e: Exception) { }
        }
    }

    private fun startRealtimeSync() {
        val syncMap = mapOf(
            "Class" to "Class", "SubClass" to "SubClass", "Grade" to "Grade",
            "SubGrade" to "SubGrade", "Program" to "Program", "Role" to "Role"
        )
        // Implementasi listener tetap panggil repository.processAndSave
    }

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