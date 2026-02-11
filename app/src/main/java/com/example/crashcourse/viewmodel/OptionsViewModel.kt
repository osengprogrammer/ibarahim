package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.options.FirestoreOptions // ✅ NEW IMPORT
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 🏛️ Azura Tech Options ViewModel
 * Menangani Sinkronisasi Master Data 6-Pilar antara Firestore dan Database Lokal.
 */
class OptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    // Tracking listeners untuk dibersihkan saat ViewModel hancur
    private val listeners = mutableListOf<ListenerRegistration>()

    // --- 📊 LOCAL DATA FLOWS (Room) ---
    val gradeOptions = db.gradeOptionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programOptions = db.programOptionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subClassOptions = db.subClassOptionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classOptions = db.classOptionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subGradeOptions = db.subGradeOptionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roleOptions = db.roleOptionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 🔄 SYNC STATE ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    init {
        startRealtimeSync()
    }

    // --- 🚀 CLOUD ACTIONS ---

    /**
     * Trigger manual untuk mengunduh ulang semua Master Data.
     */
    fun syncAllFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                val types = listOf("Class", "SubClass", "Grade", "SubGrade", "Program", "Role")
                types.forEach { type ->
                    val collectionName = getCollectionName(type)
                    // ✅ UPDATED: Use FirestoreOptions
                    val cloudData = FirestoreOptions.fetchOptionsOnce(collectionName)
                    if (cloudData.isNotEmpty()) {
                        processCloudData(type, cloudData)
                    }
                }
                Log.d("OptionsVM", "✅ Manual Sync Complete")
            } catch (e: Exception) {
                Log.e("OptionsVM", "❌ Manual Sync Failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addOption(type: String, name: String, order: Int, parentId: Int? = null) {
        val collectionName = getCollectionName(type)
        val newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val data = hashMapOf(
            "id" to newId,
            "name" to name,
            "order" to order,
            "parentId" to (parentId ?: 0)
        )
        viewModelScope.launch(Dispatchers.IO) {
            // ✅ UPDATED: Use FirestoreOptions
            FirestoreOptions.saveOption(collectionName, newId, data)
        }
    }

    fun updateOption(type: String, option: Any, name: String, order: Int, parentId: Int? = null) {
        val collectionName = getCollectionName(type)
        val id = getOptionId(option)
        val data = hashMapOf(
            "id" to id,
            "name" to name,
            "order" to order,
            "parentId" to (parentId ?: 0)
        )
        viewModelScope.launch(Dispatchers.IO) {
            // ✅ UPDATED: Use FirestoreOptions
            FirestoreOptions.saveOption(collectionName, id, data)
        }
    }

    fun deleteOption(type: String, option: Any) {
        val collectionName = getCollectionName(type)
        val id = getOptionId(option)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ✅ UPDATED: Use FirestoreOptions
                FirestoreOptions.deleteOption(collectionName, id)
                
                // Hapus lokal secara optimis
                when (option) {
                    is ClassOption -> db.classOptionDao().delete(option)
                    is SubClassOption -> db.subClassOptionDao().delete(option)
                    is GradeOption -> db.gradeOptionDao().delete(option)
                    is SubGradeOption -> db.subGradeOptionDao().delete(option)
                    is ProgramOption -> db.programOptionDao().delete(option)
                    is RoleOption -> db.roleOptionDao().delete(option)
                }
            } catch (e: Exception) {
                Log.e("OptionsVM", "❌ Delete Failed: ${e.message}")
            }
        }
    }

    // --- 🛠️ SYNC ENGINE ---

    private fun startRealtimeSync() {
        val syncMap = mapOf(
            Constants.COLL_OPT_CLASSES to "Class",
            Constants.COLL_OPT_SUBCLASSES to "SubClass",
            Constants.COLL_OPT_GRADES to "Grade",
            Constants.COLL_OPT_SUBGRADES to "SubGrade",
            Constants.COLL_OPT_PROGRAMS to "Program",
            Constants.COLL_OPT_ROLES to "Role"
        )

        syncMap.forEach { (coll, type) ->
            // ✅ UPDATED: Use FirestoreOptions
            listeners.add(FirestoreOptions.listenToOptions(coll) { list: List<Map<String, Any>> ->
                viewModelScope.launch(Dispatchers.IO) {
                    processCloudData(type, list)
                }
            })
        }
    }

    private suspend fun processCloudData(type: String, dataList: List<Map<String, Any>>) {
        try {
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
        } catch (e: Exception) {
            Log.e("OptionsVM", "Error processing cloud data: ${e.message}")
        }
    }

    // --- ⚙️ HELPERS ---

    private fun getCollectionName(type: String): String = when(type) {
        "Class" -> Constants.COLL_OPT_CLASSES
        "SubClass" -> Constants.COLL_OPT_SUBCLASSES
        "Grade" -> Constants.COLL_OPT_GRADES
        "SubGrade" -> Constants.COLL_OPT_SUBGRADES
        "Program" -> Constants.COLL_OPT_PROGRAMS
        "Role" -> Constants.COLL_OPT_ROLES
        else -> "others"
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