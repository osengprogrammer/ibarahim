package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.firestore.FirestoreAttendance
import com.example.crashcourse.firestore.student.FirestoreStudent
import com.example.crashcourse.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * 👤 FaceViewModel (V.3 - Many-to-Many Ready)
 * Mengelola pendaftaran biometrik dan proses check-in berbasis sesi mata kuliah.
 */
class FaceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val faceDao = db.faceDao()
    private val userDao = db.userDao()
    private val checkInDao = db.checkInRecordDao()

    companion object {
        private const val TAG = "FaceViewModel"
        private const val CHECK_IN_COOLDOWN_SEC = 30L
    }

    // ==========================================
    // 🔐 SESSION CONTEXT
    // ==========================================
    private val sekolahIdFlow = userDao
        .getCurrentUserFlow()
        .map { it?.sekolahId }
        .distinctUntilChanged()

    // ==========================================
    // 🛡️ SCOPED FACE LIST
    // ==========================================
    val faceList: StateFlow<List<FaceEntity>> =
        combine(sekolahIdFlow, faceDao.getAllFacesFlow()) { sid, faces ->
            if (sid.isNullOrBlank()) emptyList()
            else faces.filter { it.sekolahId == sid }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    // ==========================================
    // 1️⃣ CHECK-IN (DENGAN KONTEKS SESI MATKUL)
    // ==========================================
    /**
     * Menyimpan data kehadiran berdasarkan hasil deteksi AI dan Sesi yang dipilih Dosen.
     * @param name Nama hasil deteksi AI Scanner.
     * @param activeSession Nama mata kuliah yang dipilih di UI (KRS/Sesi Aktif).
     */
    fun saveCheckInWithSession(name: String, activeSession: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sekolahId = user.sekolahId ?: return@launch

                // Cari data biometrik lokal
                val allFaces = faceDao.getFaceByName(name)
                val face = allFaces.firstOrNull { it.sekolahId == sekolahId }
                    ?: return@launch

                // ⏱️ Anti-Spam Berdasarkan Sesi
                // Menggunakan fungsi getLastRecordForClass di DAO agar cooldown terikat pada matkul tertentu
                val lastRecord = checkInDao.getLastRecordForClass(face.studentId, activeSession)
                if (lastRecord != null && lastRecord.timestamp.isAfter(LocalDateTime.now().minusSeconds(CHECK_IN_COOLDOWN_SEC))) {
                    Log.d(TAG, "⏳ Cooldown active for ${face.name} in session $activeSession")
                    return@launch
                }

                val record = CheckInRecord(
                    id = 0,
                    studentId = face.studentId,
                    name = face.name,
                    timestamp = LocalDateTime.now(),
                    status = Constants.STATUS_PRESENT,
                    verified = true,
                    syncStatus = "PENDING",
                    photoPath = "",
                    className = activeSession, // Menyimpan sesi spesifik, bukan CSV
                    gradeName = face.grade,
                    role = face.role
                )

                // Simpan ke Lokal
                val rowId = checkInDao.insert(record)
                
                // 🔥 Kirim ke Cloud (Firestore)
                FirestoreAttendance.saveCheckIn(record.copy(id = rowId.toInt()), sekolahId)

            } catch (e: Exception) {
                Log.e(TAG, "❌ saveCheckInWithSession failed", e)
            }
        }
    }

    // ==========================================
    // 2️⃣ REGISTER FACE + MULTI ROMBEL
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sekolahId = user.sekolahId ?: return@launch

                if (faceDao.getFaceByStudentId(studentId) != null) {
                    withContext(Dispatchers.Main) { onDuplicate(studentId) }
                    return@launch
                }

                val combinedClassName = units.joinToString(", ") { it.className }
                val primaryUnit = units.firstOrNull() 

                val face = FaceEntity(
                    studentId = studentId,
                    sekolahId = sekolahId,
                    name = name,
                    photoUrl = photoUrl,
                    embedding = embedding,
                    className = combinedClassName,
                    grade = primaryUnit?.gradeName ?: "",
                    role = primaryUnit?.roleName ?: Constants.ROLE_USER,
                    program = primaryUnit?.programName ?: "",
                    subClass = primaryUnit?.subClassName ?: "",
                    subGrade = primaryUnit?.subGradeName ?: "",
                    timestamp = System.currentTimeMillis()
                )

                faceDao.insert(face)
                FirestoreStudent.uploadStudent(face)
                FaceCache.refresh(getApplication())

                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ registerFaceWithMultiUnit failed", e)
            }
        }
    }

    // ==========================================
    // 3️⃣ UPDATE FACE + MULTI ROMBEL
    // ==========================================
    fun updateFaceWithMultiUnit(
        originalFace: FaceEntity,
        newName: String,
        newUnits: List<MasterClassWithNames>,
        newPhotoPath: String?,
        newEmbedding: FloatArray?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val combinedClassName = newUnits.joinToString(", ") { it.className }
                val primaryUnit = newUnits.firstOrNull()

                val updatedFace = originalFace.copy(
                    name = newName,
                    className = if (newUnits.isNotEmpty()) combinedClassName else originalFace.className,
                    grade = primaryUnit?.gradeName ?: originalFace.grade,
                    role = primaryUnit?.roleName ?: originalFace.role,
                    program = primaryUnit?.programName ?: originalFace.program,
                    subClass = primaryUnit?.subClassName ?: originalFace.subClass,
                    subGrade = primaryUnit?.subGradeName ?: originalFace.subGrade,
                    photoUrl = newPhotoPath ?: originalFace.photoUrl,
                    embedding = newEmbedding ?: originalFace.embedding,
                    timestamp = System.currentTimeMillis()
                )

                faceDao.insert(updatedFace)
                FirestoreStudent.updateFaceWithPhoto(updatedFace)
                FaceCache.refresh(getApplication())

                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ updateFaceWithMultiUnit failed", e)
            }
        }
    }

    // ==========================================
    // 4️⃣ SYNC & DELETE
    // ==========================================
    fun syncStudentsFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sekolahId = user.sekolahId ?: return@launch
                val lastSync = faceDao.getLastSyncTimestamp() ?: 0L

                val students = FirestoreStudent.fetchSmartSyncStudents(
                    sekolahId = sekolahId,
                    assignedClasses = user.assignedClasses,
                    role = user.role,
                    lastSync = lastSync
                )

                if (students.isNotEmpty()) {
                    faceDao.insertAll(students)
                    FaceCache.refresh(getApplication())
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ sync failed", e)
            }
        }
    }

    fun deleteFace(face: FaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirestoreStudent.deleteStudent(face.studentId)
                faceDao.delete(face)
                FaceCache.refresh(getApplication())
            } catch (e: Exception) {
                Log.e(TAG, "❌ delete failed", e)
            }
        }
    }
}