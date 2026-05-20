package com.example.greenfocus.ui.screen.profile

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.data.repository.UserPreferences
import com.example.greenfocus.data.repository.UserSettingRepository
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.greenfocus.data.repository.DefaultSettings
import com.example.greenfocus.util.Sound
import com.example.greenfocus.util.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable? // Note: Displaying Drawables in Compose requires the Accompanist or Coil library
)

data class SettingsUiState(
    val currentFinishSound: Int = DefaultSettings.FINISH_SOUND,
    val deepModeAllowedApps: Set<String> = DefaultSettings.DEEP_MODE_APPS,
    val allApps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true, // Start as true while we read DataStore
    val isChanged: Boolean = false,
    val showResetDialog: Boolean = false,
    val showUnsavedDialog: Boolean = false
)

class SettingsViewModel(
    private val userSettingRepository: UserSettingRepository,
    private val soundManager: SoundManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    private val _userSettings = userSettingRepository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )
    private var _baseState = UserPreferences()
    val settingsUiState : StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 1. Read DataStore exactly once to populate the draft when the screen opens
        viewModelScope.launch {
            val savedPreferences = userSettingRepository.userPreferences.first()

            _uiState.update {
                it.copy(
                    currentFinishSound = savedPreferences.currentFinishSound,
                    deepModeAllowedApps = savedPreferences.deepModeAllowedApps,
                    isLoading = false
                )
            }
            _baseState = savedPreferences
        }
    }

    fun getLaunchableApps(context: Context) {
        val packageManager = context.packageManager

        // Ask for all apps that show up in the app drawer
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Query the OS
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        val appList = resolveInfos.map { resolveInfo ->
            InstalledApp(
                packageName = resolveInfo.activityInfo.packageName,
                appName = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager)
            )
        }
            .distinctBy { it.packageName } // Prevent duplicates
            .sortedBy { it.appName }       // Alphabetize the list
        _uiState.update { it.copy(allApps = appList) }
    }

    fun saveAllData() {
        // Grab the CURRENT draft state and write it to the disk
        val currentDraft = _uiState.value

        viewModelScope.launch {
            userSettingRepository.setFinishSound(currentDraft.currentFinishSound)
            userSettingRepository.setDeepModeAllowedApps(currentDraft.deepModeAllowedApps)

            delay(1000)
            Log.d("USER_SETTING_DATASTORE", "Current DataStore Value")
            Log.d("USER_SETTING_DATASTORE", "Sound: " + _userSettings.value.currentFinishSound)
            Log.d("USER_SETTING_DATASTORE", "Apps: " + _userSettings.value.deepModeAllowedApps)
        }
        _uiState.update { it.copy(isChanged = false) }
    }

    fun resetToDefault() {
        // 1. Reset the UI draft to our centralized defaults immediately
        _uiState.update {
            it.copy(
                currentFinishSound = DefaultSettings.FINISH_SOUND,
                deepModeAllowedApps = DefaultSettings.DEEP_MODE_APPS,
            )
        }
        saveAllData()
    }

    fun updateFinishSoundDraft(sound: Int) {
        soundManager.playStoppableSound(sound)
        _uiState.update { it.copy(currentFinishSound = sound) }
        _uiState.update { it.copy(isChanged = checkForChanges()) }
    }

    fun updateAllowedAppsDraft(appList: Set<String>) {
        _uiState.update { it.copy(deepModeAllowedApps = appList + DefaultSettings.DEEP_MODE_APPS) }
        _uiState.update { it.copy(isChanged = checkForChanges()) }
    }

    fun toggleUnsavedDialog() {
        _uiState.update { it.copy(showUnsavedDialog = !it.showUnsavedDialog) }
    }

    fun toggleResetDialog() {
        _uiState.update { it.copy(showResetDialog = !it.showResetDialog) }
    }

    private fun checkForChanges() : Boolean {
        return _baseState.currentFinishSound != _uiState.value.currentFinishSound
                || _baseState.deepModeAllowedApps != _uiState.value.deepModeAllowedApps
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GreenFocusApp)
                val userSettingRepository = application.container.userSettingRepository
                val soundManager = application.container.soundManager
                SettingsViewModel(userSettingRepository = userSettingRepository, soundManager = soundManager)
            }
        }
    }
}