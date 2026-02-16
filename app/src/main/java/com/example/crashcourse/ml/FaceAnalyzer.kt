package com.example.crashcourse.ml

import android.graphics.Rect
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.example.crashcourse.utils.toSquareRect 
import com.example.crashcourse.utils.BiometricConfig // 🚀 Import Pusat Komando
import com.example.crashcourse.ml.nativeutils.BitmapUtils 
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 👁️ FaceAnalyzer V.19.0 - Azura Stainless Steel
 * Update: Menghapus redundansi normalisasi & sinkronisasi pusat komando.
 */
@Suppress("UnsafeOptInUsageError")
class FaceAnalyzer(
    private val onResult: (FaceResult) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "FaceAnalyzer"
        private const val FACE_NET_INPUT_SIZE = 112
        private const val MIN_LUMINOSITY = 30.0 
        // Menggunakan nilai dari config agar sensitivitas jarak seragam
        private const val MIN_FACE_SIZE_PERCENT = 0.02f 
    }

    data class FaceResult(
        val bounds: List<Rect>,
        val imageSize: IntSize,
        val rotation: Int,
        val embeddings: List<Pair<Rect, FloatArray>>,
        val leftEyeOpenProb: Float?,
        val rightEyeOpenProb: Float?,
        val isLowLight: Boolean
    )

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
    )

    var faceBounds by mutableStateOf<List<Rect>>(emptyList())
        private set
    var imageSize by mutableStateOf(IntSize(0, 0))
        private set
    var isLowLightState by mutableStateOf(false)
        private set

    private val isProcessing = AtomicBoolean(false)
    private var lastProcessTime = 0L

    fun close() {
        try {
            detector.close()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menutup detector", e)
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (isProcessing.get() || (currentTime - lastProcessTime < 80)) {
            imageProxy.close()
            return
        }

        val mediaImage: Image = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        isProcessing.set(true)
        lastProcessTime = currentTime

        val luminosity = calculateLuminosity(mediaImage)
        val lowLight = luminosity < MIN_LUMINOSITY
        isLowLightState = lowLight

        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = if (rotation % 180 == 0) imageProxy.width else imageProxy.height
        val height = if (rotation % 180 == 0) imageProxy.height else imageProxy.width
        imageSize = IntSize(width, height)

        if (lowLight) {
            onResult(FaceResult(emptyList(), imageSize, rotation, emptyList(), null, null, true))
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                faceBounds = faces.map { it.boundingBox }
                val results = mutableListOf<Pair<Rect, FloatArray>>()
                
                var primaryLeftEye: Float? = null
                var primaryRightEye: Float? = null

                for (face in faces) {
                    val originalBounds = face.boundingBox
                    val faceArea = originalBounds.width() * originalBounds.height()
                    if (faceArea < (width * height * MIN_FACE_SIZE_PERCENT)) continue

                    val margin = 5
                    if (originalBounds.left < margin || originalBounds.top < margin || 
                        originalBounds.right > width - margin || originalBounds.bottom > height - margin) continue

                    primaryLeftEye = face.leftEyeOpenProbability
                    primaryRightEye = face.rightEyeOpenProbability

                    // ✅ Sinkronisasi dengan BiometricConfig.DEFAULT_FACE_PADDING (0.15f)
                    val squareBounds = originalBounds.toSquareRect(width, height, paddingFactor = BiometricConfig.DEFAULT_FACE_PADDING)

                    val buffer = BitmapUtils.preprocessFace(
                        image       = mediaImage,
                        boundingBox = squareBounds,
                        rotation    = rotation,
                        outputSize  = FACE_NET_INPUT_SIZE
                    )

                    // 🚀 FIXED: FaceRecognizer SUDAH melakukan normalisasi di dalamnya.
                    // Jangan panggil NativeMath.normalize lagi di sini agar data murni.
                    val embedding = FaceRecognizer.recognizeFace(buffer)

                    results.add(originalBounds to embedding)
                }

                onResult(FaceResult(faceBounds, imageSize, rotation, results, primaryLeftEye, primaryRightEye, false))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                faceBounds = emptyList()
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    private fun calculateLuminosity(image: Image): Double {
        val plane = image.planes[0]
        val buffer = plane.buffer
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        var sum = 0L
        for (i in data.indices step 10) {
            sum += (data[i].toInt() and 0xFF)
        }
        return if (data.isNotEmpty()) sum.toDouble() / (data.size / 10) else 0.0
    }
}