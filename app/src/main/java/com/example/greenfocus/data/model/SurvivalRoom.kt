package com.example.greenfocus.data.model

// Lưu tại Realtime Database
data class SurvivalRoom(
    val roomId: String = "",
    val hostId: String = "",
    val treeId: String = "",
    val roomStatus: String = "WAITING",
    val members: Map<String, SurvivalMember> = emptyMap()
)