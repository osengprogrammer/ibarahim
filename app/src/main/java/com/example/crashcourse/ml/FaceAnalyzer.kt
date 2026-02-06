package com.example.crashcourse.ml

import android.graphics.Rect
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.unit.IntSize
import com.example.crashcourse.ml.BitmapUtils // ✅ Explicit Import
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.ByteBuffer

class FaceAnalyzer(
    private val onResult: (FaceResult) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val FACE_NET_INPUT_SIZE = 112
        // 💡 Brightness Threshold (0 - 255)
        // Below 45 usually means the face features are lost in shadow.
        private const val MIN_LUMINOSITY = 45.0 
    }

    data class FaceResult(
        val bounds: List<Rect>,
        val imageSize: IntSize,
        val rotation: Int,
        val embeddings: List<Pair<Rect, FloatArray>>,
        val leftEyeOpenProb: Float?,
        val rightEyeOpenProb: Float?,
        val isLowLight: Boolean // 🆕 New Field for UI Warning
    )

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
    )

    private val isProcessing = AtomicBoolean(false)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing.get()) {
            imageProxy.close()
            return
        }

        val mediaImage: Image = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        isProcessing.set(true)

        // 1. 💡 CHECK BRIGHTNESS FIRST
        // If it's too dark, don't even bother with Face Detection (saves battery & prevents errors)
        val luminosity = calculateLuminosity(mediaImage)
        if (luminosity < MIN_LUMINOSITY) {
            Log.w("FaceAnalyzer", "Too Dark: $luminosity")
            onResult(
                FaceResult(
                    bounds = emptyList(),
                    imageSize = IntSize(mediaImage.width, mediaImage.height),
                    rotation = imageProxy.imageInfo.rotationDegrees,
                    embeddings = emptyList(),
                    leftEyeOpenProb = null,
                    rightEyeOpenProb = null,
                    isLowLight = true // 🚨 Tell UI to show "Turn on Light"
                )
            )
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        // 2. CONTINUE WITH RECOGNITION IF LIGHT IS GOOD
        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = if (rotation == 90 || rotation == 270) mediaImage.height else mediaImage.width
        val height = if (rotation == 90 || rotation == 270) mediaImage.width else mediaImage.height
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val results = mutableListOf<Pair<Rect, FloatArray>>()
                var primaryLeftEye: Float? = null
                var primaryRightEye: Float? = null

                for (face in faces) {
                    val bounds = face.boundingBox

                    // 🛑 EDGE CHECK: Ignore faces touching the screen edge
                    val margin = 20
                    val isTouchingEdge = bounds.left < margin || 
                                         bounds.top < margin || 
                                         bounds.right > width - margin || 
                                         bounds.bottom > height - margin

                    if (isTouchingEdge) continue 

                    if (primaryLeftEye == null) {
                        primaryLeftEye = face.leftEyeOpenProbability
                        primaryRightEye = face.rightEyeOpenProbability
                    }

                    // Preprocess and Recognize
                    val inputBuffer = BitmapUtils.preprocessFace(
                        image = mediaImage,
                        boundingBox = face.boundingBox,
                        rotation = rotation,
                        outputSize = FACE_NET_INPUT_SIZE
                    )

                    val rawEmbedding = FaceRecognizer.recognizeFace(inputBuffer)
                    results.add(face.boundingBox to rawEmbedding.clone())
                }

                onResult(
                    FaceResult(
                        bounds = faces.map { it.boundingBox },
                        imageSize = IntSize(width, height),
                        rotation = rotation,
                        embeddings = results,
                        leftEyeOpenProb = primaryLeftEye,
                        rightEyeOpenProb = primaryRightEye,
                        isLowLight = false // ✅ Light is good
                    )
                )
            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Face detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun close() {
        detector.close()
    }

    /**
     * ⚡ Ultra-fast Brightness Calculation
     * We only sample pixels from the Y-Plane (Grayscale), we don't convert the whole image.
     */
    private fun calculateLuminosity(image: Image): Double {
        val plane = image.planes[0] // Y Plane = Brightness
        val buffer = plane.buffer
        buffer.rewind()
        
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        
        var sum = 0L
        // Sample every 10th pixel to be extremely fast
        val step = 10 
        var count = 0

        for (i in 0 until data.size step step) {
            // Convert byte (-128 to 127) to unsigned int (0 to 255)
            sum += (data[i].toInt() and 0xFF)
            count++
        }

        return if (count > 0) sum.toDouble() / count else 0.0
    }
}