package io.github.aimindseye.sablehealth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import java.time.Duration
import java.time.LocalDate
import kotlin.math.abs

private const val BAR_WIDTH_FRACTION = 0.7f
private const val SECONDS_PER_HOUR = 3600L
private const val HOURLY_CHART_HEIGHT_DP = 24
private const val DEEP_STAGE_COLOR = 0xFF5554A8
private const val REM_STAGE_COLOR = 0xFFA18AE0
private const val AWAKE_STAGE_COLOR = 0xFFD9A46B

@Composable
internal fun HistoryChart(
    days: List<LocalDate>,
    points: List<LivePoint?>,
    metric: LiveMetric,
) {
    BarChart(
        values = points.map { it?.value },
        description = "${metric.label}, ${days.size} days; gaps are missing data",
        heightDp = 72,
    )
}

@Composable
internal fun TodayStepsChart(snapshot: LiveSnapshot) {
    val window = snapshot.window
    val elapsed = Duration.between(window.todayStart, window.now).seconds
    val hours = ((elapsed + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR).toInt().coerceAtLeast(1)
    if (snapshot.stepHours.isEmpty()) {
        Text("Hourly breakdown unavailable", style = MaterialTheme.typography.labelSmall)
    } else {
        val byTime = snapshot.stepHours.associateBy { it.start }
        val values =
            (0 until hours).map { hour ->
                byTime[window.todayStart.plusSeconds(hour * SECONDS_PER_HOUR)]?.count?.toDouble()
            }
        BarChart(values, "Today's observed hourly steps; gaps unknown", HOURLY_CHART_HEIGHT_DP)
    }
}

@Composable
private fun BarChart(
    values: List<Double?>,
    description: String,
    heightDp: Int,
) {
    val color = MaterialTheme.colorScheme.primary
    val observed = values.filterNotNull()
    val min = minOf(0.0, observed.minOrNull() ?: 0.0)
    val max = maxOf(1.0, observed.maxOrNull() ?: 1.0)
    Canvas(Modifier.fillMaxWidth().height(heightDp.dp).semantics { contentDescription = description }) {
        val cell = size.width / values.size.coerceAtLeast(1)
        val baseline = (max / (max - min) * size.height).toFloat()
        values.forEachIndexed { index, point ->
            if (point != null) {
                val y = ((max - point) / (max - min) * size.height).toFloat()
                val height = abs(baseline - y).coerceAtLeast(1f)
                drawRect(color, Offset(index * cell, minOf(y, baseline)), Size(cell * BAR_WIDTH_FRACTION, height))
            }
        }
    }
}

@Composable
internal fun SleepTimeline(sleep: LiveSleep) {
    if (sleep.stages.isEmpty()) {
        Text("No stages exported", style = MaterialTheme.typography.bodySmall)
        return
    }
    val accent = MaterialTheme.colorScheme.primary
    val total = Duration.between(sleep.start, sleep.end).toMillis().toDouble()
    Canvas(
        Modifier.fillMaxWidth().height(24.dp).semantics { contentDescription = "Recorded sleep stages; gaps unknown" },
    ) {
        sleep.stages.forEach { stage ->
            val start = (Duration.between(sleep.start, stage.start).toMillis() / total).coerceIn(0.0, 1.0)
            val end = (Duration.between(sleep.start, stage.end).toMillis() / total).coerceIn(0.0, 1.0)
            if (end > start) {
                drawRect(
                    color = stageColor(stage.type, accent),
                    topLeft = Offset((start * size.width).toFloat(), 0f),
                    size = Size(((end - start) * size.width).toFloat(), size.height),
                )
            }
        }
    }
    Text(
        sleep.stages
            .map { it.type }
            .distinct()
            .joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
    )
}

private fun stageColor(
    type: String,
    accent: Color,
): Color =
    when (type) {
        "Deep" -> Color(DEEP_STAGE_COLOR)
        "REM" -> Color(REM_STAGE_COLOR)
        "Light" -> accent
        "Awake", "Awake in bed", "Out of bed" -> Color(AWAKE_STAGE_COLOR)
        else -> Color.Gray
    }
