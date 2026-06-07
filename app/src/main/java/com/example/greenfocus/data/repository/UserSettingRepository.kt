package com.example.greenfocus.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.greenfocus.util.Sound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

interface UserSettingRepository {
    val userPreferences : Flow<UserPreferences>
    // We use suspend functions for writing because saving to disk takes time
    suspend fun setFinishSound(soundId: Int)
    suspend fun setDeepModeAllowedApps(appList: Set<String>)
}

object DefaultSettings {
    val FINISH_SOUND = Sound.WIN_BELL
    val DEEP_MODE_APPS = setOf(
        "com.example.greenfocus",
        "com.google.android.apps.nexuslauncher"
    )
}
data class UserPreferences(
    val currentFinishSound: Int = DefaultSettings.FINISH_SOUND,
    val deepModeAllowedApps: Set<String> = DefaultSettings.DEEP_MODE_APPS
)
class ProdUserSettingRepository(
    private var dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) : UserSettingRepository{
    private companion object {
        val CURRENT_FINISH_SOUND = intPreferencesKey("current_finish_sound")
        val DEEP_MODE_ALLOWED_APPS = stringPreferencesKey("deep_mode_allowed_app")
        const val TAG = "USER_SETTING_DATASTORE"
    }

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch {
            if(it is IOException) {
                Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {
            preferences ->
                val currentFinishSound = preferences[CURRENT_FINISH_SOUND] ?: DefaultSettings.FINISH_SOUND
                val deepModeAllowedApps = preferences[DEEP_MODE_ALLOWED_APPS]?.let { Json.decodeFromString<Set<String>>(it) } ?: DefaultSettings.DEEP_MODE_APPS
                UserPreferences(currentFinishSound, deepModeAllowedApps)
        }


    override suspend fun setFinishSound(soundId: Int) {
        Log.d(TAG, "Saving sound")
        dataStore.edit {
            preferences -> preferences[CURRENT_FINISH_SOUND] = soundId
        }
    }

    override suspend fun setDeepModeAllowedApps(appList: Set<String>) {
        Log.d(TAG, "Saving app")
        val appListAsJson = Json.encodeToString(appList)
        dataStore.edit {
            preferences -> preferences[DEEP_MODE_ALLOWED_APPS] = appListAsJson
        }
    }

}