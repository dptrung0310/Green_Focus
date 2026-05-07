package com.example.greenfocus.data

import com.example.greenfocus.R
import com.example.greenfocus.data.model.TreeType
object DataSource {
    val plants = listOf(
        TreeType(
            id = "oak",
            name = R.string.tree_oak,
            price = 50,
            growthTimeMinutes = 20,
            imageStaticSeed = R.drawable.oak1,
            imageStaticSmall = R.drawable.oak2,
            imageStaticBig = R.drawable.oak3
        ),
        TreeType(
            id = "pine",
            name = R.string.tree_pine,
            price = 50,
            growthTimeMinutes = 25,
            imageStaticSeed = R.drawable.pine1,
            imageStaticSmall = R.drawable.pine2,
            imageStaticBig = R.drawable.pine3
        ),
        TreeType(
            id = "cherry",
            name = R.string.tree_cherry,
            price = 50,
            growthTimeMinutes = 30,
            imageStaticSeed = R.drawable.cherry1,
            imageStaticSmall = R.drawable.cherry2,
            imageStaticBig = R.drawable.cherry3
        )
    )
}