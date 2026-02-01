package com.example.crashcourse.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import com.example.crashcourse.utils.ModelUtils
import java.nio.ByteBuffer
import kotlin.math.sqrt

object FaceRecognizer {
    private const val MODEL_NAME = "facenet.tflite"
    
    // Dynamic size (prevents crash on different models)
    private var embeddingSize = 192 

    private lateinit var interpreter: Interpreter
    private lateinit var outputBuffer: Array<FloatArray>

    fun initialize(context: Context) {
        try {
            Log.d("FaceRecognizer", "Initializing FaceRecognizer...")
            val modelBuffer = ModelUtils.loadModelFile(context, MODEL_NAME)
            val options = Interpreter.Options().apply { setNumThreads(4) }
            
            interpreter = Interpreter(modelBuffer, options)
            interpreter.allocateTensors()

            // 1. DYNAMIC SIZE DETECTION
            val outputTensor = interpreter.getOutputTensor(0)
            val shape = outputTensor.shape()
            
            if (shape.isEmpty() || shape[0] != 1) {
                // Fallback to 192 if shape reading fails, or throw error
                Log.w("FaceRecognizer", "Could not detect shape, defaulting to 192")
            } else {
                embeddingSize = shape[1]
            }
            Log.d("FaceRecognizer", "Model Output Size: $embeddingSize")

            // 2. Allocate buffer ONCE
            outputBuffer = Array(1) { FloatArray(embeddingSize) }

        } catch (e: Exception) {
            Log.e("FaceRecognizer", "Failed to initialize FaceRecognizer", e)
        }
    }

    fun recognizeFace(input: ByteBuffer): FloatArray {
        if (!::interpreter.isInitialized) return FloatArray(embeddingSize)

        return try {
            interpreter.run(input, outputBuffer)
            val rawEmbedding = outputBuffer[0]

            // 3. L2 NORMALIZATION
            var sum = 0.0f
            for (v in rawEmbedding) { sum += v * v }
            val norm = sqrt(sum)
            val safeNorm = if (norm > 0f) norm else 1.0f

            for (i in rawEmbedding.indices) {
                rawEmbedding[i] /= safeNorm
            }

            // 4. CLONE (Important)
            rawEmbedding.clone()

        } catch (e: Exception) {
            Log.e("FaceRecognizer", "Error during face recognition", e)
            FloatArray(embeddingSize)
        }
    }

    // THIS IS THE MISSING FUNCTION CAUSING YOUR ERROR
    fun close() {
        if (::interpreter.isInitialized) {
            interpreter.close()
        }
    }
}