package com.example.greenfocus.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.ui.screen.auth.AuthViewModel
import com.example.greenfocus.ui.screen.auth.LoginScreen
import com.example.greenfocus.ui.screen.auth.OpeningScreen
import com.example.greenfocus.ui.screen.auth.RegisterScreen

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "opening"
    ) {
        composable(route = "opening") {
            OpeningScreen(
                onFinished = {
                    val currentUser = FirebaseModule.auth.currentUser
                    val nextRoute = if (currentUser != null) Screen.Home.route else Screen.Login.route

                    navController.navigate(nextRoute) {
                        popUpTo("opening") { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Register.route) {
            val authViewModel: AuthViewModel = viewModel()
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    if (!navController.popBackStack()) {
                        authViewModel.clearErrors()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(route = Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                onNavigateToRegister = {
                    authViewModel.clearErrors()
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route){
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Chào mừng đến với GreenFocus!")
                Button(onClick = {
                    FirebaseModule.auth.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }) {
                    Text("Đăng xuất")
                }
            }
        }

        // ... Tương tự cho Shop, Forest
    }
}