package com.example.crashcourse.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.utils.FirestoreHelper
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.utils.NativeMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class FaceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val faceDao = database.faceDao()
    private val checkInRecordDao = database.checkInRecordDao()
    
    // DAOs for Options
    private val classOptionDao = database.classOptionDao()
    private val subClassOptionDao = database.subClassOptionDao()
    private val gradeOptionDao = database.gradeOptionDao()
    private val subGradeOptionDao = database.subGradeOptionDao()
    private val programOptionDao = database.programOptionDao()
    private val roleOptionDao = database.roleOptionDao()

    companion object {
        private const val DUPLICATE_DISTANCE_THRESHOLD = 0.3f
        const val RECOGNITION_DISTANCE_THRESHOLD = 0.40f
        private const val TAG = "FaceViewModel"
    }

    // --- 🛡️ TEACHER SCOPE LOGIC (ANTI-BLINKING CACHE) ---

    private var cachedScopedFlow: StateFlow<List<FaceEntity>>? = null
    private var lastUserId: String? = null

    fun getScopedFaceList(authState: AuthState.Active): StateFlow<List<FaceEntity>> {
        if (cachedScopedFlow != null && lastUserId == authState.uid) {
            return cachedScopedFlow!!
        }
        lastUserId = authState.uid
        val flow = faceDao.getAllFacesFlow()
            .map { allFaces ->
                if (authState.role == "ADMIN") allFaces 
                else allFaces.filter { face -> authState.assignedClasses.contains(face.className) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        cachedScopedFlow = flow
        return flow
    }

    val faceList: StateFlow<List<FaceEntity>> =
        faceDao.getAllFacesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- 📝 ATTENDANCE LOGIC (FIXED: SYNC MULTIPLE TIMES) ---

    fun saveCheckIn(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Cari data lengkap siswa berdasarkan nama hasil scan
                val face = faceDao.getFaceByName(name) ?: run {
                    Log.e(TAG, "❌ Gagal: Wajah '$name' tidak ada di DB Lokal")
                    return@launch
                }

                // 2. Validasi Student ID (NIK)
                if (face.studentId.isBlank()) {
                    Log.e(TAG, "❌ FATAL: Student ID Kosong untuk ${face.name}. Sync Cloud dibatalkan.")
                }

                val record = CheckInRecord(
                    studentId = face.studentId, // Diambil dari data registrasi awal
                    name = face.name,
                    timestamp = LocalDateTime.now(),
                    faceId = face.id,
                    status = "PRESENT",
                    classId = face.classId,
                    className = face.className,
                    subClassId = face.subClassId,
                    gradeId = face.gradeId,
                    gradeName = face.grade,
                    subGradeId = face.subGradeId,
                    programId = face.programId,
                    roleId = face.roleId
                )

                // 3. Simpan ke Local (Offline-first)
                checkInRecordDao.insert(record)
                Log.d(TAG, "💾 Tersimpan Local: ${record.name}")
                
                // 4. Kirim ke Firestore (Real-time sync ke App Orang Tua)
                if (face.studentId.isNotBlank()) {
                    try {
                        FirestoreHelper.syncAttendanceLog(record)
                        Log.d(TAG, "☁️ Berhasil Sync Cloud: ${face.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Gagal Sync Cloud: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "🔥 Error saveCheckIn: ${e.message}")
            }
        }
    }

    // --- 👤 FACE REGISTRATION (FIXED: FORCED CLOUD SYNC) ---

    // Ganti fungsi registerFace di FaceViewModel.kt Anda dengan ini:
fun registerFace(
    studentId: String, name: String, embedding: FloatArray, photoUrl: String? = null,
    className: String = "", classId: Int? = null, subClass: String = "", subClassId: Int? = null,
    grade: String = "", gradeId: Int? = null, subGrade: String = "", subGradeId: Int? = null,
    program: String = "", programId: Int? = null, role: String = "", roleId: Int? = null,
    onSuccess: () -> Unit, onDuplicate: (existingName: String) -> Unit
) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // 🛑 VALIDASI KRUSIAL: Jika ID Kosong, Firestore akan Error
            if (studentId.trim().isEmpty()) {
                Log.e(TAG, "❌ Registrasi Dibatalkan: studentId Kosong!")
                return@launch
            }

            // 1. Cek duplikasi Local
            val existingFace = faceDao.getFaceByStudentId(studentId)
            if (existingFace != null) {
                withContext(Dispatchers.Main) { onDuplicate("${existingFace.name} (ID: $studentId)") }
                return@launch
            }

            val face = FaceEntity(
                studentId = studentId, name = name, photoUrl = photoUrl, embedding = embedding.clone(),
                className = className, subClass = subClass, grade = grade, subGrade = subGrade,
                program = program, role = role, classId = classId, subClassId = subClassId,
                gradeId = gradeId, subGradeId = subGradeId, programId = programId, roleId = roleId,
                timestamp = System.currentTimeMillis()
            )

            // 2. Simpan Local Dulu
            faceDao.insert(face)
            Log.d(TAG, "💾 Tersimpan di Local Room")

            // 3. Sinkronisasi Cloud (Ini yang sering gagal)
            val isCloudSynced = FirestoreHelper.syncStudentToFirestore(face)
            
            if (isCloudSynced) {
                Log.d(TAG, "☁️ Berhasil Sinkron ke Firestore")
                FaceCache.refresh(getApplication())
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                Log.e(TAG, "⚠️ Gagal Sinkron Cloud. Periksa Koneksi/Rules.")
                // Tetap panggil onSuccess jika Anda ingin app jalan offline-first
                withContext(Dispatchers.Main) { onSuccess() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔥 Registrasi Error: ${e.message}")
        }
    }
}

    // --- 🛠️ DROPDOWN OPTIONS (FIXED FUNCTIONS) ---
    
    val classOptions: StateFlow<List<ClassOption>> = classOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        
    val gradeOptions: StateFlow<List<GradeOption>> = gradeOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        
    val programOptions: StateFlow<List<ProgramOption>> = programOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        
    // ✅ PERBAIKAN: Menggunakan getAllOptions() agar sesuai dengan DAO standar
    val roleOptions: StateFlow<List<RoleOption>> = roleOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getSubClassOptions(parentClassId: Int): Flow<List<SubClassOption>> = subClassOptionDao.getOptionsForClass(parentClassId)
    fun getSubGradeOptions(parentGradeId: Int): Flow<List<SubGradeOption>> = subGradeOptionDao.getOptionsForGrade(parentGradeId)

    // --- ⚙️ MANAGEMENT FUNCTIONS ---

    fun deleteFace(face: FaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.delete(face)
            FaceCache.refresh(getApplication())
        }
    }

    fun updateFace(face: FaceEntity, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.update(face)
            FirestoreHelper.syncStudentToFirestore(face) 
            FaceCache.refresh(getApplication())
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    fun updateFaceWithPhoto(
        face: FaceEntity, photoBitmap: Bitmap?, embedding: FloatArray,
        onComplete: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val embeddingCopy = embedding.clone()
                val photoUrl = if (photoBitmap != null) {
                    PhotoStorageUtils.saveFacePhoto(getApplication(), photoBitmap, face.studentId)
                } else {
                    face.photoUrl
                }
                if (photoUrl != null && photoUrl != face.photoUrl) {
                    PhotoStorageUtils.cleanupOldPhotos(getApplication(), face.studentId, photoUrl)
                }
                val updatedFace = face.copy(photoUrl = photoUrl, embedding = embeddingCopy)
                faceDao.update(updatedFace)
                FirestoreHelper.syncStudentToFirestore(updatedFace)
                FaceCache.refresh(getApplication())
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Gagal update: ${e.message}") }
            }
        }
    }
}