package com.example.greenfocus.util

import android.util.Log
import com.example.greenfocus.data.DataSource
import com.example.greenfocus.data.model.FocusSession
import com.example.greenfocus.data.repository.DataRepository
import com.example.greenfocus.data.repository.UserRepository
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
    val treeId: String = DataSource.plants[0].id
)

class TimerManager(
    private var dataRepository: DataRepository,
    private var userRepository: UserRepository
) {
    private var timerJob: Job? = null

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    fun startTimer(scope: CoroutineScope) {

        if (_timerState.value.isTimerRunning) return;

        _timerState.update { it.copy(isTimerRunning = true, sessionState = SessionState.INIT) }
        _timerState.update { it.copy(sessionState = SessionState.RUNNING) }

        timerJob = scope.launch {
            while (_timerState.value.currentTime > 0) {
                delay(1000L)
                _timerState.update { it.copy(currentTime = it.currentTime - 1) }

                //TODO: Remove this if not needed
//                if (_timerState.value.currentTime * 2 == _timerState.value.totalTime) {
//                    _timerState.update { it.copy(sessionState = SessionState.HALF_DONE) }
//                }
            }
            timerFinished(scope)
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isTimerRunning = false) }
        timerCancelled()
    }

    fun setTreeId(newTreeId: String) {
        _timerState.update { it.copy(treeId = newTreeId) }
    }
    fun setTimer(minutes: Int) {
        _timerState.update { it.copy(currentTime = 60 * minutes, totalTime = 60 * minutes, sessionState = SessionState.INIT) }
    }
    fun timerFinished(scope: CoroutineScope) {
        _timerState.update { it.copy(isTimerRunning = false, currentTime = it.totalTime, sessionState = SessionState.SUCCESS) }
        dataRepository.addSession(FocusSession(
            treeId = _timerState.value.treeId,
            startTime = System.currentTimeMillis(),
            durationMinutes = _timerState.value.totalTime / 60, // The time they successfully completed
            status = "ALIVE"
        ))
        scope.launch { try {
            userRepository.addCoins(timerState.value.totalTime / 60)
        } catch (_: Exception) {
            Log.d("USER_REPO", "Timer finished, but error updating coins value")
        } }
        Log.d("SESSION_REPO_TIMER", "Timer finished normally");
    }

    fun timerCancelled() {
        _timerState.update { it.copy(isTimerRunning = false, currentTime = it.totalTime, sessionState = SessionState.FAILED) }
        dataRepository.addSession(FocusSession(
            treeId = _timerState.value.treeId,
            startTime = System.currentTimeMillis(),
            durationMinutes = _timerState.value.totalTime / 60, // The time they successfully completed
            status = "DEAD"
        ))
        Log.d("SESSION_REPO_TIMER", "Timer stopped mid-way");
    }
}