package com.example.crashcourse.ml.nativeutils // 👈 Pastikan ini benar

import android.graphics.Rect
import android.media.Image
import com.example.crashcourse.ml.nativeutils.NativeImageProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * =====================================================
 * BITMAP UTILS - NATIVE OPTIMIZED VERSION (V.15.5)
 * =====================================================
 */
object BitmapUtils {
    private const val INPUT_SIZE = 112 
    private const val BYTES_PER_FLOAT = 4

    fun preprocessFace(
        image: Image, 
        boundingBox: Rect, 
        rotation: Int,
        outputSize: Int = INPUT_SIZE
    ): ByteBuffer {
        // Alokasi Direct Buffer untuk akses memori tingkat C++
        // Ukuran: 112 * 112 * 3 channel * 4 byte (Float32)
        val buffer = ByteBuffer.allocateDirect(outputSize * outputSize * 3 * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())

        val planes = image.planes
        
        // Memanggil fungsi JNI di native_image.cpp
        NativeImageProcessor.preprocessFace(
            yBuffer = planes[0].buffer,
            uBuffer = planes[1].buffer,
            vBuffer = planes[2].buffer,
            width = image.width,
            height = image.height,
            yRowStride = planes[0].rowStride,
            uvRowStride = planes[1].rowStride,
            yPixelStride = planes[0].pixelStride,
            uvPixelStride = planes[1].pixelStride,
            cropLeft = boundingValue(boundingBox.left, image.width),
            cropTop = boundingValue(boundingBox.top, image.height),
            cropWidth = boundingValue(boundingBox.width(), image.width),
            cropHeight = boundingValue(boundingBox.height(), image.height),
            rotation = rotation,
            outputSize = outputSize,
            outBuffer = buffer
        )

        buffer.rewind()
        return buffer
    }

    // Helper sederhana untuk mencegah angka negatif yang bikin JNI crash
    private fun boundingValue(value: Int, max: Int): Int = value.coerceIn(0, max)
}