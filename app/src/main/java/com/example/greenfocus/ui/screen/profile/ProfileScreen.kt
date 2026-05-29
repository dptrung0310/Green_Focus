package com.example.greenfocus.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.ui.theme.*

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onSettingNavigate: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val bgGradient = Brush.linearGradient(
        colors = listOf(AuthGreenLight, AuthGreenDark)
    )

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(AuthBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AuthGreenLight)
        }
        return
    }

    val user = uiState.user
    if (user == null) {
        Box(modifier = Modifier.fillMaxSize().background(AuthBackground), contentAlignment = Alignment.Center) {
            Text("Lỗi: ${uiState.errorMessage}")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp) // Tránh đè bottom nav
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(top = 48.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Profile", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { onSettingNavigate() },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Profile Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFA726)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("😊", fontSize = 36.sp)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(user.displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(user.email, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Level ${user.level}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // XP Progress
                        val xpNeeded = user.level * 500
                        val xpPercent = if (xpNeeded > 0) user.experience.toFloat() / xpNeeded else 0f
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${user.experience} / $xpNeeded XP", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("${(xpPercent * 100).toInt()}%", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { xpPercent },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFFD54F),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Stats Grid
        Column(
            modifier = Modifier
                .offset(y = (-24).dp)
                .padding(horizontal = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🏆", iconColor = Color(0xFFFFD54F), iconBg = Color(0xFFFFD54F).copy(alpha = 0.2f),
                    value = user.coins.toString(), label = "Coins Earned"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🎯", iconColor = AuthGreenLight, iconBg = AuthGreenLight.copy(alpha = 0.2f),
                    value = user.totalTreesPlanted.toString(), label = "Trees Planted"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val hours = user.totalFocusTime / 3600
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "⚡", iconColor = AuthGreenDark, iconBg = AuthGreenDark.copy(alpha = 0.2f),
                    value = "${hours}h", label = "Total Time"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🔓", iconColor = Color(0xFF42A5F5), iconBg = Color(0xFF42A5F5).copy(alpha = 0.2f),
                    value = user.unlockedTreeIds.size.toString(), label = "Trees Unlocked"
                )
            }
        }

        // Settings / Logout 
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                Column {
                    TextButton(
                        onClick = { onSettingNavigate() },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Settings", color = Color.DarkGray, fontSize = 16.sp)
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Log Out", color = Color.Red, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, icon: String, iconColor: Color, iconBg: Color, value: String, label: String) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
