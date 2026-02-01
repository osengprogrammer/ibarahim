package com.example.crashcourse.ml.nativeutils

import java.nio.ByteBuffer

object NativeImageProcessor {

    init {
        System.loadLibrary("native_image")
    }

    /**
     * =====================================================
     * UI / DEBUG ONLY
     * YUV420 → ARGB
     * =====================================================
     */
    external fun yuv420ToArgb(
        yBuffer: ByteBuffer,
        uBuffer: ByteBuffer,
        vBuffer: ByteBuffer,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        yPixelStride: Int,
        uvPixelStride: Int,
        outArgb: IntArray
    )

    /**
     * =====================================================
     * AUTHORITATIVE FACE PREPROCESS (INFERENCE)
     *
     * ⚠️ IMPORTANT:
     * - Java allocates outBuffer
     * - Native fills it
     * - Native does NOT return anything
     * =====================================================
     */
    external fun preprocessFace(
        yBuffer: ByteBuffer,
        uBuffer: ByteBuffer,
        vBuffer: ByteBuffer,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        yPixelStride: Int,
        uvPixelStride: Int,
        cropLeft: Int,
        cropTop: Int,
        cropWidth: Int,
        cropHeight: Int,
        rotation: Int,
        outputSize: Int,
        outBuffer: ByteBuffer
    )
}
