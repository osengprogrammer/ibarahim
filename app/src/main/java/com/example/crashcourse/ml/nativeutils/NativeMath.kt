package com.example.crashcourse.ml.nativeutils // ✅ Alamat Baru

import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * 🧠 NativeMath (V.15.5 - Precision Edition)
 * Penggaris Biometrik Azura Tech.
 * Menggabungkan kekuatan C++ (via JNI) dan logika Kotlin untuk akurasi tingkat tinggi.
 */
object NativeMath {
    
    init {
        // Nama library sesuai di CMakeLists.txt
        try {
            System.loadLibrary("native_image")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("NativeMath", "❌ Gagal memuat library native_image: ${e.message}")
        }
    }

    /**
     * 📏 Cosine Distance (JNI/C++)
     */
    external fun cosineDistance(a: FloatArray, b: FloatArray): Float

    /**
     * 🎨 Pixel Preprocessing (JNI/C++)
     */
    external fun preprocessImage(buffer: ByteBuffer, size: Int)

    /**
     * 🛡️ L2 Normalization (The Holy Grail)
     * Kunci agar angka "Dist" tidak melompat ke 0.5 - 0.9.
     */
    fun normalize(embedding: FloatArray): FloatArray {
        var sum = 0.0f
        for (v in embedding) {
            sum += v * v
        }
        
        val magnitude = sqrt(sum.toDouble()).toFloat()
        
        // Proteksi pembagian nol (Zero-Vector Safety)
        if (magnitude < 1e-10f) {
            return FloatArray(embedding.size) { 0f } 
        }

        for (i in embedding.indices) {
            embedding[i] /= magnitude
        }
        
        return embedding
    }
}