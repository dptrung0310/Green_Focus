package com.example.greenfocus.ui.screen.pomodoro

data class PomodoroUiState (
    val isTimerRunning: Boolean = false,
    val isTimerFinished: Boolean = false,
    val formattedTime: String = "25:00",
    val dialogTimeValue: Int = 25,
    val currentPercentage: Float = 0.0f,
    val currentTreeId: String = "",
    val showTimeDialog: Boolean = false,
    val isDeepFocusEnabled: Boolean = false
)