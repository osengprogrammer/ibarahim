package com.example.crashcourse.util

/**
 * Bridge untuk menghubungkan Kotlin dengan file C++ (native_image.cpp)
 */
object NativeKeyStore {
    init {
        // Nama library harus sesuai dengan add_library di CMakeLists.txt
        System.loadLibrary("native_image")
    }

    /**
     * Memanggil fungsi getIsoKey dari C++
     */
    external fun getIsoKey(): String
}