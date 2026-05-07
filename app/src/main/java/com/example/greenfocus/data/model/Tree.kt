package com.example.greenfocus.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class TreeType(
    val id: String,
    @StringRes val name: Int,
    val price: Int = 0,
    val growthTimeMinutes: Int = 25,
    val lottieAnimation: String = "", // Tên file lottie trong thư mục assets
    @DrawableRes val imageStaticSeed: Int,
    @DrawableRes val imageStaticSmall: Int,
    @DrawableRes val imageStaticBig: Int,
)