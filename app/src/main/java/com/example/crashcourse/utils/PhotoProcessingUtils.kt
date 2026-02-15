package com.example.crashcourse.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.scale
import android.util.Log
import com.example.crashcourse.ml.FaceRecognizer
import com.example.crashcourse.ml.nativeutils.NativeMath // ✅ Import NativeMath
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PhotoProcessingUtils {
    private const val TAG = "PhotoProcessingUtils"
    
    // ✅ Standar MobileFaceNet: 112x112
    private const val INPUT_SIZE = 112 

    /**
     * Memproses bitmap galeri menjadi embedding biometrik.
     * Outputnya adalah Pair berisi Bitmap Wajah (untuk preview) dan FloatArray (untuk database).
     */
    suspend fun processBitmapForFaceEmbedding(
        @Suppress("UNUSED_PARAMETER") context: Context,
        bitmap: Bitmap
    ): Pair<Bitmap, FloatArray>? = withContext(Dispatchers.IO) {
        try {
            // 1. Normalisasi resolusi awal agar deteksi ML Kit ringan
            val processedBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                PhotoStorageUtils.resizeBitmap(bitmap, 1024)
            } else {
                bitmap
            }

            // 2. Deteksi wajah menggunakan ML Kit
            val faces = detectFacesInBitmap(processedBitmap)

            if (faces.isEmpty()) {
                Log.w(TAG, "Wajah tidak ditemukan di foto galeri.")
                return@withContext null
            }

            // 3. Ambil wajah terbesar (asumsi itu adalah subjek utama)
            val largestFace = faces.maxByOrNull { it.width() * it.height() }
                ?: return@withContext null

            Log.d(TAG, "Memproses wajah pada koordinat: $largestFace")

            // 4. Crop wajah dengan padding standar (agar AI bisa melihat telinga/dahi sedikit)
            val faceBitmap = cropFaceFromBitmap(processedBitmap, largestFace)
            
            // 5. Generate biometrik (embedding)
            val embedding = generateEmbeddingFromFaceBitmap(faceBitmap)

            Log.d(TAG, "Embedding sukses dibuat: ${embedding.size} dimensi")

            Pair(faceBitmap, embedding)

        } catch (e: Exception) {
            Log.e(TAG, "Gagal memproses foto galeri", e)
            null
        }
    }

    private suspend fun detectFacesInBitmap(bitmap: Bitmap): List<Rect> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val detector = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .build()
            )

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val faceRects = faces.map { it.boundingBox }
                    continuation.resume(faceRects)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ML Kit Error", e)
                    continuation.resume(emptyList())
                }
        }

    private fun cropFaceFromBitmap(bitmap: Bitmap, faceRect: Rect): Bitmap {
        // Padding 15% agar framing wajah mirip dengan hasil live camera
        val padding = 0.15f 
        val paddingX = (faceRect.width() * padding).toInt()
        val paddingY = (faceRect.height() * padding).toInt()

        val left = (faceRect.left - paddingX).coerceAtLeast(0)
        val top = (faceRect.top - paddingY).coerceAtLeast(0)
        val right = (faceRect.right + paddingX).coerceAtMost(bitmap.width)
        val bottom = (faceRect.bottom + paddingY).coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun generateEmbeddingFromFaceBitmap(faceBitmap: Bitmap): FloatArray {
        // Force resize ke 112x112 agar sinkron dengan input model
        val resizedBitmap = faceBitmap.scale(INPUT_SIZE, INPUT_SIZE)

        // Konversi ke ByteBuffer
        val buffer = bitmapToByteBuffer(resizedBitmap)

        // Generate embedding (AI Inference)
        return FaceRecognizer.recognizeFace(buffer)
    }

    /**
     * 🚀 SINKRONISASI BIOMETRIK
     * Mengolah pixel bitmap menjadi format yang dimengerti MobileFaceNet lewat C++
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())

        val intVals = IntArray(INPUT_SIZE * INPUT_SIZE)
        // Paksa ke ARGB_8888 untuk ekstraksi channel RGB yang akurat
        val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        argbBitmap.getPixels(intVals, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        buffer.rewind()
        for (pixel in intVals) {
            // Ekstraksi warna RGB (Mengabaikan Alpha)
            val r = ((pixel shr 16) and 0xFF).toFloat()
            val g = ((pixel shr 8) and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()
            
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }

        // 🚀 PRE-PROCESS NATIVE (The Secret Sauce)
        // Melakukan normalisasi (x - 127.5) / 128 secara instan di level CPU/C++
        buffer.rewind()
        NativeMath.preprocessImage(buffer, INPUT_SIZE * INPUT_SIZE * 3)
        
        return buffer
    }

    suspend fun validateFaceInBitmap(bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val faces = detectFacesInBitmap(bitmap)
            faces.isNotEmpty() && faces.any { it.width() >= 50 }
        } catch (e: Exception) { false }
    }

    suspend fun getFaceConfidence(bitmap: Bitmap): Float = withContext(Dispatchers.IO) {
        try {
            val faces = detectFacesInBitmap(bitmap)
            if (faces.isEmpty()) return@withContext 0.0f
            val largestFace = faces.maxByOrNull { it.width() * it.height() } ?: return@withContext 0.0f
            
            // Hitung rasio wajah terhadap total gambar
            val faceArea = largestFace.width() * largestFace.height()
            val imageArea = bitmap.width * bitmap.height
            (faceArea.toFloat() / imageArea.toFloat() * 10).coerceAtMost(1.0f)
        } catch (e: Exception) { 0.0f }
    }
}