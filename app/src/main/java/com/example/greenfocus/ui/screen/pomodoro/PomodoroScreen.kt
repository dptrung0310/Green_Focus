package com.example.greenfocus.ui.screen.pomodoro

import android.Manifest
import android.app.Application
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.theme.GreenFocusTheme
import com.example.greenfocus.R
import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.data.model.TreeResourceMapper
import com.example.greenfocus.ui.components.CoinContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ContentCopy
import coil.compose.AsyncImage
import com.example.greenfocus.data.model.FriendRequest
import com.example.greenfocus.data.model.User
import com.example.greenfocus.ui.screen.pomodoro.room.ROOM_STATUS_STARTED
import com.example.greenfocus.ui.screen.pomodoro.room.ROOM_STATUS_WAITING
import com.example.greenfocus.ui.screen.pomodoro.room.TeamInvite
import com.example.greenfocus.ui.screen.pomodoro.room.TeamMember
import com.example.greenfocus.ui.screen.social.FriendRequestRow
import com.example.greenfocus.ui.screen.social.SocialUiState
import com.example.greenfocus.ui.screen.social.SocialViewModel
import kotlinx.coroutines.launch

@Composable
fun PomodoroScreen(
    modifier: Modifier = Modifier,
    pomodoroViewModel: PomodoroViewModel = viewModel(factory = PomodoroViewModel.Factory),
    homeRoomViewModel: HomeRoomViewModel = viewModel(),
    socialViewModel: SocialViewModel = viewModel()
) {
    val roomState by homeRoomViewModel.uiState.collectAsState()

    LaunchedEffect(roomState.activeRoomId) {
        pomodoroViewModel.setActiveRoom(roomState.activeRoomId)
        if (roomState.activeRoomId == null) {
            pomodoroViewModel.resetToDefault()
        }
    }

    if (roomState.activeRoomId == null) {
        HomeLandingContent(
            modifier = modifier,
            pomodoroViewModel = pomodoroViewModel,
            homeRoomViewModel = homeRoomViewModel,
            socialViewModel = socialViewModel
        )
    } else {
        HomeRoomContent(
            modifier = modifier,
            roomState = roomState,
            pomodoroViewModel = pomodoroViewModel,
            homeRoomViewModel = homeRoomViewModel,
            socialViewModel = socialViewModel
        )
    }
}

@Composable
private fun HomeLandingContent(
    modifier: Modifier = Modifier,
    pomodoroViewModel: PomodoroViewModel,
    homeRoomViewModel: HomeRoomViewModel,
    socialViewModel: SocialViewModel
) {
    val pomodoroUiState by pomodoroViewModel.pomodoroUiState.collectAsState()
    val socialUiState by socialViewModel.uiState.collectAsState()
    val homeRoomUiState by homeRoomViewModel.uiState.collectAsState()
    val invites = homeRoomUiState.incomingRoomInvites
    val scope = rememberCoroutineScope()

    var createError by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var isJoining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf("") }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var inviteErrorMessage by remember { mutableStateOf<String?>(null) }

    val requests = (socialUiState as? SocialUiState.Success)?.friendRequests ?: emptyList()
    val totalInvites = requests.size + invites.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7EC))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFA726)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("😊", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Xin chào ${pomodoroUiState.currentUserName}!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { showRequestsDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (totalInvites > 0) {
                                Badge(
                                    containerColor = Color.Red,
                                    modifier = Modifier.offset(x = (-2).dp, y = 2.dp)
                                ) {
                                    Text(totalInvites.toString(), color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Lời mời kết bạn",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                CoinContainer(coins = pomodoroUiState.userMoneyAmount)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Widget thống kê nhanh hôm nay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏱️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Hôm nay",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${pomodoroUiState.todayFocusMinutes}p",
                            fontSize = 15.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌳", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cây hôm nay",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${pomodoroUiState.todayTreesPlanted} cây",
                            fontSize = 15.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TimerBlock(
            currentTime = "",
            currentTree = pomodoroUiState.selectedTree.imageStaticSeed,
            currentTreeName = stringResource(id = pomodoroUiState.selectedTree.name),
            currentProgress = pomodoroUiState.currentPercentage,
            isHalfDone = false,
            seedImage = pomodoroUiState.selectedTree.imageStaticSeed,
            bigImage = pomodoroUiState.selectedTree.imageStaticBig,
            onTimerClick = {},
            isTimerRunning = false,
            showTime = false
        )

        Spacer(modifier = Modifier.height(32.dp))

        TeamActionButton(
            title = "Bắt đầu phiên tập trung của bạn",
            subtitle = "",
            onClick = {
                if (isCreating) return@TeamActionButton
                createError = ""
                isCreating = true
                val durationMs = pomodoroUiState.dialogTimeValue.coerceAtLeast(1) * 60_000L
                scope.launch {
                    val result = homeRoomViewModel.createRoom(
                        pomodoroUiState.selectedTree.id,
                        durationMs
                    )
                    isCreating = false
                    result.onFailure { error ->
                        createError = error.message ?: "Tạo phòng thất bại"
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF2E7D32)
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "Tham gia phòng",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (createError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = createError, color = Color.Red, fontSize = 12.sp)
        }
    }

    if (showJoinDialog) {
        JoinRoomDialog(
            onDismiss = {
                showJoinDialog = false
                joinError = ""
            },
            onJoin = { roomId ->
                if (isJoining) return@JoinRoomDialog
                joinError = ""
                isJoining = true
                scope.launch {
                    val result = homeRoomViewModel.joinRoom(roomId)
                    isJoining = false
                    result.onSuccess {
                        showJoinDialog = false
                    }.onFailure {
                        joinError = "Không tìm thấy phòng"
                    }
                }
            },
            isJoining = isJoining,
            externalErrorMessage = joinError
        )
    }
    if (showRequestsDialog && socialUiState is SocialUiState.Success) {
        FriendRequestsDialog(
            friendRequests = requests,
            roomInvites = invites,
            inviteErrorMessage = inviteErrorMessage,
            onAcceptFriend = { socialViewModel.acceptRequest(it) },
            onDeclineFriend = { socialViewModel.declineRequest(it) },
            onJoinRoomInvite = { invite ->
                inviteErrorMessage = null
                scope.launch {
                    val result = homeRoomViewModel.joinRoom(invite.roomId)
                    if (result.isSuccess) {
                        homeRoomViewModel.deleteInvite(invite)
                        showRequestsDialog = false
                    } else {
                        inviteErrorMessage = result.exceptionOrNull()?.message ?: "Không thể vào phòng"
                    }
                }
            },
            onDismissInvite = { invite ->
                homeRoomViewModel.deleteInvite(invite)
            },
            onDismiss = {
                inviteErrorMessage = null
                showRequestsDialog = false
            }
        )
    }
}

@Composable
private fun TeamActionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun JoinRoomDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    isJoining: Boolean,
    externalErrorMessage: String
) {
    var roomId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val combinedError = if (errorMessage.isNotEmpty()) errorMessage else externalErrorMessage

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join room",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter a room code to join",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = roomId,
                    onValueChange = {
                        roomId = it.uppercase()
                        errorMessage = ""
                    },
                    label = { Text("Room code") },
                    placeholder = { Text("Example: ABC123") },
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32),
                        focusedLabelColor = Color(0xFF2E7D32),
                        cursorColor = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (combinedError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = combinedError,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (roomId.isBlank()) {
                                errorMessage = "Please enter a room code"
                            } else if (roomId.length < 6) {
                                errorMessage = "Room code must have at least 6 characters"
                            } else {
                                onJoin(roomId)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isJoining,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Text(if (isJoining) "Joining..." else "Join")
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestsDialog(
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
        title = { Text("Invitations", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (friendRequests.isEmpty() && roomInvites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No invitations", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(roomInvites) { invite ->
                            RoomInviteRow(
                                invite = invite,
                                onJoin = { onJoinRoomInvite(invite) },
                                onDismiss = { onDismissInvite(invite) }
                            )
                        }
                        items(friendRequests) { request ->
                            FriendRequestRow(
                                request = request,
                                onAccept = { onAcceptFriend(request) },
                                onDecline = { onDeclineFriend(request) }
                            )
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

@Composable
private fun RoomInviteRow(
    invite: TeamInvite,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color(0xFF2E7D32)
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(text = invite.fromName, fontWeight = FontWeight.Bold)
            Text(
                text = "invited you to a room",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        IconButton(onClick = onJoin) {
            Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF2E7D32))
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Red)
        }
    }
}

@Composable
private fun HomeRoomContent(
    modifier: Modifier = Modifier,
    roomState: HomeRoomUiState,
    pomodoroViewModel: PomodoroViewModel,
    homeRoomViewModel: HomeRoomViewModel,
    socialViewModel: SocialViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val application = context.applicationContext as? Application
    val pomodoroUiState by pomodoroViewModel.pomodoroUiState.collectAsState()
    val socialUiState by socialViewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    DisposableEffect(application, roomState.activeRoomId) {
        val app = application
        if (app == null || roomState.activeRoomId == null) {
            onDispose { }
        } else {
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
                        homeRoomViewModel.handleAppExit()
                    }
                }
                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: android.app.Activity) = Unit
            }

            app.registerActivityLifecycleCallbacks(callbacks)
            onDispose { app.unregisterActivityLifecycleCallbacks(callbacks) }
        }
    }

    var showInviteDialog by remember { mutableStateOf(false) }
    var pendingInviteIds by remember(roomState.activeRoomId) { mutableStateOf(setOf<String>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                pomodoroViewModel.startTimerService(context)
            } else {
                Log.d("WARNING", "User denied Post Perm, now they can't run the app :(")
            }
        }
    )

    val room = roomState.room
    val durationMs = room?.durationMs ?: HomeRoomViewModel.DEFAULT_DURATION_MS
    val durationMinutes = (durationMs / 60_000L).toInt().coerceAtLeast(1)
    val rewardCoins = (durationMs / 60_000L).toInt()
    val isWaiting = room?.status == ROOM_STATUS_WAITING
    val isStarted = room?.status == ROOM_STATUS_STARTED
    val roomTreeId = room?.treeId?.ifBlank { null } ?: pomodoroUiState.selectedTree.id
    val activeTree = resolveRoomTree(roomTreeId, pomodoroUiState.unlockedTrees)
    val invitedIds = roomState.roomInvites
        .mapNotNull { invite -> invite.toUid.takeIf { it.isNotBlank() } }
        .toSet()
    val disabledInviteIds = invitedIds + pendingInviteIds
    var handledFocusLostEventId by remember(roomState.activeRoomId) { mutableStateOf<String?>(null) }
    var handledCompletedEventId by remember(roomState.activeRoomId) { mutableStateOf<String?>(null) }
    var completionArmedStartedAtMs by remember(roomState.activeRoomId) { mutableStateOf<Long?>(null) }

    val timerScale by animateFloatAsState(
        targetValue = if (pomodoroUiState.isTimerRunning) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 600),
        label = "timerScale"
    )

    fun startLocalTimer() {
        pomodoroViewModel.setTimerMinutes(durationMinutes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    pomodoroViewModel.startTimerService(context)
                }
                activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    pomodoroViewModel.toggleRationaleDialog()
                }
                else -> {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            pomodoroViewModel.startTimerService(context)
        }
    }

    LaunchedEffect(room?.durationMs) {
        if (!pomodoroUiState.isTimerRunning) {
            pomodoroViewModel.setTimerMinutes(durationMinutes)
        }
    }

    LaunchedEffect(room?.treeId) {
        if (!pomodoroUiState.isTimerRunning) {
            pomodoroViewModel.updateSelectedTree(activeTree)
        }
    }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        pomodoroViewModel.startTimerService(context)
                    } else {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    pomodoroViewModel.startTimerService(context)
                }
            }
        } else if (room != null && room.status == ROOM_STATUS_WAITING) {
            if (pomodoroUiState.isTimerRunning) {
                pomodoroViewModel.stopTimerService(
                    context = context,
                    markFailed = false,
                    resetSeconds = (room.durationMs / 1000L).toInt()
                )
            }
        }
    }

    LaunchedEffect(
        room?.focusLostEventId,
        room?.focusLostAt,
        roomState.currentUserId,
        roomState.members
    ) {
        val focusLostAtMs = room?.focusLostAt?.toDate()?.time ?: return@LaunchedEffect
        val focusLostEventId = room.focusLostEventId
            ?: "${roomState.activeRoomId.orEmpty()}_legacy_$focusLostAtMs"
        if (handledFocusLostEventId == focusLostEventId) return@LaunchedEffect

        val currentUserId = roomState.currentUserId ?: return@LaunchedEffect
        val currentMember = roomState.members.firstOrNull { it.uid == currentUserId }
            ?: return@LaunchedEffect
        if (currentMember.lastHandledFocusLostEventId == focusLostEventId) {
            handledFocusLostEventId = focusLostEventId
            return@LaunchedEffect
        }

        val joinedAtMs = currentMember.joinedAt?.toDate()?.time
        if (joinedAtMs != null && joinedAtMs > focusLostAtMs) return@LaunchedEffect

        handledFocusLostEventId = focusLostEventId
        if (pomodoroUiState.isTimerRunning) {
            pomodoroViewModel.stopTimerService(context)
        }
        pomodoroViewModel.recordFailedRoomSession(
            failureEventId = focusLostEventId,
            durationMinutes = durationMinutes,
            treeId = room.treeId.ifBlank { activeTree.id },
            roomId = roomState.activeRoomId
        )
        homeRoomViewModel.markFocusLostEventHandled(focusLostEventId)
    }

    LaunchedEffect(
        room?.completedEventId,
        room?.completedAt,
        roomState.currentUserId,
        roomState.members
    ) {
        val completedAtMs = room?.completedAt?.toDate()?.time ?: return@LaunchedEffect
        val completedEventId = room.completedEventId
            ?: "${roomState.activeRoomId.orEmpty()}_completed_legacy_$completedAtMs"
        if (handledCompletedEventId == completedEventId) return@LaunchedEffect

        val currentUserId = roomState.currentUserId ?: return@LaunchedEffect
        val currentMember = roomState.members.firstOrNull { it.uid == currentUserId }
            ?: return@LaunchedEffect
        if (currentMember.lastHandledCompletedEventId == completedEventId) {
            handledCompletedEventId = completedEventId
            return@LaunchedEffect
        }

        val joinedAtMs = currentMember.joinedAt?.toDate()?.time
        if (joinedAtMs != null && joinedAtMs > completedAtMs) return@LaunchedEffect

        handledCompletedEventId = completedEventId
        if (pomodoroUiState.isTimerRunning) {
            pomodoroViewModel.stopTimerService(
                context = context,
                markFailed = false,
                resetSeconds = (room.durationMs / 1000L).toInt()
            )
        }
        pomodoroViewModel.recordSuccessfulRoomSession(
            completedEventId = completedEventId,
            durationMinutes = durationMinutes,
            treeId = room.treeId.ifBlank { activeTree.id },
            roomId = roomState.activeRoomId
        )
        homeRoomViewModel.markCompletedEventHandled(completedEventId)
    }

    LaunchedEffect(invitedIds) {
        if (pendingInviteIds.isNotEmpty()) {
            pendingInviteIds = pendingInviteIds - invitedIds
        }
    }

    LaunchedEffect(pomodoroUiState.isTimerRunning, isStarted, room?.startedAt) {
        val startedAtMs = room?.startedAt?.toDate()?.time
        if (pomodoroUiState.isTimerRunning && isStarted && startedAtMs != null) {
            completionArmedStartedAtMs = startedAtMs
        }
    }

    LaunchedEffect(pomodoroUiState.isTimerFinished, roomState.isHost, isStarted, room?.startedAt) {
        if (pomodoroUiState.isTimerFinished) {
            val startedAtMs = room?.startedAt?.toDate()?.time
            if (
                roomState.isHost &&
                isStarted &&
                startedAtMs != null &&
                completionArmedStartedAtMs == startedAtMs
            ) {
                completionArmedStartedAtMs = null
                homeRoomViewModel.completeRoom()
            }
            pomodoroViewModel.resetAfterSession(durationMinutes)
        }
    }

    LaunchedEffect(pomodoroUiState.isTimerFailed, isStarted) {
        if (pomodoroUiState.isTimerFailed && isStarted) {
            homeRoomViewModel.reportFocusLost()
            pomodoroViewModel.resetAfterSession(durationMinutes)
        }
    }

    if (pomodoroUiState.isTimerRunning) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAF7EC))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TeamMembersRow(members = roomState.members)

            Spacer(modifier = Modifier.height(48.dp))

            TimerBlock(
                currentTime = pomodoroUiState.formattedTime,
                currentTree = activeTree.imageStaticSeed,
                currentTreeName = stringResource(id = activeTree.name),
                currentProgress = pomodoroUiState.currentPercentage,
                isHalfDone = pomodoroUiState.isHalfDone,
                seedImage = activeTree.imageStaticSeed,
                bigImage = activeTree.imageStaticBig,
                onTimerClick = {},
                isTimerRunning = pomodoroUiState.isTimerRunning,
                modifier = Modifier.scale(timerScale)
            )

            Spacer(modifier = Modifier.height(48.dp))

            TextButton(
                onClick = {
                    pomodoroViewModel.stopTimerService(context)
                    homeRoomViewModel.reportFocusLost()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.pomodoro_stop_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFAF7EC))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    pomodoroViewModel.stopTimerService(context)
                    homeRoomViewModel.leaveRoom()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color(0xFF2E7D32)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF1F8E9))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Mã phòng: ${roomState.activeRoomId.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            roomState.activeRoomId?.let { id ->
                                clipboardManager.setText(AnnotatedString(id))
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Sao chép mã phòng",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(onClick = { showInviteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Mời bạn bè",
                        tint = Color(0xFF2E7D32)
                    )
                }
            }

            TeamMembersRow(members = roomState.members)

            Spacer(modifier = Modifier.height(16.dp))

            TimerBlock(
                currentTime = pomodoroUiState.formattedTime,
                currentTree = activeTree.imageStaticSeed,
                currentTreeName = stringResource(id = activeTree.name),
                currentProgress = pomodoroUiState.currentPercentage,
                isHalfDone = pomodoroUiState.isHalfDone,
                seedImage = activeTree.imageStaticSeed,
                bigImage = activeTree.imageStaticBig,
                onTimerClick = {
                    if (roomState.isHost && isWaiting) {
                        pomodoroViewModel.toggleTimeDialog()
                    }
                },
                isTimerRunning = pomodoroUiState.isTimerRunning,
                modifier = Modifier.scale(timerScale)
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                        contentDescription = "Icon phần thưởng",
                        tint = Color(0xFFFBC02D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.home_reward_format, rewardCoins),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (roomState.isHost) {
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
                                contentDescription = "Icon khóa",
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

            if (roomState.isHost && isWaiting) {
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
                                homeRoomViewModel.updateRoomTree(it.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isWaiting && roomState.isHost -> {
                    Button(
                        onClick = {
                            pomodoroViewModel.resetAfterSession(durationMinutes)
                            homeRoomViewModel.startRoom(durationMs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pomodoro_start_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                isWaiting -> {
                    Text(
                        text = stringResource(R.string.home_waiting_host),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (roomState.roomClosed) {
        AlertDialog(
            onDismissRequest = { homeRoomViewModel.leaveRoom() },
            title = { Text("Phòng đã bị xóa") },
            text = { Text("Phòng không còn tồn tại, bạn sẽ được đưa về Home.") },
            confirmButton = {
                TextButton(onClick = { homeRoomViewModel.leaveRoom() }) {
                    Text("Quay lại")
                }
            }
        )
    }

    if (pomodoroUiState.showTimeDialog) {
        TimerDialog(
            dialogTimeValue = pomodoroUiState.dialogTimeValue,
            onUserInputChange = { pomodoroViewModel.updateTimeDialogValue(it) },
            onConfirm = {
                pomodoroViewModel.setTimer()
                if (roomState.isHost && isWaiting) {
                    homeRoomViewModel.updateRoomDuration(
                        pomodoroUiState.dialogTimeValue.coerceAtLeast(1) * 60_000L
                    )
                }
            },
            onDismiss = { pomodoroViewModel.toggleTimeDialog() }
        )
    }
    if (pomodoroUiState.showRationaleDialog) {
        RationaleDialog(
            onDismiss = { pomodoroViewModel.toggleRationaleDialog() },
            permissionLauncher = permissionLauncher
        )
    }
    if (pomodoroUiState.showUsageStatsRationaleDialog) {
        UsageStatsRationaleDialog(
            onDismiss = { pomodoroViewModel.toggleUsageStatsRationaleDialog() },
            context = context
        )
    }

    if (!roomState.focusLostMessage.isNullOrBlank()) {
        FocusLostDialog(
            message = roomState.focusLostMessage ?: "",
            onDismiss = { homeRoomViewModel.clearFocusLostMessage() }
        )
    }

    if (showInviteDialog) {
        val friends = (socialUiState as? SocialUiState.Success)?.friends ?: emptyList()
        val memberIds = roomState.members.map { it.uid }.toSet()
        val availableFriends = friends.filter { it.uid !in memberIds }
        InviteFriendDialog(
            friends = availableFriends,
            invitedIds = disabledInviteIds,
            onInvite = { friend ->
                if (disabledInviteIds.contains(friend.uid)) return@InviteFriendDialog
                pendingInviteIds = pendingInviteIds + friend.uid
                scope.launch {
                    val result = homeRoomViewModel.inviteFriend(friend.uid)
                    if (result.isFailure) {
                        pendingInviteIds = pendingInviteIds - friend.uid
                    }
                }
            },
            onDismiss = { showInviteDialog = false }
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
                    model = member.avatarUrl.ifBlank {
                        "https://ui-avatars.com/api/?name=${member.displayName}"
                    },
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
                    maxLines = 1
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
        title = { Text("Mời bạn bè", fontWeight = FontWeight.Bold) },
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
                            modifier = Modifier.width(92.dp)
                        ) {
                            AsyncImage(
                                model = friend.avatarUrl.ifBlank {
                                    "https://ui-avatars.com/api/?name=${friend.displayName}"
                                },
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
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { onInvite(friend) },
                                enabled = !isInvited,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text(if (isInvited) "Đã mời" else "Mời")
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
fun TimerBlock(
    currentTime: String,
    currentProgress: Float,
    currentTree: Int,
    currentTreeName: String,
    isHalfDone: Boolean = false,
    seedImage: Int = currentTree,
    bigImage: Int = currentTree,
    onTimerClick: () -> Unit,
    isTimerRunning: Boolean,
    showTime: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Track glow animation: pulses once when isHalfDone becomes true
    var glowVisible by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (glowVisible) 0.55f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "glowAlpha"
    )

    LaunchedEffect(isHalfDone) {
        if (isHalfDone) {
            glowVisible = true
            kotlinx.coroutines.delay(900)
            glowVisible = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(250.dp)
    ) {
        // Circular Progress Bar
        CircularProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
            color = Color(0xFF388E3C),
            trackColor = Color(0xFFC8E6C9)
        )

        // Glow ring that pulses once at the halfway point
        if (glowAlpha > 0f) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize().alpha(glowAlpha),
                strokeWidth = 12.dp,
                color = Color(0xFF66BB6A),
                trackColor = Color.Transparent
            )
        }

        // Image and Text inside the circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTreeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // AnimatedContent: swap seed → big with scale-bounce when isHalfDone
            AnimatedContent(
                targetState = isHalfDone,
                transitionSpec = {
                    // Entering big tree: scale from 40% with bouncy spring + fade in
                    (scaleIn(
                        initialScale = 0.4f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(animationSpec = tween(350))) togetherWith
                    // Leaving small tree: scale up to 160% + quick fade out
                    (scaleOut(
                        targetScale = 1.6f,
                        animationSpec = tween(280)
                    ) + fadeOut(animationSpec = tween(200)))
                },
                label = "treeGrowAnimation"
            ) { halfDone ->
                val imageRes = if (halfDone) bigImage else seedImage
                // Thay thế Image phẳng cũ bằng Interactive3DCard
                com.example.greenfocus.ui.components.Interactive3DCard(
                    treeImageRes = imageRes,
                    modifier = Modifier.size(110.dp),
                    cardColor = Color(0xFFFAF7EC), // Màu kem đồng bộ màu nền ứng dụng
                    glowColor = Color(0x334CAF50)
                )
            }

            if (showTime) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.clickable(
                        enabled = !isTimerRunning,
                        onClick = { onTimerClick() }
                    )
                )
            }
        }
    }
}

@Composable
fun TimerDialog(
    dialogTimeValue: Int,
    onDismiss: () -> Unit,
    onUserInputChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        content = {
            Box(modifier = Modifier
                .width(400.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp) // 1. Rounded corners
                )
                .padding(16.dp) // 2. Inner padding))
            )
            {
                Column() {
                    Text(
                        text = stringResource(R.string.pomodoro_timer_dialog),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        label = { Text(stringResource(R.string.pomodoro_timer_dialog)) },
                        value = dialogTimeValue.toString(),
                        onValueChange = onUserInputChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done // Shows a "Done" checkmark on keyboard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ADDED BUTTONS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End // Aligns buttons to the right
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.dialog_cancel))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            enabled = dialogTimeValue > 0,
                            onClick = {
                                onConfirm()
                                onDismiss() // Dismiss the dialog after confirming
                            }
                        ) {
                            Text(stringResource(R.string.dialog_confirm))
                        }
                    }
                }
            }

        }
    )
}

@Composable
fun TreeSelectionRow(
    isTimerRunning: Boolean,
    selectedTree: TreeType,
    unlockedTrees: List<TreeType>,
    changeSelectedTree: (TreeType) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(unlockedTrees) { tree ->
            val isSelected = selectedTree.id == tree.id

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Using Surface instead of IconButton for better background and shadow control
                Surface(
                    onClick = { changeSelectedTree(tree) },
                    enabled = !isTimerRunning,
                    modifier = Modifier.size(80.dp).alpha(if (isTimerRunning && !isSelected) 0.5f else 1f), // Slightly smaller to leave space for text
                    shape = RoundedCornerShape(20.dp), // Smooth, modern rounded corners

                    // Background Color: Use soft green when selected, white when not
                    color = if (isSelected) Color(0xFFE8F5E9) else Color.White,

                    // Border: Add a hard green outline to the selected item to make it stand out instantly
                    border = if (isSelected) BorderStroke(3.dp, Color(0xFF388E3C)) else null,

                    // Shadow: Drops a subtle shadow so the button lifts off the white background
                    shadowElevation = if (isSelected) 8.dp else 2.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = tree.imageStaticBig),
                            contentDescription = "Select tree ${tree.id}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp) // Keeps the tree image safely inside the borders
                        )
                    }
                }
                Text(
                    text = stringResource(id = tree.name),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF2E7D32) else Color.Gray
                )
            }
        }
    }
}


@Composable
fun RationaleDialog(
    onDismiss: () -> Unit,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.rationale_dialog_title)) },
        text = { Text(stringResource(R.string.rationale_dialog_content)) },
        confirmButton = {
            TextButton(onClick = {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                onDismiss()
            }) { Text(stringResource(R.string.dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.dialog_dismiss)) }
        }
    )
}

@Composable
fun UsageStatsRationaleDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.rationale_dialog_title)) },
        text = { Text(stringResource(R.string.usage_stats_rationale_dialog_content)) },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                onDismiss()
            }) { Text(stringResource(R.string.dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.dialog_dismiss)) }
        }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPomodoroScreen(){
    GreenFocusTheme() {
        PomodoroScreen()
    }
}
