package com.example.greenfocus.ui.screen.pomodoro

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenfocus.data.model.User
import com.example.greenfocus.data.repository.ProdUserRepository
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.ui.screen.social.DEFAULT_ROOM_DURATION_MS
import com.example.greenfocus.ui.screen.social.TeamInvite
import com.example.greenfocus.ui.screen.social.TeamMember
import com.example.greenfocus.ui.screen.social.TeamRoom
import com.example.greenfocus.ui.screen.social.TeamRoomRepository
import com.example.greenfocus.ui.screen.social.generateRoomId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the team-room lifecycle for the Home (Pomodoro) tab.
 *
 * It reuses the Social [TeamRoomRepository] for room/member/invite sync, but deliberately
 * does NOT run the focus timer here: the local [PomodoroViewModel] + TimerManager keep
 * driving the countdown and Home's own deep-focus enforcement. This VM only syncs the
 * shared room config (tree, duration, status) and membership.
 */
data class HomeRoomUiState(
    val activeRoomId: String? = null,
    val room: TeamRoom? = null,
    val members: List<TeamMember> = emptyList(),
    val roomInvites: List<TeamInvite> = emptyList(),
    val isHost: Boolean = false,
    val roomClosed: Boolean = false,
    val errorMessage: String? = null
)

class HomeRoomViewModel(
    private val teamRoomRepository: TeamRoomRepository = TeamRoomRepository(),
    private val userRepository: UserRepository = ProdUserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeRoomUiState())
    val uiState: StateFlow<HomeRoomUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var currentUser: User? = null
    private var hasSeenRoom = false
    private var exitCleanupHandled = false

    init {
        viewModelScope.launch {
            userRepository.getCurrentUserProfileFlow().collect { currentUser = it }
        }
    }

    /** Creates a room seeded with the host's current tree + duration, then enters it. */
    suspend fun createRoom(treeId: String, durationMs: Long): Result<String> {
        val host = currentUser ?: return Result.failure(IllegalStateException("Chua dang nhap"))
        val newRoomId = generateRoomId()
        return teamRoomRepository.createRoom(newRoomId, host, treeId, durationMs)
            .map { newRoomId }
            .onSuccess { enterRoom(it) }
    }

    /** Joins an existing room by id, then enters it. Fails if the room does not exist. */
    suspend fun joinRoom(targetRoomId: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Chua dang nhap"))
        val normalizedId = targetRoomId.trim().uppercase()
        return teamRoomRepository.joinRoom(normalizedId, user)
            .onSuccess { enterRoom(normalizedId) }
    }

    private fun enterRoom(id: String) {
        hasSeenRoom = false
        exitCleanupHandled = false
        _uiState.update { HomeRoomUiState(activeRoomId = id) }
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                teamRoomRepository.observeRoom(id),
                teamRoomRepository.observeMembers(id),
                teamRoomRepository.observeRoomInvites(id)
            ) { room, members, invites ->
                Triple(room, members, invites)
            }.collect { (room, members, invites) ->
                val uid = currentUser?.uid ?: FirebaseModule.auth.currentUser?.uid
                if (room != null) hasSeenRoom = true
                _uiState.update {
                    it.copy(
                        room = room,
                        members = members,
                        roomInvites = invites,
                        isHost = !uid.isNullOrBlank() && room?.hostId == uid,
                        // Distinguish "still loading" (null before first load) from "deleted" (null after).
                        roomClosed = hasSeenRoom && room == null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    /** Host-only: flips the room to "started" so every member begins their local session. */
    fun startRoom(durationMs: Long) {
        val id = _uiState.value.activeRoomId ?: return
        if (!_uiState.value.isHost) return
        viewModelScope.launch {
            teamRoomRepository.startRoom(id, durationMs)
                .onFailure { Log.e(TAG, "startRoom failed", it) }
        }
    }

    /** Host-only: returns the room to the waiting/config state. */
    fun stopRoom() {
        val id = _uiState.value.activeRoomId ?: return
        if (!_uiState.value.isHost) return
        viewModelScope.launch {
            teamRoomRepository.setRoomStatusWaiting(id)
                .onFailure { Log.e(TAG, "stopRoom failed", it) }
        }
    }

    /** Host-only: syncs the selected tree to all members. */
    fun updateRoomTree(treeId: String) {
        val id = _uiState.value.activeRoomId ?: return
        if (!_uiState.value.isHost) return
        viewModelScope.launch {
            teamRoomRepository.updateRoomTree(id, treeId)
                .onFailure { Log.e(TAG, "updateRoomTree failed", it) }
        }
    }

    /** Host-only: syncs the session duration to all members. */
    fun updateRoomDuration(durationMs: Long) {
        val id = _uiState.value.activeRoomId ?: return
        if (!_uiState.value.isHost) return
        viewModelScope.launch {
            teamRoomRepository.updateRoomDuration(id, durationMs)
                .onFailure { Log.e(TAG, "updateRoomDuration failed", it) }
        }
    }

    /** Sends a room invite to a friend; the backend Cloud Function delivers the FCM push. */
    suspend fun inviteFriend(friendUid: String): Result<Unit> {
        val id = _uiState.value.activeRoomId
            ?: return Result.failure(IllegalStateException("Chua o trong phong"))
        val host = currentUser ?: return Result.failure(IllegalStateException("Chua dang nhap"))
        return teamRoomRepository.sendInvite(id, friendUid, host)
    }

    /** Leaves the room and resets to the Home landing state. */
    fun leaveRoom() {
        val id = _uiState.value.activeRoomId ?: return
        val uid = currentUser?.uid ?: FirebaseModule.auth.currentUser?.uid
        observeJob?.cancel()
        observeJob = null
        hasSeenRoom = false
        _uiState.update { HomeRoomUiState() }
        if (uid != null) {
            viewModelScope.launch {
                teamRoomRepository.leaveRoom(id, uid)
                    .onFailure { Log.e(TAG, "leaveRoom failed", it) }
            }
        }
    }

    /**
     * Best-effort cleanup when the app is leaving the foreground or being closed.
     * The room is deleted by the repository only when this user was the last member.
     */
    fun handleAppExit() {
        val roomId = _uiState.value.activeRoomId ?: return
        if (exitCleanupHandled) return
        exitCleanupHandled = true

        val uid = currentUser?.uid ?: FirebaseModule.auth.currentUser?.uid

        observeJob?.cancel()
        observeJob = null
        hasSeenRoom = false
        _uiState.update { HomeRoomUiState() }

        viewModelScope.launch {
            try {
                val result = if (uid != null) {
                    teamRoomRepository.leaveRoom(roomId, uid)
                } else {
                    Result.success(Unit)
                }

                result.onFailure { error ->
                    Log.e(TAG, "App exit cleanup failed", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected app exit cleanup error", e)
            }
        }
    }

    companion object {
        private const val TAG = "HomeRoomViewModel"
        const val DEFAULT_DURATION_MS = DEFAULT_ROOM_DURATION_MS
    }
}
