package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.FirestoreAttendance // ✅ NEW IMPORT
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

/**
 * 📊 CheckInViewModel (FINAL - MERGED & FIXED)
 * Mengelola sinkronisasi log kehadiran real-time, riwayat, dan CRUD dengan FirestoreAttendance.
 */
class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val checkInRecordDao = database.checkInRecordDao()
    private val userDao = database.userDao()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var attendanceListener: ListenerRegistration? = null

    // State Loading untuk UI
    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory

    init {
        // Otomatis memulai sinkronisasi real-time saat aplikasi dibuka
        startSmartSync()
    }

    // ==========================================
    // 1. 🛡️ SMART SYNC ENGINE (Cloud -> Local)
    // ==========================================

    private fun startSmartSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getCurrentUser() ?: return@launch
            val sekolahId = user.sekolahId ?: ""

            if (sekolahId.isBlank()) {
                Log.e("CheckInVM", "❌ Sync Aborted: SekolahId tidak ditemukan.")
                return@launch
            }

            Log.d("CheckInVM", "🔄 Real-time Sync Active: $sekolahId")

            withContext(Dispatchers.Main) {
                attendanceListener?.remove()

                // ✅ GANTI: FirestoreHelper -> FirestoreAttendance
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
                            Log.d("CheckInVM", "📥 Berhasil import ${newRecords.size} log baru dari Cloud.")
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 2. 📅 HISTORY DOWNLOADER
    // ==========================================

    fun fetchHistoricalData(startDate: LocalDate, endDate: LocalDate, className: String?) {
        val days = ChronoUnit.DAYS.between(startDate, endDate)
        if (days > 31) {
            Log.e("CheckInVM", "❌ Range maksimal 31 hari.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHistory.value = true
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: ""

                val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // ✅ GANTI: FirestoreHelper -> FirestoreAttendance
                var cloudHistory = FirestoreAttendance.fetchHistoryRecords(
                    sekolahId = sid,
                    startMillis = startMillis,
                    endMillis = endMillis
                )

                if (!className.isNullOrBlank() && className != "Semua Kelas") {
                    cloudHistory = cloudHistory.filter { it.className == className }
                }

                if (cloudHistory.isNotEmpty()) {
                    val startDt = startDate.atStartOfDay()
                    val endDt = endDate.atTime(LocalTime.MAX)
                    val localRecords = checkInRecordDao.getRecordsBetween(startDt, endDt)
                    val localKeys = localRecords.map { "${it.studentId}_${it.timestamp}" }.toSet()

                    val distinctNewRecords = cloudHistory.filter {
                        !localKeys.contains("${it.studentId}_${it.timestamp}")
                    }

                    if (distinctNewRecords.isNotEmpty()) {
                        checkInRecordDao.insertAll(distinctNewRecords)
                        Log.d("CheckInVM", "✅ Sinkronisasi riwayat selesai: ${distinctNewRecords.size} data.")
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal tarik riwayat", e)
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    // ==========================================
    // 3. ✍️ CRUD OPERATIONS (Sync Both Ways)
    // ==========================================

    fun saveCheckIn(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: ""

                // 1. Simpan Lokal (Dapatkan Row ID)
                val rowId = checkInRecordDao.insert(record)
                val savedRecord = record.copy(id = rowId.toInt())

                // 2. Kirim ke Cloud
                // ✅ GANTI: FirestoreHelper -> FirestoreAttendance
                val cloudId = FirestoreAttendance.saveCheckIn(savedRecord, sid)

                // 3. Update lokal dengan Firestore ID
                if (cloudId != null) {
                    val finalRecord = savedRecord.copy(firestoreId = cloudId, syncStatus = "SYNCED")
                    checkInRecordDao.update(finalRecord)
                }
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal simpan: ${e.message}")
            }
        }
    }

    fun updateCheckInStatus(record: CheckInRecord, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update Lokal
                checkInRecordDao.updateStatus(record.studentId, record.timestamp, newStatus)

                // Update Cloud
                // ✅ LOGIKA ID YANG KONSISTEN
                val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "").take(12)
                val docId = "${record.studentId}_$timeKey"
                
                // ✅ GANTI: FirestoreHelper -> FirestoreAttendance
                FirestoreAttendance.updateAttendanceStatus(docId, newStatus)

            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal update status", e)
            }
        }
    }

    fun deleteCheckInRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Hapus Cloud
                val timeKey = record.timestamp.toString().replace(Regex("[^0-9]"), "").take(12)
                val docId = "${record.studentId}_$timeKey"

                // ✅ GANTI: FirestoreHelper -> FirestoreAttendance
                FirestoreAttendance.deleteAttendanceLog(docId)

                // Hapus Lokal
                checkInRecordDao.delete(record)
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal hapus", e)
            }
        }
    }

    // ==========================================
    // 4. 🔍 REAKTIF SEARCH & FILTER (UI Flow)
    // ==========================================

    fun getScopedCheckIns(
        role: String,
        assignedClasses: List<String>,
        nameFilter: String = "",
        startDateStr: String = "",
        endDateStr: String = "",
        className: String? = null
    ): Flow<List<CheckInRecord>> {

        val startD = try {
            if (startDateStr.isNotBlank()) LocalDate.parse(startDateStr, dateFormatter).atStartOfDay() else null
        } catch (e: Exception) { null }

        val endD = try {
            if (endDateStr.isNotBlank()) LocalDate.parse(endDateStr, dateFormatter).atTime(LocalTime.MAX) else null
        } catch (e: Exception) { null }

        return checkInRecordDao.getAllRecords()
            .distinctUntilChanged()
            .map { allRecords ->
                allRecords.filter { record ->
                    // 🛡️ SECURITY SCOPING
                    val inScope = if (role == Constants.ROLE_ADMIN) true
                    else assignedClasses.any { it.equals(record.className, ignoreCase = true) }

                    if (!inScope) return@filter false

                    // UI Filtering
                    val matchesName = nameFilter.isBlank() || record.name.contains(nameFilter, ignoreCase = true)
                    
                    val matchesDate = (startD == null || !record.timestamp.isBefore(startD)) &&
                                      (endD == null || !record.timestamp.isAfter(endD))

                    val matchesClass = if (className.isNullOrBlank() || className == "Semua Kelas") true
                                      else record.className?.equals(className, ignoreCase = true) ?: false

                    matchesName && matchesDate && matchesClass
                }
                .sortedByDescending { it.timestamp }
            }
            .flowOn(Dispatchers.Default)
    }

    override fun onCleared() {
        super.onCleared()
        attendanceListener?.remove()
    }
}