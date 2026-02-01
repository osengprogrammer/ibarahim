package com.example.crashcourse.ml

import android.graphics.*
import android.media.Image
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BitmapUtils {
    private const val INPUT_SIZE = 160
    private const val BYTES_PER_CHANNEL = 4

    fun preprocessFace(image: Image, boundingBox: Rect, rotation: Int): ByteBuffer {
        val bitmap = yuvToRgb(image)
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val left = boundingBox.left.coerceIn(0, rotated.width - 1)
        val top = boundingBox.top.coerceIn(0, rotated.height - 1)
        val width = boundingBox.width().coerceAtMost(rotated.width - left)
        val height = boundingBox.height().coerceAtMost(rotated.height - top)
        
        val faceBmp = if (width > 0 && height > 0) {
            Bitmap.createBitmap(rotated, left, top, width, height)
        } else {
            rotated.scale(INPUT_SIZE, INPUT_SIZE)
        }

        val inputBmp = faceBmp.scale(INPUT_SIZE, INPUT_SIZE)

        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * BYTES_PER_CHANNEL)
            .order(ByteOrder.nativeOrder())
        val intVals = IntArray(INPUT_SIZE * INPUT_SIZE)
        inputBmp.getPixels(intVals, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in intVals) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            buffer.putFloat((r - 127.5f) / 128.0f)
            buffer.putFloat((g - 127.5f) / 128.0f)
            buffer.putFloat((b - 127.5f) / 128.0f)
        }
        return buffer
    }

    /**
     * ✅ STRIDE-AWARE CONVERSION (CRITICAL FIX)
     * This loop correctly skips the invisible padding bytes that were causing
     * the "Static Noise" / False Positive bug.
     */
    private fun yuvToRgb(image: Image): Bitmap {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val width = image.width
        val height = image.height

        val nv21 = ByteArray(width * height * 3 / 2)
        
        // 1. Copy Y Plane Row-by-Row (Skips padding)
        var pos = 0
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(nv21, pos, width)
            pos += width
        }

        // 2. Interleave U/V
        val uvHeight = height / 2
        val uvWidth = width / 2
        val vRow = ByteArray(uvRowStride)
        val uRow = ByteArray(uvRowStride)

        for (row in 0 until uvHeight) {
            vBuffer.position(row * uvRowStride)
            uBuffer.position(row * uvRowStride)
            
            val vLen = Math.min(uvRowStride, vBuffer.remaining())
            vBuffer.get(vRow, 0, vLen)
            
            val uLen = Math.min(uvRowStride, uBuffer.remaining())
            uBuffer.get(uRow, 0, uLen)

            for (col in 0 until uvWidth) {
                val index = col * uvPixelStride
                if (index < vLen && index < uLen) {
                    nv21[pos++] = vRow[index]
                    nv21[pos++] = uRow[index]
                }
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}