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
 * PHASE-1 SAFE FACE ANALYZER — FINAL AUTHORITATIVE
 * =================================================
 *
 * Enforces FOUR LAWS:
 *
 * L1 — Embedding is a VALUE
 *      → clone() IMMEDIATELY after inference
 *
 * L2 — Analyzer does NOT own time
 *      → pixel extraction while ImageProxy is valid
 *
 * L3 — Analyzer emits FACTS only
 *      → no Compose / UI state here
 *
 * L4 — Preprocessing is MEASURED
 *      → pixel range can be verified externally
 */
class FaceAnalyzer(
    private val onResult: (FaceResult) -> Unit
) : ImageAnalysis.Analyzer {

    /**
     * Immutable fact emitted per analyzed frame
     */
    data class FaceResult(
        val bounds: List<Rect>,
        val imageSize: IntSize,
        val rotation: Int,
        val embeddings: List<Pair<Rect, FloatArray>>
    )

    // ✅ enableTracking() PRESERVED (critical for stable boxes)
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
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

        // L2 — geometry captured while buffer is VALID
        val imageSize = if (rotation % 180 == 0) {
            IntSize(imageProxy.width, imageProxy.height)
        } else {
            IntSize(imageProxy.height, imageProxy.width)
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->

                val results = mutableListOf<Pair<Rect, FloatArray>>()

                for (face in faces) {

                    // =========================
                    // L2 — SAFE PIXEL EXTRACTION
                    // =========================
                    val inputBuffer = BitmapUtils.preprocessFace(
                        image = mediaImage,
                        boundingBox = face.boundingBox,
                        rotation = rotation
                    )

                    // Native inference (buffer may be reused internally)
                    val rawEmbedding = FaceRecognizer.recognizeFace(inputBuffer)

                    // =========================
                    // L1 — CONVERT TO VALUE NOW
                    // =========================
                    val safeEmbedding = rawEmbedding.clone()

                    // ========= VERIFICATION GATE A =========
                    Log.d(
                        "PHASE1_GATE_A",
                        "Face=${face.trackingId} " +
                        "Hash=${safeEmbedding.contentHashCode()} " +
                        "Sample=${safeEmbedding[0]},${safeEmbedding[1]},${safeEmbedding[2]}"
                    )

                    results.add(face.boundingBox to safeEmbedding)
                }

                // =========================
                // L3 — EMIT ONE FACT
                // =========================
                onResult(
                    FaceResult(
                        bounds = faces.map { it.boundingBox },
                        imageSize = imageSize,
                        rotation = rotation,
                        embeddings = results
                    )
                )
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
