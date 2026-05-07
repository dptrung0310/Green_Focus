package com.example.greenfocus.data.repository

import android.content.Context
import android.util.Log
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
enum class SessionState {
    INIT,
    RUNNING,
    HALF_DONE,
    FAILED,
    SUCCESS
}

data class TimerState(
    val isTimerRunning: Boolean = false,
    val currentTime: Int = 25 * 60,
    val totalTime: Int = 25 * 60,
    val sessionState: SessionState = SessionState.INIT,
    val treeId: String = "oak"
)

interface SessionRepository {
    val timerState: StateFlow<TimerState>

    fun startTimer(scope: CoroutineScope)
    fun pauseTimer()
    fun setTreeId(newTreeId: String)
    fun setTimer(minutes: Int)
}
class ProdSessionRepository : SessionRepository {
    private var timerJob: Job? = null
    private lateinit var currentSession: FocusSession

    private val _timerState = MutableStateFlow(TimerState())
    override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    override fun startTimer(scope: CoroutineScope) {
        // Chắc phải tạo FocusSession ở đây. Sẽ cần chỉnh sửa
        currentSession = FocusSession(
            startTime = System.currentTimeMillis(),
            durationMinutes = _timerState.value.currentTime / 60)

        if (_timerState.value.isTimerRunning) return;
        _timerState.update { it.copy(isTimerRunning = true, sessionState = SessionState.INIT) }
        _timerState.update { it.copy(sessionState = SessionState.RUNNING) }
        timerJob = scope.launch {
            while (_timerState.value.currentTime > 0) {
                delay(1000L)
                _timerState.update { it.copy(currentTime = it.currentTime - 1) }
                if (_timerState.value.currentTime * 2 == _timerState.value.totalTime) {
                    _timerState.update { it.copy(sessionState = SessionState.HALF_DONE) }
                }
            }
            timerFinished()
        }
    }

    override fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isTimerRunning = false) }
        timerCancelled()
    }

    override fun setTreeId(newTreeId: String) {
        _timerState.update { it.copy(treeId = newTreeId) }
    }
    override fun setTimer(minutes: Int) {
        _timerState.update { it.copy(currentTime = 60 * minutes, totalTime = 60 * minutes, sessionState = SessionState.INIT) }
    }
    private fun timerFinished() {
        _timerState.update { it.copy(isTimerRunning = false, currentTime = it.totalTime, sessionState = SessionState.SUCCESS) }
        Log.d("SESSION_REPO_TIMER", "Timer finished normally");
    }

    private fun timerCancelled() {
        _timerState.update { it.copy(isTimerRunning = false, currentTime = it.totalTime, sessionState = SessionState.FAILED) }
        Log.d("SESSION_REPO_TIMER", "Timer stopped mid-way");
    }
}