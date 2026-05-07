package com.example.greenfocus.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LeaderboardItem(
    val rank: String,
    val icon: String,
    val name: String,
    val trees: Int,
    val isCurrentUser: Boolean = false
)

@Composable
fun LeaderboardScreen() {
    val leaderboardList = listOf(
        LeaderboardItem("🥇", "👨", "Alex Chen", 245),
        LeaderboardItem("🥈", "👩", "Sarah Kim", 238),
        LeaderboardItem("🥉", "👨", "Mike Ross", 220),
        LeaderboardItem("#12", "😊", "You", 156, true),
        LeaderboardItem("#13", "👩", "Emma Stone", 145)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(leaderboardList) { item ->
            LeaderboardRow(item)
        }
    }
}

@Composable
fun LeaderboardRow(item: LeaderboardItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCurrentUser) Color(0xFFE8F5E9) else Color.White
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
                text = item.rank,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF5F5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icon, fontSize = 24.sp)
            }
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontWeight = if (item.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = "${item.trees} trees planted",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
