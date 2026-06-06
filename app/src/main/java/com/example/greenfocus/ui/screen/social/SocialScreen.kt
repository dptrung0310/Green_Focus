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
import com.example.greenfocus.ui.screen.pomodoro.HomeRoomViewModel
import kotlinx.coroutines.launch

@Composable
fun SocialScreen(
    homeRoomViewModel: HomeRoomViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {},
    viewModel: SocialViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf("Bảng xếp hạng") }
    val uiState by viewModel.uiState.collectAsState()

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
                text = "Cộng đồng",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        }
        
        Text(
            text = "Thi đua cùng bạn bè",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("Bảng xếp hạng", "Bạn bè")
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
                        "Bảng xếp hạng" -> LeaderboardScreen(users = state.leaderboard)
                        "Bạn bè" -> FriendsScreen(
                            friends = state.friends,
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
        title = { Text("Lời mời kết bạn", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (friendRequests.isEmpty() && roomInvites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Không có lời mời nào", color = Color.Gray)
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
                Text("Đóng", color = Color(0xFF2E7D32))
            }
        }
    )
}

