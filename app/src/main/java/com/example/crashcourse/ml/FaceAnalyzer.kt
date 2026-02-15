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
import com.example.crashcourse.ml.nativeutils.NativeMath
import com.example.crashcourse.ml.nativeutils.BitmapUtils // Pastikan utility ini ada
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 👁️ FaceAnalyzer V.15.0 - Eagle Eye Edition
 * Mata utama Azura Tech yang disetel untuk presisi biometrik maksimal.
 */
@Suppress("UnsafeOptInUsageError")
class FaceAnalyzer(
    private val onResult: (FaceResult) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "FaceAnalyzer"
        private const val FACE_NET_INPUT_SIZE = 112
        
        // 💡 Toleransi cahaya rendah (Luminosity)
        private const val MIN_LUMINOSITY = 30.0 
        
        // 💡 Jarak pandang: Wajah hanya perlu mengisi 5% layar agar diproses
        private const val MIN_FACE_SIZE_PERCENT = 0.05f 
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

    // State untuk UI Monitoring (Radar HUD)
    var faceBounds by mutableStateOf<List<Rect>>(emptyList())
        private set
    var imageSize by mutableStateOf(IntSize(0, 0))
        private set
    var isLowLightState by mutableStateOf(false)
        private set

    private val isProcessing = AtomicBoolean(false)
    private var lastProcessTime = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Throttling: Menjaga FPS tetap stabil di angka ~12 FPS agar HP tidak panas
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

        // 1. Monitor Cahaya
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

                    // 🛡️ Filter Jarak: Cek apakah wajah cukup besar di layar
                    val faceArea = originalBounds.width() * originalBounds.height()
                    if (faceArea < (width * height * MIN_FACE_SIZE_PERCENT)) continue

                    // 🛡️ Filter Tepi: Abaikan wajah yang terpotong bingkai kamera
                    val margin = 10
                    if (originalBounds.left < margin || originalBounds.top < margin || 
                        originalBounds.right > width - margin || originalBounds.bottom > height - margin) continue

                    // Probabilitas mata untuk Liveness Check (Kedip)
                    primaryLeftEye = face.leftEyeOpenProbability
                    primaryRightEye = face.rightEyeOpenProbability

                    // ✅ STAINLESS STEEL CROP: Ubah ke Kotak Sempurna + Padding 25%
                    val squareBounds = originalBounds.toSquareRect(width, height)

                    // Preprocessing via JNI/C++ (Resize & RGB Conversion)
                    val buffer = BitmapUtils.preprocessFace(
                        image       = mediaImage,
                        boundingBox = squareBounds,
                        rotation    = rotation,
                        outputSize  = FACE_NET_INPUT_SIZE
                    )

                    // AI Inference (MobileFaceNet)
                    val rawEmbedding = FaceRecognizer.recognizeFace(buffer)
                    
                    // ✅ THE HOLY GRAIL: L2 Normalization
                    // Ini kunci agar jarak Cosine/Euclidean tidak membengkak ke 0.5+
                    val normalizedEmbedding = NativeMath.normalize(rawEmbedding.clone())

                    results.add(originalBounds to normalizedEmbedding)
                }

                // Kirim paket data lengkap ke ViewModel
                onResult(
                    FaceResult(
                        bounds = faceBounds,
                        imageSize = imageSize,
                        rotation = rotation,
                        embeddings = results,
                        leftEyeOpenProb = primaryLeftEye,
                        rightEyeOpenProb = primaryRightEye,
                        isLowLight = false
                    )
                )
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

    fun close() = detector.close()

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