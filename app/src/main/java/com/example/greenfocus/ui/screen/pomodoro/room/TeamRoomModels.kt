package com.example.greenfocus.ui.screen.pomodoro.room

import com.example.greenfocus.data.model.TreeType
import com.google.firebase.Timestamp
import kotlin.random.Random

const val ROOM_STATUS_WAITING = "waiting"
const val ROOM_STATUS_STARTED = "started"
const val ROOM_STATUS_CANCELLED = "cancelled"
const val ROOM_STATUS_FINISHED = "finished"

const val ROLE_HOST = "host"
const val ROLE_MEMBER = "member"

const val DEFAULT_ROOM_DURATION_MS = 25 * 60 * 1000L


data class TeamRoom(
    val roomId: String = "",
    val status: String = ROOM_STATUS_WAITING,
    val hostId: String = "",
    val startedAt: Timestamp? = null,
    val durationMs: Long = DEFAULT_ROOM_DURATION_MS,
    val treeId: String = TreeType.DEFAULT.id,
    val focusLostAt: Timestamp? = null,
    val focusLostByUid: String? = null,
    val focusLostEventId: String? = null,
    val completedAt: Timestamp? = null,
    val completedByUid: String? = null,
    val completedEventId: String? = null
)

data class TeamMember(
    val uid: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val joinedAt: Timestamp? = null,
    val role: String = ROLE_MEMBER,
    val lastFocusLostAt: Timestamp? = null,
    val lastFocusLostEventId: String? = null,
    val lastHandledFocusLostEventId: String? = null,
    val lastHandledCompletedEventId: String? = null
)

data class TeamInvite(
    val id: String = "",
    val roomId: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val createdAt: Timestamp? = null
)

fun generateRoomId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}
