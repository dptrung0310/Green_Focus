package com.example.greenfocus.ui.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.greenfocus.ui.components.BottomNavBar
import com.example.greenfocus.ui.navigation.Screen
import com.example.greenfocus.ui.screen.PlaceholderScreen
import com.example.greenfocus.ui.screen.forest.ForestScreen
import com.example.greenfocus.ui.screen.pomodoro.PomodoroScreen
import com.example.greenfocus.ui.screen.profile.ProfileScreen
import com.example.greenfocus.ui.screen.social.SocialScreen
import com.example.greenfocus.ui.screen.store.StoreScreen

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    innerNavController.navigate(route) {
                        // Tránh tạo nhiều bản sao của cùng một màn hình
                        popUpTo(innerNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                PomodoroScreen()
            }
            composable(Screen.Forest.route) {
                ForestScreen()
            }
            composable(Screen.Stats.route) {
                PlaceholderScreen(title = "Stats")
            }
            composable(Screen.Store.route) {
                StoreScreen()
            }
            composable(Screen.Social.route) {
                SocialScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onLogout = onLogout)
            }
        }
    }
}
