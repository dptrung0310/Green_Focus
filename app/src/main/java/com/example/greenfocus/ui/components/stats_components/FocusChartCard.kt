package com.example.greenfocus.ui.components.stats_components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.ui.screen.stats.DayBarData
import com.example.greenfocus.ui.screen.stats.MonthBarData
import com.example.greenfocus.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf


//  ChartMode
enum class ChartMode(val label: String) {
    WEEK("Tuần"), MONTH("Tháng"), YEAR("Năm")
}

//  FocusChartCard
@Composable
fun FocusChartCard(
    weeklyData:  List<DayBarData>,
    monthlyData: List<DayBarData>,
    yearlyData:  List<MonthBarData>,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(ChartMode.WEEK) }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardNormal),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier                = Modifier.fillMaxWidth(),
                horizontalArrangement   = Arrangement.SpaceBetween,
                verticalAlignment       = Alignment.CenterVertically
            ) {
                Text(
                    text       = when (selectedMode) {
                        ChartMode.WEEK  -> "Tuần này"
                        ChartMode.MONTH -> "Tháng này"
                        ChartMode.YEAR  -> "Năm nay"
                    },
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextDark
                )
                Text("↗", fontSize = 16.sp, color = BannerGreen)
            }

            Spacer(Modifier.height(12.dp))

            //Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuthBackground),
            ) {
                ChartMode.entries.forEach { mode ->
                    ChartTab(
                        mode       = mode,
                        isSelected = mode == selectedMode,
                        onClick    = { selectedMode = mode },
                        modifier   = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            //Chart
            when (selectedMode) {
                ChartMode.WEEK  -> VicoBarChart(
                    entries    = weeklyData.map { it.minutes.toFloat() },
                    labels     = weeklyData.map { it.label },
                    yAxisLabel = "phút"
                )
                ChartMode.MONTH -> VicoBarChart(
                    entries    = monthlyData.map { it.minutes.toFloat() / 60f },
                    labels     = monthlyData.map { it.label },
                    yAxisLabel = "giờ",
                    labelEvery = 5
                )
                ChartMode.YEAR  -> VicoBarChart(
                    entries    = yearlyData.map { it.hours },
                    labels     = yearlyData.map { it.label },
                    yAxisLabel = "giờ"
                )
            }
        }
    }
}


//  Tab item — clickable fixed here
@Composable
private fun ChartTab(
    mode:       ChartMode,
    isSelected: Boolean,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .background(
                if (isSelected)
                    Brush.horizontalGradient(listOf(BannerGreen_Start, BannerGreen_End))
                else
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = mode.label,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isSelected) Color.White else TextMuted
        )
    }
}


//  Vico bar chart
@Composable
private fun VicoBarChart(
    entries:    List<Float>,
    labels:     List<String>,
    yAxisLabel: String,
    labelEvery: Int = 1
) {
    if (entries.isEmpty()) return

    val model = entryModelOf(
        entries.mapIndexed { i, v -> FloatEntry(i.toFloat(), v) }
    )

    val bottomFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val idx = value.toInt()
        if (idx in labels.indices && (labelEvery == 1 || idx % labelEvery == 0))
            labels[idx] else ""
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                LineComponent(
                    color         = BannerGreen_Start.hashCode(),
                    thicknessDp   = if (labels.size > 15) 6f else 16f,
                    shape         = com.patrykandpatrick.vico.core.component.shape.Shapes
                        .roundedCornerShape(
                            topLeftPercent     = 50,
                            topRightPercent    = 50,
                            bottomLeftPercent  = 0,
                            bottomRightPercent = 0
                        ),
                    dynamicShader = verticalGradient(
                        arrayOf(BannerGreen_Start, BannerGreen_End)
                    )
                )
            )
        ),
        model      = model,
        startAxis  = rememberStartAxis(
            label = textComponent {
                color      = TextMuted.hashCode()
                textSizeSp = 10f
            },
            valueFormatter = AxisValueFormatter { value, _ ->
                "${value.toInt()} $yAxisLabel"
            }
        ),
        bottomAxis = rememberBottomAxis(
            label = textComponent {
                color      = TextMuted.hashCode()
                textSizeSp = 10f
            },
            valueFormatter = bottomFormatter
        ),
        modifier   = Modifier
            .fillMaxWidth()
            .height(180.dp)
    )
}

// ─────────────────────────────────────────
//  Preview
// ─────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun FocusChartCardPreview() {
    FocusChartCard(
        weeklyData = listOf(
            DayBarData("Mon", 80), DayBarData("Tue", 95), DayBarData("Wed", 0),
            DayBarData("Thu", 150), DayBarData("Fri", 130),
            DayBarData("Sat", 45), DayBarData("Sun", 60, isToday = true)
        ),
        monthlyData = (1..30).map { DayBarData(it.toString(), (20..120).random()) },
        yearlyData  = listOf("Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec")
            .map { MonthBarData(it, (5..40).random().toFloat()) }
    )
}
