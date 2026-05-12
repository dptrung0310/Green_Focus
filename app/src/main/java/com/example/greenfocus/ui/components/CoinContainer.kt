package com.example.greenfocus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.ui.theme.BuyButton_Start
import com.example.greenfocus.ui.theme.BuyButton_End

val BuyButton = Brush.linearGradient(
    colors = listOf(
        BuyButton_Start,
        BuyButton_End
    )
)

@Composable
fun CoinContainer(coins: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BuyButton)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("💰", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = coins.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
