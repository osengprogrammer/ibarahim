package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.utils.ExportUtils
import com.example.crashcourse.utils.FirestoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CheckInViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val checkInRecordDao = database.checkInRecordDao()

    // --- CRUD OPERATIONS WITH CLOUD SYNC ---

    fun addManualRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            checkInRecordDao.insert(record)
            FirestoreHelper.syncAttendanceLog(record)
        }
    }

    fun updateRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            checkInRecordDao.update(record)
            FirestoreHelper.syncAttendanceLog(record)
        }
    }

    fun deleteRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            checkInRecordDao.delete(record)
        }
    }

    // --- 📄 EXPORT FEATURES (SCOPED) ---

    /**
     * Memastikan PDF yang diekspor mematuhi hak akses Guru
     */
    fun exportToPdf(records: List<CheckInRecord>, authState: AuthState.Active): File {
        val filtered = if (authState.role == "ADMIN") records 
                       else records.filter { authState.assignedClasses.contains(it.className) }
        return ExportUtils.exportToPdf(getApplication(), filtered)
    }
    
    fun exportToCsv(records: List<CheckInRecord>, authState: AuthState.Active): File {
        val filtered = if (authState.role == "ADMIN") records 
                       else records.filter { authState.assignedClasses.contains(it.className) }
        return ExportUtils.exportToCsv(getApplication(), filtered)
    }

    // --- 🛡️ SCOPED FILTERING LOGIC ---

    /**
     * 🔥 MENGAMBIL REKAP ABSENSI DENGAN FILTER SCOPE ADMIN/GURU
     * Menggunakan assignedClasses yang sudah kita tambahkan di AuthState.Active
     */
    fun getScopedCheckIns(
        authState: AuthState.Active,
        nameFilter: String = "",
        startDate: String = "",
        endDate: String = "",
        classId: Int? = null,
        subClassId: Int? = null,
        gradeId: Int? = null,
        subGradeId: Int? = null,
        programId: Int? = null,
        roleId: Int? = null
    ): Flow<List<CheckInRecord>> {
        
        return getFilteredCheckIns(
            nameFilter, startDate, endDate, classId, subClassId, gradeId, subGradeId, programId, roleId
        ).map { records ->
            if (authState.role == "ADMIN") {
                records 
            } else {
                // Pastikan menggunakan authState.assignedClasses sesuai data class di AuthViewModel
                records.filter { record ->
                    authState.assignedClasses.contains(record.className)
                }
            }
        }
    }

    // --- BASE FILTERING LOGIC ---

    private fun getFilteredCheckIns(
        nameFilter: String = "",
        startDate: String = "",
        endDate: String = "",
        classId: Int? = null,
        subClassId: Int? = null,
        gradeId: Int? = null,
        subGradeId: Int? = null,
        programId: Int? = null,
        roleId: Int? = null
    ): Flow<List<CheckInRecord>> {
        val parsedStartDate = if (startDate.isNotBlank()) {
            try {
                LocalDateTime.parse("$startDate 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            } catch (e: Exception) { null }
        } else null

        val parsedEndDate = if (endDate.isNotBlank()) {
            try {
                LocalDateTime.parse("$endDate 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            } catch (e: Exception) { null }
        } else null

        return checkInRecordDao.getFilteredRecords(
            nameFilter = nameFilter,
            startDate = parsedStartDate,
            endDate = parsedEndDate,
            classId = classId,
            subClassId = subClassId,
            gradeId = gradeId,
            subGradeId = subGradeId,
            programId = programId,
            roleId = roleId
        )
    }
}