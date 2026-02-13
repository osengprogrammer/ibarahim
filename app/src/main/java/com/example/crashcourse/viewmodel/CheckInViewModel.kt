package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.firestore.FirestoreAttendance
import com.example.crashcourse.repository.AttendanceRepository
import com.example.crashcourse.repository.UserRepository
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 📊 CheckInViewModel (V.5.0 - Full Repository Integrated)
 * Mengelola riwayat absensi, sinkronisasi real-time, dan laporan.
 */
class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    // 🔥 Inisialisasi Repository
    private val userRepo = UserRepository(application)
    private val attendanceRepo = AttendanceRepository(application)

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var attendanceListener: ListenerRegistration? = null

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory

    init {
        startSmartSync()
    }

    // ==========================================
    // 1. 🛡️ SMART SYNC (Real-time Cloud -> Local via Repo)
    // ==========================================
    private fun startSmartSync() {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: return@launch

                attendanceListener?.remove()
                attendanceListener = FirestoreAttendance.listenToTodayCheckIns(sid) { cloudRecords ->
                    viewModelScope.launch {
                        // Logika pengecekan duplikasi sekarang ada di dalam Repo
                        attendanceRepo.syncAttendance(cloudRecords)
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckInVM", "SmartSync failed", e)
            }
        }
    }

    // ==========================================
    // 2. 📅 HISTORY SYNC
    // ==========================================
    fun fetchHistoricalData(startDate: LocalDate, endDate: LocalDate, className: String?) {
        val days = ChronoUnit.DAYS.between(startDate, endDate)
        if (days > 31) return // Batasan sync history 1 bulan

        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: return@launch
                
                val startM = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endM = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val cloudHistory = attendanceRepo.fetchHistory(sid, startM, endM)
                
                val finalHistory = if (!className.isNullOrBlank() && className != "Semua Kelas") {
                    cloudHistory.filter { it.className == className }
                } else cloudHistory

                attendanceRepo.syncAttendance(finalHistory)

            } catch (e: Exception) {
                Log.e("CheckInVM", "Error history", e)
            } finally { 
                _isLoadingHistory.value = false 
            }
        }
    }

    // ==========================================
    // 3. ✍️ CRUD OPERATIONS (Panggil Repo)
    // ==========================================
    
    /**
     * 🔥 FIXED: Ditambahkan kembali untuk mengatasi error di CheckInRecordScreen
     */
    fun saveCheckIn(record: CheckInRecord) {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.sekolahId ?: return@launch
                
                // Kirim ke repository untuk simpan lokal + cloud
                attendanceRepo.saveAttendance(record, sid)
                
                Log.d("CheckInVM", "✅ Manual Check-in saved via Repository")
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ Save Failed", e)
            }
        }
    }

    fun updateCheckInStatus(record: CheckInRecord, newStatus: String) {
        viewModelScope.launch {
            attendanceRepo.updateStatus(record, newStatus)
        }
    }

    fun deleteCheckInRecord(record: CheckInRecord) {
        viewModelScope.launch {
            attendanceRepo.deleteRecord(record)
        }
    }

    // ==========================================
    // 4. 🔍 REAKTIF SEARCH & FILTER (UI Logic)
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

        return attendanceRepo.getAllRecordsFlow()
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