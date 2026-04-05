package com.example.greenfocus.data.repository

import com.example.greenfocus.data.model.User
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections

class AuthRepository {
    private val auth = FirebaseModule.auth
    private val db = FirebaseModule.firestore

    fun signUp(email: String, password: String, name: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                // Sau khi tạo Auth xong, tạo tiếp Profile trong Firestore
                val newUser = User(uid = uid, email = email, displayName = name)

                db.collection(FirestoreCollections.USERS).document(uid).set(newUser)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e -> onResult(false, e.message) }
            }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }
}