package com.example.greenfocus.ui.screen.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun TeamScreen(
    onNavigateToRoom: (String) -> Unit = {},
    viewModel: SocialViewModel = viewModel()
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "👥", fontSize = 64.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Team Focus Mode",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Focus together with friends. If one person gives up, everyone fails!",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Create Room Button
        TeamActionButton(
            title = "Create Room",
            subtitle = "Start a team focus session",
            onClick = {
                if (isBusy) return@TeamActionButton
                actionError = ""
                isBusy = true
                scope.launch {
                    val result = viewModel.createTeamRoom()
                    isBusy = false
                    result.onSuccess { roomId ->
                        onNavigateToRoom(roomId)
                    }.onFailure { error ->
                        actionError = error.message ?: "Create room failed"
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Join Room Button
        TeamActionButton(
            title = "Join Room",
            subtitle = "Enter a room code",
            onClick = { showJoinDialog = true }
        )

        if (actionError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = actionError, color = Color.Red, fontSize = 12.sp)
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
                    val result = viewModel.joinTeamRoom(roomId)
                    isJoining = false
                    result.onSuccess {
                        showJoinDialog = false
                        onNavigateToRoom(roomId)
                    }.onFailure { error ->
                        joinError = error.message ?: "Room does not exist"
                    }
                }
            },
            isJoining = isJoining,
            externalErrorMessage = joinError
        )
    }
}

@Composable
fun TeamActionButton(title: String, subtitle: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun JoinRoomDialog(
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
                    text = "Join Room",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter the room code to join",
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
                    label = { Text("Room ID") },
                    placeholder = { Text("e.g., ABC123") },
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
                                errorMessage = "Please enter a room ID"
                            } else if (roomId.length < 6) {
                                errorMessage = "Room ID must be at least 6 characters"
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

fun generateRoomId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}
