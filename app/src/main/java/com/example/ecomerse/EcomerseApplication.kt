package com.example.ecomerse

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class EcomerseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase explicitly
        try {
            FirebaseApp.initializeApp(this)
            
            // Enable Firestore offline persistence
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
            
            Log.d("Firebase", "Firebase initialized successfully with offline persistence")
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to initialize Firebase", e)
        }
    }
}

