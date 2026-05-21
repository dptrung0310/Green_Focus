package com.example.greenfocus.data.repository

import com.example.greenfocus.R
import com.example.greenfocus.data.model.TreeResourceMapper
import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.util.FirestoreCollections
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

interface ForestRepository {
    suspend fun getAllTrees(): List<TreeType>
}

data class TreeDTO(
    @SerializedName("id") val id: String,
    @SerializedName("nameKey") val name: String,
    @SerializedName("price") val price: Int = 0,
    @SerializedName("description") val description: String = "",
    @SerializedName("growthTimeMinutes") val growthTimeMinutes: Int = 25,
    @SerializedName("lottieAnimation") val lottieAnimation: String = "",
    @SerializedName("imageStaticSeed") val imageStaticSeed: String,
    @SerializedName("imageStaticSmall") val imageStaticSmall: String,
    @SerializedName("imageStaticBig") val imageStaticBig: String,
)

class ProdForestRepository(
    private val context: android.content.Context
) : ForestRepository {
    private val db = FirebaseModule.firestore

    private val nameKeyToStringRes = mapOf(
        "tree_oak" to R.string.tree_oak,
        "tree_pine" to R.string.tree_pine,
        "tree_cherry" to R.string.tree_cherry,
        "tree_maple" to R.string.tree_maple,
        "tree_palm" to R.string.tree_palm,
        "tree_cactus" to R.string.tree_cactus,
        "tree_bamboo" to R.string.tree_bamboo,
        "tree_sakura" to R.string.tree_sakura,
    )

    private fun parseLocalTrees(): List<TreeType> {
        val json = context.assets.open("trees.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<TreeDTO>>() {}.type
        val dtos: List<TreeDTO> = Gson().fromJson(json, type)

        return dtos.map { dto ->
            TreeType(
                id = dto.id,
                name = (nameKeyToStringRes[dto.name] ?: R.string.tree_oak),
                price = dto.price,
                description = dto.description,
                growthTimeMinutes = dto.growthTimeMinutes,
                lottieAnimation = dto.lottieAnimation,
                imageStaticSeed = TreeResourceMapper.getSeedDrawable(dto.id),
                imageStaticBig  = TreeResourceMapper.getBigDrawable(dto.id),
            )
        }
    }

    private suspend fun fetchFirestoreMetadata(): Map<String, TreeFirestoreMetadata> {
        return try {
            val snapshot = db.collection(FirestoreCollections.TREES).get().await()
            snapshot.documents.associate { doc ->
                doc.id to TreeFirestoreMetadata(
                    price = doc.getLong("price")?.toInt()
                )
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private data class TreeFirestoreMetadata(
        val price: Int? = null
    )

    override suspend fun getAllTrees(): List<TreeType> {
        // 1. Đọc local JSON từ assets
        val localTrees = parseLocalTrees()

        // 2. Fetch Firestore metadata (optional – override price nếu cần)
        //    Nếu Firestore chưa có collection trees thì dùng local data.
        val firestoreOverrides = fetchFirestoreMetadata()

        // 3. Merge: Firestore thắng nếu tồn tại
        return localTrees.map { tree ->
            val override = firestoreOverrides[tree.id]
            if (override != null) {
                tree.copy(price = override.price ?: tree.price)
            } else {
                tree
            }
        }
    }

}


