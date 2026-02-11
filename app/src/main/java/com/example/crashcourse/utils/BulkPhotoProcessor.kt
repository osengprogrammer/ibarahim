package com.example.crashcourse.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

/**
 * 📸 Azura Tech Bulk Photo Processor
 * Handles memory-safe downloading, decoding, and optimization of student photos.
 */
object BulkPhotoProcessor {
    private const val TAG = "BulkPhotoProcessor"
    
    // Photo processing constants
    private const val MAX_PHOTO_SIZE = 512 // Optimal size for Face Recognition
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB limit for incoming streams
    private const val DOWNLOAD_TIMEOUT = 30L // seconds
    
    // HTTP client for downloading photos
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    /**
     * Photo processing result
     */
    data class PhotoProcessResult(
        val success: Boolean,
        val localPhotoUrl: String? = null,
        val error: String? = null,
        val originalSize: Long = 0,
        val processedSize: Long = 0
    )
    
    /**
     * 🚀 MAIN ENTRY: Process photo from various sources (URL, local file, base64)
     */
    suspend fun processPhotoSource(
        context: Context,
        photoSource: String,
        studentId: String
    ): PhotoProcessResult {
        if (photoSource.isBlank()) {
            return PhotoProcessResult(success = true, localPhotoUrl = null)
        }
        
        return try {
            Log.d(TAG, "Memproses foto untuk $studentId...")
            
            val bitmap = when {
                photoSource.startsWith("http://") || photoSource.startsWith("https://") -> {
                    downloadPhotoFromUrl(photoSource)
                }
                photoSource.startsWith("data:image") -> {
                    decodeBase64Photo(photoSource)
                }
                else -> {
                    // Try as local file path or file://
                    val cleanPath = photoSource.removePrefix("file://")
                    loadLocalPhoto(cleanPath)
                }
            }
            
            if (bitmap == null) {
                return PhotoProcessResult(
                    success = false,
                    error = "Gagal memuat gambar dari sumber (URL/File tidak valid)"
                )
            }
            
            // 1. Optimize (Resize to 512px max for AI efficiency)
            val optimizedBitmap = optimizePhoto(bitmap)
            
            // 2. Save to local storage using your utility
            val savedPhotoUrl = PhotoStorageUtils.saveFacePhoto(context, optimizedBitmap, studentId)
            
            // 3. Clean up bitmap memory immediately
            if (optimizedBitmap != bitmap) bitmap.recycle()
            
            if (savedPhotoUrl != null) {
                val savedFile = File(savedPhotoUrl)
                PhotoProcessResult(
                    success = true,
                    localPhotoUrl = savedPhotoUrl,
                    processedSize = savedFile.length()
                )
            } else {
                PhotoProcessResult(success = false, error = "Gagal menyimpan foto ke storage.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing photo for $studentId", e)
            PhotoProcessResult(success = false, error = "System Error: ${e.message}")
        }
    }
    
    /**
     * 🛡️ DOWNLOAD LOGIC: Includes headers and memory-safe decoding
     */
    private suspend fun downloadPhotoFromUrl(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    // Standard headers to bypass basic anti-bot filters
                    .addHeader("User-Agent", "Mozilla/5.0 (Android 13; Mobile)")
                    .addHeader("Accept", "image/webp,image/apng,image/*")
                    .build()
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    
                    val bytes = response.body?.bytes() ?: return@withContext null
                    
                    // 🧠 MEMORY SAFETY: Read dimensions only first
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    
                    // Auto-Downsample if the image is massive (e.g., 4K photo from Google Drive)
                    if (options.outWidth > 2000 || options.outHeight > 2000) {
                        options.inSampleSize = 2
                    }
                    
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal download: $url", e)
                null
            }
        }
    }
    
    private fun decodeBase64Photo(dataUrl: String): Bitmap? {
        return try {
            val base64Data = if (dataUrl.contains(",")) dataUrl.substringAfter(",") else dataUrl
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }
    
    private fun loadLocalPhoto(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            FileInputStream(file).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) { null }
    }
    
    /**
     * 📏 OPTIMIZATION: Keeps aspect ratio while capping at MAX_PHOTO_SIZE
     */
    private fun optimizePhoto(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_PHOTO_SIZE && height <= MAX_PHOTO_SIZE) return bitmap
        
        val ratio = minOf(
            MAX_PHOTO_SIZE.toFloat() / width,
            MAX_PHOTO_SIZE.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return bitmap.scale(newWidth, newHeight)
    }
    
    /**
     * ⏱️ ESTIMATOR: Predicts total processing time for the Admin UI
     */
    fun estimateProcessingTime(photoSources: List<String>): Long {
        var totalSeconds = 0L
        photoSources.forEach { source ->
            totalSeconds += when {
                source.isBlank() -> 0
                source.startsWith("http") -> 3 // Assume 3s for download + AI processing
                else -> 1 // Assume 1s for local processing
            }
        }
        return totalSeconds
    }

    fun getPhotoSourceType(photoSource: String): String {
        return when {
            photoSource.isBlank() -> "None"
            photoSource.startsWith("http") -> "Cloud URL"
            photoSource.startsWith("data:image") -> "Base64"
            else -> "Local Storage"
        }
    }
}