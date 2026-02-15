package com.example.crashcourse.ml.nativeutils

import java.nio.ByteBuffer

/**
 * 🔌 JNI Bridge (NativeImageProcessor)
 * Menghubungkan Kotlin ke file C++ (native_image.cpp)
 */
object NativeImageProcessor {

    /**
     * Mesin utama pengolah gambar di level C++.
     * Menangani YUV to RGB, Cropping, Rotation, dan Normalization ke Float32.
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
        outBuffer: ByteBuffer // 🚨 Harus Direct ByteBuffer (Float32)
    )

    init {
        // Nama library sesuai di CMakeLists.txt
        System.loadLibrary("native_image")
    }
}