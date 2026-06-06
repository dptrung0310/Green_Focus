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
import kotlin.random.Random

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
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
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
                    text = "Tham gia phòng",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nhập mã phòng để tham gia",
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
                    label = { Text("Mã phòng") },
                    placeholder = { Text("Ví dụ: ABC123") },
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
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            if (roomId.isBlank()) {
                                errorMessage = "Vui lòng nhập mã phòng"
                            } else if (roomId.length < 6) {
                                errorMessage = "Mã phòng phải có ít nhất 6 ký tự"
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
                        Text(if (isJoining) "Đang tham gia..." else "Tham gia")
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
