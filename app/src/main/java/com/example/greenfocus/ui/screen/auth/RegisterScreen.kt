package com.example.greenfocus.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
    ) {
        // Main Content (Scrollable for smaller screens)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 140.dp), // Space for wave
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthTreeLogo(modifier = Modifier.padding(bottom = 32.dp))

            Text(
                text = "Tạo tài khoản",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AuthGreenDark
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Bắt đầu hành trình tập trung của bạn",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AuthTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Họ và tên",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Xác nhận mật khẩu",
                icon = Icons.Default.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator(color = AuthGreenLight)
            } else {
                AuthGradientButton(
                    text = "Đăng ký",
                    onClick = {
                        localError = null
                        if (password != confirmPassword) {
                            localError = "Mật khẩu xác nhận không khớp"
                            return@AuthGradientButton
                        }
                        viewModel.register(email, password, name) { onNavigateToHome() }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Đã có tài khoản?", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Đăng nhập", color = AuthGreenLight, fontWeight = FontWeight.Bold)
                }
            }

            // Display Local Error or ViewModel Error
            val displayError = localError ?: viewModel.errorMessage
            displayError?.let {
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