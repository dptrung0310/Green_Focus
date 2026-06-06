package com.example.greenfocus.ui.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.ui.components.BottomNavBar
import com.example.greenfocus.ui.navigation.Screen
import com.example.greenfocus.ui.screen.PlaceholderScreen
import com.example.greenfocus.ui.screen.forest.ForestScreen
import com.example.greenfocus.ui.screen.pomodoro.PomodoroScreen
import com.example.greenfocus.ui.screen.pomodoro.HomeRoomViewModel
import com.example.greenfocus.ui.screen.profile.ProfileScreen
import com.example.greenfocus.ui.screen.social.SocialScreen
import com.example.greenfocus.ui.screen.store.StoreScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.screen.stats.StatsScreen
import androidx.compose.runtime.LaunchedEffect
import com.example.greenfocus.fcm.FcmTokenManager
import com.example.greenfocus.util.NotificationNavigationManager
import com.example.greenfocus.ui.screen.store.StoreViewModel
import com.example.greenfocus.util.Sound

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onSettingNavigate: () -> Unit,
    onArForestNavigate: () -> Unit
) {
    val context = LocalContext.current
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val soundManager = (LocalContext.current.applicationContext as GreenFocusApp).container.soundManager
    val timerManager = (LocalContext.current.applicationContext as GreenFocusApp).container.timerManager
    val homeRoomViewModel: HomeRoomViewModel = viewModel()
    val timerState by timerManager.timerState.collectAsState()
    val isTimerRunning = timerState.isTimerRunning

    // Update FCM token on successful login/startup entry
    LaunchedEffect(Unit) {
        FcmTokenManager.updateTokenInFirestore(context)
    }

    // Observe and perform pending notification clicks for dynamic redirection
    LaunchedEffect(Unit) {
        NotificationNavigationManager.pendingRoute.collect { route ->
            if (route != null) {
                innerNavController.navigate(route) {
                    popUpTo(innerNavController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                NotificationNavigationManager.clearPendingRoute()
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!isTimerRunning) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        innerNavController.navigate(route) {
                            // Tránh tạo nhiều bản sao của cùng một màn hình
                            soundManager.playSound(Sound.CLICK)
                            popUpTo(innerNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                PomodoroScreen(homeRoomViewModel = homeRoomViewModel)
            }
            composable(Screen.Forest.route) {
                ForestScreen(
                    onArForestNavigate = onArForestNavigate
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            composable(Screen.Store.route) {
                val context = LocalContext.current
                val viewModel: StoreViewModel = viewModel(
                    factory = StoreViewModel.factory(context)
                )
                StoreScreen(viewModel = viewModel)
            }
            composable(Screen.Social.route) {
                SocialScreen(
                    homeRoomViewModel = homeRoomViewModel,
                    onNavigateToHome = {
                        innerNavController.navigate(Screen.Home.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = onLogout,
                    onSettingNavigate = onSettingNavigate)
            }
        }
    }
}