package com.example.greenfocus.data

import com.example.greenfocus.R
import com.example.greenfocus.data.model.TreeResourceMapper
import com.example.greenfocus.data.model.TreeType
object DataSource {
    val plants = listOf(
        TreeType(
            id = "tree_oak",
            name = R.string.tree_oak,
            price = 50,
            growthTimeMinutes = 20,
            imageStaticSeed = R.drawable.oak1,
            imageStaticBig = TreeResourceMapper.getDrawableResId("tree_oak"),
        ),
        TreeType(
            id = "tree_pine",
            name = R.string.tree_pine,
            price = 50,
            growthTimeMinutes = 25,
            imageStaticSeed = R.drawable.pine1,
            imageStaticBig = TreeResourceMapper.getDrawableResId("tree_pine"),
        ),
        TreeType(
            id = "tree_cherry",
            name = R.string.tree_cherry,
            price = 50,
            growthTimeMinutes = 30,
            imageStaticSeed = R.drawable.cherry1,
            imageStaticBig = TreeResourceMapper.getDrawableResId("tree_cherry"),
        )
    )
}