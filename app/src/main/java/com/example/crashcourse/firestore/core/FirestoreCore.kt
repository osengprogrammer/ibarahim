package com.example.crashcourse.firestore.core

import com.google.firebase.firestore.FirebaseFirestore

/**
 * 🔥 FirestoreCore
 * Single entry point untuk FirebaseFirestore
 */
object FirestoreCore {
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
}