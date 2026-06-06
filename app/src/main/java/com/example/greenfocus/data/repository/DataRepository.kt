package com.example.greenfocus.data.repository

import android.util.Log
import com.example.greenfocus.data.model.FocusSession
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

interface DataRepository {

    fun addSession(session: FocusSession)
    fun setSession(sessionId: String, session: FocusSession)
    fun getSessions(): Flow<List<FocusSession>>
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

    override fun setSession(sessionId: String, session: FocusSession) {
        val uid = currentUid
        requireNotNull(uid) { "Cannot set session: User is not logged in." }

        sessionDb.collection(FirestoreCollections.SESSIONS).document(uid).collection("user_sessions")
            .document(sessionId)
            .set(session.toFirestoreMap())
            .addOnSuccessListener {
                Log.d("ProdDataRepository", "Session saved with fixed ID: $sessionId")
            }
            .addOnFailureListener { e ->
                Log.e("ProdDataRepository", "Failed to save fixed session: $sessionId", e)
            }
    }

    override fun getSessions(): Flow<List<FocusSession>> = callbackFlow {
        val uid = currentUid

        if (uid == null) {
            trySend(emptyList())
            close(IllegalStateException("User is not logged in."))
            return@callbackFlow
        }

        // Path: sessions -> {uid} -> user_sessions
        val listenerRegistration = sessionDb.collection(FirestoreCollections.SESSIONS)
            .document(uid).collection("user_sessions")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProdDataRepository", "Listen failed for sessions.", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessions = snapshot.documents.map { it.toFocusSession() }
                    trySend(sessions)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

}

private fun FocusSession.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "treeId" to treeId,
        "startTime" to startTime,
        "durationMinutes" to durationMinutes,
        "status" to status,
        "isGroupSession" to isGroupSession,
        "roomId" to roomId
    )
}

fun DocumentSnapshot.toFocusSession(): FocusSession {
    return FocusSession(
        sessionId = id,
        treeId = getString("treeId").orEmpty(),
        startTime = getLong("startTime") ?: 0L,
        durationMinutes = getLong("durationMinutes")?.toInt() ?: 0,
        status = getString("status") ?: "ALIVE",
        isGroupSession = getBoolean("isGroupSession") ?: false,
        roomId = getString("roomId")
    )
}
