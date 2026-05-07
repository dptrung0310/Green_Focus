package com.example.greenfocus.ui.screen.pomodoro

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.greenfocus.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.data.repository.SessionRepository
import com.example.greenfocus.data.repository.SessionState
import com.example.greenfocus.util.TimerForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PomodoroViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroUiState())
    var pomodoroUiState : StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    init {
        // As soon as the ViewModel is created, start listening to the TimerManager
        observeTimer()
    }

    private fun observeTimer() {
        viewModelScope.launch {
            sessionRepository.timerState.collect {
                timerState ->
                val progress = if (timerState.totalTime > 0) {
                    timerState.currentTime.toFloat() / timerState.totalTime.toFloat()
                } else 1f
                _uiState.update { currentState ->
                    currentState.copy(
                        dialogTimeValue = timerState.currentTime / 60,
                        isTimerRunning = timerState.isTimerRunning,
                        currentPercentage = progress,
                        formattedTime = formatTime(timerState.currentTime),
                    )
                }
                when (timerState.sessionState) {
                    SessionState.INIT -> {
                        _uiState.update { it.copy(selectedTreeImage = it.selectedTree.imageStaticSeed)}
                    }
                    SessionState.SUCCESS -> {
                        onTimerFinishedSuccessfully()
                    }
                    SessionState.FAILED -> {
                        onTimerFailed()
                    }
                    SessionState.HALF_DONE -> {
                        _uiState.update { it.copy(selectedTreeImage = it.selectedTree.imageStaticSmall)}
                    }
                    else -> {
                        // Do nothing for INIT or RUNNING
                    }
                }
            }
        }
    }

    fun toggleTimeDialog() {
        _uiState.update { currentState -> currentState.copy(showTimeDialog = !currentState.showTimeDialog)}
    }
    fun toggleRationaleDialog() {
        _uiState.update { currentState -> currentState.copy(showRationaleDialog = !currentState.showRationaleDialog)}
    }
    fun updateTimeDialogValue(value: String) {
        _uiState.update { currentState -> currentState.copy(dialogTimeValue = value.toIntOrNull() ?: 0) }
    }

    fun updateSelectedTree(value: TreeType) {
        sessionRepository.setTreeId(value.id)
        _uiState.update { currentState -> currentState.copy(selectedTree = value) }
        updateSelectedTreeImage(value.imageStaticSeed)
    }

    fun updateSelectedTreeImage(value: Int) {
        _uiState.update { currentState -> currentState.copy(selectedTreeImage = value) }
    }
    fun toggleDeepFocus() {
        _uiState.update { currentState -> currentState.copy(isDeepFocusEnabled = !currentState.isDeepFocusEnabled)}
    }

    fun setTimer() {
         sessionRepository.setTimer(_uiState.value.dialogTimeValue)
    }

    fun startTimerService(context: Context) {

        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = "ACTION_START"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTimerService(context: Context) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)
    }


    private fun onTimerFailed() {
        _uiState.update { it.copy(selectedTreeImage = R.drawable.dead_tree)}
    }

    private fun onTimerFinishedSuccessfully() {
        _uiState.update { it.copy(selectedTreeImage = it.selectedTree.imageStaticBig)}
    }
    /**
     * Helper function to format seconds into MM:SS string
     */
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        // Use String.format to ensure two digits (e.g., "05" instead of "5")
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GreenFocusApp)
                val sessionRepository = application.container.sessionRepository
                PomodoroViewModel(sessionRepository = sessionRepository)
            }
        }
    }
}