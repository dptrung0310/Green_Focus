package com.example.greenfocus.ui.screen.forest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.data.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForestViewModel(
    private var dataRepository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForestUiState())
    var forestUiState : StateFlow<ForestUiState> = _uiState.asStateFlow()

    init {
        // As soon as the ViewModel is created, start listening to Firestore session list of user
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            dataRepository.getSessions().collect {
                sessions ->
                    val totalCount = sessions.size
                    val aliveCount = sessions.count { it.status == "ALIVE" }
                    val deadCount = sessions.count { it.status == "DEAD" }
                    _uiState.update { it.copy(
                        currentAliveTree = aliveCount,
                        currentDeadTree = deadCount,
                        currentTotalTree = totalCount,
                        treeList = sessions) }
            }
        }
    }

    fun setFilter(filter: ForestFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GreenFocusApp)
                val dataRepository = application.container.dataRepository
                ForestViewModel(dataRepository = dataRepository)
            }
        }
    }
}