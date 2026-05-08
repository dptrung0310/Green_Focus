package com.example.greenfocus.data.repository

import com.example.greenfocus.data.model.User
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UserRepository {
    suspend fun getUserProfile(uid: String): User?
    fun getUserProfileFlow(uid: String): Flow<User?>
    suspend fun updateUser(user: User): Boolean
    suspend fun addExperience(uid: String, xpToAdd: Int): Boolean
}

class ProdUserRepository : UserRepository {
    private val db = FirebaseModule.firestore
    private val usersCollection = db.collection(FirestoreCollections.USERS)

    override suspend fun getUserProfile(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getUserProfileFlow(uid: String): Flow<User?> = callbackFlow {
        val listenerRegistration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun updateUser(user: User): Boolean {
        return try {
            if (user.uid.isEmpty()) return false
            usersCollection.document(user.uid).set(user, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun addExperience(uid: String, xpToAdd: Int): Boolean {
        return try {
            val userRef = usersCollection.document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction false
                
                // Logic tính toán cấp độ
                var newXp = user.experience + xpToAdd
                var newLevel = user.level
                var xpNeeded = newLevel * 500
                
                while (newXp >= xpNeeded) {
                    newXp -= xpNeeded
                    newLevel++
                    xpNeeded = newLevel * 500
                }

                // Cập nhật trực tiếp vào transaction
                transaction.update(userRef, mapOf(
                    "experience" to newXp,
                    "level" to newLevel
                ))
                true
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
