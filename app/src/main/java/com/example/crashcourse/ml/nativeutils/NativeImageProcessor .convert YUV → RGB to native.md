package com.example.crashcourse.ml.nativeutils

import java.nio.ByteBuffer

object NativeImageProcessor {

    init {
        System.loadLibrary("native_image")
    }

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
}
