package com.example.greenfocus.util

import android.Manifest
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.data.repository.SessionRepository
import kotlinx.coroutines.launch

class TimerForegroundService : LifecycleService() {

    private val CHANNEL_ID = "PomodoroTimerChannel"
    private val NOTIFICATION_ID = 1

    private lateinit var repository: SessionRepository
    private lateinit var notificationManager: NotificationManager

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        repository = (application as GreenFocusApp).container.sessionRepository
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()

        // 1. Calculate the exact finish time based on the repository's current state
        val remainingSeconds = repository.timerState.value.currentTime
        val targetTimeMillis = System.currentTimeMillis() + (remainingSeconds * 1000L)

        // 2. Start the foreground service immediately with the Chronometer notification
        startForeground(NOTIFICATION_ID, buildChronometerNotification(targetTimeMillis))

        lifecycleScope.launch {
            repository.timerState.collect { state ->
                if (state.currentTime <= 0) {
                    // Timer is done! Play a sound and kill the service.
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "ACTION_START" -> repository.startTimer(lifecycleScope)
            "ACTION_PAUSE" -> {
                repository.pauseTimer()
                // If paused, you might want to switch to a static text notification
                // because a Chronometer cannot easily be "paused" in a notification
            }
            "ACTION_STOP" -> {
                repository.pauseTimer()
                stopSelf()
            }
        }
        return START_STICKY
    }

    // --- THE CHRONOMETER NOTIFICATION ---
    private fun buildChronometerNotification(targetTimeMillis: Long): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus!")
            .setSmallIcon(R.drawable.ic_dialog_info)

            // The Magic Methods
            .setWhen(targetTimeMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true) // Note: Requires Android 7.0 (API 24)+

            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}