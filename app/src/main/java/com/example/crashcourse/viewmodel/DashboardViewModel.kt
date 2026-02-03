package com.example.crashcourse.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        val today = LocalDate.now().toString() // Format: 2026-02-03
        
        // Query: Ambil log yang timestamp-nya diawali tanggal hari ini
        // Kita gunakan range string query trick untuk filter tanggal di field string
        db.collection("attendance_logs")
            .whereGreaterThanOrEqualTo("timestamp", today)
            .whereLessThan("timestamp", today + "\uf8ff") 
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newLogs = mutableListOf<LiveLog>()
                    var p = 0; var s = 0; var i = 0; var a = 0

                    for (doc in snapshot) {
                        // 1. Hitung Statistik
                        val status = doc.getString("status") ?: "ALPHA"
                        when (status) {
                            "PRESENT" -> p++
                            "SAKIT" -> s++
                            "IZIN" -> i++
                            "ALPHA" -> a++
                        }

                        // 2. Buat List Log
                        val fullTime = doc.getString("timestamp") ?: ""
                        // Ambil jam saja (10:05:00) dari string ISO
                        val timeOnly = if(fullTime.length > 11) fullTime.substring(11, 19) else fullTime

                        newLogs.add(
                            LiveLog(
                                name = doc.getString("name") ?: "Unknown",
                                status = status,
                                time = timeOnly,
                                className = doc.getString("className") ?: "-"
                            )
                        )
                    }

                    _stats.value = AttendanceStat(
                        total = newLogs.size,
                        present = p, sick = s, permit = i, alpha = a
                    )
                    _logs.value = newLogs
                }
            }
    }
}