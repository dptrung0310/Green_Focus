package com.example.greenfocus.ui.screen.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.greenfocus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = Color.Gray) },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuthGreenLight,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = AuthGreenLight
        )
    )
}

@Composable
fun AuthGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(AuthGreenLight, AuthGreenDark)
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun AuthWavyBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        
        val scaleX = width / 360f
        val scaleY = height / 120f

        // Background Wave (opacity 0.3)
        val path1 = Path().apply {
            moveTo(0f, 60f * scaleY)
            quadraticBezierTo(90f * scaleX, 20f * scaleY, 180f * scaleX, 60f * scaleY)
            quadraticBezierTo(270f * scaleX, 100f * scaleY, 360f * scaleX, 60f * scaleY)
            lineTo(360f * scaleX, 120f * scaleY)
            lineTo(0f, 120f * scaleY)
            close()
        }
        drawPath(path = path1, color = AuthWaveLight.copy(alpha = 0.3f))

        // Foreground Wave (opacity 0.5)
        val path2 = Path().apply {
            moveTo(0f, 80f * scaleY)
            quadraticBezierTo(90f * scaleX, 40f * scaleY, 180f * scaleX, 80f * scaleY)
            quadraticBezierTo(270f * scaleX, 120f * scaleY, 360f * scaleX, 80f * scaleY)
            lineTo(360f * scaleX, 120f * scaleY)
            lineTo(0f, 120f * scaleY)
            close()
        }
        drawPath(path = path2, color = AuthWaveDark.copy(alpha = 0.5f))
    }
}

@Composable
fun AuthTreeLogo(modifier: Modifier = Modifier) {
    val gradient = Brush.linearGradient(
        colors = listOf(AuthGreenLight, AuthGreenDark)
    )

    Box(
        modifier = modifier
            .size(96.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val scaleX = size.width / 40f
            val scaleY = size.height / 40f
            
            // Draw tree based on SVG coordinates
            // <circle cx="20" cy="12" r="5" fill="currentColor"/>
            drawCircle(color = Color.White, radius = 5f * scaleX, center = Offset(20f * scaleX, 12f * scaleY))
            // <circle cx="13" cy="20" r="4" fill="currentColor" opacity="0.8"/>
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 4f * scaleX, center = Offset(13f * scaleX, 20f * scaleY))
            // <circle cx="27" cy="20" r="4" fill="currentColor" opacity="0.8"/>
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 4f * scaleX, center = Offset(27f * scaleX, 20f * scaleY))
            // <circle cx="20" cy="28" r="5" fill="currentColor" opacity="0.9"/>
            drawCircle(color = Color.White, radius = 5f * scaleX, center = Offset(20f * scaleX, 28f * scaleY))
            // <rect x="19" y="32" width="2" height="8" fill="currentColor"/>
            drawRect(
                color = Color.White,
                topLeft = Offset(19f * scaleX, 32f * scaleY),
                size = Size(2f * scaleX, 8f * scaleY)
            )
        }
    }
}
