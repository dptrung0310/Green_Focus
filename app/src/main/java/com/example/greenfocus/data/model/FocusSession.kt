package com.example.greenfocus.data.model

data class FocusSession(
    val sessionId: String = "",
    val userId: String = "",
    val treeId: String = "",
    val startTime: Long = 0,          // System.currentTimeMillis()
    val durationMinutes: Int = 0,
    val status: String = "ALIVE",     // ALIVE hoặc WITHERED
    val isGroupSession: Boolean = false,
    val roomId: String? = null        // Nếu trồng nhóm thì lưu ID phòng vào đây
)