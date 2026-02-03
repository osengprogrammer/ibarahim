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

    // --- 🛡️ TEACHER SCOPE LOGIC (FIXED FOR BLINKING) ---

    // Cache untuk menyimpan flow agar tidak dibuat ulang berkali-kali (Mencegah Blinking)
    private var cachedScopedFlow: StateFlow<List<FaceEntity>>? = null
    private var lastUserId: String? = null

    /**
     * 🔥 MENGAMBIL DATA WAJAH YANG SUDAH DIFILTER SESUAI SCOPE GURU
     * Diperbaiki dengan Caching agar UI tidak berkedip (Blinking).
     */
    fun getScopedFaceList(authState: AuthState.Active): StateFlow<List<FaceEntity>> {
        // Jika User ID masih sama, kembalikan cache yang sudah ada
        if (cachedScopedFlow != null && lastUserId == authState.uid) {
            return cachedScopedFlow!!
        }

        lastUserId = authState.uid
        val flow = faceDao.getAllFacesFlow()
            .map { allFaces ->
                Log.d(TAG, "Filtering data for scope: ${authState.assignedClasses}")
                if (authState.role == "ADMIN") {
                    allFaces 
                } else {
                    allFaces.filter { face ->
                        authState.assignedClasses.contains(face.className)
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
        
        cachedScopedFlow = flow
        return flow
    }

    /** Default List */
    val faceList: StateFlow<List<FaceEntity>> =
        faceDao.getAllFacesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- 📝 ATTENDANCE LOGIC ---

    fun saveCheckIn(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val face = faceDao.getFaceByName(name) ?: run {
                    Log.e(TAG, "Could not find face record for $name")
                    return@launch
                }

                val record = CheckInRecord(
                    name = face.name,
                    timestamp = LocalDateTime.now(),
                    faceId = face.id,
                    status = "PRESENT",
                    classId = face.classId,
                    subClassId = face.subClassId,
                    gradeId = face.gradeId,
                    subGradeId = face.subGradeId,
                    programId = face.programId,
                    roleId = face.roleId,
                    className = face.className,
                    gradeName = face.grade 
                )

                checkInRecordDao.insert(record)
                FirestoreHelper.syncAttendanceLog(record)

            } catch (e: Exception) {
                Log.e(TAG, "Error saving check-in: ${e.message}")
            }
        }
    }

    // --- 🛠️ DROPDOWN OPTIONS ---

    val classOptions: StateFlow<List<ClassOption>> = classOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val gradeOptions: StateFlow<List<GradeOption>> = gradeOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val programOptions: StateFlow<List<ProgramOption>> = programOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val roleOptions: StateFlow<List<RoleOption>> = roleOptionDao.getAllOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getSubClassOptions(parentClassId: Int): Flow<List<SubClassOption>> = subClassOptionDao.getOptionsForClass(parentClassId)
    fun getSubGradeOptions(parentGradeId: Int): Flow<List<SubGradeOption>> = subGradeOptionDao.getOptionsForGrade(parentGradeId)

    // --- 👤 FACE REGISTRATION & UPDATES ---

    fun registerFace(
        studentId: String, name: String, embedding: FloatArray, photoUrl: String? = null,
        className: String = "", classId: Int? = null, subClass: String = "", subClassId: Int? = null,
        grade: String = "", gradeId: Int? = null, subGrade: String = "", subGradeId: Int? = null,
        program: String = "", programId: Int? = null, role: String = "", roleId: Int? = null,
        onSuccess: () -> Unit, onDuplicate: (existingName: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val embeddingCopy = embedding.clone()
                val existingFace = faceDao.getFaceByStudentId(studentId)
                if (existingFace != null) {
                    withContext(Dispatchers.Main) { onDuplicate(existingFace.name) }
                    return@launch
                }

                val cached = FaceCache.load(getApplication())
                var bestName: String? = null
                var bestDist = Float.MAX_VALUE
                
                for ((existingName, existingEmb) in cached) {
                    val dist = NativeMath.cosineDistance(existingEmb, embeddingCopy)
                    if (dist < bestDist) {
                        bestDist = dist
                        bestName = existingName
                    }
                }

                if (bestName != null && bestDist <= DUPLICATE_DISTANCE_THRESHOLD) {
                    withContext(Dispatchers.Main) { onDuplicate(bestName) }
                    return@launch
                }

                val face = FaceEntity(
                    studentId = studentId, name = name, photoUrl = photoUrl, embedding = embeddingCopy,
                    className = className, subClass = subClass, grade = grade, subGrade = subGrade,
                    program = program, role = role, classId = classId, subClassId = subClassId,
                    gradeId = gradeId, subGradeId = subGradeId, programId = programId, roleId = roleId,
                    timestamp = System.currentTimeMillis()
                )
                faceDao.insert(face)
                FirestoreHelper.syncStudentToFirestore(face)
                FaceCache.refresh(getApplication())
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                Log.e(TAG, "Registration failed: ${e.message}")
            }
        }
    }

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
                withContext(Dispatchers.Main) { onError("Failed to update face: ${e.message}") }
            }
        }
    }
}