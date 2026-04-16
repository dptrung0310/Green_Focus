package com.example.greenfocus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.greenfocus.ui.screen.auth.RegisterScreen
import com.example.greenfocus.ui.screen.pomodoro.PomodoroScreen

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Register.route // Hiện tại để Register làm màn đầu tiên
    ) {
        // 1. Ô Màn hình Đăng ký
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Login.route) {
            // LoginScreen()
        }

        composable(route = Screen.Home.route) {
            PomodoroScreen()
        }

        // ... Tương tự cho Shop, Forest
    }
}