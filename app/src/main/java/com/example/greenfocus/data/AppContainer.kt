package com.example.greenfocus.data

import com.example.greenfocus.data.repository.AuthRepository
import com.example.greenfocus.data.repository.DataRepository
import com.example.greenfocus.data.repository.ProdAuthRepository
import com.example.greenfocus.data.repository.ProdDataRepository
import com.example.greenfocus.data.repository.ProdSessionRepository
import com.example.greenfocus.data.repository.SessionRepository

interface AppContainer {
    val authRepository: AuthRepository
    val sessionRepository: SessionRepository
    val dataRepository: DataRepository
}

class DefaultAppContainer : AppContainer {
    override val authRepository: AuthRepository by lazy {
        ProdAuthRepository()
    }
    override val sessionRepository: SessionRepository by lazy {
        ProdSessionRepository()
    }
    override val dataRepository: DataRepository by lazy {
        ProdDataRepository()
    }
}