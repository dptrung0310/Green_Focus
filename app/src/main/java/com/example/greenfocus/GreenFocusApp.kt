package com.example.greenfocus

import android.app.Application
import com.example.greenfocus.data.AppContainer
import com.example.greenfocus.data.DefaultAppContainer

class GreenFocusApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}