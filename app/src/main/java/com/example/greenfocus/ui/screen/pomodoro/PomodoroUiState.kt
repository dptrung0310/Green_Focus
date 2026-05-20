package com.example.greenfocus.ui.screen.pomodoro

import com.example.greenfocus.R
import com.example.greenfocus.data.DataSource
import com.example.greenfocus.data.model.TreeType

data class PomodoroUiState (
    val userMoneyAmount: Int = 0,

    val currentUserName: String = "Placeholder",
    val isTimerRunning: Boolean = false,
    val isTimerFinished: Boolean = false,
    val formattedTime: String = "25:00",
    val dialogTimeValue: Int = 25,
    val currentPercentage: Float = 0.0f,

    val selectedTree: TreeType = DataSource.plants[0],
    val selectedTreeImage: Int = selectedTree.imageStaticSeed,

    val showTimeDialog: Boolean = false,
    val showRationaleDialog: Boolean = false,
    val showUsageStatsRationaleDialog: Boolean = false,
    val isDeepFocusEnabled: Boolean = false
)