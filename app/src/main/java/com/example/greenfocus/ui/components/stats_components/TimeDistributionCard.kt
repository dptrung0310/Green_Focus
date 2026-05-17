package com.example.greenfocus.ui.components.stats_components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.greenfocus.ui.screen.stats.DistributionEntry
import com.example.greenfocus.ui.theme.CardNormal
import com.example.greenfocus.ui.theme.TextDark
import com.example.greenfocus.ui.theme.TextMuted

// Five shades of green used for donut segments + legend dots
val DonutColors = listOf(
    Color(0xFF2f7f33),   // BannerGreen_End — darkest
    Color(0xFF4bae4f),   // BannerGreen_Start
    Color(0xFF7ac87d),   // mid-green
    Color(0xFFa3d5a5),   // CardOwnedBackGround_Start
    Color(0xFFcaead1)    // very light
)

@Composable
fun TimeDistributionCard(
    entries:  List<DistributionEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardNormal),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = "Time Distribution",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextDark
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutChart(
                    entries  = entries,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.width(20.dp))
                Legend(entries = entries.take(5))
            }
        }
    }
}


@Composable
private fun DonutChart(
    entries:  List<DistributionEntry>,
    modifier: Modifier = Modifier
) {
    // Animate each segment independently with staggered delay
    val animated = entries.take(5).mapIndexed { i, entry ->
        val value by animateFloatAsState(
            targetValue    = entry.fraction,
            animationSpec  = tween(durationMillis = 800, delayMillis = i * 80),
            label          = "donut_$i"
        )
        value
    }

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.18f
        val radius      = (size.minDimension - strokeWidth) / 2f
        val arcTopLeft  = Offset(center.x - radius, center.y - radius)
        val arcSize     = Size(radius * 2, radius * 2)
        var startAngle  = -90f

        animated.forEachIndexed { i, fraction ->
            val sweep = fraction * 360f
            drawArc(
                color      = DonutColors.getOrElse(i) { DonutColors.last() },
                startAngle = startAngle,
                sweepAngle = (sweep - 2f).coerceAtLeast(0f),   // 2° gap
                useCenter  = false,
                topLeft    = arcTopLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}



@Composable
private fun Legend(entries: List<DistributionEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEachIndexed { i, entry ->
            LegendRow(
                color = DonutColors.getOrElse(i) { DonutColors.last() },
                label = entry.label,
                pct   = (entry.fraction * 100).toInt()
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, pct: Int) {
    Row(
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text     = label,
            fontSize = 13.sp,
            color    = TextDark,
            modifier = Modifier.weight(1f)
        )
        Text(
            text      = "$pct%",
            fontSize  = 13.sp,
            color     = TextMuted,
            textAlign = TextAlign.End
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun TimeDistributionCardPreview() {
    TimeDistributionCard(
        entries = listOf(
            DistributionEntry("Oak",    600, 0.40f),
            DistributionEntry("Pine",   450, 0.30f),
            DistributionEntry("Cherry", 300, 0.20f),
            DistributionEntry("Bamboo", 150, 0.10f)
        )
    )
}