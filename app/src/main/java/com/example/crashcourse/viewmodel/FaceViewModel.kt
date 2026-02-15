package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.ml.FaceRecognitionEngine
import com.example.crashcourse.repository.FaceRepository
import com.example.crashcourse.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 👤 FaceViewModel (V.10.60 - Secure Registration Edition)
 * Mengelola data personel dengan verifikasi biometrik ganda dan multi-unit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FaceViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepo = UserRepository(application)
    private val faceRepo = FaceRepository(application)
    
    // 🔥 Mesin biometrik untuk mendeteksi kemiripan wajah (Obama vs Makhachev Guard)
    private val recognitionEngine = FaceRecognitionEngine(application)

    companion object {
        private const val TAG = "FaceViewModel"
    }

    // ==========================================
    // 🔍 FILTER STATES (UI Input)
    // ==========================================
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedUnit = MutableStateFlow<MasterClassWithNames?>(null)
    val selectedUnit = _selectedUnit.asStateFlow()

    // ==========================================
    // 🔐 SESSION CONTEXT & 🛡️ REACTIVE PIPELINE
    // ==========================================
    
    // Mengambil schoolId Admin yang sedang login
    private val schoolIdFlow = userRepo.getCurrentUserFlow()
        .map { it?.schoolId }
        .distinctUntilChanged()

    // Memuat data wajah hanya untuk sekolah yang aktif
    private val rawFacesFlow = schoolIdFlow.flatMapLatest { id ->
        if (id.isNullOrBlank()) flowOf(emptyList()) 
        else faceRepo.getAllFacesFlow(id) 
    }
    
    // Gabungkan data wajah dengan filter pencarian dan unit secara real-time
    val filteredFaces: StateFlow<List<FaceEntity>> = combine(
        rawFacesFlow,
        _searchQuery,
        _selectedUnit
    ) { faces, query, unit ->
        faces.filter { face ->
            val matchUnit = unit == null || face.enrolledClasses.contains(unit.className)
            val matchSearch = query.isEmpty() || 
                             face.name.contains(query, ignoreCase = true) || 
                             face.studentId.contains(query)
            matchUnit && matchSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ==========================================
    // 1️⃣ REGISTER ACTIONS (With Biometric Gatekeeper)
    // ==========================================
    fun registerFaceWithMultiUnit(
        studentId: String,
        name: String,
        embedding: FloatArray,
        units: List<MasterClassWithNames>, 
        photoUrl: String? = null,
        onSuccess: () -> Unit,
        onDuplicateId: (String) -> Unit, // Callback jika NIK/ID sudah ada
        onSimilarFace: (String) -> Unit, // ✅ Callback jika wajah sudah ada (Biometrik)
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser()
                val currentSchoolId = user?.schoolId
                
                if (currentSchoolId.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onError("Sesi sekolah tidak valid. Silakan login ulang.") }
                    return@launch
                }

                // 🛡️ STEP 1: Administrative Check (Cek duplikasi ID di Database)
                if (faceRepo.getFaceByStudentId(studentId) != null) {
                    withContext(Dispatchers.Main) { onDuplicateId(studentId) }
                    return@launch
                }

                // 🛡️ STEP 2: Biometric Check (Cek duplikasi wujud wajah di RAM)
                // Ini mencegah satu orang punya dua ID berbeda
                val similarPersonName = recognitionEngine.detectDuplicate(embedding)
                if (similarPersonName != null) {
                    withContext(Dispatchers.Main) { onSimilarFace(similarPersonName) }
                    return@launch
                }

                // STEP 3: Simpan Data
                faceRepo.registerFace(
                    studentId = studentId,
                    schoolId = currentSchoolId,
                    name = name,
                    embedding = embedding,
                    units = units,
                    photoUrl = photoUrl
                )
                
                // 🔄 Sinkronisasi RAM agar scanner langsung kenal wajah baru ini
                FaceCache.refresh(getApplication())

                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ registerFaceWithMultiUnit failed", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Gagal menyimpan data.") }
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
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                faceRepo.registerFace(
                    studentId = originalFace.studentId,
                    schoolId = originalFace.schoolId,
                    name = newName,
                    embedding = newEmbedding ?: originalFace.embedding,
                    units = newUnits,
                    photoUrl = newPhotoPath ?: originalFace.photoUrl
                )
                
                FaceCache.refresh(getApplication())
                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ update failed", e)
                withContext(Dispatchers.Main) { onError("Gagal memperbarui data.") }
            }
        }
    }

    // ==========================================
    // 3️⃣ DELETE ACTIONS
    // ==========================================
    fun deleteFace(face: FaceEntity, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                faceRepo.deleteFace(face.studentId, face)
                FaceCache.refresh(getApplication())
                Log.d(TAG, "✅ Delete Berhasil")
            } catch (e: Exception) {
                Log.e(TAG, "❌ delete failed", e)
                withContext(Dispatchers.Main) { onError("Gagal menghapus data.") }
            }
        }
    }

    // Helpers
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSelectedUnit(unit: MasterClassWithNames?) { _selectedUnit.value = unit }
    fun resetFilters() {
        _searchQuery.value = ""
        _selectedUnit.value = null
    }
}