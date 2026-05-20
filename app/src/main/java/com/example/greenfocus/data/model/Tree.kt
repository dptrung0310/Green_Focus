package com.example.greenfocus.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.example.greenfocus.R

enum class TreeStatus {
    OWNED,
    BUYABLE,
    LOCKED
}

// Firestore model — plain data, no @StringRes
data class TreeTypeDto(
    val id: String = "",
    val nameKey: String = "",
    val price: Int = 0,
    val description: String = "",
    val growthTimeMinutes: Int = 25,
    val lottieAnimation: String = "",
    val imageStaticSeed: String = "",
    val imageStaticSmall: String = "",
    val imageStaticBig: String = "",
)


data class TreeType(
    val id: String,
    @StringRes val name: Int,
    val price: Int = 0,
    val description: String = "",
    val growthTimeMinutes: Int = 25,
    val lottieAnimation: String = "", // Tên file lottie trong thư mục assets
    @DrawableRes val imageStaticSeed: Int,
    @DrawableRes val imageStaticBig: Int,
) {
    companion object {
        val DEFAULT = TreeType(
            id = "default_oak",
            name = R.string.tree_oak,
            price = 300,
            growthTimeMinutes = 25,
            imageStaticSeed = R.drawable.oak1,
            imageStaticBig = R.drawable.oak1
        )
    }
}

private val nameKeyToStringRes = mapOf(
    "tree_oak"    to R.string.tree_oak,
    "tree_pine"   to R.string.tree_pine,
    "tree_cherry" to R.string.tree_cherry,
    "tree_maple"  to R.string.tree_maple,
    "tree_palm"   to R.string.tree_palm,
    "tree_cactus" to R.string.tree_cactus,
    "tree_bamboo" to R.string.tree_bamboo,
    "tree_sakura" to R.string.tree_sakura,
)

// Trong ProdForestRepository
fun TreeTypeDto.toTreeType(): TreeType {
    return TreeType(
        id = id,
        name = nameKeyToStringRes[nameKey] ?: R.string.tree_oak,
        price = price,
        description = description,
        growthTimeMinutes = growthTimeMinutes,
        lottieAnimation = lottieAnimation,
        imageStaticSeed = TreeResourceMapper.getDrawableResId(imageStaticSeed),
        imageStaticBig = TreeResourceMapper.getDrawableResId(imageStaticBig),
    )
}

data class StoreTreeItem(
    val tree: TreeType,
    val status: TreeStatus
)

object TreeResourceMapper {
    fun getDrawableResId(imageName: String): Int {
        return when (imageName) {
            //All image is tree for testing purpose
            "tree_oak" -> R.drawable.oak1
            "tree_pine" -> R.drawable.pine1
            "tree_cherry" -> R.drawable.cherry1
            "tree_maple" -> R.drawable.maple_tree
            "tree_palm" -> R.drawable.palm_tree
            "tree_cactus" -> R.drawable.cactus
            "tree_bamboo" -> R.drawable.bamboo_tree
            "tree_sakura" -> R.drawable.sakura
            else -> R.drawable.forest
        }
    }
}
