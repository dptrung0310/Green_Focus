package com.example.greenfocus.ui.navigation

sealed class Screen(val route: String) {
    object Register : Screen("register")
    object Login : Screen("login")
    object Home : Screen("home")
    object Shop : Screen("shop")
    object Forest : Screen("forest")
}