package com.example.greenfocus.data.model

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromEmail: String = "",
    val fromDisplayName: String = "",
    val fromAvatarUrl: String = "",
    val toUid: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
