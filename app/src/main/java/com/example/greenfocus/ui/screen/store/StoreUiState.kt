package com.example.greenfocus.ui.screen.store

import com.example.greenfocus.data.model.StoreTreeItem

data class StoreUiState(
    val trees: List<StoreTreeItem> = emptyList(),
    val userCoins: Int = 0,
    val isLoading: Boolean = false,

    val showPurchaseConfirmDialog: Boolean = false,
    val pendingItem: StoreTreeItem? = null,
    val showInsufficientFundsDialog: Boolean = false,
    val errorMessage: String? = null,
)