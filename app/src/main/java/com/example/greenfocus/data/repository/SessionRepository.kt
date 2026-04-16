package com.example.greenfocus.data.repository

import android.content.Context
import androidx.work.WorkManager
import com.example.greenfocus.data.model.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerState(
    val isTimerRunning: Boolean = false,
    val currentTime: Int = 25 * 60,
    val totalTime: Int = 25 * 60
)
class SessionRepository() {
    private var timerJob: Job? = null
    private lateinit var currentSession: FocusSession

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    fun startTimer(scope: CoroutineScope) {
        // Chắc phải tạo FocusSession ở đây. Sẽ cần chỉnh sửa
        currentSession = FocusSession(
            startTime = System.currentTimeMillis(),
            durationMinutes = _timerState.value.currentTime / 60)

        if (_timerState.value.isTimerRunning) return;
        _timerState.update { it.copy(isTimerRunning = true) }
        timerJob = scope.launch {
            while (_timerState.value.currentTime > 0) {
                delay(1000L)
                _timerState.update { it.copy(currentTime = it.currentTime - 1) }
            }
            timerFinished()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isTimerRunning = false) }
    }

    fun setTimer(minutes: Int) {
        pauseTimer()
        _timerState.update { it.copy(currentTime = 60 * minutes, totalTime = 60 * minutes) }
    }
    private fun timerFinished() {
        _timerState.update { it.copy(isTimerRunning = false) }
    }
}