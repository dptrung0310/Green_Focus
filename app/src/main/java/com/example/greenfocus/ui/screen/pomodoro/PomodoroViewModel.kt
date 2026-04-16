package com.example.greenfocus.ui.screen.pomodoro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenfocus.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PomodoroViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroUiState())
    var pomodoroUiState : StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private val sessionRepository: SessionRepository = SessionRepository()
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
                        isTimerRunning = timerState.isTimerRunning,
                        currentPercentage = progress,
                        formattedTime = formatTime(timerState.currentTime)
                    )
                }
            }
        }
    }
    fun toggleTimeDialog() {
        _uiState.update { currentState -> currentState.copy(showTimeDialog = !currentState.showTimeDialog)}
    }
    fun toggleDeepFocus() {
        _uiState.update { currentState -> currentState.copy(isDeepFocusEnabled = !currentState.isDeepFocusEnabled)}
    }

    fun startTimer() {
        sessionRepository.startTimer(viewModelScope)
    }

    fun pauseTimer() {
        sessionRepository.pauseTimer()
    }

    fun setTimer(minutes: Int) {
        sessionRepository.setTimer(minutes)
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
}