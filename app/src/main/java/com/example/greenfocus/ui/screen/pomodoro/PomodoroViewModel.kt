package com.example.greenfocus.ui.screen.pomodoro

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.data.repository.SessionRepository
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

//    fun pauseTimerService(context: Context) {
//        val intent = Intent(context, TimerForegroundService::class.java).apply {
//            action = "ACTION_PAUSE"
//        }
//        context.startService(intent)
//    }

    fun stopTimerService(context: Context) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)
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