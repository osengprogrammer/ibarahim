package com.example.crashcourse.ml

import android.graphics.Rect
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.unit.IntSize
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * =================================================
 * PHASE-3 LIVENESS-ENABLED FACE ANALYZER
 * =================================================
 *
 * Adds Eye-Tracking capabilities to detect blinks, preventing 
 * photo-spoofing attacks.
 */
class FaceAnalyzer(
    private val onResult: (FaceResult) -> Unit
) : ImageAnalysis.Analyzer {

    /**
     * Data class updated to include Eye Open Probabilities
     */
    data class FaceResult(
        val bounds: List<Rect>,
        val imageSize: IntSize,
        val rotation: Int,
        val embeddings: List<Pair<Rect, FloatArray>>,
        val leftEyeOpenProb: Float?,  // 🆕 Range [0.0, 1.0]
        val rightEyeOpenProb: Float?  // 🆕 Range [0.0, 1.0]
    )

    // ✅ PHASE-3: Updated options to enable Classification
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // 👈 CRITICAL
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

        val rotation = imageProxy.imageInfo.rotationDegrees

        val imageSize = if (rotation % 180 == 0) {
            IntSize(imageProxy.width, imageProxy.height)
        } else {
            IntSize(imageProxy.height, imageProxy.width)
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->

                val results = mutableListOf<Pair<Rect, FloatArray>>()
                
                // Track eyes of the primary face (the one being recognized)
                var primaryLeftEye: Float? = null
                var primaryRightEye: Float? = null

                for (face in faces) {
                    // Capture eye data for the first detected face
                    if (primaryLeftEye == null) {
                        primaryLeftEye = face.leftEyeOpenProbability
                        primaryRightEye = face.rightEyeOpenProbability
                    }

                    // L2 — SAFE PIXEL EXTRACTION
                    val inputBuffer = BitmapUtils.preprocessFace(
                        image = mediaImage,
                        boundingBox = face.boundingBox,
                        rotation = rotation
                    )

                    // Native inference
                    val rawEmbedding = FaceRecognizer.recognizeFace(inputBuffer)
                    val safeEmbedding = rawEmbedding.clone()

                    results.add(face.boundingBox to safeEmbedding)
                }

                // EMIT FACT WITH LIVENESS DATA
                onResult(
                    FaceResult(
                        bounds = faces.map { it.boundingBox },
                        imageSize = imageSize,
                        rotation = rotation,
                        embeddings = results,
                        leftEyeOpenProb = primaryLeftEye,
                        rightEyeOpenProb = primaryRightEye
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
}