package com.example.greenfocus.ui.screen.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenfocus.data.model.FriendRequest
import com.example.greenfocus.data.model.User
import com.example.greenfocus.data.repository.ProdUserRepository
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.di.FirebaseModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SocialUiState {
    object Loading : SocialUiState()
    data class Success(
        val currentUser: User,
        val friends: List<User>,
        val leaderboard: List<User>,
        val friendRequests: List<FriendRequest> = emptyList()
    ) : SocialUiState()
    data class Error(val message: String) : SocialUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModel(
    private val userRepository: UserRepository = ProdUserRepository(),
    private val teamRoomRepository: TeamRoomRepository = TeamRoomRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SocialUiState>(SocialUiState.Loading)
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val _searchResult = MutableStateFlow<User?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _requestSentStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val requestSentStatus = _requestSentStatus.asStateFlow()

    private val _inviteList = MutableStateFlow<List<TeamInvite>>(emptyList())
    val inviteList: StateFlow<List<TeamInvite>> = _inviteList.asStateFlow()

    init {
        observeSocialData()
        observeInvites()
    }

    private fun observeSocialData() {
        viewModelScope.launch {
            userRepository.getCurrentUserProfileFlow()
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(SocialUiState.Error("User not logged in"))
                    } else {
                        userRepository.getFriendRequestsFlow(user.uid).map { requests ->
                            val friends = userRepository.getUsersByIds(user.friendIds)
                            val leaderboard = (friends + user).sortedWith(
                                compareByDescending<User> { it.totalTreesPlanted }.thenBy { it.displayName }
                            )
                            SocialUiState.Success(user, friends, leaderboard, requests)
                        }
                    }
                }
                .catch { e ->
                    emit(SocialUiState.Error(e.message ?: "Unknown error"))
                }
                .collect {
                    _uiState.value = it
                }
        }
    }

    private fun observeInvites() {
        viewModelScope.launch {
            userRepository.getCurrentUserProfileFlow()
                .map { it?.uid }
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    if (uid == null) {
                        flowOf(emptyList())
                    } else {
                        teamRoomRepository.observeInvites(uid)
                            .catch { emit(emptyList()) }
                    }
                }
                .catch { emit(emptyList()) }
                .collect { invites ->
                    _inviteList.value = invites
                    cleanupInvalidInvites(invites)
                }
        }
    }

    private fun cleanupInvalidInvites(invites: List<TeamInvite>) {
        viewModelScope.launch {
            invites.forEach { invite ->
                val uid = FirebaseModule.auth.currentUser?.uid ?: return@forEach
                if (invite.roomId.isBlank()) {
                    teamRoomRepository.deleteInviteForUser(uid, invite)
                    return@forEach
                }
                val exists = teamRoomRepository.roomExists(invite.roomId)
                if (!exists) {
                    teamRoomRepository.deleteInviteForUser(uid, invite)
                }
            }
        }
    }

    fun searchUser(email: String) {
        viewModelScope.launch {
            val user = userRepository.getUserByEmail(email)
            _searchResult.value = user
            if (user != null) {
                val currentUid = FirebaseModule.auth.currentUser?.uid ?: return@launch
                val alreadySent = userRepository.isFriendRequestSent(currentUid, user.uid)
                _requestSentStatus.update { it + (user.uid to alreadySent) }
            }
        }
    }

    fun sendFriendRequest(toUser: User) {
        val currentUser = (uiState.value as? SocialUiState.Success)?.currentUser ?: return
        viewModelScope.launch {
            val request = FriendRequest(
                fromUid = currentUser.uid,
                fromEmail = currentUser.email,
                fromDisplayName = currentUser.displayName,
                fromAvatarUrl = currentUser.avatarUrl,
                toUid = toUser.uid
            )
            if (userRepository.sendFriendRequest(request)) {
                _requestSentStatus.update { it + (toUser.uid to true) }
            }
        }
    }

    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            userRepository.acceptFriendRequest(request)
        }
    }

    fun declineRequest(request: FriendRequest) {
        viewModelScope.launch {
            userRepository.declineFriendRequest(request.id)
        }
    }

    suspend fun createTeamRoom(): Result<String> {
        val currentUser = (uiState.value as? SocialUiState.Success)?.currentUser
            ?: return Result.failure(IllegalStateException("User not logged in"))
        val roomId = generateRoomId()
        val result = teamRoomRepository.createRoom(roomId, currentUser)
        return result.map { roomId }
    }

    suspend fun joinTeamRoom(roomId: String): Result<Unit> {
        val currentUser = (uiState.value as? SocialUiState.Success)?.currentUser
            ?: return Result.failure(IllegalStateException("User not logged in"))
        return teamRoomRepository.joinRoom(roomId, currentUser)
    }

    suspend fun sendRoomInvite(friendUid: String, roomId: String): Result<Unit> {
        val currentUser = (uiState.value as? SocialUiState.Success)?.currentUser
            ?: return Result.failure(IllegalStateException("User not logged in"))
        return teamRoomRepository.sendInvite(roomId, friendUid, currentUser)
    }

    suspend fun acceptInvite(invite: TeamInvite): Result<Unit> {
        val joinResult = joinTeamRoom(invite.roomId)
        if (joinResult.isSuccess) {
            val uid = FirebaseModule.auth.currentUser?.uid
            if (uid != null) {
                teamRoomRepository.deleteInviteForUser(uid, invite)
            }
        }
        return joinResult
    }

    fun deleteInvite(invite: TeamInvite) {
        viewModelScope.launch {
            val uid = FirebaseModule.auth.currentUser?.uid ?: return@launch
            teamRoomRepository.deleteInviteForUser(uid, invite)
        }
    }

    fun clearSearchResult() {
        _searchResult.value = null
    }
}
