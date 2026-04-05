package com.example.greenfocus.data.model

data class TreeType(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val growthTimeMinutes: Int = 25,
    val lottieAnimation: String = "", // Tên file lottie trong thư mục assets
    val imageStatic: String = ""      // Ảnh hiện trong khu rừng 2D
)