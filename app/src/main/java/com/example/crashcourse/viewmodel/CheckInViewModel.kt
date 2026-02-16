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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 📊 CheckInViewModel (V.20.0 - Cloud Shield Integrated)
 * Mengelola riwayat absensi secara reaktif dengan sinkronisasi Cloud Shield.
 * Menghubungkan UI Dashboard dengan sistem keamanan Firebase Functions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepo = UserRepository(application)
    private val attendanceRepo = AttendanceRepository(application)

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private var attendanceListener: ListenerRegistration? = null

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory

    // ==========================================
    // 🔐 CONTEXT OBSERVATION
    // ==========================================
    
    private val schoolIdFlow = userRepo.getCurrentUserFlow()
        .map { it?.schoolId }
        .distinctUntilChanged()

    init {
        viewModelScope.launch {
            schoolIdFlow.collect { schoolId ->
                if (!schoolId.isNullOrBlank()) {
                    startSmartSync(schoolId)
                }
            }
        }
    }

    private fun startSmartSync(schoolId: String) {
        attendanceListener?.remove()
        // Menggunakan FirestoreAttendance (Read-Only) yang sudah kita arahkan ke log Cloud
        attendanceListener = FirestoreAttendance.listenToTodayCheckIns(schoolId) { cloudRecords ->
            viewModelScope.launch {
                attendanceRepo.syncAttendance(cloudRecords)
            }
        }
        Log.d("CheckInVM", "🔄 SmartSync active for school: $schoolId")
    }

    // ==========================================
    // 📅 HISTORY SYNC (Manual Pull)
    // ==========================================
    
    fun fetchHistoricalData(startDate: LocalDate, endDate: LocalDate, classNameFilter: String?) {
        val days = ChronoUnit.DAYS.between(startDate, endDate)
        if (days > 31) return 

        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.schoolId ?: return@launch
                
                val startM = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endM = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Memanggil Repository yang sudah terhubung ke collection logs yang sah
                val cloudHistory = attendanceRepo.fetchHistoricalData(sid, startM, endM)
                
                val finalHistory: List<CheckInRecord> = if (!classNameFilter.isNullOrBlank() && classNameFilter != "Semua Kelas") {
                    cloudHistory.filter { it.className.equals(classNameFilter, ignoreCase = true) }
                } else cloudHistory

                attendanceRepo.syncAttendance(finalHistory)

            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ fetchHistoricalData failed", e)
            } finally { 
                _isLoadingHistory.value = false 
            }
        }
    }

    // ==========================================
    // ✍️ CRUD OPERATIONS (Security Integrated)
    // ==========================================
    
    /**
     * saveCheckIn: Sekarang wajib membawa 'distance' dari AI
     */
    fun saveCheckIn(record: CheckInRecord, distance: Float) {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.schoolId ?: return@launch
                // Memanggil repository dengan parameter rawDistance (Stainless Steel Path)
                attendanceRepo.saveAttendance(record, sid, distance)
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ saveCheckIn failed", e)
            }
        }
    }

    fun updateCheckInStatus(record: CheckInRecord, newStatus: String) {
        viewModelScope.launch {
            // Memanggil fungsi update yang sudah kita tambahkan kembali di Repository
            attendanceRepo.updateStatus(record, newStatus)
        }
    }

    fun deleteCheckInRecord(record: CheckInRecord) {
        viewModelScope.launch {
            // Memanggil fungsi delete yang sudah kita tambahkan kembali di Repository
            attendanceRepo.deleteRecord(record)
        }
    }

    // ==========================================
    // 🔍 REAKTIF SEARCH & FILTER
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

        return schoolIdFlow.flatMapLatest { schoolId ->
            if (schoolId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                attendanceRepo.getRecordsBySchoolFlow(schoolId).map { allRecords ->
                    allRecords.filter { record ->
                        // Filter Otoritas
                        val inScope = if (role == Constants.ROLE_ADMIN) true
                        else assignedClasses.any { it.equals(record.className, ignoreCase = true) }
                        
                        if (!inScope) return@filter false

                        // Filter Nama
                        val matchesName = nameFilter.isBlank() || 
                                          record.name.contains(nameFilter, ignoreCase = true) || 
                                          record.studentId.contains(nameFilter)
                        
                        // Filter Tanggal
                        val matchesDate = (startD == null || !record.timestamp.isBefore(startD)) &&
                                          (endD == null || !record.timestamp.isAfter(endD))
                        
                        // Filter Kelas
                        val matchesClass = if (className.isNullOrBlank() || className == "Semua Kelas") true
                                          else record.className.equals(className, ignoreCase = true)

                        matchesName && matchesDate && matchesClass
                    }
                }
            }
        }.flowOn(Dispatchers.Default)
    }

    override fun onCleared() {
        super.onCleared()
        attendanceListener?.remove()
    }
}