package com.example.greenfocus.ui.navigation

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.ui.screen.auth.AuthViewModel
import com.example.greenfocus.ui.screen.auth.LoginScreen
import com.example.greenfocus.ui.screen.auth.OpeningScreen
import com.example.greenfocus.ui.screen.auth.RegisterScreen
import com.example.greenfocus.ui.screen.main.MainScreen
import com.example.greenfocus.ui.screen.profile.SettingsScreen
import com.example.greenfocus.util.TimerForegroundService
import com.example.greenfocus.util.TimerManager
import kotlinx.coroutines.delay

@Composable
fun SetupNavGraph(navController: NavHostController) {
    val context = LocalContext.current.applicationContext
    val timerManager = (LocalContext.current.applicationContext as GreenFocusApp).container.timerManager
    NavHost(
        navController = navController,
        startDestination = "opening"
    ) {
        composable(route = "opening") {
            OpeningScreen(
                onFinished = {
                    val currentUser = FirebaseModule.auth.currentUser
                    val nextRoute = if (currentUser != null) Screen.Main.route else Screen.Login.route
                    navController.navigate(nextRoute) {
                        popUpTo("opening") { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
            LoginScreen(
                onNavigateToRegister = {
                    authViewModel.clearErrors()
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Register.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
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

        composable(route = Screen.Main.route) {
            MainScreen(
                onLogout = {
                    // Stop running timer

                    if (timerManager.timerState.value.isTimerRunning) {
                        Log.d("NavGraph", "Stopping timer")
                        timerManager.timerCancelled()
                        val intent = Intent(context, TimerForegroundService::class.java).apply {
                            action = "ACTION_STOP"
                        }
                        context.startService(intent)
                        Log.d("NavGraph", "Timer stopped")
                    }


                    FirebaseModule.auth.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSettingNavigate = {
                    Log.d("NavGraph", "Trying to navigate to settings")
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Main.route)
                }
            )
        }
    }
}