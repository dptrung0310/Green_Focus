package com.example.greenfocus.ui.components.stats_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.R
import com.example.greenfocus.ui.theme.BannerGreen_End
import com.example.greenfocus.ui.theme.BannerGreen_Start

@Composable
fun TotalFocusCard(totalMinutes: Int, modifier: Modifier = Modifier) {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val timeText = when {
        hours > 0 -> "${hours}g ${minutes}p"
        else      -> "${minutes}p"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(BannerGreen_Start, BannerGreen_End))
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Tổng thời gian tập trung",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = timeText,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(2.dp))
            Text(
                text = "Tháng này",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.clock_1),
                contentDescription = "Timer Icon",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TotalFocusCardPreview() {
    TotalFocusCard(totalMinutes = 2550)
}