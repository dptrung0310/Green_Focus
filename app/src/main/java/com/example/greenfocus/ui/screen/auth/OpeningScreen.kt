package com.example.greenfocus.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.R
import kotlinx.coroutines.delay

// Giữ nguyên bảng màu chủ đạo
val PrimaryGreen = Color(0xFF4CAF50)
val AppBackground = Color(0xFFFEF8EC)
val TextDark = Color(0xFF333333)

@Composable
fun OpeningScreen(
    onFinished: () -> Unit
) {
    LaunchedEffect(key1 = true) {
        delay(2000L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GreenFocus",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = PrimaryGreen,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Trồng cây, xây sự tập trung",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}