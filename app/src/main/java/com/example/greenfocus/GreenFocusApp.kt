package com.example.greenfocus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.greenfocus.data.AppContainer
import com.example.greenfocus.data.DefaultAppContainer

class GreenFocusApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "green_focus_notifications",
                "Green Focus Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Green Focus push notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}