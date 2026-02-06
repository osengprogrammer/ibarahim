package com.example.crashcourse.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import com.example.crashcourse.utils.ModelUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object FaceRecognizer {
    private const val TAG = "FaceRecognizer"
    
    // ✅ Updated to match your new model file name
    private const val MODEL_NAME = "mobilefacenet.tflite"
    
    // ✅ Updated to match your metadata: float32[1,192]
    private var embeddingSize = 192 

    private lateinit var interpreter: Interpreter
    private lateinit var outputBuffer: Array<FloatArray>

    /**
     * Initialize the TFLite interpreter with the new 5MB MobileFaceNet model.
     */
    fun initialize(context: Context) {
        try {
            Log.d(TAG, "Initializing FaceRecognizer with model: $MODEL_NAME")
            val modelBuffer = ModelUtils.loadModelFile(context, MODEL_NAME)
            
            // Optimization: 4 threads is usually the sweet spot for mobile CPUs
            val options = Interpreter.Options().apply { 
                setNumThreads(4) 
                // You could also add NNAPI here if you want hardware acceleration
            }
            
            interpreter = Interpreter(modelBuffer, options)
            interpreter.allocateTensors()

            // 1. DYNAMIC SIZE DETECTION (Safety Check)
            val outputTensor = interpreter.getOutputTensor(0)
            val shape = outputTensor.shape() // Expected: [1, 192]
            
            if (shape.size >= 2) {
                embeddingSize = shape[1]
                Log.i(TAG, "Detected Model Output Size: $embeddingSize")
            } else {
                Log.w(TAG, "Could not detect shape accurately, defaulting to $embeddingSize")
            }

            // 2. Pre-allocate buffer to prevent GC pressure during camera stream
            outputBuffer = Array(1) { FloatArray(embeddingSize) }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FaceRecognizer", e)
        }
    }

    /**
     * Generates a 192-dimension embedding from the preprocessed ByteBuffer.
     */
    fun recognizeFace(input: ByteBuffer): FloatArray {
        if (!::interpreter.isInitialized) {
            Log.e(TAG, "Interpreter not initialized!")
            return FloatArray(embeddingSize)
        }

        return try {
            // Ensure the input buffer is at the start
            input.rewind()
            
            // Run inference
            interpreter.run(input, outputBuffer)
            
            // Get the result
            val rawEmbedding = outputBuffer[0]

            // 3. L2 NORMALIZATION (Crucial for MobileFaceNet accuracy)
            // This ensures all vectors have a magnitude of 1.0
            var sum = 0.0f
            for (v in rawEmbedding) { 
                sum += v * v 
            }
            val norm = sqrt(sum.toDouble()).toFloat()
            val safeNorm = if (norm > 0.00001f) norm else 1.0f

            val normalizedEmbedding = FloatArray(embeddingSize)
            for (i in rawEmbedding.indices) {
                normalizedEmbedding[i] = rawEmbedding[i] / safeNorm
            }

            normalizedEmbedding

        } catch (e: Exception) {
            Log.e(TAG, "Error during face recognition inference", e)
            FloatArray(embeddingSize)
        }
    }

    /**
     * Call this when the app or activity is destroyed to free up memory.
     */
    fun close() {
        try {
            if (::interpreter.isInitialized) {
                interpreter.close()
                Log.d(TAG, "FaceRecognizer interpreter closed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter", e)
        }
    }
}