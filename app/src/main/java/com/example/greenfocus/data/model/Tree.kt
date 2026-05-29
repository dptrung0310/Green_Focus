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
            imageStaticSeed = R.drawable.oak_small,
            imageStaticBig = R.drawable.oak_big
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
        imageStaticSeed = TreeResourceMapper.getSeedDrawable(id),
        imageStaticBig  = TreeResourceMapper.getBigDrawable(id),
    )
}

data class StoreTreeItem(
    val tree: TreeType,
    val status: TreeStatus
)

object TreeResourceMapper {
    fun getSeedDrawable(treeId: String): Int {
        return when (treeId) {
            "default_oak" -> R.drawable.oak_small
            "pine"        -> R.drawable.pine_small
            "cherry"      -> R.drawable.cherry_small
            "maple"       -> R.drawable.maple_small
            "palm"        -> R.drawable.palm_small
            "cactus"      -> R.drawable.cactus_small
            "bamboo"      -> R.drawable.bamboo_small
            "sakura"      -> R.drawable.sakura_small
            else          -> R.drawable.forest
        }
    }

    fun getBigDrawable(treeId: String): Int {
        return when (treeId) {
            "default_oak" -> R.drawable.oak_big
            "pine"        -> R.drawable.pine_big
            "cherry"      -> R.drawable.cherry_big
            "maple"       -> R.drawable.maple_big
            "palm"        -> R.drawable.palm_big
            "cactus"      -> R.drawable.cactus_big
            "bamboo"      -> R.drawable.bamboo_big
            "sakura"      -> R.drawable.sakura_big
            else          -> R.drawable.forest
        }
    }

    // Keep for backward-compatibility (used in ForestScreen TreeGrid)
    fun getDrawableResId(treeId: String): Int = getBigDrawable(treeId)

    fun getNameResId(treeId: String): Int {
        return when (treeId) {
            "default_oak" -> R.string.tree_oak
            "pine"        -> R.string.tree_pine
            "cherry"      -> R.string.tree_cherry
            "maple"       -> R.string.tree_maple
            "palm"        -> R.string.tree_palm
            "cactus"      -> R.string.tree_cactus
            "bamboo"      -> R.string.tree_bamboo
            "sakura"      -> R.string.tree_sakura
            else          -> R.string.tree_oak
        }
    }
}
