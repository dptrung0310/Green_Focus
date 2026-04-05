package com.example.greenfocus.data.model

data class SurvivalMember(
    val uid: String = "",
    val name: String = "",
    val isReady: Boolean = false,
    val isFailed: Boolean = false     // Nếu 1 ông isFailed = true, cả phòng chết
)