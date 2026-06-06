package com.example.greenfocus.ui.screen.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SocialScreen(
    viewModel: SocialViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf("Leaderboard") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7EC))
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = "Community",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )

        Text(
            text = "Compete with your friends",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        RowTabs(
            selectedTab = selectedTab,
            onSelectedTabChange = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                        "Leaderboard" -> LeaderboardScreen(users = state.leaderboard)
                        "Friends" -> FriendsScreen(
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
private fun RowTabs(
    selectedTab: String,
    onSelectedTabChange: (String) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Leaderboard", "Friends").forEach { tab ->
            val isSelected = selectedTab == tab
            Button(
                onClick = { onSelectedTabChange(tab) },
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
}
