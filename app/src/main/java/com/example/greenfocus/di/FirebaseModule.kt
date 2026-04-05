package com.example.greenfocus.di

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.database.database

object FirebaseModule {
    val auth by lazy { Firebase.auth }
    val firestore by lazy { Firebase.firestore }
    val realtimeDb by lazy { Firebase.database }
}