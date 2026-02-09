package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.ui.OptionsHelpers
import com.example.crashcourse.utils.Constants // 🚀 Import Constants
import com.example.crashcourse.utils.FirestoreHelper
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    
    // --- 1. LOCAL DATA FLOWS (Offline First) ---
    val classOptions: StateFlow<List<ClassOption>> = db.classOptionDao().getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subClassOptions: StateFlow<List<SubClassOption>> = db.subClassOptionDao().getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gradeOptions: StateFlow<List<GradeOption>> = db.gradeOptionDao().getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subGradeOptions: StateFlow<List<SubGradeOption>> = db.subGradeOptionDao().getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programOptions: StateFlow<List<ProgramOption>> = db.programOptionDao().getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roleOptions: StateFlow<List<RoleOption>> = db.roleOptionDao().getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 2. SYNC STATE ---
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        startRealtimeSync()
    }

    // --- 3. MANUAL PULL ENGINE (Cloud -> Local) ---
    fun syncAllFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            Log.d("OptionsVM", "📥 Memulai Pull Data dari Cloud...")
            
            try {
                // List kategori ini digunakan untuk looping logic, bukan nama koleksi DB
                val categories = listOf("Class", "SubClass", "Grade", "SubGrade", "Program", "Role")
                
                categories.forEach { type ->
                    val collection = getCollectionName(type) // Mengambil nama koleksi dari Constants via fungsi helper
                    val cloudData = FirestoreHelper.fetchOptionsOnce(collection)
                    
                    if (cloudData.isNotEmpty()) {
                        when (type) {
                            "Class" -> db.classOptionDao().insertAll(cloudData.map { map -> 
                                ClassOption(
                                    id = map["id"].toString().toInt(), 
                                    name = map["name"].toString(), 
                                    displayOrder = map["order"].toString().toIntOrNull() ?: 0
                                ) 
                            })
                            "SubClass" -> db.subClassOptionDao().insertAll(cloudData.map { map -> 
                                SubClassOption(
                                    id = map["id"].toString().toInt(), 
                                    name = map["name"].toString(), 
                                    parentClassId = map["parentId"].toString().toIntOrNull() ?: 0, 
                                    displayOrder = map["order"].toString().toIntOrNull() ?: 0
                                ) 
                            })
                            "Grade" -> db.gradeOptionDao().insertAll(cloudData.map { map -> 
                                GradeOption(
                                    id = map["id"].toString().toInt(), 
                                    name = map["name"].toString(), 
                                    displayOrder = map["order"].toString().toIntOrNull() ?: 0
                                ) 
                            })
                            "SubGrade" -> db.subGradeOptionDao().insertAll(cloudData.map { map -> 
                                SubGradeOption(
                                    id = map["id"].toString().toInt(), 
                                    name = map["name"].toString(), 
                                    parentGradeId = map["parentId"].toString().toIntOrNull() ?: 0, 
                                    displayOrder = map["order"].toString().toIntOrNull() ?: 0
                                ) 
                            })
                            "Program" -> db.programOptionDao().insertAll(cloudData.map { map -> 
                                ProgramOption(
                                    id = map["id"].toString().toInt(), 
                                    name = map["name"].toString(), 
                                    displayOrder = map["order"].toString().toIntOrNull() ?: 0
                                ) 
                            })
                            "Role" -> db.roleOptionDao().insertAll(cloudData.map { map -> 
                                RoleOption(
                                    id = map["id"].toString().toInt(), 
                                    name = map["name"].toString(), 
                                    displayOrder = map["order"].toString().toIntOrNull() ?: 0
                                ) 
                            })
                        }
                    }
                }
                Log.d("OptionsVM", "✅ Pull Data Selesai")
            } catch (e: Exception) {
                Log.e("OptionsVM", "❌ Gagal Pull Data: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // --- 4. REALTIME SYNC ENGINE ---
    private fun startRealtimeSync() {
        Log.d("OptionsVM", "🚀 Memulai Realtime Sync Master Data...")

        // 🚀 MENGGUNAKAN CONSTANTS
        listeners.add(FirestoreHelper.listenToOptions(Constants.COLL_OPT_CLASSES) { list ->
            viewModelScope.launch(Dispatchers.IO) {
                db.classOptionDao().insertAll(list.map { map -> 
                    ClassOption(map["id"].toString().toInt(), map["name"].toString(), map["order"].toString().toIntOrNull() ?: 0) 
                })
            }
        })

        listeners.add(FirestoreHelper.listenToOptions(Constants.COLL_OPT_SUBCLASSES) { list ->
            viewModelScope.launch(Dispatchers.IO) {
                db.subClassOptionDao().insertAll(list.map { map -> 
                    SubClassOption(map["id"].toString().toInt(), map["name"].toString(), map["parentId"].toString().toIntOrNull() ?: 0, map["order"].toString().toIntOrNull() ?: 0) 
                })
            }
        })

        listeners.add(FirestoreHelper.listenToOptions(Constants.COLL_OPT_GRADES) { list ->
            viewModelScope.launch(Dispatchers.IO) {
                db.gradeOptionDao().insertAll(list.map { map -> 
                    GradeOption(map["id"].toString().toInt(), map["name"].toString(), map["order"].toString().toIntOrNull() ?: 0) 
                })
            }
        })

        listeners.add(FirestoreHelper.listenToOptions(Constants.COLL_OPT_SUBGRADES) { list ->
            viewModelScope.launch(Dispatchers.IO) {
                db.subGradeOptionDao().insertAll(list.map { map -> 
                    SubGradeOption(map["id"].toString().toInt(), map["name"].toString(), map["parentId"].toString().toIntOrNull() ?: 0, map["order"].toString().toIntOrNull() ?: 0) 
                })
            }
        })

        listeners.add(FirestoreHelper.listenToOptions(Constants.COLL_OPT_PROGRAMS) { list ->
            viewModelScope.launch(Dispatchers.IO) {
                db.programOptionDao().insertAll(list.map { map -> 
                    ProgramOption(map["id"].toString().toInt(), map["name"].toString(), map["order"].toString().toIntOrNull() ?: 0) 
                })
            }
        })

        listeners.add(FirestoreHelper.listenToOptions(Constants.COLL_OPT_ROLES) { list ->
            viewModelScope.launch(Dispatchers.IO) {
                db.roleOptionDao().insertAll(list.map { map -> 
                    RoleOption(map["id"].toString().toInt(), map["name"].toString(), map["order"].toString().toIntOrNull() ?: 0) 
                })
            }
        })
    }

    // --- 5. CRUD ACTIONS ---
    
    fun addOption(type: String, name: String, order: Int, parentId: Int? = null) {
        val collection = getCollectionName(type)
        val newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val data = hashMapOf("id" to newId, "name" to name, "order" to order, "parentId" to (parentId ?: 0))

        viewModelScope.launch(Dispatchers.IO) {
            FirestoreHelper.saveOptionToFirestore(collection, newId, data)
        }
    }

    fun updateOption(type: String, option: Any, name: String, order: Int, parentId: Int? = null) {
        val collection = getCollectionName(type)
        val id = OptionsHelpers.getId(option)
        val data = hashMapOf("id" to id, "name" to name, "order" to order, "parentId" to (parentId ?: 0))

        viewModelScope.launch(Dispatchers.IO) {
            FirestoreHelper.saveOptionToFirestore(collection, id, data)
        }
    }

    fun deleteOption(type: String, option: Any) {
        val collection = getCollectionName(type)
        val id = OptionsHelpers.getId(option)
        
        viewModelScope.launch(Dispatchers.IO) {
            FirestoreHelper.deleteOptionFromFirestore(collection, id)
            try {
                when (option) {
                    is ClassOption -> db.classOptionDao().delete(option)
                    is SubClassOption -> db.subClassOptionDao().delete(option)
                    is GradeOption -> db.gradeOptionDao().delete(option)
                    is SubGradeOption -> db.subGradeOptionDao().delete(option)
                    is ProgramOption -> db.programOptionDao().delete(option)
                    is RoleOption -> db.roleOptionDao().delete(option)
                }
            } catch (e: Exception) {
                Log.e("OptionsVM", "Gagal delete lokal: ${e.message}")
            }
        }
    }

    private fun getCollectionName(type: String): String {
        // 🚀 MENGGUNAKAN CONSTANTS
        return when(type) {
            "Class" -> Constants.COLL_OPT_CLASSES
            "SubClass" -> Constants.COLL_OPT_SUBCLASSES
            "Grade" -> Constants.COLL_OPT_GRADES
            "SubGrade" -> Constants.COLL_OPT_SUBGRADES
            "Program" -> Constants.COLL_OPT_PROGRAMS
            "Role" -> Constants.COLL_OPT_ROLES
            else -> Constants.COLL_OPT_OTHERS
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
        Log.d("OptionsVM", "🛑 Sync Stopped")
    }
}