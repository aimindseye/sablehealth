package io.github.aimindseye.sablehealth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DemoCharts

/** R5 charts use synthetic fixtures. Today is daily-only; history charts live in other tabs. */
private const val BAR_WIDTH_FRACTION = 0.57f
private const val HALF_HEIGHT_FRACTION = 0.5f
private const val REM_STAGE_ALPHA = 0.60f
private const val LIGHT_STAGE_ALPHA = 0.45f
private const val SWATCH_WIDTH_FRACTION = 0.10f
private const val HEART_PADDING_PX_RATIO = 2f
private const val HR_PADDING_BPM = 2f
private const val MIN_CANVAS_RANGE = 1f
private const val STEPS_COMPACT_HEIGHT_DP = 44
private const val STEPS_REGULAR_HEIGHT_DP = 64
private const val MARKER_RADIUS_DP = 3
private const val CHART_LINE_WIDTH_DP = 2
private const val BAR_CORNER_DP = 3
private const val SLEEP_BAR_HEIGHT_DP = 17
private const val SLEEP_BAR_CORNER_DP = 6
private const val STAGE_SWATCH_HEIGHT_DP = 8
private const val CHART_CARD_HORIZONTAL_DP = 10
private const val CHART_CARD_VERTICAL_DP = 6
private const val GOAL_BAR_HEIGHT_DP = 15
private const val GOAL_CORNER_DP = 6
private const val STAGE_ROW_SPACING_DP = 5
private const val LEGEND_SPACING_DP = 4
private const val SWATCH_TOP_PADDING_DP = 4
private const val STAGE_CELL_SPACING_DP = 3

@Composable
internal fun TodayStepGoalChart() {
    ChartSurface("Today · step goal") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("7,420 of 9,000 steps", style = MaterialTheme.typography.bodyMedium)
            Text("82% · Demo", color = MaterialTheme.colorScheme.primary)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(GOAL_BAR_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(GOAL_CORNER_DP.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(DemoCharts.todayGoalFraction())
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            "Progress toward your chosen goal, not a medical assessment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun StepsChart(compact: Boolean = false) {
    ChartSurface("Steps · last 7 days") {
        val scheme = MaterialTheme.colorScheme
        val dark = scheme.background == SablePalette.darkBase
        val barColors = DemoCharts.stepBands.map { demoBandColor(it, dark) }
        StepBars(barColors, if (compact) STEPS_COMPACT_HEIGHT_DP else STEPS_REGULAR_HEIGHT_DP)
        DayLabels()
        ChartLegend()
    }
}

@Composable
private fun StepBars(
    barColors: List<Color>,
    heightDp: Int,
) {
    Canvas(
        Modifier.fillMaxWidth().height(heightDp.dp).semantics {
            contentDescription = "Synthetic steps, seven days: " +
                DemoCharts.steps.zip(DemoCharts.stepBands).joinToString { "${it.first}, ${it.second}" }
        },
    ) {
        val steps = DemoCharts.steps
        val maximum = steps.maxOrNull()?.coerceAtLeast(MIN_CANVAS_RANGE) ?: MIN_CANVAS_RANGE
        val slotWidth = size.width / steps.size
        val barWidth = slotWidth * BAR_WIDTH_FRACTION
        drawLine(
            color = Color.Gray.copy(alpha = LIGHT_STAGE_ALPHA),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
        steps.forEachIndexed { index, count ->
            val height = size.height * count / maximum
            val x = slotWidth * index + (slotWidth - barWidth) / HEART_PADDING_PX_RATIO
            drawRoundRect(
                color = barColors[index],
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(BAR_CORNER_DP.dp.toPx()),
            )
        }
    }
}

@Composable
internal fun RestingHeartRateChart(compact: Boolean = false) {
    ChartSurface("Resting heart rate · 7 days") {
        HeartRateLine(if (compact) STEPS_COMPACT_HEIGHT_DP else STEPS_REGULAR_HEIGHT_DP)
        DayLabels()
        Text(
            "Sable Blue: informational · no personal HR baseline in M0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeartRateLine(heightDp: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        Modifier.fillMaxWidth().height(heightDp.dp).semantics {
            contentDescription = "Synthetic resting heart rate, seven days: " +
                DemoCharts.restingHr.joinToString()
        },
    ) {
        val samples = DemoCharts.restingHr
        val low = (samples.minOrNull() ?: 0f) - HR_PADDING_BPM
        val high = (samples.maxOrNull() ?: 0f) + HR_PADDING_BPM
        val range = (high - low).coerceAtLeast(MIN_CANVAS_RANGE)
        val inset = MARKER_RADIUS_DP.dp.toPx()
        val spanX = (size.width - inset * HEART_PADDING_PX_RATIO).coerceAtLeast(0f)
        val spanY = (size.height - inset * HEART_PADDING_PX_RATIO).coerceAtLeast(0f)
        drawLine(
            grid,
            Offset(0f, size.height * HALF_HEIGHT_FRACTION),
            Offset(size.width, size.height * HALF_HEIGHT_FRACTION),
        )
        val points =
            samples.mapIndexed { index, sample ->
                Offset(inset + spanX * index / samples.lastIndex, inset + spanY * (1f - (sample - low) / range))
            }
        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, primary, style = Stroke(width = CHART_LINE_WIDTH_DP.dp.toPx()))
        points.forEach { drawCircle(primary, radius = MARKER_RADIUS_DP.dp.toPx(), center = it) }
    }
}

@Composable
internal fun SleepStageChart() {
    ChartSurface("Last night · sleep stages") {
        val colors =
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = REM_STAGE_ALPHA),
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LIGHT_STAGE_ALPHA),
            )
        Row(
            Modifier
                .fillMaxWidth()
                .height(SLEEP_BAR_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(SLEEP_BAR_CORNER_DP.dp))
                .semantics {
                    contentDescription = "Synthetic last-night sleep: deep 1h24, REM 1h48, light 4h30"
                },
        ) {
            DemoCharts.sleepStageMinutes.forEachIndexed { index, minutes ->
                Box(Modifier.weight(minutes.toFloat()).fillMaxHeight().background(colors[index]))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(STAGE_ROW_SPACING_DP.dp)) {
            listOf("Deep 1h24", "REM 1h48", "Light 4h30").forEachIndexed { index, label ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(STAGE_CELL_SPACING_DP.dp)) {
                    Box(
                        Modifier
                            .padding(top = SWATCH_TOP_PADDING_DP.dp)
                            .height(STAGE_SWATCH_HEIGHT_DP.dp)
                            .fillMaxWidth(SWATCH_WIDTH_FRACTION)
                            .background(colors[index]),
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegend() {
    val dark = MaterialTheme.colorScheme.background == SablePalette.darkBase
    val samples =
        listOf(
            "Info" to DemoBandColor.INFO,
            "Goal" to DemoBandColor.GOOD,
            "Below" to DemoBandColor.CAUTION,
            "Demo outlier" to DemoBandColor.OUTLIER,
        )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LEGEND_SPACING_DP.dp)) {
        samples.forEach { (label, band) ->
            Text(
                "● $label",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = demoLegendColor(band, dark),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChartSurface(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = CHART_CARD_HORIZONTAL_DP.dp, vertical = CHART_CARD_VERTICAL_DP.dp),
            verticalArrangement = Arrangement.spacedBy(STAGE_CELL_SPACING_DP.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "DEMO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            content()
        }
    }
}

@Composable
private fun DayLabels() {
    Row(Modifier.fillMaxWidth()) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
            Text(
                label,
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
