package com.example.greenfocus.ui.screen.social

import android.app.AppOpsManager
import android.app.Application
import android.os.Bundle
import android.content.Context
import android.os.Process
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.greenfocus.R
import com.example.greenfocus.data.model.TreeResourceMapper
import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.data.model.User
import com.example.greenfocus.ui.screen.pomodoro.PomodoroViewModel
import com.example.greenfocus.ui.screen.pomodoro.TimerBlock
import com.example.greenfocus.ui.screen.pomodoro.TreeSelectionRow
import com.example.greenfocus.ui.screen.pomodoro.UsageStatsRationaleDialog
import kotlinx.coroutines.launch

@Composable
fun TeamRoomScreen(
    roomId: String,
    onBack: () -> Unit,
    pomodoroViewModel: PomodoroViewModel = viewModel(factory = PomodoroViewModel.Factory),
    teamRoomViewModel: TeamRoomViewModel = viewModel(factory = TeamRoomViewModel.Factory),
    socialViewModel: SocialViewModel = viewModel()
) {
    val pomodoroUiState by pomodoroViewModel.pomodoroUiState.collectAsState()
    val teamRoomUiState by teamRoomViewModel.uiState.collectAsState()
    val socialUiState by socialViewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showInviteDialog by remember { mutableStateOf(false) }
    var pendingInviteIds by remember(roomId) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(roomId) {
        teamRoomViewModel.bindRoom(roomId)
    }

    val room = teamRoomUiState.room

    // Sync room started status to start local countdown and foreground service
    LaunchedEffect(room?.status, room?.startedAt) {
        if (room != null && room.status == ROOM_STATUS_STARTED && room.startedAt != null) {
            val startedAtMs = room.startedAt.toDate().time
            val elapsed = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
            val remainingSeconds = (((room.durationMs - elapsed) / 1000L).toInt())
                .coerceIn(0, (room.durationMs / 1000L).toInt())
            
            if (remainingSeconds > 0 && !pomodoroUiState.isTimerRunning) {
                // 1. Configure remaining time in seconds
                pomodoroViewModel.setTimerSeconds(remainingSeconds)
                // 2. Select host's tree
                val resolvedTree = resolveRoomTree(room.treeId, pomodoroUiState.unlockedTrees)
                pomodoroViewModel.updateSelectedTree(resolvedTree)
                // 3. Start foreground service
                pomodoroViewModel.startTimerService(context)
            }
        } else if (room != null && room.status == ROOM_STATUS_WAITING) {
            if (pomodoroUiState.isTimerRunning) {
                pomodoroViewModel.stopTimerService(context)
            }
        }
    }

    DisposableEffect(context.applicationContext) {
        val app = context.applicationContext as? Application
        if (app == null) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }

        var startedCount = 0
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: android.app.Activity) {
                startedCount += 1
            }
            override fun onActivityResumed(activity: android.app.Activity) = Unit
            override fun onActivityPaused(activity: android.app.Activity) = Unit
            override fun onActivityStopped(activity: android.app.Activity) {
                startedCount = (startedCount - 1).coerceAtLeast(0)
                if (startedCount == 0 && activity.isFinishing && !activity.isChangingConfigurations) {
                    teamRoomViewModel.leaveRoom()
                }
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: android.app.Activity) = Unit
        }

        app.registerActivityLifecycleCallbacks(callbacks)
        onDispose { app.unregisterActivityLifecycleCallbacks(callbacks) }
    }

    val timerScale by animateFloatAsState(
        targetValue = if (pomodoroUiState.isTimerRunning) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 600),
        label = "timerScale"
    )

    val formattedTime = pomodoroUiState.formattedTime
    val progress = pomodoroUiState.currentPercentage
    val rewardCoins = (teamRoomUiState.durationMs / 60000L).toInt()
    val isWaiting = teamRoomUiState.room?.status == ROOM_STATUS_WAITING
    val roomTreeId = teamRoomUiState.room?.treeId?.ifBlank { null } ?: pomodoroUiState.selectedTree.id
    val activeTree = resolveRoomTree(
        treeId = roomTreeId,
        unlockedTrees = pomodoroUiState.unlockedTrees
    )
    val invitedIds = teamRoomUiState.roomInvites
        .mapNotNull { invite -> invite.toUid.takeIf { it.isNotBlank() } }
        .toSet()
    val disabledInviteIds = invitedIds + pendingInviteIds

    LaunchedEffect(invitedIds) {
        if (pendingInviteIds.isNotEmpty()) {
            pendingInviteIds = pendingInviteIds - invitedIds
        }
    }

    LaunchedEffect(pomodoroUiState.isTimerFinished, teamRoomUiState.isHost, room?.status) {
        if (pomodoroUiState.isTimerFinished && room?.status == ROOM_STATUS_STARTED) {
            if (teamRoomUiState.isHost) {
                teamRoomViewModel.stopRoom()
            }
            pomodoroViewModel.resetAfterSession((room.durationMs / 60000L).toInt())
        }
    }

    LaunchedEffect(pomodoroUiState.isTimerFailed, room?.status) {
        if (pomodoroUiState.isTimerFailed && room?.status == ROOM_STATUS_STARTED) {
            teamRoomViewModel.reportFocusLost()
            pomodoroViewModel.resetAfterSession((room.durationMs / 60000L).toInt())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7EC))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // 1. Header Row: Back Button and Invite Button
        if (!pomodoroUiState.isTimerRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    teamRoomViewModel.leaveRoom()
                    pomodoroViewModel.resetToDefault()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF2E7D32)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID Room: $roomId",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(roomId)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(onClick = { showInviteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Invite Friend",
                        tint = Color(0xFF2E7D32)
                    )
                }
            }
        }

        if (!teamRoomUiState.errorMessage.isNullOrBlank()) {
            Text(
                text = teamRoomUiState.errorMessage ?: "",
                color = Color.Red,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!pomodoroUiState.isTimerRunning) {
            TeamMembersRow(members = teamRoomUiState.members)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Timer Block (Reuse from PomodoroScreen)
        TimerBlock(
            currentTime = formattedTime,
            currentTree = activeTree.imageStaticSeed,
            currentTreeName = stringResource(id = activeTree.name),
            currentProgress = progress,
            isHalfDone = pomodoroUiState.isHalfDone,
            seedImage = activeTree.imageStaticSeed,
            bigImage = activeTree.imageStaticBig,
            onTimerClick = {},
            isTimerRunning = pomodoroUiState.isTimerRunning,
            modifier = Modifier.scale(timerScale)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Reward Info Card
        AnimatedVisibility(
            visible = !pomodoroUiState.isTimerRunning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Reward Icon",
                    tint = Color(0xFFFBC02D),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Hoàn thành để kiếm được: $rewardCoins xu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Deep Focus Mode Toggle Block
        if (teamRoomUiState.isHost) {
            AnimatedVisibility(
                visible = !pomodoroUiState.isTimerRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .toggleable(
                                value = pomodoroUiState.isDeepFocusEnabled,
                                enabled = !pomodoroUiState.isTimerRunning,
                                onValueChange = {
                                    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                                    val mode = appOps.checkOpNoThrow(
                                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                                        Process.myUid(),
                                        context.packageName
                                    )
                                    if (mode == AppOpsManager.MODE_ALLOWED) {
                                        pomodoroViewModel.toggleDeepFocus()
                                    } else {
                                        pomodoroViewModel.toggleUsageStatsRationaleDialog()
                                    }
                                },
                                role = Role.Switch
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = Color(0xFF388E3C),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.pomodoro_deep_mode_button),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = pomodoroUiState.isDeepFocusEnabled,
                            onCheckedChange = null,
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF388E3C),
                                checkedTrackColor = Color(0xFFC8E6C9),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                }
            }
        }

        // 5. Tree Selection Row
        if (teamRoomUiState.isHost) {
            AnimatedVisibility(
                visible = !pomodoroUiState.isTimerRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    TreeSelectionRow(
                        isTimerRunning = pomodoroUiState.isTimerRunning,
                        selectedTree = activeTree,
                        unlockedTrees = pomodoroUiState.unlockedTrees,
                        changeSelectedTree = {
                            pomodoroViewModel.updateSelectedTree(it)
                            teamRoomViewModel.updateRoomTree(it.id)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. Start / Waiting State vs Stop button
        if (!pomodoroUiState.isTimerRunning) {
            if (teamRoomUiState.isHost && isWaiting) {
                Button(
                    onClick = { teamRoomViewModel.startRoom() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pomodoro_start_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else if (isWaiting) {
                Text(
                    text = "Chờ chủ phòng bắt đầu...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            TextButton(
                onClick = { teamRoomViewModel.reportFocusLost() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.pomodoro_stop_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
        }
        }
    }

    if (showInviteDialog) {
        val friends = (socialUiState as? SocialUiState.Success)?.friends ?: emptyList()
        val memberIds = teamRoomUiState.members.map { it.uid }.toSet()
        val availableFriends = friends.filter { it.uid !in memberIds }
        InviteFriendDialog(
            friends = availableFriends,
            invitedIds = disabledInviteIds,
            onInvite = { friend ->
                if (disabledInviteIds.contains(friend.uid)) return@InviteFriendDialog
                pendingInviteIds = pendingInviteIds + friend.uid
                scope.launch {
                    val result = socialViewModel.sendRoomInvite(friend.uid, roomId)
                    if (result.isFailure) {
                        pendingInviteIds = pendingInviteIds - friend.uid
                    }
                }
            },
            onDismiss = { showInviteDialog = false }
        )
    }

    if (!teamRoomUiState.focusLostMessage.isNullOrBlank()) {
        FocusLostDialog(
            message = teamRoomUiState.focusLostMessage ?: "",
            onDismiss = { teamRoomViewModel.clearFocusLostMessage() }
        )
    }

    if (pomodoroUiState.showUsageStatsRationaleDialog) {
        UsageStatsRationaleDialog(
            onDismiss = { pomodoroViewModel.toggleUsageStatsRationaleDialog() },
            context = context
        )
    }
}

@Composable
private fun TeamMembersRow(members: List<TeamMember>) {
    if (members.isEmpty()) {
        Text(text = "Chưa có thành viên", color = Color.Gray, fontSize = 12.sp)
        return
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(members) { member ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp)
            ) {
                AsyncImage(
                    model = member.avatarUrl.ifBlank { "https://ui-avatars.com/api/?name=${member.displayName}" },
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = member.displayName,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InviteFriendDialog(
    friends: List<User>,
    invitedIds: Set<String>,
    onInvite: (User) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mời bạn bè", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
        text = {
            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có bạn bè để mời", color = Color.Gray)
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(friends) { friend ->
                        val isInvited = invitedIds.contains(friend.uid)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(88.dp)
                        ) {
                            AsyncImage(
                                model = friend.avatarUrl.ifBlank { "https://ui-avatars.com/api/?name=${friend.displayName}" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = friend.displayName,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = Color(0xFF2C3E50),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { onInvite(friend) },
                                enabled = !isInvited,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text(
                                    text = if (isInvited) "Đã mời" else "Mời",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
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

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun resolveRoomTree(treeId: String, unlockedTrees: List<TreeType>): TreeType {
    val unlocked = unlockedTrees.firstOrNull { it.id == treeId }
    if (unlocked != null) return unlocked
    return TreeType(
        id = treeId,
        name = TreeResourceMapper.getNameResId(treeId),
        price = TreeType.DEFAULT.price,
        description = TreeType.DEFAULT.description,
        growthTimeMinutes = TreeType.DEFAULT.growthTimeMinutes,
        lottieAnimation = TreeType.DEFAULT.lottieAnimation,
        imageStaticSeed = TreeResourceMapper.getSeedDrawable(treeId),
        imageStaticBig = TreeResourceMapper.getBigDrawable(treeId)
    )
}

@Composable
private fun FocusLostDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
            }
        },
        title = { Text("Có người đã mất tập trung", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tiếp tục", color = Color(0xFF2E7D32))
            }
        }
    )
}
