package com.example.greenfocus.data

import com.example.greenfocus.data.repository.AuthRepository
import com.example.greenfocus.data.repository.DataRepository
import com.example.greenfocus.data.repository.ProdAuthRepository
import com.example.greenfocus.data.repository.ProdDataRepository
import com.example.greenfocus.data.repository.ProdUserRepository
import com.example.greenfocus.data.repository.UserRepository
import com.example.greenfocus.util.TimerManager

interface AppContainer {
    val authRepository: AuthRepository
    val dataRepository: DataRepository
    val userRepository: UserRepository
    val timerManager: TimerManager
}

class DefaultAppContainer : AppContainer {
    override val authRepository: AuthRepository by lazy {
        ProdAuthRepository()
    }
    override val dataRepository: DataRepository by lazy {
        ProdDataRepository()
    }
    override val userRepository: UserRepository by lazy {
        ProdUserRepository()
    }
    override val timerManager: TimerManager by lazy {
        TimerManager(dataRepository, userRepository)
    }
}