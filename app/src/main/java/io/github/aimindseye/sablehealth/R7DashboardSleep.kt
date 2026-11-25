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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.SleepPlotPolicy
import java.time.Duration

private val asleepNames = setOf("Asleep (unspecified)", "Light", "Deep", "REM")

@Composable
internal fun R7SleepWidgetReading(
    snapshot: LiveSnapshot,
    metric: LiveMetric,
) {
    val sleep = snapshot.sleeps.lastOrNull { snapshot.window.day(it.end) == snapshot.window.today }
    if (sleep == null) {
        LiveMetricValue(snapshot, metric, prominent = true)
        return
    }
    val seconds = SleepPlotPolicy.secondsByStage(sleep)
    val asleep = seconds.filterKeys { it in asleepNames }.values.sum()
    val duration = Duration.between(sleep.start, sleep.end).seconds
    val recorded = seconds.keys.any { it in asleepNames }
    Text(
        r7HoursMinutes(if (recorded) asleep else duration),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        if (recorded) "Latest session · recorded asleep" else "Latest session · session time",
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
internal fun R7DashboardSleepStrip(sleep: LiveSleep) {
    val stages = SleepPlotPolicy.intervals(sleep)
    val palette = stagePalette()
    if (stages.isEmpty()) {
        Text("No stages exported", style = MaterialTheme.typography.labelSmall)
    } else {
        Canvas(
            Modifier.fillMaxWidth().height(18.dp).semantics {
                contentDescription = "Recorded sleep stages; gaps unknown"
            },
        ) {
            stages.forEach { stage ->
                val left = SleepPlotPolicy.fraction(sleep, stage.start) * size.width
                val right = SleepPlotPolicy.fraction(sleep, stage.end) * size.width
                drawRect(palette.getValue(plotStage(stage.type)), Offset(left, 0f), Size(right - left, size.height))
            }
        }
    }
}
