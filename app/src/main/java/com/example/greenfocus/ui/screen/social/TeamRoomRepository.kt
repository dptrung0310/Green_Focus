package com.example.greenfocus.ui.screen.social

import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.data.model.User
import com.example.greenfocus.di.FirebaseModule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TeamRoomRepository(
    private val db: FirebaseFirestore = FirebaseModule.firestore
) {
    private val roomsCollection = db.collection("rooms")

    private fun roomDoc(roomId: String) = roomsCollection.document(roomId)
    private fun membersCol(roomId: String) = roomDoc(roomId).collection("members")
    private fun invitesCol(uid: String) = db.collection("users").document(uid).collection("invites")

    suspend fun createRoom(roomId: String, host: User): Result<Unit> = runCatching {
        val roomData = mapOf(
            "status" to ROOM_STATUS_WAITING,
            "hostId" to host.uid,
            "startedAt" to null,
            "durationMs" to DEFAULT_ROOM_DURATION_MS,
            "treeId" to TreeType.DEFAULT.id,
            "focusLostAt" to null
        )
        val memberData = mapOf(
            "uid" to host.uid,
            "displayName" to host.displayName,
            "avatarUrl" to host.avatarUrl,
            "joinedAt" to FieldValue.serverTimestamp(),
            "role" to ROLE_HOST,
            "lastFocusLostAt" to null
        )
        val roomRef = roomDoc(roomId)
        val memberRef = membersCol(roomId).document(host.uid)
        db.runBatch { batch ->
            batch.set(roomRef, roomData)
            batch.set(memberRef, memberData)
        }.await()
    }

    suspend fun joinRoom(roomId: String, user: User): Result<Unit> = runCatching {
        val roomSnapshot = roomDoc(roomId).get().await()
        if (!roomSnapshot.exists()) {
            throw IllegalStateException("Room does not exist")
        }
        val memberData = mapOf(
            "uid" to user.uid,
            "displayName" to user.displayName,
            "avatarUrl" to user.avatarUrl,
            "joinedAt" to FieldValue.serverTimestamp(),
            "role" to ROLE_MEMBER,
            "lastFocusLostAt" to null
        )
        membersCol(roomId).document(user.uid).set(memberData).await()
    }

    suspend fun leaveRoom(roomId: String, userId: String): Result<Unit> = runCatching {
        val roomRef = roomDoc(roomId)
        val membersRef = membersCol(roomId)
        val roomSnapshot = roomRef.get().await()
        if (!roomSnapshot.exists()) return@runCatching

        val membersSnapshot = membersRef.get().await()
        val members = membersSnapshot.documents.mapNotNull { doc ->
            doc.toObject(TeamMember::class.java)
        }
        val remainingMembers = members.filter { member -> member.uid != userId }

        val batch = db.batch()
        batch.delete(membersRef.document(userId))

        if (remainingMembers.isEmpty()) {
            batch.delete(roomRef)
        } else {
            val hostId = roomSnapshot.getString("hostId") ?: ""
            if (hostId == userId) {
                val newHost = remainingMembers
                    .sortedBy { member -> member.joinedAt?.toDate()?.time ?: Long.MAX_VALUE }
                    .first()
                batch.update(roomRef, "hostId", newHost.uid)
                batch.update(membersRef.document(newHost.uid), "role", ROLE_HOST)
            }
        }

        batch.commit().await()
    }

    suspend fun startRoom(roomId: String, durationMs: Long): Result<Unit> = runCatching {
        roomDoc(roomId).update(
            mapOf(
                "status" to ROOM_STATUS_STARTED,
                "startedAt" to FieldValue.serverTimestamp(),
                "durationMs" to durationMs,
                "focusLostAt" to FieldValue.delete()
            )
        ).await()
    }

    suspend fun updateRoomTree(roomId: String, treeId: String): Result<Unit> = runCatching {
        roomDoc(roomId).update("treeId", treeId).await()
    }

    suspend fun resetRoomToWaiting(roomId: String): Result<Unit> = runCatching {
        roomDoc(roomId).update(
            mapOf(
                "status" to ROOM_STATUS_WAITING,
                "startedAt" to FieldValue.delete(),
                "focusLostAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun markFocusLost(roomId: String, user: User): Result<Unit> = runCatching {
        val roomRef = roomDoc(roomId)
        val memberRef = membersCol(roomId).document(user.uid)
        db.runBatch { batch ->
            batch.update(memberRef, "lastFocusLostAt", FieldValue.serverTimestamp())
            batch.update(
                roomRef,
                mapOf(
                    "status" to ROOM_STATUS_WAITING,
                    "startedAt" to FieldValue.delete(),
                    "focusLostAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun sendInvite(friendUid: String, roomId: String, fromUser: User): Result<Unit> = runCatching {
        val inviteRef = invitesCol(friendUid).document()
        val inviteData = mapOf(
            "roomId" to roomId,
            "fromUid" to fromUser.uid,
            "fromName" to fromUser.displayName,
            "createdAt" to FieldValue.serverTimestamp()
        )
        inviteRef.set(inviteData).await()
    }

    suspend fun deleteInvite(uid: String, inviteId: String): Result<Unit> = runCatching {
        invitesCol(uid).document(inviteId).delete().await()
    }

    suspend fun roomExists(roomId: String): Boolean {
        return roomDoc(roomId).get().await().exists()
    }

    fun observeRoom(roomId: String): Flow<TeamRoom?> = callbackFlow {
        val listener = roomDoc(roomId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val room = snapshot.toObject(TeamRoom::class.java)?.copy(roomId = snapshot.id)
                trySend(room)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeMembers(roomId: String): Flow<List<TeamMember>> = callbackFlow {
        val listener = membersCol(roomId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val members = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(TeamMember::class.java)
            } ?: emptyList()
            trySend(members)
        }
        awaitClose { listener.remove() }
    }

    fun observeInvites(uid: String): Flow<List<TeamInvite>> = callbackFlow {
        val listener = invitesCol(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val invites = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(TeamInvite::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(invites)
        }
        awaitClose { listener.remove() }
    }
}
