package com.example.greenfocus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.greenfocus.data.repository.AuthRepository
import com.example.greenfocus.data.repository.DataRepository
import com.example.greenfocus.data.repository.ProdAuthRepository
import com.example.greenfocus.data.repository.ProdDataRepository
import com.example.greenfocus.data.repository.ProdUserRepository
import com.example.greenfocus.data.repository.ProdUserSettingRepository
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.data.repository.UserSettingRepository
import com.example.greenfocus.data.repository.ForestRepository
import com.example.greenfocus.data.repository.ProdForestRepository
import com.example.greenfocus.util.SoundManager
import com.example.greenfocus.util.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val USER_PREFERENCE_NAME = "user_preferences"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCE_NAME
)
interface AppContainer {
    val authRepository: AuthRepository
    val dataRepository: DataRepository
    val userRepository: UserRepository
    val userSettingRepository: UserSettingRepository
    val timerManager: TimerManager
    val soundManager: SoundManager
    val forestRepository: ForestRepository
}

class DefaultAppContainer(
    private var  context: Context
) : AppContainer {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val authRepository: AuthRepository by lazy {
        ProdAuthRepository()
    }
    override val dataRepository: DataRepository by lazy {
        ProdDataRepository()
    }
    override val userRepository: UserRepository by lazy {
        ProdUserRepository()
    }
    override val userSettingRepository: UserSettingRepository by lazy {
        ProdUserSettingRepository(context.dataStore, applicationScope)
    }
    override val timerManager: TimerManager by lazy {
        TimerManager(dataRepository, userRepository, userSettingRepository, applicationScope)
    }
    override val soundManager: SoundManager by lazy {
        SoundManager(context)
    }
    override val forestRepository: ForestRepository by lazy {
        ProdForestRepository(context)
    }
}