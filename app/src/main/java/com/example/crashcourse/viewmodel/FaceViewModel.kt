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
 * 👤 FaceViewModel (FINAL - FIXED)
 *
 * RESPONSIBILITY:
 * - Local Face DB (Room)
 * - Orchestration logic (AI → DB → Firestore)
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
    // 🛡️ SCOPED FACE LIST (UI SAFE)
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
    // 1️⃣ CHECK-IN (AI RESULT → LOCAL → FIRESTORE)
    // ==========================================
    fun saveCheckInByName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Ambil User & Sekolah ID
                val user = userDao.getCurrentUser() ?: return@launch
                val sekolahId = user.sekolahId ?: return@launch

                // 2. Cari data siswa dari DB lokal berdasarkan nama
                val allFaces = faceDao.getFaceByName(name)
                val face = allFaces.firstOrNull { it.sekolahId == sekolahId }
                    ?: return@launch

                // 3. ⏱️ Cek Cooldown (Anti Spam 30 Detik)
                val last = checkInDao.getLastTimestampByStudentId(face.studentId)
                if (last != null && last.isAfter(LocalDateTime.now().minusSeconds(CHECK_IN_COOLDOWN_SEC))) {
                    Log.d(TAG, "⏳ Cooldown active for ${face.name}")
                    return@launch
                }

                // 4. Siapkan Data Record
                val record = CheckInRecord(
                    id = 0,
                    studentId = face.studentId,
                    name = face.name,
                    timestamp = LocalDateTime.now(),
                    status = Constants.STATUS_PRESENT,
                    verified = true,
                    syncStatus = "PENDING",
                    photoPath = "",
                    className = face.className,
                    gradeName = face.grade,
                    role = face.role
                )

                // 5. Simpan ke Room (Local)
                val rowId = checkInDao.insert(record)
                val saved = record.copy(id = rowId.toInt())

                // 6. 🔥 Kirim ke FIRESTORE
                FirestoreAttendance.saveCheckIn(saved, sekolahId)

            } catch (e: Exception) {
                Log.e(TAG, "❌ saveCheckInByName failed", e)
            }
        }
    }

    // ==========================================
    // 2️⃣ REGISTER FACE + ROMBEL
    // ==========================================
    fun registerFaceWithUnit(
        studentId: String,
        name: String,
        embedding: FloatArray,
        unit: MasterClassWithNames,
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

                val face = FaceEntity(
                    studentId = studentId,
                    sekolahId = sekolahId,
                    name = name,
                    photoUrl = photoUrl,
                    embedding = embedding,
                    className = unit.className,
                    grade = unit.gradeName ?: "",
                    role = unit.roleName ?: Constants.ROLE_USER,
                    program = unit.programName ?: "",
                    subClass = unit.subClassName ?: "",
                    subGrade = unit.subGradeName ?: "",
                    timestamp = System.currentTimeMillis()
                )

                faceDao.insert(face)
                FirestoreStudent.uploadStudent(face)
                FaceCache.refresh(getApplication())

                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ registerFaceWithUnit failed", e)
            }
        }
    }

    // ==========================================
    // 3️⃣ UPDATE FACE (Dipakai EditUserScreen)
    // ==========================================
    fun updateFaceWithPhoto(
        originalFace: FaceEntity,
        newName: String,
        newClass: MasterClassWithNames?,
        newPhotoPath: String?,
        newEmbedding: FloatArray?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Buat object copy dengan data baru
                val updatedFace = originalFace.copy(
                    name = newName,
                    // Jika user memilih kelas baru, update semua atributnya
                    className = newClass?.className ?: originalFace.className,
                    grade = newClass?.gradeName ?: originalFace.grade,
                    role = newClass?.roleName ?: originalFace.role,
                    program = newClass?.programName ?: originalFace.program,
                    subClass = newClass?.subClassName ?: originalFace.subClass,
                    subGrade = newClass?.subGradeName ?: originalFace.subGrade,
                    
                    // Update foto & embedding jika ada
                    photoUrl = newPhotoPath ?: originalFace.photoUrl,
                    embedding = newEmbedding ?: originalFace.embedding,
                    timestamp = System.currentTimeMillis()
                )

                // 2. Simpan ke Room (Insert onConflict=Replace akan mengupdate data lama)
                faceDao.insert(updatedFace)

                // 3. Upload ke Firestore
                FirestoreStudent.updateFaceWithPhoto(updatedFace)

                // 4. Refresh Cache agar perubahan langsung terasa
                FaceCache.refresh(getApplication())

                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                Log.e(TAG, "❌ updateFaceWithPhoto failed", e)
            }
        }
    }

    // ==========================================
    // 4️⃣ SYNC STUDENTS (SMART / ROMBEL SAFE)
    // ==========================================
    fun syncStudentsFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sekolahId = user.sekolahId ?: return@launch

                val lastSync = faceDao.getLastSyncTimestamp() ?: 0L

                // ✅ FIX: Use 'fetchSmartSyncStudents' with correct parameters
                val students = FirestoreStudent.fetchSmartSyncStudents(
                    sekolahId = sekolahId,
                    assignedClasses = user.assignedClasses, // Parameter order fixed
                    role = user.role,
                    lastSync = lastSync
                )

                if (students.isNotEmpty()) {
                    faceDao.insertAll(students)
                    FaceCache.refresh(getApplication())
                    Log.d(TAG, "✅ ${students.size} students synced")
                } else {
                    Log.d(TAG, "✅ No new students to sync")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ syncStudentsFromCloud failed", e)
            }
        }
    }

    // ==========================================
    // 5️⃣ DELETE FACE (LOCAL + CLOUD)
    // ==========================================
    fun deleteFace(face: FaceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirestoreStudent.deleteStudent(face.studentId)
                faceDao.delete(face)
                FaceCache.refresh(getApplication())
            } catch (e: Exception) {
                Log.e(TAG, "❌ deleteFace failed", e)
            }
        }
    }
}