package com.example.greenfocus.ui.screen.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.data.model.FriendRequest
import kotlinx.coroutines.launch

@Composable
fun SocialScreen(
    onNavigateToTeamRoom: (String) -> Unit = {},
    viewModel: SocialViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf("Leaderboard") }
    val uiState by viewModel.uiState.collectAsState()
    val invites by viewModel.inviteList.collectAsState()
    var showRequestsDialog by remember { mutableStateOf(false) }
    var inviteErrorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val requests = (uiState as? SocialUiState.Success)?.friendRequests ?: emptyList()
    val totalInvites = requests.size + invites.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7EC))
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Social",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            // Notification Bell Button with Badge
            IconButton(onClick = { showRequestsDialog = true }) {
                BadgedBox(
                    badge = {
                        if (totalInvites > 0) {
                            Badge(
                                containerColor = Color.Red,
                                modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Friend Requests",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        Text(
            text = "Compete with friends",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("Leaderboard", "Friend", "Team")
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Button(
                    onClick = { selectedTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE0E0E0),
                        contentColor = if (isSelected) Color.White else Color.Black
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = tab, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val state = uiState) {
                is SocialUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SocialUiState.Success -> {
                    when (selectedTab) {
                        "Leaderboard" -> LeaderboardScreen(users = state.leaderboard)
                        "Friend" -> FriendsScreen(
                            friends = state.friends,
                            viewModel = viewModel
                        )
                        "Team" -> TeamScreen(
                            onNavigateToRoom = onNavigateToTeamRoom,
                            viewModel = viewModel
                        )
                    }
                }
                is SocialUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Friend Requests Dialog
    if (showRequestsDialog && uiState is SocialUiState.Success) {
        FriendRequestsDialog(
            friendRequests = requests,
            roomInvites = invites,
            inviteErrorMessage = inviteErrorMessage,
            onAcceptFriend = { viewModel.acceptRequest(it) },
            onDeclineFriend = { viewModel.declineRequest(it) },
            onJoinRoomInvite = { invite ->
                inviteErrorMessage = null
                scope.launch {
                    val result = viewModel.acceptInvite(invite)
                    if (result.isSuccess) {
                        showRequestsDialog = false
                        onNavigateToTeamRoom(invite.roomId)
                    } else {
                        inviteErrorMessage = result.exceptionOrNull()?.message ?: "Không thể vào phòng"
                    }
                }
            },
            onDismissInvite = { invite ->
                viewModel.deleteInvite(invite)
            },
            onDismiss = {
                inviteErrorMessage = null
                showRequestsDialog = false
            }
        )
    }
}

@Composable
fun FriendRequestsDialog(
    friendRequests: List<FriendRequest>,
    roomInvites: List<TeamInvite>,
    inviteErrorMessage: String?,
    onAcceptFriend: (FriendRequest) -> Unit,
    onDeclineFriend: (FriendRequest) -> Unit,
    onJoinRoomInvite: (TeamInvite) -> Unit,
    onDismissInvite: (TeamInvite) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Friend Invitations", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (friendRequests.isEmpty() && roomInvites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No pending invitations", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        if (roomInvites.isNotEmpty()) {
                            items(roomInvites) { invite ->
                                RoomInviteRow(
                                    invite = invite,
                                    onJoin = { onJoinRoomInvite(invite) },
                                    onDismiss = { onDismissInvite(invite) }
                                )
                            }
                        }
                        if (friendRequests.isNotEmpty()) {
                            items(friendRequests) { request ->
                                FriendRequestRow(
                                    request = request,
                                    onAccept = { onAcceptFriend(request) },
                                    onDecline = { onDeclineFriend(request) }
                                )
                            }
                        }
                    }
                }

                if (!inviteErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = inviteErrorMessage,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF2E7D32))
            }
        }
    )
}

