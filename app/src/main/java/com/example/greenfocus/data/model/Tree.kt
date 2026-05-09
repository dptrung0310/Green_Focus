package com.example.greenfocus.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.example.greenfocus.R

enum class TreeStatus {
    OWNED,
    BUYABLE,
    LOCKED
}

data class TreeType(
    val id: String,
    val name: String = "",
    val price: Int = 0,
    val description: String = "",
    val growthTimeMinutes: Int = 25,
    val lottieAnimation: String = "", // Tên file lottie trong thư mục assets
    val imageStaticSeed: String = "",
    val imageStaticSmall: String = "",
    val imageStaticBig: String = "",
)

data class StoreTreeItem(
    val tree: TreeType,
    val status: TreeStatus
)

object TreeResourceMapper {
    fun getDrawableResId(imageName: String): Int {
        return when (imageName) {
            //All image is tree for testing purpose
            "tree_oak" -> R.drawable.tree
            "tree_pine" -> R.drawable.tree
            "tree_cherry" -> R.drawable.tree
            "tree_maple" -> R.drawable.tree
            "tree_palm" -> R.drawable.tree
            "tree_cactus" -> R.drawable.tree
            "tree_bamboo" -> R.drawable.tree
            "tree_sakura" -> R.drawable.tree
            else -> R.drawable.forest
        }
    }
}
