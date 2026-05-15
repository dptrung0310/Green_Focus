package com.example.greenfocus.ui.screen.social

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.ui.theme.GreenFocusTheme

@Composable
fun SocialScreen() {
    var selectedTab by remember { mutableStateOf("Leaderboard") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        // Header
        Text(
            text = "Social",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        
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
            when (selectedTab) {
                "Leaderboard" -> LeaderboardScreen()
                "Friend" -> FriendsScreen()
                "Team" -> TeamScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SocialScreenPreview() {
    GreenFocusTheme {
        SocialScreen()
    }
}
