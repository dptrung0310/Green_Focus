package com.example.greenfocus.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greenfocus.ui.components.stats_components.*
import com.example.greenfocus.ui.theme.AuthBackground
import com.example.greenfocus.ui.theme.BannerGreen
import com.example.greenfocus.ui.theme.TextDark
import com.example.greenfocus.ui.theme.TextMuted

// ─────────────────────────────────────────
//  Entry point
// ─────────────────────────────────────────

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBackground)
    ) {
        when {
            uiState.isLoading  -> LoadingContent(Modifier.align(Alignment.Center))
            uiState.error != null -> ErrorContent(
                onRetry  = viewModel::reload,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> StatsContent(uiState)
        }
    }
}

// ─────────────────────────────────────────
//  States
// ─────────────────────────────────────────

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier, color = BannerGreen)
}

@Composable
private fun ErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ối! Không thể tải dữ liệu thống kê.", color = TextDark, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) { Text("Thử lại", color = BannerGreen) }
    }
}

// ─────────────────────────────────────────
//  Main scrollable layout — pure orchestration
// ─────────────────────────────────────────

@Composable
private fun StatsContent(state: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text("Phân tích",       fontSize = 26.sp, fontWeight = FontWeight.Bold, color = BannerGreen)
        Text("Theo dõi tiến trình của bạn", fontSize = 14.sp, color = TextMuted, modifier = Modifier.offset(y = (-8).dp))

        //Total Focus Card
        TotalFocusCard(totalMinutes = state.totalFocusMinutesThisMonth)

        //Sessions Card
        SessionStats(
            totalSessions  = state.totalSessionsThisMonth,
            completionRate = state.completionRateThisMonth
        )

        //Streak
        StreakCard(days = state.currentStreakDays)

        // Focus time chart
        FocusChartCard(
            weeklyData  = state.weeklyData,
            monthlyData = state.monthlyData,
            yearlyData  = state.yearlyData
        )

        //Time distribution by tree type
        if (state.timeDistribution.isNotEmpty()) {
            TimeDistributionCard(entries = state.timeDistribution)
        }
    }
}

// ─────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFFF9FBE7)
@Composable
private fun StatsScreenPreview() {
    StatsContent(
        StatsUiState(
            isLoading                  = false,
            totalFocusMinutesThisMonth = 2550,
            totalSessionsThisMonth     = 156,
            completionRateThisMonth    = 0.92f,
            currentStreakDays          = 7,
            weeklyData = listOf(
                DayBarData("Mon", 80), DayBarData("Tue", 95), DayBarData("Wed", 0),
                DayBarData("Thu", 150), DayBarData("Fri", 130),
                DayBarData("Sat", 45), DayBarData("Sun", 60, isToday = true)
            ),
            monthlyData = (1..30).map { DayBarData(it.toString(), (20..120).random()) },
            yearlyData  = listOf("Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec")
                .map { MonthBarData(it, (5..40).random().toFloat()) },
            timeDistribution = listOf(
                DistributionEntry("Oak",    600, 0.40f),
                DistributionEntry("Pine",   450, 0.30f),
                DistributionEntry("Cherry", 300, 0.20f),
                DistributionEntry("Bamboo", 150, 0.10f)
            )
        )
    )
}