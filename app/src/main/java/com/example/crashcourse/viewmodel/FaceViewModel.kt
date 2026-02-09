package com.example.crashcourse.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.utils.Constants // 🚀 Import Constants
import com.example.crashcourse.utils.FirestoreHelper
import com.example.crashcourse.utils.PhotoStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class FaceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val faceDao = database.faceDao()
    private val checkInRecordDao = database.checkInRecordDao()
    
    // DAOs for Options (Master Data)
    private val classOptionDao = database.classOptionDao()
    private val subClassOptionDao = database.subClassOptionDao()
    private val gradeOptionDao = database.gradeOptionDao()
    private val subGradeOptionDao = database.subGradeOptionDao()
    private val programOptionDao = database.programOptionDao()
    private val roleOptionDao = database.roleOptionDao()

    companion object {
        private const val TAG = "FaceViewModel"
        const val RECOGNITION_DISTANCE_THRESHOLD = 0.75f 
        private const val CHECK_IN_COOLDOWN_MS = 5000L 
    }

    // --- 🛡️ STATE MANAGEMENT ---
    @Volatile private var lastCheckInTime: Long = 0
    @Volatile private var lastCheckInId: String? = null

    // StateFlows
    // 🚀 Update: ditambahkan distinctUntilChanged() untuk efisiensi UI
    val faceList: StateFlow<List<FaceEntity>> = faceDao.getAllFacesFlow()
        .distinctUntilChanged() 
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCheckInRecords: StateFlow<List<CheckInRecord>> = checkInRecordDao.getAllRecords()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- 🛡️ TEACHER SCOPE LOGIC ---
    private var cachedScopedFlow: StateFlow<List<FaceEntity>>? = null
    private var lastUserId: String? = null

    fun getScopedFaceList(authState: AuthState.Active): StateFlow<List<FaceEntity>> {
        if (cachedScopedFlow != null && lastUserId == authState.uid) {
            return cachedScopedFlow!!
        }
        lastUserId = authState.uid
        
        val flow = faceDao.getAllFacesFlow()
            .distinctUntilChanged() // 🚀 Menghindari emisi data yang duplikat
            .map { allFaces ->
                if (authState.role == Constants.ROLE_ADMIN) allFaces 
                else allFaces.filter { face -> authState.assignedClasses.contains(face.className) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        
        cachedScopedFlow = flow
        return flow
    }

    // --- 📝 ATTENDANCE LOGIC ---

    fun saveCheckIn(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val face = faceDao.getFaceByName(name) ?: run {
                    Log.e(TAG, "❌ Gagal: Wajah '$name' tidak ditemukan")
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                if (face.studentId == lastCheckInId && (currentTime - lastCheckInTime < CHECK_IN_COOLDOWN_MS)) {
                    return@launch
                }

                val lastRecordTimestamp = checkInRecordDao.getLastTimestampByStudentId(face.studentId)
                if (lastRecordTimestamp != null && lastRecordTimestamp.isAfter(LocalDateTime.now().minusMinutes(1))) {
                    return@launch
                }

                lastCheckInId = face.studentId
                lastCheckInTime = currentTime

                val record = CheckInRecord(
                    studentId = face.studentId,
                    name = face.name,
                    timestamp = LocalDateTime.now(),
                    faceId = 0, // Menggunakan 0 karena id Int sudah dihapus dari FaceEntity
                    status = Constants.STATUS_PRESENT,
                    classId = face.classId,
                    className = face.className,
                    subClassId = face.subClassId,
                    gradeId = face.gradeId,
                    gradeName = face.grade,
                    subGradeId = face.subGradeId,
                    programId = face.programId,
                    roleId = face.roleId
                )

                checkInRecordDao.insert(record)
                
                if (face.studentId.isNotBlank()) {
                    FirestoreHelper.syncAttendanceLog(record)
                }

            } catch (e: Exception) {
                Log.e(TAG, "🔥 Error saveCheckIn: ${e.message}")
            }
        }
    }

    // --- 👤 FACE REGISTRATION ---

    fun registerFace(
        studentId: String, name: String, embedding: FloatArray, photoUrl: String? = null,
        className: String = "", classId: Int? = null, subClass: String = "", subClassId: Int? = null,
        grade: String = "", gradeId: Int? = null, subGrade: String = "", subGradeId: Int? = null,
        program: String = "", programId: Int? = null, role: String = "", roleId: Int? = null,
        onSuccess: () -> Unit, onDuplicate: (existingName: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (studentId.trim().isEmpty()) return@launch

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

                faceDao.insert(face)
                val isCloudSynced = FirestoreHelper.syncStudentToFirestore(face)
                
                FaceCache.refresh(getApplication())
                
                withContext(Dispatchers.Main) { onSuccess() }
                
                if (isCloudSynced) Log.d(TAG, "✅ Synced to Cloud: $name")
                
            } catch (e: Exception) {
                Log.e(TAG, "🔥 Registrasi Error: ${e.message}")
            }
        }
    }

    // --- 🛠️ MASTER DATA OPTIONS ---
    
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

    // --- ⚙️ MANAGEMENT FUNCTIONS ---

    /**
     * 🔄 SMART SYNC ORCHESTRATOR
     */
    fun syncStudentsWithCloud(authState: AuthState.Active) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🚀 Memulai Sinkronisasi Pintar...")

                val cloudFaces = FirestoreHelper.getScopedStudentsFromFirestore(authState.uid)
                
                if (cloudFaces.isEmpty()) {
                    Log.d(TAG, "☁️ Cloud kosong atau gagal koneksi.")
                    return@launch
                }

                faceDao.insertAll(cloudFaces)
                Log.d(TAG, "📥 Berhasil Upsert ${cloudFaces.size} siswa ke Lokal.")

                val cloudIds = cloudFaces.map { it.studentId }
                faceDao.deleteOrphanedRecords(cloudIds)
                Log.d(TAG, "🧹 Pembersihan data hantu selesai.")

                FaceCache.refresh(getApplication())
                
                Log.d(TAG, "✅ Sinkronisasi Selesai!")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saat sinkronisasi: ${e.message}")
            }
        }
    }

    /**
     * 🚀 DELETE FACE
     */
    fun deleteFace(face: FaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirestoreHelper.deleteStudentFromFirestore(face.studentId)
                faceDao.delete(face)

                face.photoUrl?.let { path ->
                    val file = java.io.File(path)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (deleted) Log.d(TAG, "📁 File foto berhasil dihapus dari storage")
                    }
                }
                
                FaceCache.refresh(getApplication())
                
                Log.d(TAG, "🗑️ Berhasil menghapus wajah: ${face.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gagal menghapus wajah", e)
            }
        }
    }

    fun updateFace(face: FaceEntity, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                faceDao.update(face)
                FirestoreHelper.updateStudentInFirestore(face)
                FaceCache.refresh(getApplication())
                withContext(Dispatchers.Main) { onComplete() }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gagal update profil: ${e.message}")
            }
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
                withContext(Dispatchers.Main) { onError("Gagal update foto: ${e.message}") }
            }
        }
    }
}