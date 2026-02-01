package com.example.crashcourse.utils

import java.nio.ByteBuffer

object NativeMath {
    init {
        // Pastikan nama ini sesuai dengan CMakeLists.txt kamu
        System.loadLibrary("azura_native")
    }

    /**
     * Menghitung jarak cosine antara dua embedding wajah.
     * Digunakan untuk membandingkan wajah di kamera dengan database.
     */
    external fun cosineDistance(a: FloatArray, b: FloatArray): Float

    /**
     * Melakukan normalisasi pixel massal (0-255 ke -1.0 sampai 1.0).
     * @param buffer Direct ByteBuffer berisi data RGB float mentah.
     * @param size Total elemen float (Width * Height * 3).
     */
    external fun preprocessImage(buffer: ByteBuffer, size: Int)
}