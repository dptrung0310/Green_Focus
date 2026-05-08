package com.example.greenfocus.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val coins: Int = 0,
    val level: Int = 1,
    val experience: Int = 0,
    val totalFocusTime: Long = 0, // Tính theo giây
    val totalTreesPlanted: Int = 0, // Để làm Bảng xếp hạng
    val unlockedTreeIds: List<String> = listOf("default_oak"),
    val friendIds: List<String> = emptyList(), // Danh sách UID bạn bè
    val blacklistApps: List<String> = emptyList() // Danh sách package name bị chặn (com.facebook.katana...)
)