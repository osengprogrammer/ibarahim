package com.example.crashcourse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class AttendanceStat(
    val total: Int = 0,
    val present: Int = 0,
    val sick: Int = 0,
    val permit: Int = 0,
    val alpha: Int = 0
)

data class LiveLog(
    val name: String,
    val status: String,
    val time: String,
    val className: String
)

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "DashboardVM"

    private val _stats = MutableStateFlow(AttendanceStat())
    val stats = _stats.asStateFlow()

    private val _logs = MutableStateFlow<List<LiveLog>>(emptyList())
    val logs = _logs.asStateFlow()

    init {
        listenToLiveAttendance()
    }

    private fun listenToLiveAttendance() {
        // --- LOGIKA FILTER TANGGAL HARI INI ---
        val calendar = Calendar.getInstance()
        
        // Set ke jam 00:00:00 hari ini
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.time

        // Format untuk jam menit (HH:mm)
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        Log.d(TAG, "Mencari data Timestamp mulai dari: $startOfToday")

        db.collection("attendance_logs")
            // ✅ Filter berdasarkan objek Date/Timestamp
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed! Pastikan field 'timestamp' di Firestore bertipe Timestamp (bukan String). Error: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newLogs = mutableListOf<LiveLog>()
                    var p = 0; var s = 0; var i = 0; var a = 0

                    for (doc in snapshot) {
                        // ✅ Baca sebagai Timestamp
                        val firestoreTimestamp = doc.getTimestamp("timestamp")
                        val status = (doc.getString("status") ?: "ALPHA").uppercase()

                        // Hitung statistik
                        when (status) {
                            "PRESENT" -> p++
                            "SAKIT" -> s++
                            "IZIN" -> i++
                            "ALPHA" -> a++
                        }

                        // Konversi Timestamp ke jam menit (HH:mm)
                        val timeDisplay = if (firestoreTimestamp != null) {
                            timeFormatter.format(firestoreTimestamp.toDate())
                        } else {
                            "--:--"
                        }

                        newLogs.add(LiveLog(
                            name = doc.getString("name") ?: "Unknown",
                            status = status,
                            time = timeDisplay,
                            className = doc.getString("className") ?: "-"
                        ))
                    }

                    _stats.value = AttendanceStat(
                        total = newLogs.size,
                        present = p, sick = s, permit = i, alpha = a
                    )
                    _logs.value = newLogs
                    Log.d(TAG, "Update Berhasil: ${newLogs.size} data ditemukan hari ini.")
                }
            }
    }
}