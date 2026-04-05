package com.example.greenfocus.util

object FirestoreCollections {
    const val USERS = "users"
    const val TREES = "trees_catalog"     // Danh mục cây trong shop
    const val SESSIONS = "focus_sessions"  // Nhật ký trồng cây (để vẽ biểu đồ)
    const val FRIEND_REQUESTS = "friend_requests"
    const val CHATS = "chats"             // Lưu nội dung tin nhắn
}

object RealtimePaths {
    const val ROOMS = "survival_rooms"    // Dùng cho Team Survival
}

object Status {
    const val ALIVE = "ALIVE"             // Cây sống
    const val WITHERED = "WITHERED"       // Cây héo
    const val PENDING = "PENDING"         // Đang chờ (cho kết bạn/phòng)
}

object RoomStatus {
    const val WAITING = "WAITING"
    const val STARTED = "STARTED"
    const val FINISHED = "FINISHED"
}