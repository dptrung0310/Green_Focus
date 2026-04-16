package com.example.greenfocus.ui.screen.pomodoro

data class PomodoroUiState (
    val isTimerRunning: Boolean = false,
    val formattedTime: String = "25:00",
    val currentPercentage: Float = 0.0f,
    val currentTreeId: String = "",
    val showTimeDialog: Boolean = false,
    val isDeepFocusEnabled: Boolean = false
)