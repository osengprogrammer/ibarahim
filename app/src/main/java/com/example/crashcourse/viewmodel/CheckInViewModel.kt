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
 * 📊 CheckInViewModel (V.7.1 - Build Success Version)
 * Mengelola riwayat absensi secara reaktif dengan sinkronisasi Multi-Tenant.
 * Menghubungkan UI Dashboard dengan Data Lokal (Room) & Cloud (Firestore).
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
    
    // Aliran reaktif memantau ID Sekolah yang sedang aktif
    private val schoolIdFlow = userRepo.getCurrentUserFlow()
        .map { it?.schoolId }
        .distinctUntilChanged()

    init {
        // Otomatis memulai sinkronisasi saat Sesi Sekolah terdeteksi
        viewModelScope.launch {
            schoolIdFlow.collect { schoolId ->
                if (!schoolId.isNullOrBlank()) {
                    startSmartSync(schoolId)
                }
            }
        }
    }

    // ==========================================
    // 1. 🛡️ SMART SYNC (Real-time Cloud -> Local)
    // ==========================================
    
    private fun startSmartSync(schoolId: String) {
        attendanceListener?.remove()
        attendanceListener = FirestoreAttendance.listenToTodayCheckIns(schoolId) { cloudRecords ->
            viewModelScope.launch {
                // SINKRON: Mengirim List<CheckInRecord> ke Repository
                attendanceRepo.syncAttendance(cloudRecords)
            }
        }
        Log.d("CheckInVM", "🔄 SmartSync active for school: $schoolId")
    }

    // ==========================================
    // 2. 📅 HISTORY SYNC (Manual Pull)
    // ==========================================
    
    fun fetchHistoricalData(startDate: LocalDate, endDate: LocalDate, classNameFilter: String?) {
        val days = ChronoUnit.DAYS.between(startDate, endDate)
        if (days > 31) return // Limitasi penarikan data 1 bulan untuk performa

        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.schoolId ?: return@launch
                
                val startM = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endM = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // 🔥 FIXED: Menggunakan fetchHistoricalData sesuai update di AttendanceRepository
                val cloudHistory = attendanceRepo.fetchHistoricalData(sid, startM, endM)
                
                // Filter lokal berdasarkan kelas jika diperlukan
                val finalHistory: List<CheckInRecord> = if (!classNameFilter.isNullOrBlank() && classNameFilter != "Semua Kelas") {
                    cloudHistory.filter { it.className.equals(classNameFilter, ignoreCase = true) }
                } else cloudHistory

                // Simpan ke database lokal (Room) agar UI terupdate secara otomatis
                attendanceRepo.syncAttendance(finalHistory)

            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ fetchHistoricalData failed", e)
            } finally { 
                _isLoadingHistory.value = false 
            }
        }
    }

    // ==========================================
    // 3. ✍️ CRUD OPERATIONS
    // ==========================================
    
    fun saveCheckIn(record: CheckInRecord) {
        viewModelScope.launch {
            try {
                val user = userRepo.getCurrentUser() ?: return@launch
                val sid = user.schoolId ?: return@launch
                attendanceRepo.saveAttendance(record, sid)
            } catch (e: Exception) {
                Log.e("CheckInVM", "❌ saveCheckIn failed", e)
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
    // 4. 🔍 REAKTIF SEARCH & FILTER (The Engine)
    // ==========================================
    
    /**
     * Mesin pencari reaktif yang menggabungkan filter Nama, Tanggal, dan Otoritas Kelas.
     */
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
                // Menarik data dari Room berdasarkan sekolah yang aktif
                attendanceRepo.getRecordsBySchoolFlow(schoolId).map { allRecords ->
                    allRecords.filter { record ->
                        // 1. Filter Otoritas (Guru hanya lihat kelasnya, Admin lihat semua)
                        val inScope = if (role == Constants.ROLE_ADMIN) true
                        else assignedClasses.any { it.equals(record.className, ignoreCase = true) }
                        
                        if (!inScope) return@filter false

                        // 2. Filter Nama / ID Siswa
                        val matchesName = nameFilter.isBlank() || 
                                          record.name.contains(nameFilter, ignoreCase = true) || 
                                          record.studentId.contains(nameFilter)
                        
                        // 3. Filter Rentang Tanggal
                        val matchesDate = (startD == null || !record.timestamp.isBefore(startD)) &&
                                          (endD == null || !record.timestamp.isAfter(endD))
                        
                        // 4. Filter Dropdown Kelas
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