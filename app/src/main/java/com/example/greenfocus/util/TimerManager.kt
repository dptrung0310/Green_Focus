package com.example.greenfocus.util

import android.util.Log
import com.example.greenfocus.data.model.FocusSession
import com.example.greenfocus.data.model.TreeType
import com.example.greenfocus.data.repository.DataRepository
import com.example.greenfocus.data.repository.DefaultSettings
import com.example.greenfocus.data.repository.UserPreferences
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.data.repository.UserSettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val isDeepModeEnabled: Boolean = false,
    val currentTime: Int = 25 * 60,
    val totalTime: Int = 25 * 60,
    val sessionState: SessionState = SessionState.INIT,
    val currentTree: TreeType = TreeType.DEFAULT,

    val currentFinishSound: Int = DefaultSettings.FINISH_SOUND,
    val deepModeAllowedApps: Set<String> = DefaultSettings.DEEP_MODE_APPS
)

class TimerManager(
    private var dataRepository: DataRepository,
    private var userRepository: UserRepository,
    private var userSettingRepository: UserSettingRepository,
    private var scope: CoroutineScope
) {
    private var timerJob: Job? = null
    private var activeRoomId: String? = null

    private val _timerState = MutableStateFlow(TimerState())
    private val _userSettings = userSettingRepository.userPreferences.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = UserPreferences()
    )
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    init {
        scope.launch {
            _userSettings.collect { userSettings -> _timerState.update {
                it.copy(currentFinishSound = userSettings.currentFinishSound,
                        deepModeAllowedApps = userSettings.deepModeAllowedApps)
                }
            }
        }
    }
    fun startTimer(scope: CoroutineScope) {

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
            timerFinished(scope)
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerCancelled()
    }

    fun setActiveRoom(roomId: String?) {
        activeRoomId = roomId
    }

    fun resetTimer(resetSeconds: Int? = null) {
        timerJob?.cancel()
        val nextTime = resetSeconds ?: _timerState.value.totalTime
        _timerState.update {
            it.copy(
                isTimerRunning = false,
                currentTime = nextTime,
                totalTime = nextTime,
                sessionState = SessionState.INIT
            )
        }
    }

    fun toggleDeepMode() {
        _timerState.update { it.copy(isDeepModeEnabled = !it.isDeepModeEnabled) }
    }
    fun setTree(newTree: TreeType) {
        _timerState.update { it.copy(currentTree = newTree) }
    }
    fun setTimer(minutes: Int) {
        _timerState.update { it.copy(currentTime = 60 * minutes, totalTime = 60 * minutes, sessionState = SessionState.INIT) }
    }
    fun setTimerSeconds(seconds: Int) {
        _timerState.update { it.copy(currentTime = seconds, totalTime = seconds, sessionState = SessionState.INIT) }
    }
    fun timerFinished(scope: CoroutineScope) {
        _timerState.update { it.copy(isTimerRunning = false, currentTime = it.totalTime, sessionState = SessionState.SUCCESS) }
        val groupRoomId = activeRoomId
        dataRepository.addSession(FocusSession(
            treeId = _timerState.value.currentTree.id,
            startTime = System.currentTimeMillis(),
            durationMinutes = _timerState.value.totalTime / 60, // The time they successfully completed
            status = "ALIVE",
            isGroupSession = groupRoomId != null,
            roomId = groupRoomId
        ))
        val durationSeconds = _timerState.value.totalTime.toLong()
        val durationMinutes = (durationSeconds / 60).toInt()
        val coinsIncrement = durationMinutes
        val treesPlantedIncrement = 1
        val xpIncrement = durationMinutes * 10
        scope.launch { try {
            userRepository.updateFocusStats(
                coinsIncrement = coinsIncrement,
                durationSeconds = durationSeconds,
                treesPlantedIncrement = treesPlantedIncrement,
                xpIncrement = xpIncrement
            )
        } catch (e: Exception) {
            Log.d("USER_REPO", "Timer finished, but error updating focus stats: ${e.message}")
        } }
        Log.d("SESSION_REPO_TIMER", "Timer finished normally");
    }

    fun timerCancelled() {
        _timerState.update { it.copy(isTimerRunning = false, currentTime = it.totalTime, sessionState = SessionState.FAILED) }
        val groupRoomId = activeRoomId
        if (groupRoomId == null) {
            dataRepository.addSession(FocusSession(
                treeId = _timerState.value.currentTree.id,
                startTime = System.currentTimeMillis(),
                durationMinutes = _timerState.value.totalTime / 60, // The time they successfully completed
                status = "DEAD"
            ))
        } else {
            Log.d(
                "SESSION_REPO_TIMER",
                "Group timer failed; waiting for room failure event $groupRoomId"
            )
        }
        Log.d("SESSION_REPO_TIMER", "Timer stopped mid-way");
    }
}
