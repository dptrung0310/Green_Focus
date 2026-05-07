package com.example.greenfocus.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    object Register : Screen("register")
    object Login    : Screen("login")

    // Main wrapper (sau đăng nhập)
    object Main     : Screen("main")

    // Bottom Nav tabs
    object Home     : Screen("home")
    object Forest   : Screen("forest")
    object Stats    : Screen("stats")
    object Store    : Screen("store")
    object Social   : Screen("social")
    object Profile  : Screen("profile")
}