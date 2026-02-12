package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.FirestoreAttendance 
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val checkInRecordDao = database.checkInRecordDao()
    private val userDao = database.userDao()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var attendanceListener: ListenerRegistration? = null

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory

    init {
        startSmartSync()
    }

    // ==========================================
    // 1. 🛡️ SMART SYNC (Real-time Cloud -> Local)
    // ==========================================
    private fun startSmartSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getCurrentUser() ?: return@launch
            val sekolahId = user.sekolahId ?: ""
            if (sekolahId.isBlank()) return@launch

            withContext(Dispatchers.Main) {
                attendanceListener?.remove()
                attendanceListener = FirestoreAttendance.listenToTodayCheckIns(sekolahId) { cloudRecords ->
                    viewModelScope.launch(Dispatchers.IO) {
                        if (cloudRecords.isEmpty()) return@launch
                        val todayStart = LocalDate.now().atStartOfDay()
                        val todayEnd = LocalDate.now().atTime(LocalTime.MAX)

                        val localRecordsToday = checkInRecordDao.getRecordsBetween(todayStart, todayEnd)
                        val localKeys = localRecordsToday.map { "${it.studentId}_${it.timestamp}" }.toSet()

                        val newRecords = cloudRecords.filter { cloud ->
                            val uniqueKey = "${cloud.studentId}_${cloud.timestamp}"
                            !localKeys.contains(uniqueKey)
                        }

                        if (newRecords.isNotEmpty()) {
                            checkInRecordDao.insertAll(newRecords)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 2. 📅 HISTORY SYNC
    // ==========================================
    fun fetchHistoricalData(startDate: LocalDate, endDate: LocalDate, className: String?) {
        val days = ChronoUnit.DAYS.between(startDate, endDate)
        if (days > 31) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHistory.value = true
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: ""
                val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                var cloudHistory = FirestoreAttendance.fetchHistoryRecords(sid, startMillis, endMillis)
                if (!className.isNullOrBlank() && className != "Semua Kelas") {
                    cloudHistory = cloudHistory.filter { it.className == className }
                }

                if (cloudHistory.isNotEmpty()) {
                    val localRecords = checkInRecordDao.getRecordsBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX))
                    val localKeys = localRecords.map { "${it.studentId}_${it.timestamp}" }.toSet()
                    val distinctNewRecords = cloudHistory.filter { !localKeys.contains("${it.studentId}_${it.timestamp}") }
                    if (distinctNewRecords.isNotEmpty()) checkInRecordDao.insertAll(distinctNewRecords)
                }
            } catch (e: Exception) {
                Log.e("CheckInVM", "Error history", e)
            } finally { _isLoadingHistory.value = false }
        }
    }

    // ==========================================
    // 3. ✍️ CRUD OPERATIONS (FIXED & ADDED)
    // ==========================================
    
    fun saveCheckIn(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: ""
                val rowId = checkInRecordDao.insert(record)
                val savedRecord = record.copy(id = rowId.toInt())
                val cloudId = FirestoreAttendance.saveCheckIn(savedRecord, sid)
                if (cloudId != null) {
                    checkInRecordDao.update(savedRecord.copy(firestoreId = cloudId, syncStatus = "SYNCED"))
                }
            } catch (e: Exception) { Log.e("CheckInVM", "Save Failed", e) }
        }
    }

    /**
     * 🔥 ADDED: Fungsi untuk mengubah status (PRESENT/SAKIT/IZIN)
     */
    fun updateCheckInStatus(record: CheckInRecord, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Update Lokal
                checkInRecordDao.updateStatus(record.studentId, record.timestamp, newStatus)

                // 2. Update Cloud (Gunakan ID dokumen yang konsisten)
                val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "").take(12)
                val docId = "${record.studentId}_$timeKey"
                FirestoreAttendance.updateAttendanceStatus(docId, newStatus)
            } catch (e: Exception) {
                Log.e("CheckInVM", "Update failed", e)
            }
        }
    }

    /**
     * 🔥 ADDED: Fungsi untuk menghapus log
     */
    fun deleteCheckInRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Hapus Cloud
                val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "").take(12)
                val docId = "${record.studentId}_$timeKey"
                FirestoreAttendance.deleteAttendanceLog(docId)

                // 2. Hapus Lokal
                checkInRecordDao.delete(record)
            } catch (e: Exception) {
                Log.e("CheckInVM", "Delete failed", e)
            }
        }
    }

    // ==========================================
    // 4. 🔍 REAKTIF SEARCH & FILTER
    // ==========================================
    fun getScopedCheckIns(
        role: String,
        assignedClasses: List<String>,
        nameFilter: String = "",
        startDateStr: String = "",
        endDateStr: String = "",
        className: String? = null
    ): Flow<List<CheckInRecord>> {

        val startD = try { if (startDateStr.isNotBlank()) LocalDate.parse(startDateStr, dateFormatter).atStartOfDay() else null } catch (e: Exception) { null }
        val endD = try { if (endDateStr.isNotBlank()) LocalDate.parse(endDateStr, dateFormatter).atTime(LocalTime.MAX) else null } catch (e: Exception) { null }

        return checkInRecordDao.getAllRecordsFlow()
            .map { allRecords ->
                allRecords.filter { record ->
                    val inScope = if (role == Constants.ROLE_ADMIN) true
                    else assignedClasses.any { it.equals(record.className, ignoreCase = true) }
                    if (!inScope) return@filter false

                    val matchesName = nameFilter.isBlank() || record.name.contains(nameFilter, ignoreCase = true)
                    val matchesDate = (startD == null || !record.timestamp.isBefore(startD)) &&
                                      (endD == null || !record.timestamp.isAfter(endD))
                    val matchesClass = if (className.isNullOrBlank() || className == "Semua Kelas") true
                                      else record.className.equals(className, ignoreCase = true)

                    matchesName && matchesDate && matchesClass
                }.sortedByDescending { it.timestamp }
            }.flowOn(Dispatchers.Default)
    }

    override fun onCleared() {
        super.onCleared()
        attendanceListener?.remove()
    }
}