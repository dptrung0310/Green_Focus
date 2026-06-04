package com.example.greenfocus.ui.screen.social

import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.data.model.User
import com.example.greenfocus.di.FirebaseModule
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentReference
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
    private fun roomInvitesCol(roomId: String) = roomDoc(roomId).collection("invites")
    private fun legacyInvitesCol(uid: String) = db.collection("users").document(uid).collection("invites")

    suspend fun createRoom(
        roomId: String,
        host: User,
        treeId: String = TreeType.DEFAULT.id,
        durationMs: Long = DEFAULT_ROOM_DURATION_MS
    ): Result<Unit> = runCatching {
        val roomData = mapOf(
            "status" to ROOM_STATUS_WAITING,
            "hostId" to host.uid,
            "startedAt" to null,
            "durationMs" to durationMs,
            "treeId" to treeId,
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

        if (remainingMembers.isEmpty()) {
            membersSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            val inviteSnapshot = roomInvitesCol(roomId).get().await()
            inviteSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.delete(roomRef)
        } else {
            batch.delete(membersRef.document(userId))
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

    suspend fun leaveAllRoomsForUser(userId: String): Result<Unit> = runCatching {
        val memberSnapshots = db.collectionGroup("members")
            .whereEqualTo("uid", userId)
            .get()
            .await()

        val roomIds = memberSnapshots.documents.mapNotNull { memberDoc ->
            memberDoc.reference.parent.parent?.id
        }.distinct()

        roomIds.forEach { roomId ->
            leaveRoom(roomId, userId).getOrThrow()
        }
    }

    suspend fun deleteRoom(roomId: String): Result<Unit> = runCatching {
        val roomRef = roomDoc(roomId)
        val membersSnapshot = membersCol(roomId).get().await()
        val invitesSnapshot = roomInvitesCol(roomId).get().await()

        val documentRefs = buildList<DocumentReference> {
            add(roomRef)
            membersSnapshot.documents.forEach { add(it.reference) }
            invitesSnapshot.documents.forEach { inviteDoc ->
                add(inviteDoc.reference)
                val invite = inviteDoc.toObject(TeamInvite::class.java)
                val inviteId = inviteDoc.id
                val invitedUid = invite?.toUid.orEmpty()
                if (invitedUid.isNotBlank() && inviteId.isNotBlank()) {
                    add(legacyInvitesCol(invitedUid).document(inviteId))
                }
            }
        }.distinctBy { it.path }

        documentRefs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { ref -> batch.delete(ref) }
            batch.commit().await()
        }
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

    suspend fun updateRoomDuration(roomId: String, durationMs: Long): Result<Unit> = runCatching {
        roomDoc(roomId).update("durationMs", durationMs).await()
    }

    suspend fun setRoomStatusWaiting(roomId: String): Result<Unit> = runCatching {
        roomDoc(roomId).update(
            mapOf(
                "status" to ROOM_STATUS_WAITING,
                "startedAt" to FieldValue.delete()
            )
        ).await()
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

    suspend fun sendInvite(roomId: String, friendUid: String, fromUser: User): Result<Unit> = runCatching {
        val existing = roomInvitesCol(roomId)
            .whereEqualTo("toUid", friendUid)
            .limit(1)
            .get()
            .await()
        if (!existing.isEmpty) return@runCatching

        val inviteRef = roomInvitesCol(roomId).document()
        val inviteId = inviteRef.id
        val inviteData = mapOf(
            "roomId" to roomId,
            "fromUid" to fromUser.uid,
            "fromName" to fromUser.displayName,
            "toUid" to friendUid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        
        db.runBatch { batch ->
            batch.set(roomInvitesCol(roomId).document(inviteId), inviteData)
            batch.set(legacyInvitesCol(friendUid).document(inviteId), inviteData)
        }.await()
    }

    suspend fun deleteInvite(roomId: String, inviteId: String): Result<Unit> = runCatching {
        roomInvitesCol(roomId).document(inviteId).delete().await()
    }

    suspend fun deleteInviteForUser(uid: String, invite: TeamInvite): Result<Unit> = runCatching {
        val batch = db.batch()
        if (invite.roomId.isNotBlank() && invite.id.isNotBlank()) {
            batch.delete(roomInvitesCol(invite.roomId).document(invite.id))
        }
        if (invite.id.isNotBlank()) {
            batch.delete(legacyInvitesCol(uid).document(invite.id))
        }
        batch.commit().await()
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

    fun observeRoomInvites(roomId: String): Flow<List<TeamInvite>> = callbackFlow {
        val listener = roomInvitesCol(roomId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val invites = snapshot?.documents?.mapNotNull { doc ->
                val invite = doc.toObject(TeamInvite::class.java) ?: return@mapNotNull null
                invite.copy(id = doc.id, roomId = invite.roomId.ifBlank { roomId })
            } ?: emptyList()
            trySend(invites)
        }
        awaitClose { listener.remove() }
    }

    fun observeInvites(uid: String): Flow<List<TeamInvite>> = callbackFlow {
        var roomInvites: List<TeamInvite> = emptyList()
        var legacyInvites: List<TeamInvite> = emptyList()

        val query = db.collectionGroup("invites").whereEqualTo("toUid", uid)
        val roomListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("TeamRoomRepository", "Error observing room invites via collectionGroup: ${error.message}", error)
                return@addSnapshotListener
            }
            val invites = snapshot?.documents?.mapNotNull { doc ->
                val invite = doc.toObject(TeamInvite::class.java) ?: return@mapNotNull null
                val resolvedRoomId = invite.roomId.ifBlank {
                    doc.reference.parent.parent?.id.orEmpty()
                }
                invite.copy(id = doc.id, roomId = resolvedRoomId)
            } ?: emptyList()
            roomInvites = invites
            trySend(mergeInvites(roomInvites, legacyInvites))
        }
        val legacyListener = legacyInvitesCol(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("TeamRoomRepository", "Error observing legacy invites: ${error.message}", error)
                return@addSnapshotListener
            }
            val invites = snapshot?.documents?.mapNotNull { doc ->
                val invite = doc.toObject(TeamInvite::class.java) ?: return@mapNotNull null
                invite.copy(
                    id = doc.id,
                    toUid = invite.toUid.ifBlank { uid }
                )
            } ?: emptyList()
            legacyInvites = invites
            trySend(mergeInvites(roomInvites, legacyInvites))
        }
        awaitClose {
            roomListener.remove()
            legacyListener.remove()
        }
    }

    private fun mergeInvites(
        roomInvites: List<TeamInvite>,
        legacyInvites: List<TeamInvite>
    ): List<TeamInvite> {
        return (roomInvites + legacyInvites)
            .distinctBy { "${it.roomId}|${it.fromUid}|${it.toUid}" }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }
}
