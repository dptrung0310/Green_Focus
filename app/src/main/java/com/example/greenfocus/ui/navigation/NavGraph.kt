package com.example.greenfocus.ui.navigation

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.fcm.FcmTokenManager
import com.example.greenfocus.ui.screen.auth.AuthViewModel
import com.example.greenfocus.ui.screen.auth.LoginScreen
import com.example.greenfocus.ui.screen.auth.RegisterScreen
import com.example.greenfocus.ui.screen.auth.SessionState
import com.example.greenfocus.ui.screen.auth.SessionViewModel
import com.example.greenfocus.ui.screen.main.MainScreen
import com.example.greenfocus.ui.screen.profile.SettingsScreen
import com.example.greenfocus.ui.screen.forest.ArForestScreen
import com.example.greenfocus.util.TimerForegroundService

@Composable
fun SetupNavGraph(navController: NavHostController) {
    val context = LocalContext.current.applicationContext
    val timerManager = (context as GreenFocusApp).container.timerManager

    // Session is the single source of truth for which flow (auth vs. main) is shown.
    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory)
    val sessionState by sessionViewModel.state.collectAsState()

    // Global redirect: any time the session resolves or is lost, route to the matching flow.
    // This is what guarantees a stale/invalid session can always escape back to login.
    SessionRedirect(navController, sessionState)

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.clearErrors()
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = { sessionViewModel.onAuthenticated() }
            )
        }

        composable(route = Screen.Register.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToHome = { sessionViewModel.onAuthenticated() },
                onNavigateToLogin = {
                    authViewModel.clearErrors()
                    if (!navController.popBackStack()) {
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
                    // Stop a running timer and its foreground service before tearing down the session.
                    if (timerManager.timerState.value.isTimerRunning) {
                        Log.d(TAG, "Stopping timer on logout")
                        timerManager.timerCancelled()
                        val intent = Intent(context, TimerForegroundService::class.java).apply {
                            action = TimerForegroundService.ACTION_STOP
                        }
                        context.startService(intent)
                    }

                    // Best-effort cleanup of the FCM token before signing out.
                    FcmTokenManager.deleteTokenFromFirestore(context)

                    // Signing out here flips session state -> Unauthenticated, which drives navigation.
                    sessionViewModel.onSignedOut()
                },
                onSettingNavigate = {
                    navController.navigate(Screen.Settings.route)
                },
                onArForestNavigate = {
                    navController.navigate(Screen.ArForest.route)
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ArForest.route) {
            ArForestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun SessionRedirect(navController: NavHostController, sessionState: SessionState) {
    androidx.compose.runtime.LaunchedEffect(sessionState) {
        val currentRoute = navController.currentDestination?.route
        when (sessionState) {
            SessionState.Checking -> Unit // Stay on the current auth route while session resolves.

            SessionState.Authenticated -> {
                if (currentRoute != Screen.Main.route) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            is SessionState.Unauthenticated -> {
                // Allow staying on Register; otherwise force back to the login screen.
                if (currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
                    Log.d(TAG, "Session unauthenticated; routing to login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

private const val TAG = "NavGraph"
