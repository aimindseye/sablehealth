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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DemoCharts

/** Chart widgets are M0 demonstration-only; no chart reads live records. */
@Composable
internal fun StepsChart() {
    ChartSurface("Steps · last 7 days") {
        val primary = MaterialTheme.colorScheme.primary
        val grid = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            Modifier.fillMaxWidth().height(52.dp).semantics {
                contentDescription = "Synthetic steps, 7 days: " + DemoCharts.steps.joinToString()
            },
        ) {
            val samples = DemoCharts.steps
            val maximum = samples.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val slot = size.width / samples.size
            val base = size.height
            val width = slot * 0.57f
            drawLine(grid, Offset(0f, base * 0.5f), Offset(size.width, base * 0.5f), 1.dp.toPx())
            samples.forEachIndexed { index, sample ->
                val barHeight = base * sample / maximum
                val x = slot * index + (slot - width) / 2f
                drawRoundRect(
                    color = if (index == samples.lastIndex) primary else primary.copy(alpha = 0.52f),
                    topLeft = Offset(x, base - barHeight),
                    size = Size(width, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
        DayLabels()
    }
}

@Composable
internal fun RestingHeartRateChart() {
    ChartSurface("Resting heart rate · 7 days") {
        val blue = MaterialTheme.colorScheme.primary
        val grid = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            Modifier.fillMaxWidth().height(52.dp).semantics {
                contentDescription = "Synthetic resting heart rate, 7 days: " + DemoCharts.restingHr.joinToString()
            },
        ) {
            val samples = DemoCharts.restingHr
            val minimum = (samples.minOrNull() ?: 0f) - 2f
            val maximum = (samples.maxOrNull() ?: 1f) + 2f
            val range = (maximum - minimum).coerceAtLeast(1f)
            val xPadding = 5.dp.toPx()
            val yPadding = 5.dp.toPx()
            val spanX = (size.width - xPadding * 2).coerceAtLeast(0f)
            val spanY = (size.height - yPadding * 2).coerceAtLeast(0f)
            drawLine(grid, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), 1.dp.toPx())
            val points = samples.mapIndexed { index, sample ->
                Offset(
                    xPadding + (spanX * index / (samples.size - 1).coerceAtLeast(1)),
                    yPadding + spanY * (1f - (sample - minimum) / range),
                )
            }
            val line = Path()
            points.forEachIndexed { index, point ->
                if (index == 0) line.moveTo(point.x, point.y) else line.lineTo(point.x, point.y)
            }
            drawPath(line, blue, style = Stroke(width = 2.dp.toPx()))
            points.forEachIndexed { index, point ->
                drawCircle(blue, radius = if (index == points.lastIndex) 4.dp.toPx() else 2.dp.toPx(), center = point)
            }
        }
        DayLabels()
    }
}

@Composable
internal fun SleepStageChart() {
    ChartSurface("Last night · sleep stages") {
        val colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.60f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
        Row(
            Modifier.fillMaxWidth().height(17.dp)
                .clip(RoundedCornerShape(6.dp))
                .semantics {
                    contentDescription = "Synthetic sleep stages: deep 1 hour 24 minutes; " +
                        "REM 1 hour 48 minutes; light 4 hours 30 minutes"
                },
        ) {
            DemoCharts.sleepStageMinutes.forEachIndexed { index, minutes ->
                Box(Modifier.weight(minutes.toFloat()).fillMaxHeight().background(colors[index]))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Deep 1h24", "REM 1h48", "Light 4h30").forEachIndexed { index, label ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.padding(top = 4.dp).height(8.dp).fillMaxWidth(0.10f).background(colors[index]))
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
private fun ChartSurface(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text("DEMO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            content()
        }
    }
}

@Composable
private fun DayLabels() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
            Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
