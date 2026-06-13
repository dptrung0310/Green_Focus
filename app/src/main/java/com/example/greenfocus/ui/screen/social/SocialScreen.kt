package com.example.greenfocus.ui.screen.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SocialScreen(
    viewModel: SocialViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf("Bảng xếp hạng") }
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7EC))
            .statusBarsPadding()
    ) {
        val compactHeight = maxHeight < 420.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp,
                    vertical = if (compactHeight) 8.dp else 16.dp
                )
        ) {
            if (compactHeight) {
                CompactSocialHeader(
                    selectedTab = selectedTab,
                    onSelectedTabChange = { selectedTab = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text(
                    text = "Cộng đồng",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                Text(
                    text = "Thi đua với bạn bè của bạn",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                RowTabs(
                    selectedTab = selectedTab,
                    onSelectedTabChange = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

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
                            "Bảng xếp hạng" -> LeaderboardScreen(users = state.leaderboard)
                            "Bạn bè" -> FriendsScreen(
                                friends = state.friends,
                                viewModel = viewModel
                            )
                        }
                    }

                    is SocialUiState.Error -> {
                        Text(
                            text = "Lỗi: ${state.message}",
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSocialHeader(
    selectedTab: String,
    onSelectedTabChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = "Cộng đồng",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Thi đua với bạn bè",
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        RowTabs(
            selectedTab = selectedTab,
            onSelectedTabChange = onSelectedTabChange,
            modifier = Modifier.weight(1.2f),
            compact = true
        )
    }
}

@Composable
private fun RowTabs(
    selectedTab: String,
    onSelectedTabChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        listOf("Bảng xếp hạng", "Bạn bè").forEach { tab ->
            val isSelected = selectedTab == tab
            Button(
                onClick = { onSelectedTabChange(tab) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE0E0E0),
                    contentColor = if (isSelected) Color.White else Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 40.dp else 48.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = tab,
                    fontSize = if (compact) 12.sp else 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
