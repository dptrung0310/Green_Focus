package com.example.greenfocus.ui.screen.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FriendItem(
    val icon: String,
    val name: String,
    val trees: Int
)

@Composable
fun FriendsScreen() {
    val friendsList = listOf(
        FriendItem("👨", "Alex Chen", 245),
        FriendItem("👩", "Sarah Kim", 238),
        FriendItem("👨", "Mike Ross", 220),
        FriendItem("👩", "Emma Stone", 145)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { /* Add Friend */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Friend")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(friendsList) { friend ->
                FriendRow(friend)
            }
        }
    }
}

@Composable
fun FriendRow(friend: FriendItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF5F5F5), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = friend.icon, fontSize = 28.sp)
        }
        
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = friend.name,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = "${friend.trees} trees",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        // Optional: Chat or Profile icon could go here
    }
}
