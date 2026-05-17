package com.example.greenfocus.ui.screen.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.greenfocus.data.repository.ForestRepository
import com.example.greenfocus.data.repository.ProdForestRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

// ─────────────────────────────────────────
//  Data models
// ─────────────────────────────────────────

data class StatsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Top card
    val totalFocusMinutesThisMonth: Int = 0,

    // Two small cards
    val totalSessionsThisMonth: Int = 0,
    val completionRateThisMonth: Float = 0f,   // 0..1

    // Streak
    val currentStreakDays: Int = 0,

    // Chart data
    val weeklyData: List<DayBarData> = emptyList(),   // 7 entries Mon...Sun
    val monthlyData: List<DayBarData> = emptyList(),  // days in current month
    val yearlyData: List<MonthBarData> = emptyList(), // 12 entries

    // Time distribution (treeId → minutes)
    val timeDistribution: List<DistributionEntry> = emptyList()
)

data class DayBarData(
    val label: String,       // "Mon", "Tue" ... OR "1","2",...
    val minutes: Int,
    val isToday: Boolean = false
)

data class MonthBarData(
    val label: String,       // "Jan"..."Dec"
    val hours: Float
)

data class DistributionEntry(
    val label: String,       // treeId or tag name
    val minutes: Int,
    val fraction: Float      // 0..1
)

/** Raw Firestore document mapped from user_sessions */
private data class SessionDoc(
    val durationMinutes: Long,
    val startTime: Long,      // epoch millis
    val status: String,       // "ALIVE" | "DEAD"
    val treeId: String
)

// ─────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db               = FirebaseFirestore.getInstance()
    private val auth             = FirebaseAuth.getInstance()
    private val zone             = ZoneId.systemDefault()
    private val forestRepository : ForestRepository =
        ProdForestRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init { loadStats() }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = StatsUiState(isLoading = true)
            try {
                val uid = auth.currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                val sessions  = fetchSessions(uid)
                val treeNames = buildTreeNameMap()
                _uiState.value = buildUiState(sessions, treeNames)
            } catch (e: Exception) {
                _uiState.value = StatsUiState(isLoading = false, error = e.message)
            }
        }
    }

    // ── Tree name map (treeId → display name) ────────────────────
    private suspend fun buildTreeNameMap(): Map<String, String> {
        return try {
            val context = getApplication<Application>().applicationContext
            forestRepository.getAllTrees().associate { tree ->
                tree.id to context.getString(tree.name)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── Firestore query ──────────────────

    private suspend fun fetchSessions(uid: String): List<SessionDoc> {
        val snapshot = db
            .collection("sessions")
            .document(uid)
            .collection("user_sessions")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val duration = doc.getLong("durationMinutes") ?: return@mapNotNull null
            val startTime = doc.getLong("startTime")      ?: return@mapNotNull null
            val status    = doc.getString("status")       ?: return@mapNotNull null
            val treeId    = doc.getString("treeId")       ?: "unknown"
            SessionDoc(duration, startTime, status, treeId)
        }
    }

    // ── Business logic ───────────────────

    private fun buildUiState(sessions: List<SessionDoc>, treeNames: Map<String, String>): StatsUiState {
        val today   = LocalDate.now(zone)
        val zone    = this.zone

        // Helpers
        fun SessionDoc.localDate(): LocalDate =
            Instant.ofEpochMilli(startTime).atZone(zone).toLocalDate()

        val completed = sessions.filter { it.status == "ALIVE" }

        // ── Current month ────────────────
        val monthStart = today.withDayOfMonth(1)
        val thisMonth  = completed.filter { it.localDate() >= monthStart }

        val totalMinutesMonth = thisMonth.sumOf { it.durationMinutes }.toInt()
        val totalSessionsMonth = sessions.count { it.localDate() >= monthStart }
        val completedMonth     = completed.count { it.localDate() >= monthStart }
        val completionRate     = if (totalSessionsMonth > 0)
            completedMonth.toFloat() / totalSessionsMonth else 0f

        // ── Streak (consecutive days with ≥1 completed session) ──
        val completedDates = completed
            .map { it.localDate() }
            .toSet()
            .sortedDescending()

        var streak = 0
        var check  = today
        while (completedDates.contains(check)) {
            streak++
            check = check.minusDays(1)
        }
        // If today has no session yet, check from yesterday
        if (streak == 0) {
            check = today.minusDays(1)
            while (completedDates.contains(check)) {
                streak++
                check = check.minusDays(1)
            }
        }

        // ── Weekly data (Mon...Sun of current week) ────────────────
        val weekStart  = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dayLabels  = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val weeklyData = (0..6).map { offset ->
            val day      = weekStart.plusDays(offset.toLong())
            val mins     = completed
                .filter { it.localDate() == day }
                .sumOf { it.durationMinutes }
                .toInt()
            DayBarData(dayLabels[offset], mins, isToday = day == today)
        }

        // ── Monthly data (day 1...N of current month) ──────────────
        val daysInMonth = today.lengthOfMonth()
        val monthlyData = (1..daysInMonth).map { day ->
            val date = today.withDayOfMonth(day)
            val mins = completed
                .filter { it.localDate() == date }
                .sumOf { it.durationMinutes }
                .toInt()
            DayBarData(day.toString(), mins, isToday = date == today)
        }

        // ── Yearly data (Jan...Dec of current year) ────────────────
        val monthNames = listOf(
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
        )
        val yearlyData = (1..12).map { month ->
            val hours = completed
                .filter {
                    val d = it.localDate()
                    d.year == today.year && d.monthValue == month
                }
                .sumOf { it.durationMinutes }
                .toFloat() / 60f
            MonthBarData(monthNames[month - 1], hours)
        }

        // ── Time distribution by treeId ───────────────────────────
        val totalMins = completed.sumOf { it.durationMinutes }.toFloat()
        val grouped   = completed
            .groupBy { it.treeId }
            .map { (treeId, docs) ->
                val mins = docs.sumOf { it.durationMinutes }.toInt()
                DistributionEntry(
                    label    = treeNames[treeId] ?: treeId,   // fallback: raw treeId
                    minutes  = mins,
                    fraction = if (totalMins > 0) mins / totalMins else 0f
                )
            }
            .sortedByDescending { it.minutes }

        return StatsUiState(
            isLoading                = false,
            totalFocusMinutesThisMonth = totalMinutesMonth,
            totalSessionsThisMonth   = totalSessionsMonth,
            completionRateThisMonth  = completionRate,
            currentStreakDays        = streak,
            weeklyData               = weeklyData,
            monthlyData              = monthlyData,
            yearlyData               = yearlyData,
            timeDistribution         = grouped
        )
    }

}