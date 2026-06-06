package com.example.greenfocus.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.MainActivity
import com.example.greenfocus.R
import com.example.greenfocus.data.repository.UserSettingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class TimerForegroundService : LifecycleService() {

    private val CHANNEL_ID = "PomodoroTimerChannel"
    private val NOTIFICATION_ID = 1
    private val FINISHED_NOTIFICATION_ID = 2
    private val FAILED_NOTIFICATION_ID = 3
    private lateinit var timerManager: TimerManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var soundManager: SoundManager

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        timerManager = (application as GreenFocusApp).container.timerManager
        soundManager = (application as GreenFocusApp).container.soundManager

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        createNotificationChannel()

        val currentTimerConfigs = timerManager.timerState.value
        // 1. Calculate the exact finish time based on the repository's current state
        val remainingSeconds = currentTimerConfigs.currentTime
        val targetTimeMillis = System.currentTimeMillis() + (remainingSeconds * 1000L)

        // 2. Start the foreground service immediately with the Chronometer notification
        startForeground(NOTIFICATION_ID, buildChronometerNotification(targetTimeMillis))

        lifecycleScope.launch {
            timerManager.timerState.collect { state ->
                if (state.currentTime <= 0) {
                    playSound(currentTimerConfigs.currentFinishSound)
                    notificationManager.notify(FINISHED_NOTIFICATION_ID, buildFinishedNotification())
                    stopSelf()
                }
            }
        }

        //If DEEP MODE: Activate app usage detection
        if (timerManager.timerState.value.isDeepModeEnabled) {
            val serviceStartTime = System.currentTimeMillis()
            lifecycleScope.launch {
                Log.d("TIMER_MANAGER", "getUsageStatsStream activated")
                getUsageStatsStream(usageStatsManager, serviceStartTime).collect { state ->
                    if (!isScreenInteractive()) {
                        Log.d("TIMER_MANAGER", "Screen is not interactive, skipping check.")
                        return@collect
                    }
                    
                    val allowedUserApps = timerManager.timerState.value.deepModeAllowedApps
                    val systemApps = setOf(
                        packageName,
                        "com.android.systemui",
                        "com.android.keyguard"
                    )
                    val launcherApps = getLauncherPackages()
                    val allAllowedApps = allowedUserApps + systemApps + launcherApps

                    Log.d("TIMER_MANAGER", "Active app = $state. Allowed = ${allAllowedApps.contains(state)}")
                    if (!allAllowedApps.contains(state)) {
                        playSound(Sound.LOSE)
                        timerManager.pauseTimer()
                        notificationManager.notify(FAILED_NOTIFICATION_ID, buildFailedNotification())
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "ACTION_START" -> {
                playSound(Sound.CLICK)
                timerManager.startTimer(lifecycleScope)
            }

            "ACTION_STOP" -> {
                if (timerManager.timerState.value.isTimerRunning) {
                    playSound(Sound.LOSE)
                    timerManager.pauseTimer()
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (timerManager.timerState.value.isTimerRunning) {
            timerManager.pauseTimer()
        }
        stopForeground(true)
        stopSelf()
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
            .setSmallIcon(R.drawable.ic_greenfocus_noti)

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
            .setSmallIcon(R.drawable.ic_greenfocus_noti)

            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildFailedNotification(): Notification {
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
            .setContentTitle("DEEP MODE: You failed due to entering other apps!")
            .setSmallIcon(R.drawable.ic_greenfocus_noti)

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

    private fun getUsageStatsStream(usageStatsManager: UsageStatsManager, serviceStartTime: Long) : Flow<String> = flow {
        while (true) {
            val endTime = System.currentTimeMillis()
            val startTime = serviceStartTime.coerceAtLeast(endTime - 60000)
            if (endTime > startTime) {
                val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()
                var latestApp: String? = null
                var latestTime = 0L

                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        if (event.timeStamp > latestTime) {
                            latestApp = event.packageName
                            latestTime = event.timeStamp
                        }
                    }
                }
                Log.d("TIMER_MANAGER", "Usage stats checking...")
                if (latestApp != null) {
                    Log.d("TIMER_MANAGER", "Latest resumed app: $latestApp at $latestTime")
                    emit(latestApp)
                }
            }
            delay(2000)
        }
    }

    private fun isScreenInteractive(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isInteractive
    }

    private fun getLauncherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfos.map { it.activityInfo.packageName }.toSet()
    }

    private fun playSound(resId: Int) {
        soundManager.playSound(resId)
    }
}
