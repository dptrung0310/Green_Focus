package com.example.greenfocus.data.repository

import com.example.greenfocus.data.model.FriendRequest
import com.example.greenfocus.data.model.User
import com.example.greenfocus.data.model.FocusSession
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


sealed class PurchaseResult {
    object Success : PurchaseResult()
    object InsufficientFunds : PurchaseResult()
    object AlreadyOwned : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

interface UserRepository {
    suspend fun getCurrentUserProfile(): User?
    fun getCurrentUserProfileFlow(): Flow<User?>
    suspend fun updateUser(user: User): Boolean
    suspend fun addExperience(xpToAdd: Int): Boolean
    suspend fun addCoins(amount: Int): Boolean
    suspend fun purchaseTree(uid: String, treeId: String, price: Int): PurchaseResult
    suspend fun getUsersByIds(uids: List<String>): List<User>
    suspend fun getUserByEmail(email: String): User?
    suspend fun sendFriendRequest(request: FriendRequest): Boolean
    fun getFriendRequestsFlow(uid: String): Flow<List<FriendRequest>>
    suspend fun acceptFriendRequest(request: FriendRequest): Boolean
    suspend fun declineFriendRequest(requestId: String): Boolean
    suspend fun isFriendRequestSent(fromUid: String, toUid: String): Boolean
    suspend fun updateFocusStats(coinsIncrement: Int, durationSeconds: Long, treesPlantedIncrement: Int, xpIncrement: Int): Boolean
    suspend fun syncUserStatsWithSessions(): Boolean
}

class ProdUserRepository : UserRepository {
    private val db = FirebaseModule.firestore
    private val usersCollection = db.collection(FirestoreCollections.USERS)
    private val requestsCollection = db.collection(FirestoreCollections.FRIEND_REQUESTS)

    override suspend fun getCurrentUserProfile(): User? {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return null
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

    override fun getCurrentUserProfileFlow(): Flow<User?> = callbackFlow {
        val uid = FirebaseModule.auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listenerRegistration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(User::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listenerRegistration.remove() }
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

    override suspend fun addExperience(xpToAdd: Int): Boolean {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return false
        return try {
            val userRef = usersCollection.document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction false
                
                var newXp = user.experience + xpToAdd
                var newLevel = user.level
                var xpNeeded = newLevel * 500
                
                while (newXp >= xpNeeded) {
                    newXp -= xpNeeded
                    newLevel++
                    xpNeeded = newLevel * 500
                }

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

    override suspend fun purchaseTree(uid: String, treeId: String, price: Int): PurchaseResult {
        return try {
            val userRef = usersCollection.document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java)
                    ?: return@runTransaction PurchaseResult.Error("User not found")

                if (user.unlockedTreeIds.contains(treeId)) {
                    return@runTransaction PurchaseResult.AlreadyOwned
                }
                if (user.coins < price) {
                    return@runTransaction PurchaseResult.InsufficientFunds
                }

                // Atomic: trừ coins + thêm treeId
                transaction.update(
                    userRef,
                    mapOf(
                        "coins" to user.coins - price,
                        "unlockedTreeIds" to FieldValue.arrayUnion(treeId)
                    )
                )
                PurchaseResult.Success
            }.await()

        } catch (e: Exception) {
            e.printStackTrace()
            PurchaseResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun addCoins(amount: Int): Boolean {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return false
        return try {
            usersCollection.document(uid).update("coins", FieldValue.increment(amount.toLong())).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun updateFocusStats(
        coinsIncrement: Int,
        durationSeconds: Long,
        treesPlantedIncrement: Int,
        xpIncrement: Int
    ): Boolean {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return false
        return try {
            val userRef = usersCollection.document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction false
                
                var newXp = user.experience + xpIncrement
                var newLevel = user.level
                var xpNeeded = newLevel * 500
                while (xpNeeded > 0 && newXp >= xpNeeded) {
                    newXp -= xpNeeded
                    newLevel++
                    xpNeeded = newLevel * 500
                }
                
                val updates = mapOf(
                    "coins" to user.coins + coinsIncrement,
                    "totalFocusTime" to user.totalFocusTime + durationSeconds,
                    "totalTreesPlanted" to user.totalTreesPlanted + treesPlantedIncrement,
                    "experience" to newXp,
                    "level" to newLevel
                )
                transaction.update(userRef, updates)
                true
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun syncUserStatsWithSessions(): Boolean {
        val uid = FirebaseModule.auth.currentUser?.uid ?: return false
        return try {
            val sessionsSnapshot = db.collection(FirestoreCollections.SESSIONS)
                .document(uid).collection("user_sessions")
                .get().await()
            
            val sessions = sessionsSnapshot.toObjects(FocusSession::class.java)
            val aliveCount = sessions.count { it.status == "ALIVE" }
            val totalFocusTimeSeconds = sessions.filter { it.status == "ALIVE" }
                .sumOf { it.durationMinutes * 60L }
            
            val userRef = usersCollection.document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction false
                
                if (user.totalTreesPlanted != aliveCount || user.totalFocusTime != totalFocusTimeSeconds) {
                    transaction.update(userRef, mapOf(
                        "totalTreesPlanted" to aliveCount,
                        "totalFocusTime" to totalFocusTimeSeconds
                    ))
                }
                true
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getUsersByIds(uids: List<String>): List<User> {
        if (uids.isEmpty()) return emptyList()
        return try {
            val result = mutableListOf<User>()
            val chunks = uids.chunked(10)
            for (chunk in chunks) {
                val querySnapshot = usersCollection.whereIn("uid", chunk).get().await()
                result.addAll(querySnapshot.toObjects(User::class.java))
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getUserByEmail(email: String): User? {
        return try {
            val querySnapshot = usersCollection.whereEqualTo("email", email).limit(1).get().await()
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].toObject(User::class.java)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun isFriendRequestSent(fromUid: String, toUid: String): Boolean {
        return try {
            val query = requestsCollection
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("toUid", toUid)
                .limit(1)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendFriendRequest(request: FriendRequest): Boolean {
        return try {
            val docRef = requestsCollection.document()
            val finalRequest = request.copy(id = docRef.id)
            docRef.set(finalRequest).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getFriendRequestsFlow(uid: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = requestsCollection.whereEqualTo("toUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.toObjects(FriendRequest::class.java) ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun acceptFriendRequest(request: FriendRequest): Boolean {
        return try {
            db.runTransaction { transaction ->
                val currentUid = request.toUid
                val friendUid = request.fromUid
                
                transaction.update(usersCollection.document(currentUid), 
                    "friendIds", FieldValue.arrayUnion(friendUid))
                transaction.update(usersCollection.document(friendUid), 
                    "friendIds", FieldValue.arrayUnion(currentUid))
                transaction.delete(requestsCollection.document(request.id))
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun declineFriendRequest(requestId: String): Boolean {
        return try {
            requestsCollection.document(requestId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
