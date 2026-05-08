package com.example.greenfocus.data.repository

import com.example.greenfocus.data.model.User
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections

interface AuthRepository {
    fun signUp(email: String, password: String, name: String, onResult: (Boolean, String?) -> Unit)
    fun login(email: String, pass: String, onResult: (isSuccess: Boolean, errorMessage: String?) -> Unit)
}
class ProdAuthRepository : AuthRepository {
    private val auth = FirebaseModule.auth
    private val db = FirebaseModule.firestore

    override fun signUp(email: String, password: String, name: String, onResult: (Boolean, String?) -> Unit) {
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

    override fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }
}