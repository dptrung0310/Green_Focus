package com.example.greenfocus.ui.screen.pomodoro

import com.example.greenfocus.R
import com.example.greenfocus.data.model.TreeType

data class PomodoroUiState (
    val userMoneyAmount: Int = 0,

    val currentUserName: String = "Placeholder",
    val isTimerRunning: Boolean = false,
    val isTimerFinished: Boolean = false,
    val isTimerFailed: Boolean = false,
    val isHalfDone: Boolean = false,
    val formattedTime: String = "25:00",
    val dialogTimeValue: Int = 25,
    val currentPercentage: Float = 0.0f,

    val selectedTree: TreeType = TreeType.DEFAULT,
    val selectedTreeImage: Int = selectedTree.imageStaticSeed,
    val unlockedTrees: List<TreeType> = listOf(TreeType.DEFAULT),

    val showTimeDialog: Boolean = false,
    val showRationaleDialog: Boolean = false,
    val showUsageStatsRationaleDialog: Boolean = false,
    val isDeepFocusEnabled: Boolean = false,
    val todayFocusMinutes: Int = 0,
    val todayTreesPlanted: Int = 0
)