package com.example.greenfocus.ui.screen.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.data.model.User
import com.example.greenfocus.di.FirebaseModule

@Composable
fun LeaderboardScreen(users: List<User>) {
    val currentUid = FirebaseModule.auth.currentUser?.uid

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(users) { index, user ->
            val rankLabel = when (index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "#${index + 1}"
            }
            LeaderboardRow(
                rank = rankLabel,
                user = user,
                isCurrentUser = user.uid == currentUid
            )
        }
    }
}

@Composable
fun LeaderboardRow(rank: String, user: User, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(45.dp)
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF5F5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Hiển thị icon mặc định hoặc xử lý avatarUrl bằng Coil sau này
                Text(text = if (isCurrentUser) "😊" else "👤", fontSize = 24.sp)
            }
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = if (isCurrentUser) "Bạn" else user.displayName.ifEmpty { "Không rõ" },
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = "Đã trồng ${user.totalTreesPlanted} cây",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
