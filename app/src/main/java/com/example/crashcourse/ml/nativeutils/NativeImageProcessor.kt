package com.example.crashcourse.ml.nativeutils

import java.nio.ByteBuffer

object NativeImageProcessor {
    /**
     * JNI Bridge to our C++ implementation in native_image.cpp
     */
    external fun preprocessFace(
        yBuffer: ByteBuffer, uBuffer: ByteBuffer, vBuffer: ByteBuffer,
        width: Int, height: Int, yRowStride: Int, uvRowStride: Int,
        yPixelStride: Int, uvPixelStride: Int, cropLeft: Int, cropTop: Int,
        cropWidth: Int, cropHeight: Int, rotation: Int, outputSize: Int,
        outBuffer: ByteBuffer
    )

    init {
        // Loads the library we compiled (CMakeLists.txt name)
        System.loadLibrary("native_image")
    }
}