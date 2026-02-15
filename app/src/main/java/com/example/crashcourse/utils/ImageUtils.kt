package com.example.crashcourse.utils

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * 🛡️ BIOMETRIC CONSTANTS
 * Kunci konsistensi: Pendaftaran & Absen WAJIB pakai angka yang sama.
 */
object BiometricConfig {
    const val FACE_PADDING = 1.25f // Padding 25% untuk menangkap dahi & telinga
    const val FACE_INPUT_SIZE = 112 // Standard MobileFaceNet
}

/**
 * Konversi ImageProxy (YUV) menjadi Bitmap (RGB).
 * ⚠️ Gunakan hanya untuk pendaftaran (Add User), JANGAN di loop scanner 
 * karena proses JPEG compression ini memakan CPU yang besar.
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
 * 🛡️ STAINLESS STEEL RECT FIX (V.15.2)
 * Memastikan wajah selalu Kotak Sempurna dengan padding yang konsisten.
 */
fun Rect.toSquareRect(imageWidth: Int, imageHeight: Int): Rect {
    val centerX = this.centerX()
    val centerY = this.centerY()
    
    // Gunakan Konstanta Global agar Pendaftaran & Absen selalu sinkron
    val size = (maxOf(this.width(), this.height()) * BiometricConfig.FACE_PADDING).toInt()
    val halfSize = size / 2

    // Clamping: Menjamin koordinat tidak keluar dari dimensi gambar 0..width/height
    val left = (centerX - halfSize).coerceIn(0, imageWidth)
    val top = (centerY - halfSize).coerceIn(0, imageHeight)
    val right = (centerX + halfSize).coerceIn(0, imageWidth)
    val bottom = (centerY + halfSize).coerceIn(0, imageHeight)

    return Rect(left, top, right, bottom)
}