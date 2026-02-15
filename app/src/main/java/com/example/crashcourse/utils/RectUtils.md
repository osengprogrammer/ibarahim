package com.example.crashcourse.utils

import android.graphics.Rect

/**
 * 🛠️ toSquareRect (Stainless Steel Edition)
 * Mengubah Rect lonjong hasil deteksi ML Kit menjadi Kotak Sempurna (1:1).
 * Rumus: center(x,y) ± (max(w,h) * padding / 2)
 */
fun Rect.toSquareRect(maxWidth: Int, maxHeight: Int): Rect {
    // 1. Cari sisi terpanjang agar hasil potongan selalu kotak
    val currentWidth = width()
    val currentHeight = height()
    val size = maxOf(currentWidth, currentHeight)
    
    // 2. Tentukan titik tengah wajah
    val centerX = centerX()
    val centerY = centerY()
    
    // 3. Tambahkan Padding (1.1f = +10%) 
    // Agar dahi, telinga, dan dagu tidak terpotong terlalu mepet
    val halfSize = (size * 1.1f).toInt() / 2

    // 4. Hitung koordinat baru dengan pengaman (coerceIn)
    // Agar kotak tidak keluar dari area gambar kamera
    val left = (centerX - halfSize).coerceIn(0, maxWidth)
    val top = (centerY - halfSize).coerceIn(0, maxHeight)
    val right = (centerX + halfSize).coerceIn(0, maxWidth)
    val bottom = (centerY + halfSize).coerceIn(0, maxHeight)

    return Rect(left, top, right, bottom)
}