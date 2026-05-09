package com.example.ecomerse

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class EcomerseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase explicitly
        try {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to initialize Firebase", e)
        }
    }
}

