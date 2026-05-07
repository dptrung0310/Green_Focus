package com.example.greenfocus.util

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.MainActivity
import com.example.greenfocus.data.repository.SessionRepository
import kotlinx.coroutines.launch

class TimerForegroundService : LifecycleService() {

    private val CHANNEL_ID = "PomodoroTimerChannel"
    private val NOTIFICATION_ID = 1
    private val FINISHED_NOTIFICATION_ID = 2

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
                    notificationManager.notify(FINISHED_NOTIFICATION_ID, buildFinishedNotification())
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
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            // CRITICAL UX FIX: If the app is already open in the background,
            // this flag brings it to the front instead of creating a cloned second app.
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0, // Request code (usually 0 if you don't need to distinguish multiple intents)
            clickIntent,
            // FLAG_IMMUTABLE is strictly required by modern Android versions for security
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus!")
            .setSmallIcon(R.drawable.ic_dialog_info)

            // The Magic Methods
            .setWhen(targetTimeMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true) // Note: Requires Android 7.0 (API 24)+

            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildFinishedNotification(): Notification {
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            // CRITICAL UX FIX: If the app is already open in the background,
            // this flag brings it to the front instead of creating a cloned second app.
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0, // Request code (usually 0 if you don't need to distinguish multiple intents)
            clickIntent,
            // FLAG_IMMUTABLE is strictly required by modern Android versions for security
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session Over. Collect your reward!")
            .setSmallIcon(R.drawable.ic_dialog_info)

            .setContentIntent(pendingIntent)
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