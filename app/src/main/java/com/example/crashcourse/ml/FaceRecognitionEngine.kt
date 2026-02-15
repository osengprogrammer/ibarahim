package com.example.crashcourse.ml

import android.content.Context
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.ml.nativeutils.NativeMath

/**
 * 🧠 FaceRecognitionEngine (V.15.8 - Guarded Eagle Edition)
 * Tugas: Menghitung jarak matematis dan melakukan filtrasi awal (Rejection).
 * * Versi ini memastikan tidak ada "Best Guess" yang konyol (seperti Obama jadi Maksum)
 * dengan menerapkan Hard Threshold di level Engine.
 */
class FaceRecognitionEngine(private val context: Context) {

    companion object {
        // 🛡️ DUPLICATE_THRESHOLD: Digunakan saat registrasi (Admin)
        private const val DUPLICATE_THRESHOLD = 0.22f   
        
        // 🛡️ REJECTION_THRESHOLD: Batas maksimal toleransi biometrik.
        // Jika jarak > 0.45, maka itu dipastikan BUKAN orang yang sama.
        private const val REJECTION_THRESHOLD = 0.45f
        
        private const val TAG = "FaceRecognitionEngine"
    }

    /**
     * 1️⃣ DETEKSI DUPLIKAT (Anti-Fraud saat Registrasi)
     */
    fun detectDuplicate(newEmbedding: FloatArray): String? {
        val allFaces = FaceCache.getFaces()
        
        for (face in allFaces) {
            val distance = NativeMath.cosineDistance(newEmbedding, face.embedding)
            if (distance < DUPLICATE_THRESHOLD) {
                return face.name
            }
        }
        return null 
    }

    /**
     * 2️⃣ RECOGNITION (Saat Check-in)
     * Mencari kecocokan terbaik dengan batasan keamanan (Hard Rejection).
     */
    fun recognize(embedding: FloatArray): MatchResult {
        val allFaces = FaceCache.getFaces()
        if (allFaces.isEmpty()) return MatchResult.NoData

        var bestMatch: FaceEntity? = null
        var bestDist = Float.MAX_VALUE

        // Pencarian linear di dalam cache RAM
        for (face in allFaces) {
            val dist = NativeMath.cosineDistance(embedding, face.embedding)
            if (dist < bestDist) {
                bestDist = dist
                bestMatch = face
            }
        }

        return when {
            bestMatch == null -> MatchResult.Unknown
            
            // 🔥 HARD REJECTION: Mencegah 'Nearest Neighbor Fallacy'
            // Jika Obama masuk, jaraknya mungkin 0.6. Karena 0.6 > 0.45, 
            // Engine akan lapor 'Unknown', bukan Maksum.
            bestDist > REJECTION_THRESHOLD -> MatchResult.Unknown
            
            // Lulus sensor awal, kirim ke ViewModel untuk verifikasi stabilitas/kedip
            else -> MatchResult.Success(bestMatch, bestDist)
        }
    }
}

/**
 * Representasi hasil pencocokan wajah yang lebih ketat.
 */
sealed class MatchResult {
    object NoData : MatchResult()
    object Unknown : MatchResult()
    data class Success(val face: FaceEntity, val dist: Float) : MatchResult()
}