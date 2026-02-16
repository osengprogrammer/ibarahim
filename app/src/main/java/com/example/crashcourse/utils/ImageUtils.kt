package com.example.crashcourse.utils

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * 🛡️ BIOMETRIC CONSTANTS
 * Kunci konsistensi: Pendaftaran & Absen WAJIB pakai angka yang sama jika tidak ditentukan lain.
 */
object BiometricConfig {
    // 📸 Pengaturan Gambar (Eagle Eye 720p)
    const val DEFAULT_FACE_PADDING = 0.15f 
    const val FACE_INPUT_SIZE = 112 

    // ⚖️ Pagar Keamanan (Threshold)
    // Jalur Tengah: Aman dari Makhachev, Ramah untuk Obama
    const val STRICT_THRESHOLD = 0.39001f 
    
    // 🔥 ENGINE REJECTION: Harus sedikit lebih longgar dari STRICT_THRESHOLD
    // tapi tetap menjaga agar tidak ada "Nearest Neighbor Fallacy"
    const val ENGINE_REJECTION_THRESHOLD = 0.42001f 

    // 🛡️ ANTI-DUPLIKAT (Saat Registrasi)
    const val DUPLICATE_THRESHOLD = 0.22001f   

    // 🛡️ Stabilitas
    const val REQUIRED_STABILITY = 5 
}

/**
 * Konversi ImageProxy (YUV) menjadi Bitmap (RGB).
 * ⚠️ Gunakan hanya untuk pendaftaran (Add User).
 */
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    
    val nv21 = ByteArray(ySize + uSize + vSize)
    
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
    
    val imageBytes = out.toByteArray()
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    
    val matrix = Matrix().apply { 
        postRotate(this@toBitmap.imageInfo.rotationDegrees.toFloat()) 
    }
    
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * 🛡️ STAINLESS STEEL RECT FIX (V.18.0)
 * Memastikan wajah selalu Kotak Sempurna dengan padding dinamis (Eagle Eye Support).
 */
fun Rect.toSquareRect(imageWidth: Int, imageHeight: Int, paddingFactor: Float = BiometricConfig.DEFAULT_FACE_PADDING): Rect {
    val centerX = this.centerX()
    val centerY = this.centerY()
    
    // ✅ Menggunakan paddingFactor (1 + 0.15 = 1.15x dari ukuran asli wajah)
    val size = (maxOf(this.width(), this.height()) * (1 + paddingFactor)).toInt()
    val halfSize = size / 2

    // Clamping: Menjamin koordinat tidak keluar dari dimensi gambar
    val left = (centerX - halfSize).coerceIn(0, imageWidth)
    val top = (centerY - halfSize).coerceIn(0, imageHeight)
    val right = (centerX + halfSize).coerceIn(0, imageWidth)
    val bottom = (centerY + halfSize).coerceIn(0, imageHeight)

    return Rect(left, top, right, bottom)
}