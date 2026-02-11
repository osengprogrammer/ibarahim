package com.example.crashcourse.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.db.AppDatabase
import com.example.crashcourse.firestore.dashboard.FirestoreDashboard // ✅ NEW IMPORT
import com.example.crashcourse.firestore.dashboard.LiveLog // ✅ NEW IMPORT
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 📊 Azura Tech Dashboard ViewModel
 * Manages real-time monitoring and attendance statistics.
 */

// UI State for Stats
data class AttendanceStats(
    val total: Int = 0,
    val present: Int = 0,
    val sick: Int = 0,
    val permit: Int = 0,
    val alpha: Int = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userDao = AppDatabase.getInstance(application).userDao()
    private val TAG = "DashboardVM"

    private val _stats = MutableStateFlow(AttendanceStats())
    val stats = _stats.asStateFlow()

    private val _logs = MutableStateFlow<List<LiveLog>>(emptyList())
    val logs = _logs.asStateFlow()

    private var liveListener: ListenerRegistration? = null

    init {
        initDashboardSession()
    }

    private fun initDashboardSession() {
        viewModelScope.launch {
            userDao.getCurrentUserFlow().collect { user ->
                if (user != null) {
                    val sid = user.sekolahId
                    Log.d(TAG, "🛡️ Initializing Monitor for School: $sid")
                    listenToLiveAttendance(sid ?: "") 
                } else {
                    stopListening()
                }
            }
        }
    }

    /**
     * 🕵️ REAL-TIME LISTENER
     * Delegates query logic to FirestoreDashboard and handles Stats Calculation here.
     */
    private fun listenToLiveAttendance(sekolahId: String) {
        if (sekolahId.isBlank()) return
        
        stopListening()

        // ✅ Call Repository
        liveListener = FirestoreDashboard.listenToLiveToday(sekolahId) { newLogs ->
            
            // 🧮 Calculate Stats in ViewModel (Business Logic)
            var p = 0; var s = 0; var i = 0; var a = 0

            newLogs.forEach { log ->
                when (log.status) {
                    "PRESENT", "LATE" -> p++
                    "SAKIT", "SICK" -> s++
                    "IZIN", "PERMIT" -> i++
                    else -> a++
                }
            }

            // Update State
            _stats.value = AttendanceStats(
                total = newLogs.size,
                present = p, sick = s, permit = i, alpha = a
            )
            _logs.value = newLogs
        }
    }

    private fun stopListening() {
        liveListener?.remove()
        liveListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}