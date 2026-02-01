package com.example.crashcourse.utils

object NativeMath {
    init {
        System.loadLibrary("azura_native")
    }

    external fun cosineDistance(a: FloatArray, b: FloatArray): Float
}