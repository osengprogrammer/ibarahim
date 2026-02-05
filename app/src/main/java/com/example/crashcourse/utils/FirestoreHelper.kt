package com.example.crashcourse.utils

import android.util.Log
import com.example.crashcourse.db.CheckInRecord
import com.example.crashcourse.db.FaceEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.util.Date

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    
    // Inisialisasi Firestore secara Lazy
    private val db by lazy { FirebaseFirestore.getInstance() }

    // ==========================================
    // 1. DATA SISWA (Sync Profil & Wajah)
    // ==========================================

    /**
     * Mengambil data siswa dari Cloud berdasarkan hak akses Guru (Assigned Classes).
     */
    suspend fun getScopedStudentsFromFirestore(uid: String): List<FaceEntity> {
        return try {
            // 1. Cek hak akses di dokumen user
            val userDoc = db.collection("users").document(uid).get().await()
            val role = userDoc.getString("role") ?: "USER"
            
            @Suppress("UNCHECKED_CAST")
            val assignedClasses = userDoc.get("assigned_classes") as? List<String> ?: emptyList()

            Log.d(TAG, "Fetching scope for UID: $uid | Role: $role")

            // 2. Buat Query berdasarkan Role
            val query: Query = if (role == "ADMIN" || assignedClasses.isEmpty()) {
                db.collection("students")
            } else {
                db.collection("students").whereIn("className", assignedClasses)
            }

            val snapshot = query.get().await()
            
            // 3. Mapping hasil ke List FaceEntity
            snapshot.documents.mapNotNull { doc ->
                val embeddingList = doc.get("embedding") as? List<Double>
                val embeddingFloatArray = embeddingList?.map { it.toFloat() }?.toFloatArray()
                
                if (embeddingFloatArray != null) {
                    mapDocToFaceEntity(doc, embeddingFloatArray)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error Download Students: ${e.message}")
            emptyList()
        }
    }

    /**
     * Mapping Document Firestore ke FaceEntity lokal
     */
    private fun mapDocToFaceEntity(doc: com.google.firebase.firestore.DocumentSnapshot, embedding: FloatArray): FaceEntity {
        return FaceEntity(
            studentId = doc.getString("studentId") ?: "",
            name = doc.getString("name") ?: "Unknown",
            photoUrl = doc.getString("photoUrl"), 
            embedding = embedding,
            className = doc.getString("className") ?: "",
            subClass = doc.getString("subClass") ?: "",
            grade = doc.getString("grade") ?: "",
            subGrade = doc.getString("subGrade") ?: "",
            program = doc.getString("program") ?: "",
            role = doc.getString("role") ?: "",
            classId = (doc.getLong("classId") ?: 0).toInt().takeIf { it != 0 },
            subClassId = (doc.getLong("subClassId") ?: 0).toInt().takeIf { it != 0 },
            gradeId = (doc.getLong("gradeId") ?: 0).toInt().takeIf { it != 0 },
            subGradeId = (doc.getLong("subGradeId") ?: 0).toInt().takeIf { it != 0 },
            programId = (doc.getLong("programId") ?: 0).toInt().takeIf { it != 0 },
            roleId = (doc.getLong("roleId") ?: 0).toInt().takeIf { it != 0 },
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Upload profil siswa (saat registrasi wajah) ke Cloud.
     * Menggunakan studentId sebagai Document ID agar data tidak duplikat.
     */
    suspend fun syncStudentToFirestore(face: FaceEntity): Boolean {
        if (face.studentId.isBlank()) {
            Log.e(TAG, "❌ Gagal Sync: studentId kosong!")
            return false
        }

        return try {
            val embeddingList = face.embedding.map { it.toDouble() }
            val studentData = hashMapOf(
                "studentId" to face.studentId,
                "name" to face.name,
                "className" to face.className,
                "subClass" to face.subClass,
                "grade" to face.grade,
                "subGrade" to face.subGrade,
                "program" to face.program,
                "role" to face.role,
                "classId" to face.classId,
                "subClassId" to face.subClassId,
                "gradeId" to face.gradeId,
                "subGradeId" to face.subGradeId,
                "programId" to face.programId,
                "roleId" to face.roleId,
                "embedding" to embeddingList,
                "last_updated" to System.currentTimeMillis(),
                "photoUrl" to (face.photoUrl ?: "")
            )

            db.collection("students") 
                .document(face.studentId)
                .set(studentData, SetOptions.merge())
                .await() 

            Log.d(TAG, "✅ Profil Siswa Tersinkron: ${face.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal Sync Siswa: ${e.message}")
            false
        }
    }

    // ==========================================
    // 2. LOG ABSENSI (Parent App Sync)
    // ==========================================

    /**
     * Kirim log absen ke Firestore. 
     * Menggunakan .add() agar tersimpan sebagai log riwayat (History).
     */
    suspend fun syncAttendanceLog(record: CheckInRecord) {
        try {
            // Konversi java.time.LocalDateTime ke java.util.Date untuk Firebase
            val localDateTime = record.timestamp
            val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val date = Date.from(instant)
            val dateString = localDateTime.toLocalDate().toString()

            val logData = hashMapOf(
                "studentId" to record.studentId, // Kunci utama untuk App Orang Tua
                "name" to record.name,
                "timestamp" to Timestamp(date), // Tipe data Timestamp Firestore
                "date_str" to dateString,       // Helper format YYYY-MM-DD
                "status" to record.status,
                "note" to (record.note ?: ""),
                "className" to (record.className ?: ""),
                "gradeName" to (record.gradeName ?: ""),
                "synced_at" to System.currentTimeMillis()
            )

            // Menggunakan .add() agar tercipta dokumen baru setiap kali absen
            db.collection("attendance_logs")
                .add(logData)
                .await()
                
            Log.d(TAG, "✅ Log Absen Terkirim ke Cloud: ${record.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gagal Upload Log Absen: ${e.message}")
        }
    }
}