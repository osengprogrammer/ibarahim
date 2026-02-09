package com.example.crashcourse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crashcourse.utils.Constants // 🚀 Import Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Sesuaikan nama class dengan UI (AttendanceStats)
data class AttendanceStats(
    val total: Int = 0,
    val present: Int = 0,
    val sick: Int = 0,
    val permit: Int = 0,
    val alpha: Int = 0
)

data class LiveLog(
    val id: String, // 🚀 Wajib ada untuk LazyColumn Key
    val name: String,
    val status: String,
    val time: String,
    val className: String
)

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "DashboardVM"

    private val _stats = MutableStateFlow(AttendanceStats())
    val stats = _stats.asStateFlow()

    private val _logs = MutableStateFlow<List<LiveLog>>(emptyList())
    val logs = _logs.asStateFlow()

    init {
        listenToLiveAttendance()
    }

    private fun listenToLiveAttendance() {
        // --- LOGIKA FILTER TANGGAL HARI INI ---
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.time

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        Log.d(TAG, "Mulai monitoring: $startOfToday")

        // 🚀 Gunakan Constants.COLL_ATTENDANCE ("attendance") agar sinkron dengan CheckIn
        db.collection(Constants.COLL_ATTENDANCE)
            .whereGreaterThanOrEqualTo(Constants.FIELD_TIMESTAMP, startOfToday)
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Gagal Listen Live Data: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newLogs = mutableListOf<LiveLog>()
                    var p = 0; var s = 0; var i = 0; var a = 0

                    for (doc in snapshot) {
                        val firestoreTimestamp = doc.getTimestamp(Constants.FIELD_TIMESTAMP)
                        val status = (doc.getString("status") ?: "ALPHA").uppercase()

                        // Hitung statistik
                        when (status) {
                            "PRESENT", "LATE" -> p++
                            "SAKIT", "SICK" -> s++
                            "IZIN", "PERMIT" -> i++
                            else -> a++ // ALPHA
                        }

                        val timeDisplay = firestoreTimestamp?.toDate()?.let { timeFormatter.format(it) } ?: "--:--"

                        newLogs.add(LiveLog(
                            id = doc.id, // 🚀 Ambil ID Dokumen untuk Key UI
                            name = doc.getString("name") ?: "Unknown",
                            status = status,
                            time = timeDisplay,
                            className = doc.getString("className") ?: "-"
                        ))
                    }

                    _stats.value = AttendanceStats(
                        total = newLogs.size,
                        present = p, sick = s, permit = i, alpha = a
                    )
                    _logs.value = newLogs
                }
            }
    }
}