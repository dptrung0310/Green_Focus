package com.example.greenfocus.ui.screen.forest

import com.example.greenfocus.data.model.FocusSession

enum class ForestFilter { ALL, ALIVE, DEAD }
data class ForestUiState (
    val currentAliveTree: Int = 0,
    val currentDeadTree: Int = 0,
    val currentTotalTree: Int = 0,
    val currentFilter: ForestFilter = ForestFilter.ALL,
    val treeList: List<FocusSession> = emptyList()
)