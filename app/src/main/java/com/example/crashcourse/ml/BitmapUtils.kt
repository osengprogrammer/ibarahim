package com.example.crashcourse.ml

import android.graphics.Rect
import android.media.Image
import com.example.crashcourse.ml.nativeutils.NativeImageProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * =====================================================
 * BITMAP UTILS - NATIVE OPTIMIZED VERSION
 * =====================================================
 * This object is now a high-speed bridge. It no longer 
 * uses Android Bitmaps for the camera stream, which 
 * saves RAM and prevents lag.
 */
object BitmapUtils {
    // ✅ Matches MobileFaceNet requirement
    private const val INPUT_SIZE = 112 
    private const val BYTES_PER_FLOAT = 4

    /**
     * 🚀 FAST NATIVE PATH
     * Directly processes YUV planes into a normalized Float buffer.
     * * @param image The raw ImageProxy.image from the camera
     * @param boundingBox The face location detected by ML Kit
     * @param rotation The sensor rotation (usually 90 or 270)
     * @param outputSize Targeted model input (112)
     */
    fun preprocessFace(
        image: Image, 
        boundingBox: Rect, 
        rotation: Int,
        outputSize: Int = INPUT_SIZE
    ): ByteBuffer {
        // 1. Allocate Direct Buffer (C++ requires Direct for address access)
        // Size = 112 * 112 * 3 (RGB) * 4 (Float)
        val buffer = ByteBuffer.allocateDirect(outputSize * outputSize * 3 * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())

        val planes = image.planes
        
        // 2. 🚀 Call the C++ Engine (native_image.cpp)
        // This function handles YUV->RGB, Cropping, Rotating, and Scaling in ONE pass.
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
            cropLeft = boundingBox.left,
            cropTop = boundingBox.top,
            cropWidth = boundingBox.width(),
            cropHeight = boundingBox.height(),
            rotation = rotation,
            outputSize = outputSize,
            outBuffer = buffer
        )

        // Reset pointer for TFLite reader
        buffer.rewind()
        return buffer
    }
}