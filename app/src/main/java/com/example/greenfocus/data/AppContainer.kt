package com.example.greenfocus.data

import com.example.greenfocus.data.repository.DataRepository
import com.example.greenfocus.data.repository.ProdDataRepository
import com.example.greenfocus.data.repository.ProdSessionRepository
import com.example.greenfocus.data.repository.ProdUserRepository
import com.example.greenfocus.data.repository.SessionRepository
import com.example.greenfocus.data.repository.UserRepository

interface AppContainer {
    val sessionRepository: SessionRepository
    val dataRepository: DataRepository
    val userRepository: UserRepository
}

class DefaultAppContainer : AppContainer {
    override val sessionRepository: SessionRepository by lazy {
        ProdSessionRepository()
    }
    override val dataRepository: DataRepository by lazy {
        ProdDataRepository()
    }
    override val userRepository: UserRepository by lazy {
        ProdUserRepository()
    }
}