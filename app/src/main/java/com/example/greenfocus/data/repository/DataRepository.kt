package com.example.greenfocus.data.repository

import android.util.Log
import com.example.greenfocus.data.model.FocusSession
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

interface DataRepository {

    fun addSession(session: FocusSession)
    suspend fun getSessions(): List<FocusSession>
}

class ProdDataRepository : DataRepository{
    private val sessionDb = FirebaseModule.firestore

    private val currentUid: String?
        get() = FirebaseModule.auth.currentUser?.uid

    override fun addSession(session: FocusSession) {
        val uid = currentUid
        requireNotNull(uid) { "Cannot add session: User is not logged in." }

        sessionDb.collection(FirestoreCollections.SESSIONS).document(uid).collection("user_sessions")
            .add(session)
            .addOnSuccessListener { documentReference ->
                Log.d("ProdDataRepository", "Session saved with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("ProdDataRepository", "Failed to save session", e)
            }
    }

    override suspend fun getSessions(): List<FocusSession> {
        val uid = currentUid
        requireNotNull(uid) { "Cannot fetch sessions: User is not logged in." }

        return try {
            val snapshot = sessionDb.document(uid).collection("user_sessions")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await() // Pauses the coroutine until Firebase returns the data

            // Magically converts all the Firestore documents back into your Kotlin data class!
            snapshot.toObjects(FocusSession::class.java)

        } catch (e: Exception) {
            Log.e("ProdDataRepository", "Error fetching sessions: ${e.message}")
            emptyList()
        }
    }

}