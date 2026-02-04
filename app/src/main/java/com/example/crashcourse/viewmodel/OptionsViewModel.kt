package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val classOptionDao = database.classOptionDao()
    private val subClassOptionDao = database.subClassOptionDao()
    private val gradeOptionDao = database.gradeOptionDao()
    private val subGradeOptionDao = database.subGradeOptionDao()
    private val programOptionDao = database.programOptionDao()
    private val roleOptionDao = database.roleOptionDao()

    // --- 🔥 REAKTIF: Convert Flow ke StateFlow ---
    // Gunakan WhileSubscribed(5000) agar database berhenti bekerja saat aplikasi di background
    
    val classOptions: StateFlow<List<ClassOption>> = classOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subClassOptions: StateFlow<List<SubClassOption>> = subClassOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gradeOptions: StateFlow<List<GradeOption>> = gradeOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subGradeOptions: StateFlow<List<SubGradeOption>> = subGradeOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programOptions: StateFlow<List<ProgramOption>> = programOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roleOptions: StateFlow<List<RoleOption>> = roleOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 📝 CRUD OPERATIONS ---

    fun addOption(optionType: String, name: String, displayOrder: Int, parentId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            when (optionType) {
                "Class" -> {
                    // Tip: Jika Brother pakai autoGenerate = true, set id = 0 saja
                    val maxId = classOptionDao.getMaxId() ?: 0
                    classOptionDao.insert(ClassOption(id = maxId + 1, name = name, displayOrder = displayOrder))
                }
                "SubClass" -> parentId?.let {
                    val maxId = subClassOptionDao.getMaxId() ?: 0
                    subClassOptionDao.insert(SubClassOption(id = maxId + 1, name = name, parentClassId = it, displayOrder = displayOrder))
                }
                "Grade" -> {
                    val maxId = gradeOptionDao.getMaxId() ?: 0
                    gradeOptionDao.insert(GradeOption(id = maxId + 1, name = name, displayOrder = displayOrder))
                }
                "SubGrade" -> parentId?.let {
                    val maxId = subGradeOptionDao.getMaxId() ?: 0
                    subGradeOptionDao.insert(SubGradeOption(id = maxId + 1, name = name, parentGradeId = it, displayOrder = displayOrder))
                }
                "Program" -> {
                    val maxId = programOptionDao.getMaxId() ?: 0
                    programOptionDao.insert(ProgramOption(id = maxId + 1, name = name, displayOrder = displayOrder))
                }
                "Role" -> {
                    val maxId = roleOptionDao.getMaxId() ?: 0
                    roleOptionDao.insert(RoleOption(id = maxId + 1, name = name, displayOrder = displayOrder))
                }
            }
        }
    }

    fun updateOption(optionType: String, option: Any, name: String, displayOrder: Int, parentId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            when (optionType) {
                "Class" -> if (option is ClassOption) {
                    classOptionDao.update(option.copy(name = name, displayOrder = displayOrder))
                }
                "SubClass" -> if (option is SubClassOption && parentId != null) {
                    subClassOptionDao.update(option.copy(name = name, displayOrder = displayOrder, parentClassId = parentId))
                }
                "Grade" -> if (option is GradeOption) {
                    gradeOptionDao.update(option.copy(name = name, displayOrder = displayOrder))
                }
                "SubGrade" -> if (option is SubGradeOption && parentId != null) {
                    subGradeOptionDao.update(option.copy(name = name, displayOrder = displayOrder, parentGradeId = parentId))
                }
                "Program" -> if (option is ProgramOption) {
                    programOptionDao.update(option.copy(name = name, displayOrder = displayOrder))
                }
                "Role" -> if (option is RoleOption) {
                    roleOptionDao.update(option.copy(name = name, displayOrder = displayOrder))
                }
            }
        }
    }

    fun deleteOption(optionType: String, option: Any) {
        viewModelScope.launch(Dispatchers.IO) {
            when (optionType) {
                "Class" -> if (option is ClassOption) classOptionDao.delete(option)
                "SubClass" -> if (option is SubClassOption) subClassOptionDao.delete(option)
                "Grade" -> if (option is GradeOption) gradeOptionDao.delete(option)
                "SubGrade" -> if (option is SubGradeOption) subGradeOptionDao.delete(option)
                "Program" -> if (option is ProgramOption) programOptionDao.delete(option)
                "Role" -> if (option is RoleOption) roleOptionDao.delete(option)
            }
        }
    }
}