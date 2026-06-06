package com.example.greenfocus.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.theme.AuthBackground
import com.example.greenfocus.ui.theme.AuthGreenDark
import com.example.greenfocus.ui.theme.AuthGreenLight

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp), // Leave space for the wave
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthTreeLogo(modifier = Modifier.padding(bottom = 32.dp))

            Text(
                text = "Chào mừng quay trở lại",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AuthGreenDark
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Đăng nhập để tiếp tục phát triển",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                icon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Mật khẩu",
                icon = Icons.Default.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            TextButton(
                onClick = { /* Handle Forgot Password */ },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Quên mật khẩu?", color = AuthGreenLight, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator(color = AuthGreenLight)
            } else {
                AuthGradientButton(
                    text = "Đăng nhập",
                    onClick = { viewModel.login(email, password) { onNavigateToHome() } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Chưa có tài khoản?", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Đăng ký", color = AuthGreenLight, fontWeight = FontWeight.Bold)
                }
            }

            viewModel.errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = Color.Red, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Wavy Background at the bottom
        AuthWavyBackground(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}