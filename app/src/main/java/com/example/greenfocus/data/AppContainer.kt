package com.example.greenfocus.data

import com.example.greenfocus.data.repository.SessionRepository

interface AppContainer {
    val sessionRepository: SessionRepository
}

class DefaultAppContainer : AppContainer {
    override val sessionRepository: SessionRepository by lazy {
        SessionRepository()
    }
}