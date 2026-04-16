package com.example.greenfocus.ui.screen.pomodoro

data class PomodoroUiState (
    val currentTimer: Int = 25 * 60,
    val currentPercentage: Int = 0,
    val currentTreeId: String = "",
    val showTimeDialog: Boolean = false,
    val isDeepFocusEnabled: Boolean = false
)