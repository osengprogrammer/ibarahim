package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.utils.Constants
import com.example.crashcourse.utils.FirestoreHelper // ✅ Cukup satu saja
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val checkInRecordDao = database.checkInRecordDao()
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var attendanceListener: ListenerRegistration? = null

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory

    init {
        startSmartSync()
    }

    // ==========================================
    // 1. SMART SYNC ENGINE (Real-time Cloud -> Local)
    // ==========================================
    private fun startSmartSync() {
        Log.d("CheckInVM", "🔄 Memulai Smart Sync Absensi...")
        
        attendanceListener = FirestoreHelper.listenToTodayCheckIns { cloudRecords ->
            viewModelScope.launch(Dispatchers.IO) {
                if (cloudRecords.isEmpty()) return@launch

                val todayStart = LocalDate.now().atStartOfDay()
                val todayEnd = LocalDate.now().atTime(LocalTime.MAX)
                
                // Ambil ID lokal hari ini untuk mencegah duplikasi
                val localRecordsToday = checkInRecordDao.getRecordsBetween(todayStart, todayEnd)
                val localKeys = localRecordsToday.map { "${it.studentId}_${it.timestamp}" }.toSet()

                val newRecords = cloudRecords.filter { cloud ->
                    val uniqueKey = "${cloud.studentId}_${cloud.timestamp}"
                    !localKeys.contains(uniqueKey)
                }
                
                if (newRecords.isNotEmpty()) {
                    checkInRecordDao.insertAll(newRecords)
                    Log.d("CheckInVM", "📥 Batch Sync: ${newRecords.size} data baru dari Cloud")
                }
            }
        }
    }

    // ==========================================
    // 2. ON-DEMAND SYNC (History Downloader)
    // ==========================================
    fun fetchHistoricalData(startDate: LocalDate, endDate: LocalDate, className: String?) {
        val days = ChronoUnit.DAYS.between(startDate, endDate)
        if (days > 30) {
            Log.e("CheckInVM", "❌ Download ditolak: Rentang $days hari (Max 30)")
            return 
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHistory.value = true
            try {
                val cloudHistory = FirestoreHelper.fetchHistoryRecords(startDate, endDate, className)
                
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
                        Log.d("CheckInVM", "✅ Berhasil sync ${distinctNewRecords.size} data history")
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal sync history", e)
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    // ==========================================
    // 3. CRUD OPERATIONS (Local + Cloud Sync)
    // ==========================================

    /**
     * CREATE: Diganti namanya agar seragam dengan UI
     */
    fun saveCheckIn(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Anti-Spam (Cooldown 30 detik)
                val lastTimestamp = checkInRecordDao.getLastTimestampByStudentId(record.studentId)
                if (lastTimestamp != null && lastTimestamp.plusSeconds(30).isAfter(record.timestamp)) {
                    Log.d("CheckInVM", "⏳ Cooldown aktif: ${record.name}")
                    return@launch
                }

                // 1. Simpan Lokal & Dapatkan Row ID
                val rowId = checkInRecordDao.insert(record)

                // 2. Kirim ke Cloud
                val cloudId = FirestoreHelper.syncAttendanceLog(record)
                
                // 3. Update Lokal dengan Firestore ID agar bisa di-update/delete nanti
                if (cloudId != null) {
                    val updatedRecord = record.copy(
                        id = rowId.toInt(), 
                        firestoreId = cloudId
                    )
                    checkInRecordDao.update(updatedRecord)
                }
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal Simpan Absensi", e)
            }
        }
    }

    /**
     * UPDATE: Mengubah status (PRESENT -> SICK, dll)
     */
    fun updateCheckInStatus(record: CheckInRecord, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Update Lokal
                val updatedRecord = record.copy(status = newStatus)
                checkInRecordDao.update(updatedRecord)

                // 2. Update Cloud
                record.firestoreId?.let { docId ->
                    FirestoreHelper.updateAttendanceStatus(docId, newStatus)
                    Log.d("CheckInVM", "☁️ Status Cloud Updated: ${record.name}")
                }
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal Update Status", e)
            }
        }
    }

    /**
     * DELETE: Menghapus data dari Local & Cloud
     */
    fun deleteCheckInRecord(record: CheckInRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Hapus Cloud dulu (Jika gagal, data lokal tetap ada)
                record.firestoreId?.let { docId ->
                    FirestoreHelper.deleteAttendanceLog(docId)
                }
                
                // 2. Hapus Lokal
                checkInRecordDao.delete(record)
                Log.d("CheckInVM", "🗑️ Data Terhapus: ${record.name}")
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Gagal Hapus Data", e)
            }
        }
    }

    // ==========================================
    // 4. READ & FILTER LOGIC
    // ==========================================

    fun getScopedCheckIns(
        role: String, 
        assignedClasses: List<String>,
        nameFilter: String = "",
        startDateStr: String = "",
        endDateStr: String = "",
        className: String? = null
    ): Flow<List<CheckInRecord>> {
        
        // Pre-parsing date strings
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
                    // 🔒 Security Filter
                    val inScope = if (role == Constants.ROLE_ADMIN) true 
                    else assignedClasses.any { it.equals(record.className, ignoreCase = true) }
                    
                    if (!inScope) return@filter false

                    // 🔍 UI Filter
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