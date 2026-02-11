package com.example.crashcourse.firestore.dashboard

import android.util.Log
import com.example.crashcourse.firestore.core.FirestoreCore
import com.example.crashcourse.firestore.core.FirestorePaths
import com.example.crashcourse.utils.Constants
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📊 LiveLog DTO
 * Represents a single attendance row from Firestore.
 */
data class LiveLog(
    val id: String,
    val name: String,
    val status: String,
    val time: String,
    val className: String
)

/**
 * 📈 FirestoreDashboard
 * Handles real-time monitoring queries.
 */
object FirestoreDashboard {

    private const val TAG = "FirestoreDashboard"
    private val db = FirestoreCore.db

    /**
     * 🎧 Listen to Today's Attendance
     * Returns a real-time list of logs for a specific school, starting from 00:00 today.
     */
    fun listenToLiveToday(
        sekolahId: String,
        onUpdate: (List<LiveLog>) -> Unit
    ): ListenerRegistration {
        
        // 1. Calculate Start of Day (00:00:00)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.time
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        // 2. Build Query
        return db.collection(FirestorePaths.ATTENDANCE)
            .whereEqualTo(Constants.KEY_SEKOLAH_ID, sekolahId)
            .whereGreaterThanOrEqualTo(Constants.FIELD_TIMESTAMP, startOfToday)
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "❌ Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val logs = snapshot.documents.map { doc ->
                        val firestoreTimestamp = doc.getTimestamp(Constants.FIELD_TIMESTAMP)
                        val timeDisplay = firestoreTimestamp?.toDate()?.let {
                            timeFormatter.format(it)
                        } ?: "--:--"

                        LiveLog(
                            id = doc.id,
                            name = doc.getString(Constants.KEY_NAME) ?: "Unknown",
                            status = (doc.getString(Constants.FIELD_STATUS) ?: "ALPHA").uppercase(),
                            time = timeDisplay,
                            className = doc.getString(Constants.PILLAR_CLASS) ?: "-"
                        )
                    }
                    onUpdate(logs)
                }
            }
    }
}