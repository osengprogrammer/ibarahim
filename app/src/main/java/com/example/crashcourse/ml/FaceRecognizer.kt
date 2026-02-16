package com.example.crashcourse.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import com.example.crashcourse.utils.ModelUtils
import java.nio.ByteBuffer

/**
 * 🧠 Azura Tech Face Recognizer (V.20 - Pure Native Edition)
 * Zero Redundancy: Normalisasi 100% ditangani oleh model TFLite internal.
 */
object FaceRecognizer {
    private const val TAG = "FaceRecognizer"
    private const val MODEL_NAME = "mobilefacenet.tflite"
    
    private var embeddingSize = 192 
    private lateinit var interpreter: Interpreter
    private lateinit var outputBuffer: Array<FloatArray>

    fun initialize(context: Context) {
        try {
            Log.d(TAG, "Initializing FaceRecognizer with model: $MODEL_NAME")
            val modelBuffer = ModelUtils.loadModelFile(context, MODEL_NAME)
            
            val options = Interpreter.Options().apply { 
                setNumThreads(4) 
                // Jika ingin lebih kencang, bisa gunakan NNAPI/GPU Delegate di sini
            }
            
            interpreter = Interpreter(modelBuffer, options)
            interpreter.allocateTensors()

            val outputTensor = interpreter.getOutputTensor(0)
            val shape = outputTensor.shape() 
            
            if (shape.size >= 2) {
                embeddingSize = shape[1]
                Log.i(TAG, "Detected Model Output Size: $embeddingSize")
            }

            // Pre-allocate buffer untuk efisiensi memori (Anti-Lag)
            outputBuffer = Array(1) { FloatArray(embeddingSize) }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FaceRecognizer", e)
        }
    }

    /**
     * Menghasilkan embedding wajah murni dari TFLite.
     */
    fun recognizeFace(input: ByteBuffer): FloatArray {
        if (!::interpreter.isInitialized) {
            Log.e(TAG, "Interpreter not initialized!")
            return FloatArray(embeddingSize)
        }

        return try {
            input.rewind()
            
            // 1. Jalankan AI Inference (Model TFLite bekerja di sini)
            interpreter.run(input, outputBuffer)
            
            // 2. 🚀 PURE OUTPUT (Netron Verified)
            // Model sudah melakukan L2Normalization di internal (Layer 230).
            // Kita TIDAK PERLU memanggil fungsi normalisasi manual lagi.
            // Kita WAJIB me-return .clone() agar array ini aman dikirim ke 
            // ViewModel dan tidak tertimpa oleh tangkapan frame kamera berikutnya.
            outputBuffer[0].clone() 

        } catch (e: Exception) {
            Log.e(TAG, "Error during face recognition inference", e)
            FloatArray(embeddingSize)
        }
    }

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