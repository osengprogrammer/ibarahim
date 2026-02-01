package com.example.crashcourse.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.*
import com.example.crashcourse.utils.PhotoStorageUtils
import com.example.crashcourse.utils.cosineDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
// Add this at the top with your other imports
import com.example.crashcourse.utils.NativeMath
class FaceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val faceDao = database.faceDao()
    private val checkInRecordDao = database.checkInRecordDao()
    private val classOptionDao = database.classOptionDao()
    private val subClassOptionDao = database.subClassOptionDao()
    private val gradeOptionDao = database.gradeOptionDao()
    private val subGradeOptionDao = database.subGradeOptionDao()
    private val programOptionDao = database.programOptionDao()
    private val roleOptionDao = database.roleOptionDao()

    companion object {
        // distance ≤ 0.3 for duplicate detection during registration
        private const val DUPLICATE_DISTANCE_THRESHOLD = 0.3f
        
        // ✅ FIX: Relaxed from 0.25 to 0.40
        const val RECOGNITION_DISTANCE_THRESHOLD = 0.40f
    }

    /**
     * 🚀 NEW: Records a check-in event in the database.
     * Called by CheckInScreen after successful Blink + Recognition.
     */
    fun saveCheckIn(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch the full face data to get department/role IDs
                val face = faceDao.getFaceByName(name) ?: run {
                    Log.e("ATTENDANCE", "Could not find face record for $name")
                    return@launch
                }

                // 2. Build the CheckInRecord using current timestamp
                val record = CheckInRecord(
                    name = face.name,
                    timestamp = LocalDateTime.now(), // Handled by your Converters
                    faceId = face.id,
                    classId = face.classId,
                    subClassId = face.subClassId,
                    gradeId = face.gradeId,
                    subGradeId = face.subGradeId,
                    programId = face.programId,
                    roleId = face.roleId,
                    className = face.className,
                    gradeName = face.grade
                )

                // 3. Insert into DB
                checkInRecordDao.insert(record)
                Log.d("ATTENDANCE", "Successfully recorded check-in for: ${face.name}")
            } catch (e: Exception) {
                Log.e("ATTENDANCE", "Error saving check-in: ${e.message}")
            }
        }
    }

    /** Exposes a StateFlow of all FaceEntity in the DB */
    val faceList: StateFlow<List<FaceEntity>> =
        faceDao.getAllFacesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Dropdown options as StateFlows
    val classOptions: StateFlow<List<ClassOption>> =
        classOptionDao.getAllOptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val gradeOptions: StateFlow<List<GradeOption>> =
        gradeOptionDao.getAllOptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val programOptions: StateFlow<List<ProgramOption>> =
        programOptionDao.getAllOptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val roleOptions: StateFlow<List<RoleOption>> =
        roleOptionDao.getAllOptions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getSubClassOptions(parentClassId: Int): Flow<List<SubClassOption>> {
        return subClassOptionDao.getOptionsForClass(parentClassId)
    }

    fun getSubGradeOptions(parentGradeId: Int): Flow<List<SubGradeOption>> {
        return subGradeOptionDao.getOptionsForGrade(parentGradeId)
    }

    /** Populates initial dropdown options if they don't exist */
    fun populateInitialOptions() {
        viewModelScope.launch(Dispatchers.IO) {
            if (classOptionDao.getAllOptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(100), emptyList()).value.isEmpty()) {
                val classOptions = listOf(
                    ClassOption(id = 1, name = "Class A", displayOrder = 1),
                    ClassOption(id = 2, name = "Class B", displayOrder = 2),
                    ClassOption(id = 3, name = "Class C", displayOrder = 3)
                )
                classOptionDao.insertAll(classOptions)

                val subClassOptions = listOf(
                    SubClassOption(id = 1, name = "Subclass A1", parentClassId = 1, displayOrder = 1),
                    SubClassOption(id = 2, name = "Subclass A2", parentClassId = 1, displayOrder = 2),
                    SubClassOption(id = 3, name = "Subclass B1", parentClassId = 2, displayOrder = 1),
                    SubClassOption(id = 4, name = "Subclass C1", parentClassId = 3, displayOrder = 1)
                )
                subClassOptionDao.insertAll(subClassOptions)

                val gradeOptions = listOf(
                    GradeOption(id = 1, name = "Grade 1", displayOrder = 1),
                    GradeOption(id = 2, name = "Grade 2", displayOrder = 2),
                    GradeOption(id = 3, name = "Grade 3", displayOrder = 3)
                )
                gradeOptionDao.insertAll(gradeOptions)

                val subGradeOptions = listOf(
                    SubGradeOption(id = 1, name = "Section A", parentGradeId = 1, displayOrder = 1),
                    SubGradeOption(id = 2, name = "Section B", parentGradeId = 1, displayOrder = 2),
                    SubGradeOption(id = 3, name = "Section A", parentGradeId = 2, displayOrder = 1)
                )
                subGradeOptionDao.insertAll(subGradeOptions)

                val programOptions = listOf(
                    ProgramOption(id = 1, name = "Regular", displayOrder = 1),
                    ProgramOption(id = 2, name = "Special", displayOrder = 2),
                    ProgramOption(id = 3, name = "Advanced", displayOrder = 3)
                )
                programOptionDao.insertAll(programOptions)

                val roleOptions = listOf(
                    RoleOption(id = 1, name = "Student", displayOrder = 1),
                    RoleOption(id = 2, name = "Teacher", displayOrder = 2),
                    RoleOption(id = 3, name = "Staff", displayOrder = 3),
                    RoleOption(id = 4, name = "Admin", displayOrder = 4)
                )
                roleOptionDao.insertAll(roleOptions)
            }
        }
    }

    fun registerFace(
        studentId: String,
        name: String,
        embedding: FloatArray,
        photoUrl: String? = null,
        className: String = "",
        subClass: String = "",
        grade: String = "",
        subGrade: String = "",
        program: String = "",
        role: String = "",
        onSuccess: () -> Unit,
        onDuplicate: (existingName: String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val embeddingCopy = embedding.clone()

            val existingFace = faceDao.getFaceByStudentId(studentId)
            if (existingFace != null) {
                withContext(Dispatchers.Main) {
                    onDuplicate(existingFace.name)
                }
                return@launch
            }

            val cached = FaceCache.load(getApplication())
            var bestName: String? = null
            var bestDist = Float.MAX_VALUE
            
            for ((existingName, existingEmb) in cached) {
                val dist = NativeMath.cosineDistance(existingEmb, embeddingCopy)
                // val dist = cosineDistance(existingEmb, embeddingCopy)
                if (dist < bestDist) {
                    bestDist = dist
                    bestName = existingName
                }
            }

            if (bestName != null && bestDist <= DUPLICATE_DISTANCE_THRESHOLD) {
                withContext(Dispatchers.Main) {
                    onDuplicate(bestName)
                }
                return@launch
            }

            val face = FaceEntity(
                studentId = studentId,
                name = name,
                photoUrl = photoUrl,
                embedding = embeddingCopy,
                className = className,
                subClass = subClass,
                grade = grade,
                subGrade = subGrade,
                program = program,
                role = role,
                timestamp = System.currentTimeMillis()
            )
            faceDao.insert(face)
            FaceCache.refresh(getApplication())
            withContext(Dispatchers.Main) {
                onSuccess()
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
            FaceCache.refresh(getApplication())
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun updateFaceWithPhoto(
        face: FaceEntity,
        photoBitmap: Bitmap?,
        embedding: FloatArray,
        onComplete: () -> Unit,
        onError: (String) -> Unit
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

                val updatedFace = face.copy(
                    photoUrl = photoUrl,
                    embedding = embeddingCopy
                )

                faceDao.update(updatedFace)
                FaceCache.refresh(getApplication())

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Failed to update face: ${e.message}")
                }
            }
        }
    }

    fun registerFace(
        name: String,
        embedding: FloatArray,
        onSuccess: () -> Unit,
        onDuplicate: (existingName: String) -> Unit
    ) {
        val studentId = "STU" + System.currentTimeMillis().toString()
        registerFace(
            studentId = studentId,
            name = name,
            embedding = embedding,
            onSuccess = onSuccess,
            onDuplicate = onDuplicate
        )
    }
}