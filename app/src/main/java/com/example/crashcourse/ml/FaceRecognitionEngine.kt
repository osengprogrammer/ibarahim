package com.example.crashcourse.ml

import android.content.Context
import com.example.crashcourse.db.FaceCache
import com.example.crashcourse.db.FaceEntity
import com.example.crashcourse.ml.nativeutils.NativeMath
import com.example.crashcourse.utils.BiometricConfig // 🚀 Pusat Komando Keamanan

/**
 * 🧠 FaceRecognitionEngine (V.19.0 - Unified Config)
 * Tugas: Menghitung jarak matematis dan melakukan filtrasi awal (Rejection).
 * Sekarang tersinkronisasi 100% dengan BiometricConfig Azura Tech.
 */
class FaceRecognitionEngine(private val context: Context) {

    companion object {
        private const val TAG = "FaceRecognitionEngine"
    }

    /**
     * 1️⃣ DETEKSI DUPLIKAT (Anti-Fraud saat Registrasi)
     * Mencegah satu orang didaftarkan dengan dua nama berbeda.
     */
    fun detectDuplicate(newEmbedding: FloatArray): String? {
        val allFaces = FaceCache.getFaces()
        
        for (face in allFaces) {
            val distance = NativeMath.cosineDistance(newEmbedding, face.embedding)
            
            // ✅ Menggunakan konstanta global: DUPLICATE_THRESHOLD (0.22001f)
            if (distance < BiometricConfig.DUPLICATE_THRESHOLD) {
                return face.name
            }
        }
        return null 
    }

    /**
     * 2️⃣ RECOGNITION (Saat Check-in)
     * Mencari kecocokan terbaik dengan batasan keamanan ketat.
     */
    fun recognize(embedding: FloatArray): MatchResult {
        val allFaces = FaceCache.getFaces()
        if (allFaces.isEmpty()) return MatchResult.NoData

        var bestMatch: FaceEntity? = null
        var bestDist = Float.MAX_VALUE

        // Pencarian linear di dalam cache RAM (L2 Normalized)
        for (face in allFaces) {
            val dist = NativeMath.cosineDistance(embedding, face.embedding)
            if (dist < bestDist) {
                bestDist = dist
                bestMatch = face
            }
        }

        return when {
            bestMatch == null -> MatchResult.Unknown
            
            /**
             * 🔥 HARD REJECTION: Mencegah 'Nearest Neighbor Fallacy'
             * Jika jarak > ENGINE_REJECTION_THRESHOLD (0.42001f), 
             * maka Engine akan langsung melapor 'Unknown'.
             */
            bestDist > BiometricConfig.ENGINE_REJECTION_THRESHOLD -> MatchResult.Unknown
            
            // Lulus filter awal, kirim ke ViewModel untuk verifikasi Stabilitas & Kedip
            else -> MatchResult.Success(bestMatch, bestDist)
        }
    }
}

/**
 * Representasi hasil pencocokan wajah yang sinkron dengan alur ViewModel.
 */
sealed class MatchResult {
    object NoData : MatchResult()
    object Unknown : MatchResult()
    data class Success(val face: FaceEntity, val dist: Float) : MatchResult()
}