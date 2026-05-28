package com.example.greenfocus.ui.screen.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenfocus.data.model.User
import com.example.greenfocus.data.repository.ProdUserRepository
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.di.FirebaseModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class TeamRoomUiState(
    val room: TeamRoom? = null,
    val members: List<TeamMember> = emptyList(),
    val isHost: Boolean = false,
    val durationMs: Long = DEFAULT_ROOM_DURATION_MS,
    val remainingMs: Long = DEFAULT_ROOM_DURATION_MS,
    val isRunning: Boolean = false,
    val isHalfDone: Boolean = false,
    val focusLostMessage: String? = null,
    val errorMessage: String? = null
)

class TeamRoomViewModel(
    private val teamRoomRepository: TeamRoomRepository = TeamRoomRepository(),
    private val userRepository: UserRepository = ProdUserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamRoomUiState())
    val uiState: StateFlow<TeamRoomUiState> = _uiState.asStateFlow()

    private var roomId: String? = null
    private var observeJob: Job? = null
    private var countdownJob: Job? = null
    private var lastStatus: String? = null
    private var lastFocusLostAtMsSeen: Long? = null
    private var activeStartedAtMs: Long? = null
    private var activeDurationMs: Long = DEFAULT_ROOM_DURATION_MS
    private var currentUser: User? = null
    private var hasLeftRoom = false
    private var isReportingFocusLost = false

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUserProfileFlow().collect { user ->
                currentUser = user
            }
        }
    }

    fun bindRoom(id: String) {
        if (roomId == id && observeJob != null) return
        roomId = id
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                teamRoomRepository.observeRoom(id),
                teamRoomRepository.observeMembers(id)
            ) { room, members ->
                room to members
            }.collect { (room, members) ->
                val uid = currentUser?.uid ?: FirebaseModule.auth.currentUser?.uid
                val isHost = room?.hostId == uid && !uid.isNullOrBlank()
                val durationMs = room?.durationMs ?: DEFAULT_ROOM_DURATION_MS
                val currentMember = members.firstOrNull { it.uid == uid }
                val focusLostMessage = resolveFocusLostMessage(room, currentMember)
                _uiState.update {
                    it.copy(
                        room = room,
                        members = members,
                        isHost = isHost,
                        durationMs = durationMs,
                        focusLostMessage = focusLostMessage,
                        errorMessage = if (room == null) "Room not found" else null
                    )
                }
                updateCountdown(room, durationMs)
            }
        }
    }

    fun startRoom() {
        val id = roomId ?: return
        if (!_uiState.value.isHost) return
        viewModelScope.launch {
            teamRoomRepository.startRoom(id, DEFAULT_ROOM_DURATION_MS)
        }
    }

    fun updateRoomTree(treeId: String) {
        val id = roomId ?: return
        if (!_uiState.value.isHost) return
        viewModelScope.launch {
            teamRoomRepository.updateRoomTree(id, treeId)
        }
    }

    fun reportFocusLost() {
        val id = roomId ?: return
        val user = currentUser ?: return
        val room = _uiState.value.room ?: return
        if (room.status != ROOM_STATUS_STARTED) return
        if (isReportingFocusLost) return
        isReportingFocusLost = true
        viewModelScope.launch {
            teamRoomRepository.markFocusLost(id, user)
            isReportingFocusLost = false
        }
    }

    fun stopRoom() {
        val id = roomId ?: return
        viewModelScope.launch {
            teamRoomRepository.resetRoomToWaiting(id)
        }
    }

    fun clearFocusLostMessage() {
        _uiState.update { it.copy(focusLostMessage = null) }
    }

    fun leaveRoom() {
        if (hasLeftRoom) return
        val id = roomId ?: return
        val uid = currentUser?.uid ?: FirebaseModule.auth.currentUser?.uid ?: return
        hasLeftRoom = true
        viewModelScope.launch {
            val room = _uiState.value.room
            if (room?.status == ROOM_STATUS_STARTED && currentUser != null && !isReportingFocusLost) {
                isReportingFocusLost = true
                teamRoomRepository.markFocusLost(id, currentUser!!)
                isReportingFocusLost = false
            }
            teamRoomRepository.leaveRoom(id, uid)
        }
    }

    private fun resolveFocusLostMessage(room: TeamRoom?, currentMember: TeamMember?): String? {
        val status = room?.status
        val previousStatus = lastStatus
        lastStatus = status

        val focusLostAtMs = room?.focusLostAt?.toDate()?.time
        if (currentMember == null) return null
        val joinedAtMs = currentMember.joinedAt?.toDate()?.time

        if (focusLostAtMs != null) {
            val joinedAfterEvent = joinedAtMs != null && joinedAtMs > focusLostAtMs
            val isNewEvent = lastFocusLostAtMsSeen == null || focusLostAtMs > lastFocusLostAtMsSeen!!
            if (isNewEvent && !joinedAfterEvent) {
                lastFocusLostAtMsSeen = focusLostAtMs
                return "Có người đã mất tập trung"
            }
        }

        return when {
            status == ROOM_STATUS_STARTED -> null
            previousStatus == ROOM_STATUS_STARTED && status == ROOM_STATUS_WAITING -> _uiState.value.focusLostMessage
            else -> _uiState.value.focusLostMessage
        }
    }

    private fun updateCountdown(room: TeamRoom?, durationMs: Long) {
        if (room == null || room.status != ROOM_STATUS_STARTED || room.startedAt == null) {
            stopCountdown(durationMs)
            return
        }

        val startedAtMs = room.startedAt.toDate().time
        if (activeStartedAtMs == startedAtMs && activeDurationMs == durationMs && countdownJob != null) {
            return
        }

        activeStartedAtMs = startedAtMs
        activeDurationMs = durationMs
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startedAtMs
                val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                val isHalfDone = remaining <= durationMs / 2
                _uiState.update {
                    it.copy(
                        remainingMs = remaining,
                        isRunning = remaining > 0L,
                        isHalfDone = isHalfDone
                    )
                }
                if (remaining <= 0L) break
                delay(1000L)
            }
        }
    }

    private fun stopCountdown(durationMs: Long) {
        activeStartedAtMs = null
        activeDurationMs = durationMs
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                remainingMs = durationMs,
                isRunning = false,
                isHalfDone = false
            )
        }
    }
}
