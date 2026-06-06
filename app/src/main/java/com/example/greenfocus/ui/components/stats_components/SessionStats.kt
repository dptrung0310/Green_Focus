package com.example.greenfocus.ui.components.stats_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.ui.theme.CardNormal
import com.example.greenfocus.ui.theme.CardOwnedBackGround_Start
import com.example.greenfocus.ui.theme.TextDark
import com.example.greenfocus.ui.theme.TextMuted

@Composable
private fun SessionsCard(count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardNormal),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("🏆", fontSize = 22.sp)
            Text(
                text       = "$count",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = TextDark
            )
            Text(text = "Sessions", fontSize = 12.sp, color = TextMuted)
        }
    }
}


@Composable
private fun CompletionCard(rate: Float, modifier: Modifier = Modifier) {
    val pct = (rate * 100).toInt()
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardNormal),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CardOwnedBackGround_Start.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎯", fontSize = 16.sp)
            }
            Text(
                text       = "$pct%",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = TextDark
            )
            Text(text = "Success Rate", fontSize = 12.sp, color = TextMuted)
        }
    }
}


@Composable
fun SessionStats(
    totalSessions: Int,
    completionRate: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SessionsCard(
            count = totalSessions,
            modifier = Modifier.weight(1f)
        )

        CompletionCard(
            rate = completionRate,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionStatsRowPreview() {
    SessionStats(totalSessions = 156, completionRate = 0.92f)
}