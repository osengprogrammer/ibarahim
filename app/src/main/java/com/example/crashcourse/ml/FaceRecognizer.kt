package com.example.crashcourse.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import com.example.crashcourse.utils.ModelUtils
import com.example.crashcourse.ml.nativeutils.NativeMath // ✅ Import pusat matematika kita
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 🧠 Azura Tech Face Recognizer
 * Tugas: Melakukan inference menggunakan MobileFaceNet TFLite.
 * Arsitektur: Delegasi Normalisasi ke NativeMath untuk akurasi Stainless Steel.
 */
object FaceRecognizer {
    private const val TAG = "FaceRecognizer"
    private const val MODEL_NAME = "mobilefacenet.tflite"
    
    private var embeddingSize = 192 
    private lateinit var interpreter: Interpreter
    private lateinit var outputBuffer: Array<FloatArray>

    /**
     * Inisialisasi Interpreter dengan model MobileFaceNet 5MB.
     */
    fun initialize(context: Context) {
        try {
            Log.d(TAG, "Initializing FaceRecognizer with model: $MODEL_NAME")
            val modelBuffer = ModelUtils.loadModelFile(context, MODEL_NAME)
            
            // Setel 4 threads: Performa puncak untuk kebanyakan CPU Mobile
            val options = Interpreter.Options().apply { 
                setNumThreads(4) 
            }
            
            interpreter = Interpreter(modelBuffer, options)
            interpreter.allocateTensors()

            // Deteksi ukuran output secara dinamis
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
     * Menghasilkan embedding wajah dari ByteBuffer hasil preprocess.
     */
    fun recognizeFace(input: ByteBuffer): FloatArray {
        if (!::interpreter.isInitialized) {
            Log.e(TAG, "Interpreter not initialized!")
            return FloatArray(embeddingSize)
        }

        return try {
            input.rewind()
            
            // 1. Jalankan AI Inference
            interpreter.run(input, outputBuffer)
            
            // 2. Ambil data mentah (Raw Embedding)
            val rawEmbedding = outputBuffer[0]

            // 3. 🛡️ L2 NORMALIZATION (Delegasikan ke NativeMath)
            // Menggunakan fungsi eksternal agar standar pendaftaran & check-in identik 100%
            NativeMath.normalize(rawEmbedding.clone()) 

        } catch (e: Exception) {
            Log.e(TAG, "Error during face recognition inference", e)
            FloatArray(embeddingSize)
        }
    }

    /**
     * Membersihkan memori saat aplikasi ditutup.
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