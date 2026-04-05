package com.example.greenfocus.data.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val type: String = "TEXT"         // TEXT hoặc SYSTEM (thông báo)
)