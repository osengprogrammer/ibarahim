package com.example.crashcourse

import android.os.Bundle
import android.util.Log // Added for logging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.crashcourse.ui.theme.CrashcourseTheme
import com.example.crashcourse.ui.MainScreen
import com.example.crashcourse.ml.FaceRecognizer

// --- FIREBASE IMPORTS ---
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Hide system UI (status bar and navigation bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // --- AZURATECH: FIREBASE SYNC TEST START ---
        // This code will try to write a "Handshake" document to your Firestore
        val db = Firebase.firestore
        val handshake = hashMapOf(
            "device_model" to android.os.Build.MODEL,
            "status" to "Online",
            "developer_platform" to "VS Code",
            "last_sync" to Timestamp.now()
        )

        db.collection("azura_sync_test")
            .add(handshake)
            .addOnSuccessListener { documentReference ->
                Log.d("AZURA_SYNC", "SUCCESS: Document added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("AZURA_SYNC", "ERROR: Could not connect to Firebase", e)
            }
        // --- AZURATECH: FIREBASE SYNC TEST END ---

        // Initialize the TFLite interpreter once, with error handling
        try {
            FaceRecognizer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: show fallback UI or error message
        }

        setContent {
            CrashcourseTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release interpreter resources
        FaceRecognizer.close()
    }
}