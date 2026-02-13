package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.repository.FaceRepository
import com.example.crashcourse.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 👤 FaceViewModel (V.7.0 - Data Logistics Specialist)
 * Khusus mengelola Pendaftaran Biometrik, Filter Data, dan Sinkronisasi Cloud.
 * Urusan Recognition & Absensi telah dipindah ke RecognitionViewModel.
 */
class FaceViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepo = UserRepository(application)
    private val faceRepo = FaceRepository(application)

    companion object {
        private const val TAG = "FaceViewModel"
    }

    // ==========================================
    // 🔍 FILTER STATES (Internal)
    // ==========================================
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedUnit = MutableStateFlow<MasterClassWithNames?>(null)
    val selectedUnit = _selectedUnit.asStateFlow()

    // ==========================================
    // 🔐 SESSION CONTEXT
    // ==========================================
    private val sekolahIdFlow = userRepo
        .getCurrentUserFlow()
        .map { it?.sekolahId }
        .distinctUntilChanged()

    // ==========================================
    // 🛡️ REAKTIF: SCOPED & FILTERED FACE LIST
    // ==========================================
    // Menyediakan daftar siswa yang sudah difilter untuk kebutuhan UI Management
    val filteredFaces: StateFlow<List<FaceEntity>> = combine(
        sekolahIdFlow,
        faceRepo.getAllFacesFlow(),
        _searchQuery,
        _selectedUnit
    ) { sid, faces, query, unit ->
        if (sid.isNullOrBlank()) {
            emptyList()
        } else {
            faces.filter { face ->
                val isMySchool = face.sekolahId == sid
                val matchUnit = unit == null || face.className.contains(unit.className, ignoreCase = true)
                val matchSearch = query.isEmpty() || 
                                 face.name.contains(query, ignoreCase = true) || 
                                 face.studentId.contains(query)
                
                isMySchool && matchUnit && matchSearch
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ==========================================
    // 🎮 FILTER ACTIONS
    // ==========================================
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedUnit(unit: MasterClassWithNames?) {
        _selectedUnit.value = unit
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedUnit.value = null
    }

    // ==========================================
    // 1️⃣ REGISTER ACTIONS
    // ==========================================
    fun registerFaceWithMultiUnit(
        studentId: String,
        name: String,
        embedding: FloatArray,
        units: List<MasterClassWithNames>,
        photoUrl: String? = null,
        onSuccess: () -> Unit,
        onDuplicate: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sekolahId = user.sekolahId ?: return@launch

                if (faceRepo.getFaceByStudentId(studentId) != null) {
                    withContext(Dispatchers.Main) { onDuplicate(studentId) }
                    return@launch
                }

                faceRepo.registerFace(
                    studentId = studentId,
                    sekolahId = sekolahId,
                    name = name,
                    embedding = embedding,
                    units = units,
                    photoUrl = photoUrl
                )

                // Refresh RAM cache agar data baru siap digunakan RecognitionViewModel
                FaceCache.refresh(getApplication())
                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ registerFaceWithMultiUnit failed", e)
            }
        }
    }

    // ==========================================
    // 2️⃣ UPDATE ACTIONS
    // ==========================================
    fun updateFaceWithMultiUnit(
        originalFace: FaceEntity,
        newName: String,
        newUnits: List<MasterClassWithNames>,
        newPhotoPath: String?,
        newEmbedding: FloatArray?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                faceRepo.registerFace(
                    studentId = originalFace.studentId,
                    sekolahId = originalFace.sekolahId,
                    name = newName,
                    embedding = newEmbedding ?: originalFace.embedding,
                    units = newUnits,
                    photoUrl = newPhotoPath ?: originalFace.photoUrl
                )

                FaceCache.refresh(getApplication())
                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ updateFaceWithMultiUnit failed", e)
            }
        }
    }

    // ==========================================
    // 3️⃣ MAINTENANCE ACTIONS (Sync & Delete)
    // ==========================================
    fun syncStudentsFromCloud() {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                faceRepo.syncStudents(user)
                FaceCache.refresh(getApplication())
                Log.d(TAG, "✅ Sync Berhasil")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Sinkronisasi gagal", e)
            }
        }
    }

    fun deleteFace(face: FaceEntity) {
        viewModelScope.launch {
            try {
                faceRepo.deleteFace(face.studentId, face)
                FaceCache.refresh(getApplication())
                Log.d(TAG, "✅ Delete Berhasil")
            } catch (e: Exception) {
                Log.e(TAG, "❌ delete failed", e)
            }
        }
    }
}