package com.example.crashcourse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.utils.FirestoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CheckInViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val checkInRecordDao = database.checkInRecordDao()
    
    // Formatter agar sinkron dengan input dari UI (DatePicker)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // --- CRUD OPERATIONS ---

    fun addManualRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Simpan ke Database Lokal (Room)
            checkInRecordDao.insert(record)
            
            // 2. Kirim ke Cloud (Firestore) agar muncul di HP Orang Tua
            // Karena kita sudah update FirestoreHelper, ini akan mengirim studentId (NIK) juga.
            FirestoreHelper.syncAttendanceLog(record)
        }
    }

    fun updateRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            checkInRecordDao.update(record)
            // Update juga ke Cloud (Optional: Tergantung kebutuhan, tapi aman untuk dipanggil)
            FirestoreHelper.syncAttendanceLog(record)
        }
    }

    fun deleteRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            checkInRecordDao.delete(record)
        }
    }

    // --- 📊 DASHBOARD STATISTICS ---

    fun getAttendanceSummary(records: List<CheckInRecord>): Map<String, Int> {
        val summary = mutableMapOf("PRESENT" to 0, "SAKIT" to 0, "IZIN" to 0, "ALPHA" to 0)
        records.forEach { record ->
            val status = record.status.uppercase()
            if (summary.containsKey(status)) {
                summary[status] = summary.getOrDefault(status, 0) + 1
            }
        }
        return summary
    }

    // --- 🛡️ SMART SCOPED FILTERING ---

    fun getScopedCheckIns(
        authState: AuthState.Active,
        nameFilter: String = "",
        startDate: String = "",
        endDate: String = "",
        className: String? = null,
        gradeId: Int? = null,
        programId: Int? = null
    ): Flow<List<CheckInRecord>> {
        
        // Ambil semua data dari Room, lalu filter di memory (Kotlin)
        // Ini lebih fleksibel daripada query SQL yang rumit untuk filter opsional
        return checkInRecordDao.getAllRecords().map { allRecords ->
            
            // Parsing Tanggal dengan aman
            val startD = if (startDate.isNotBlank()) LocalDate.parse(startDate, dateFormatter).atStartOfDay() else null
            val endD = if (endDate.isNotBlank()) LocalDate.parse(endDate, dateFormatter).atTime(LocalTime.MAX) else null

            allRecords.filter { record ->
                // A. Match Nama
                val matchesName = nameFilter.isEmpty() || record.name.contains(nameFilter, ignoreCase = true)

                // B. Match Tanggal
                val matchesDate = (startD == null || record.timestamp >= startD) && 
                                 (endD == null || record.timestamp <= endD)

                // C. Match Kelas (Normalisasi Spasi: "1 A" == "1A")
                val matchesClass = if (className.isNullOrBlank() || className == "Semua Kelas") true
                else {
                    val target = className.replace(" ", "").uppercase()
                    val current = (record.className ?: "").replace(" ", "").uppercase()
                    target == current
                }

                // D. Match Grade & Program
                val matchesGrade = gradeId == null || record.gradeId == gradeId
                val matchesProgram = programId == null || record.programId == programId

                // E. Role Security (Admin lihat semua, Guru hanya lihat kelas yang ditugaskan)
                val inScope = if (authState.role == "ADMIN") true 
                              else authState.assignedClasses.contains(record.className)

                matchesName && matchesDate && matchesClass && matchesGrade && matchesProgram && inScope
            }
            .sortedByDescending { it.timestamp } // ✅ FITUR BARU: Urutkan dari yang terbaru ke terlama
        }
    }
}