package com.example.greenfocus.ui.components.stats_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.ui.theme.BuyButton_End
import com.example.greenfocus.ui.theme.BuyButton_Start
import com.example.greenfocus.ui.theme.StreakTextDark
import com.example.greenfocus.ui.theme.StreakTextMedium

@Composable
fun StreakCard(days: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(BuyButton_Start, BuyButton_End))
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text       = "Chuỗi ngày hiện tại",
                color      = StreakTextMedium,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text       = "$days ngày",
                    color      = StreakTextDark,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text("🔥", fontSize = 28.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text     = if (days > 0) "Hãy tiếp tục phát huy!" else "Bắt đầu chuỗi ngày của bạn ngay hôm nay!",
                color    = StreakTextMedium.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StreakCardPreview() {
    StreakCard(days = 7)
}
