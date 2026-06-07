package com.example.greenfocus.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.greenfocus.GreenFocusApp
import com.example.greenfocus.MainActivity
import com.example.greenfocus.R
import com.example.greenfocus.di.FirebaseModule
import com.example.greenfocus.ui.screen.pomodoro.room.TeamRoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerForegroundService : LifecycleService() {

    private val channelId = "PomodoroTimerChannel"
    private val notificationId = 1
    private val finishedNotificationId = 2
    private val failedNotificationId = 3

    private lateinit var timerManager: TimerManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var soundManager: SoundManager
    private lateinit var teamRoomRepository: TeamRoomRepository

    private var timerStateJob: Job? = null
    private var deepModeJob: Job? = null
    private var isStoppingSelf = false

    override fun onCreate() {
        super.onCreate()
        timerManager = (application as GreenFocusApp).container.timerManager
        soundManager = (application as GreenFocusApp).container.soundManager
        teamRoomRepository = TeamRoomRepository()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        createNotificationChannel()
        startForeground(notificationId, buildIdleNotification())
        observeTimerState()
        Log.d(TAG, "onCreate: foreground service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val action = intent?.action
        Log.d(
            TAG,
            "onStartCommand: action=$action, startId=$startId, activeRoom=${timerManager.getActiveRoomId()}, " +
                "storedRoom=${getStoredRoomId()}, isTimerRunning=${timerManager.timerState.value.isTimerRunning}"
        )

        if (action == null) {
            restoreStoredRoomOrStop()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_WATCH_ROOM -> {
                val roomId = intent.getStringExtra(EXTRA_ROOM_ID)
                if (!roomId.isNullOrBlank()) {
                    timerManager.setActiveRoom(roomId)
                    storeRoomId(roomId)
                    Log.d(TAG, "ACTION_WATCH_ROOM: now watching room=$roomId")
                    if (!timerManager.timerState.value.isTimerRunning) {
                        startForeground(notificationId, buildRoomPresenceNotification())
                    }
                } else {
                    Log.w(TAG, "ACTION_WATCH_ROOM ignored: missing room id")
                }
            }

            ACTION_CLEAR_ROOM -> {
                Log.d(TAG, "ACTION_CLEAR_ROOM: clearing room watcher")
                timerManager.setActiveRoom(null)
                clearStoredRoomId()
                if (timerManager.timerState.value.isTimerRunning) {
                    startForeground(notificationId, buildChronometerNotification())
                } else {
                    stopForegroundAndSelf()
                }
            }

            ACTION_START -> {
                playSound(Sound.CLICK)
                timerManager.startTimer(lifecycleScope)
                Log.d(TAG, "ACTION_START: timer started for activeRoom=${timerManager.getActiveRoomId()}")
                startForeground(notificationId, buildChronometerNotification())
                startDeepModeWatcherIfNeeded()
            }

            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP: requested")
                if (timerManager.timerState.value.isTimerRunning) {
                    playSound(Sound.LOSE)
                    timerManager.pauseTimer()
                }
                stopDeepModeWatcher()
                if (timerManager.getActiveRoomId() != null) {
                    startForeground(notificationId, buildRoomPresenceNotification())
                } else {
                    stopForegroundAndSelf()
                }
            }

            ACTION_RESET -> {
                val resetSeconds = intent.getIntExtra(
                    EXTRA_RESET_SECONDS,
                    timerManager.timerState.value.totalTime
                )
                Log.d(TAG, "ACTION_RESET: resetSeconds=$resetSeconds")
                timerManager.resetTimer(resetSeconds)
                stopDeepModeWatcher()
                if (timerManager.getActiveRoomId() != null) {
                    startForeground(notificationId, buildRoomPresenceNotification())
                } else {
                    stopForegroundAndSelf()
                }
            }

            else -> {
                Log.w(TAG, "onStartCommand: unknown action=$action")
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.w(
            TAG,
            "onDestroy: activeRoom=${timerManager.getActiveRoomId()}, storedRoom=${getStoredRoomId()}, " +
                "isTimerRunning=${timerManager.timerState.value.isTimerRunning}"
        )
        timerStateJob?.cancel()
        timerStateJob = null
        stopDeepModeWatcher()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(
            TAG,
            "onTaskRemoved: rootIntent=$rootIntent, activeRoom=${timerManager.getActiveRoomId()}, " +
                "storedRoom=${getStoredRoomId()}, isTimerRunning=${timerManager.timerState.value.isTimerRunning}"
        )
        lifecycleScope.launch {
            val wasFocusing = timerManager.timerState.value.isTimerRunning
            try {
                withContext(Dispatchers.IO) {
                    cleanupRoomStateAfterTaskRemoved(wasFocusing)
                }
                Log.d(TAG, "onTaskRemoved: cleanup finished successfully")
            } catch (e: Exception) {
                Log.e(TAG, "onTaskRemoved: cleanup failed unexpectedly", e)
            } finally {
                if (wasFocusing) {
                    timerManager.pauseTimer()
                }
                timerManager.setActiveRoom(null)
                clearStoredRoomId()
                Log.d(TAG, "onTaskRemoved: stopping foreground service")
                stopForegroundAndSelf()
            }
        }
    }

    private fun observeTimerState() {
        timerStateJob?.cancel()
        timerStateJob = lifecycleScope.launch {
            timerManager.timerState.collect { state ->
                if (state.sessionState == SessionState.SUCCESS || state.currentTime <= 0) {
                    playSound(state.currentFinishSound)
                    notificationManager.notify(finishedNotificationId, buildFinishedNotification())
                    stopDeepModeWatcher()
                    if (timerManager.getActiveRoomId() != null) {
                        startForeground(notificationId, buildRoomPresenceNotification())
                    } else {
                        stopForegroundAndSelf()
                    }
                }
            }
        }
    }

    private suspend fun cleanupRoomStateAfterTaskRemoved(wasFocusing: Boolean) {
        val userId = FirebaseModule.auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "cleanupRoomStateAfterTaskRemoved: skipped because Firebase user is null")
            return
        }

        val activeRoomId = timerManager.getActiveRoomId()
        val storedRoomId = getStoredRoomId()
        Log.d(
            TAG,
            "cleanupRoomStateAfterTaskRemoved: userId=$userId, wasFocusing=$wasFocusing, " +
                "activeRoom=$activeRoomId, storedRoom=$storedRoomId"
        )

        val knownRoomIds = listOfNotNull(activeRoomId, storedRoomId)
            .filter { it.isNotBlank() }
            .distinct()
        val queriedRoomIds = if (knownRoomIds.isEmpty()) {
            teamRoomRepository.getJoinedRoomIds(userId)
                .onFailure { error -> Log.e(TAG, "cleanup: cannot query joined rooms", error) }
                .getOrDefault(emptyList())
        } else {
            Log.d(TAG, "cleanup: skip joined-room query because knownRoomIds=$knownRoomIds")
            emptyList()
        }

        val roomIds = (knownRoomIds + queriedRoomIds).distinct()

        Log.d(TAG, "cleanupRoomStateAfterTaskRemoved: targetRooms=$roomIds")

        if (roomIds.isEmpty()) {
            Log.w(TAG, "cleanupRoomStateAfterTaskRemoved: no room found for user=$userId")
            return
        }

        roomIds.forEach { roomId ->
            if (wasFocusing) {
                Log.d(TAG, "cleanup: cancelling running session in room=$roomId")
                teamRoomRepository.cancelSession(roomId, userId)
                    .onSuccess { Log.d(TAG, "cleanup: cancelSession success room=$roomId") }
                    .onFailure { error -> Log.e(TAG, "cleanup: cancelSession failed room=$roomId", error) }
            }
            Log.d(TAG, "cleanup: leaving room=$roomId")
            teamRoomRepository.leaveRoom(roomId, userId)
                .onSuccess { Log.d(TAG, "cleanup: leaveRoom success room=$roomId") }
                .onFailure { error -> Log.e(TAG, "cleanup: leaveRoom failed room=$roomId", error) }
        }
    }

    private fun restoreStoredRoomOrStop() {
        val storedRoomId = getStoredRoomId()
        if (!storedRoomId.isNullOrBlank()) {
            timerManager.setActiveRoom(storedRoomId)
            Log.d(TAG, "restoreStoredRoomOrStop: restored room watcher for room=$storedRoomId")
            startForeground(notificationId, buildRoomPresenceNotification())
            return
        }

        if (!timerManager.timerState.value.isTimerRunning) {
            Log.d(TAG, "restoreStoredRoomOrStop: no room or timer to keep, stopping service")
            stopForegroundAndSelf()
        }
    }

    private fun startDeepModeWatcherIfNeeded() {
        if (!timerManager.timerState.value.isDeepModeEnabled || deepModeJob?.isActive == true) return

        val serviceStartTime = System.currentTimeMillis()
        deepModeJob = lifecycleScope.launch {
            Log.d(TAG, "Deep focus usage detection activated")
            getUsageStatsStream(usageStatsManager, serviceStartTime).collect { activePackage ->
                if (!isScreenInteractive()) {
                    Log.d(TAG, "Screen is not interactive, skipping usage check")
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

                Log.d(TAG, "Active app = $activePackage. Allowed = ${allAllowedApps.contains(activePackage)}")
                if (!allAllowedApps.contains(activePackage)) {
                    playSound(Sound.LOSE)
                    timerManager.pauseTimer()
                    notificationManager.notify(failedNotificationId, buildFailedNotification())
                    stopDeepModeWatcher()
                    if (timerManager.getActiveRoomId() != null) {
                        startForeground(notificationId, buildRoomPresenceNotification())
                    } else {
                        stopForegroundAndSelf()
                    }
                }
            }
        }
    }

    private fun stopDeepModeWatcher() {
        deepModeJob?.cancel()
        deepModeJob = null
    }

    private fun buildIdleNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("GreenFocus")
            .setContentText("Đang theo dõi phiên tập trung")
            .setSmallIcon(R.drawable.ic_greenfocus_noti)
            .setContentIntent(buildMainPendingIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun buildRoomPresenceNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("GreenFocus")
            .setContentText("Đang ở trong phòng tập trung")
            .setSmallIcon(R.drawable.ic_greenfocus_noti)
            .setContentIntent(buildMainPendingIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun buildChronometerNotification(): Notification {
        val state = timerManager.timerState.value
        val targetTimeMillis = System.currentTimeMillis() + (state.currentTime * 1000L)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Đang tập trung")
            .setSmallIcon(R.drawable.ic_greenfocus_noti)
            .setWhen(targetTimeMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(buildMainPendingIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun buildFinishedNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Phiên tập trung đã hoàn thành")
            .setContentText("Mở GreenFocus để nhận phần thưởng")
            .setSmallIcon(R.drawable.ic_greenfocus_noti)
            .setContentIntent(buildMainPendingIntent())
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildFailedNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Deep Focus đã dừng")
            .setContentText("Bạn đã mở ứng dụng ngoài danh sách cho phép")
            .setSmallIcon(R.drawable.ic_greenfocus_noti)
            .setContentIntent(buildMainPendingIntent())
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildMainPendingIntent(): PendingIntent {
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getUsageStatsStream(
        usageStatsManager: UsageStatsManager,
        serviceStartTime: Long
    ): Flow<String> = flow {
        while (true) {
            val endTime = System.currentTimeMillis()
            val startTime = serviceStartTime.coerceAtLeast(endTime - 60_000)
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
                if (latestApp != null) {
                    emit(latestApp)
                }
            }
            delay(2_000)
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

    private fun stopForegroundAndSelf() {
        if (isStoppingSelf) {
            Log.d(TAG, "stopForegroundAndSelf ignored: already stopping")
            return
        }
        isStoppingSelf = true
        Log.d(TAG, "stopForegroundAndSelf")
        timerStateJob?.cancel()
        timerStateJob = null
        stopDeepModeWatcher()
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    private fun playSound(resId: Int) {
        soundManager.playSound(resId)
    }

    private fun storeRoomId(roomId: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_ROOM_ID, roomId)
            .apply()
        Log.d(TAG, "storeRoomId: room=$roomId")
    }

    private fun getStoredRoomId(): String? {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_ROOM_ID, null)
    }

    private fun clearStoredRoomId() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .remove(KEY_ROOM_ID)
            .apply()
        Log.d(TAG, "clearStoredRoomId")
    }

    companion object {
        const val ACTION_START = "com.example.greenfocus.action.START_TIMER"
        const val ACTION_STOP = "com.example.greenfocus.action.STOP_TIMER"
        const val ACTION_RESET = "com.example.greenfocus.action.RESET_TIMER"
        const val ACTION_WATCH_ROOM = "com.example.greenfocus.action.WATCH_ROOM"
        const val ACTION_CLEAR_ROOM = "com.example.greenfocus.action.CLEAR_ROOM"
        const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        const val EXTRA_RESET_SECONDS = "RESET_SECONDS"
        private const val TAG = "TimerForegroundService"
        private const val PREFS_NAME = "room_task_cleanup"
        private const val KEY_ROOM_ID = "active_room_id"
    }
}
