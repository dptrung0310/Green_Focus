package com.example.greenfocus.ui.screen.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenfocus.data.model.StoreTreeItem
import com.example.greenfocus.data.model.TreeStatus
import com.example.greenfocus.data.repository.ForestRepository
import com.example.greenfocus.data.repository.PurchaseResult
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.di.FirebaseModule
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StoreViewModel(
    private val forestRepository: ForestRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private var pendingPurchaseItem: StoreTreeItem? = null

    private val uid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid


    private fun loadData() {
        val currentUid = uid ?: run {
            _uiState.update { it.copy(errorMessage = "User not logged in") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Load danh sách cây từ repository (assets + Firestore merge)
                val allTrees = forestRepository.getAllTrees()

                // Lắng nghe user profile realtime (coins + unlockedTreeIds thay đổi sau mua)
                userRepository.getUserProfileFlow(currentUid).collect { user ->
                    val unlockedIds = user?.unlockedTreeIds ?: emptyList()

                    // Map TreeType → StoreTreeItem với đúng status
                    val storeItems = allTrees.map { tree ->
                        val status = when {
                            unlockedIds.contains(tree.id) -> TreeStatus.OWNED
                            tree.price > 0 -> TreeStatus.BUYABLE
                            else -> TreeStatus.LOCKED
                        }
                        StoreTreeItem(tree = tree, status = status)
                    }

                    _uiState.update {
                        it.copy(
                            trees = storeItems,
                            userCoins = user?.coins ?: 0,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load store")
                }
            }
        }
    }

    init {
        loadData()
    }
    
    fun onBuyClicked(item: StoreTreeItem) {
        if (item.status != TreeStatus.BUYABLE) return

        pendingPurchaseItem = item
        _uiState.update { it.copy(showPurchaseConfirmDialog = true, pendingItem = item) }
    }

    fun onDismissDialog() {
        pendingPurchaseItem = null
        _uiState.update {
            it.copy(showPurchaseConfirmDialog = false, showInsufficientFundsDialog = false, pendingItem = null)
        }
    }

    fun onConfirmPurchase() {
        val item = pendingPurchaseItem ?: return
        val currentUid = uid ?: return

        _uiState.update { it.copy(showPurchaseConfirmDialog = false, isLoading = true) }
    }
}