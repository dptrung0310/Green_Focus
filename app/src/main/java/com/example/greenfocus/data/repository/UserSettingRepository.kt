package com.example.greenfocus.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.greenfocus.util.Sound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

interface UserSettingRepository {
    val userPreferences : Flow<UserPreferences>
    // We use suspend functions for writing because saving to disk takes time
    suspend fun setFinishSound(soundId: Int)
    suspend fun setDeepModeAllowedApps(appList: Set<String>)
}

data class UserPreferences(
    val currentFinishSound: Int = Sound.WIN_BELL,
    val deepModeAllowedApps: Set<String> = emptySet()
)
class ProdUserSettingRepository(
    private var dataStore: DataStore<Preferences>
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
                val currentFinishSound = preferences[CURRENT_FINISH_SOUND] ?: Sound.WIN_BELL
                val deepModeAllowedApps = preferences[DEEP_MODE_ALLOWED_APPS]?.let { Json.decodeFromString<Set<String>>(it) } ?: emptySet()
                UserPreferences(currentFinishSound, deepModeAllowedApps)
        }


    override suspend fun setFinishSound(soundId: Int) {
        dataStore.edit {
            preferences -> preferences[CURRENT_FINISH_SOUND] = soundId
        }
    }

    override suspend fun setDeepModeAllowedApps(appList: Set<String>) {
        val appListAsJson = Json.encodeToString(appList)
        dataStore.edit {
            preferences -> preferences[DEEP_MODE_ALLOWED_APPS] = appListAsJson
        }
    }

}