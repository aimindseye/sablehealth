package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveStatus
import java.time.ZoneId

private const val SUMMARY_MIN_WIDTH = 350
private const val SUMMARY_MAX_FONT = 1.3f

@Composable
internal fun M2HistoryEvidence(
    metric: LiveMetric,
    status: LiveStatus?,
    points: List<LivePoint?>,
    zone: ZoneId,
    summary: (@Composable () -> Unit)? = null,
    externalExpanded: Boolean? = null,
) {
    var expanded by remember(metric) { mutableStateOf(false) }
    if (externalExpanded == null) {
        HistoryEvidenceHeader(summary) { expanded = !expanded }
    } else {
        summary?.invoke()
    }
    if (externalExpanded ?: expanded) {
        HistoryEvidenceBody(metric, status, points, zone)
    }
}

@Composable
private fun HistoryEvidenceHeader(
    summary: (@Composable () -> Unit)?,
    toggle: () -> Unit,
) {
    BoxWithConstraints {
        val sideBySide =
            summary != null &&
                maxWidth >= SUMMARY_MIN_WIDTH.dp &&
                LocalConfiguration.current.fontScale <= SUMMARY_MAX_FONT
        if (sideBySide) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { summary() }
                Box(Modifier.weight(1f)) {
                    R7aRow("Method and sources", action = toggle)
                }
            }
        } else {
            Column {
                summary?.invoke()
                R7aRow("Method and sources", action = toggle)
            }
        }
    }
}

@Composable
private fun HistoryEvidenceBody(
    metric: LiveMetric,
    status: LiveStatus?,
    points: List<LivePoint?>,
    zone: ZoneId,
) {
    R7aNote(historyMethod(metric))
    R7aNote("Coverage: ${status?.label ?: "Not loaded"} · method v1")
    val observed = points.filterNotNull()
    if (observed.isEmpty()) {
        R7aNote("No observations in this period.")
    } else {
        observed.asReversed().forEach { point ->
            Text(displayValue(metric, point), style = MaterialTheme.typography.labelMedium)
            R7aNote(observation(point, zone))
        }
    }
}

internal fun historyMethod(metric: LiveMetric): String =
    when (metric) {
        LiveMetric.HRV -> "Latest reported RMSSD (ms) per day. Not inferred from BPM or mixed with SDNN."
        LiveMetric.RESTING_HR -> "Latest reported resting HR per day, not the lowest heart-rate sample."
        LiveMetric.HEART_RATE -> "Latest observed heart-rate sample per day, not a daily average."
        LiveMetric.SLEEP_DURATION -> "Completed session spans by wake day after overlap selection; not time asleep."
        LiveMetric.EXERCISE_SESSIONS -> "Count of selected nonoverlapping completed exercise sessions by end day."
        LiveMetric.STEPS,
        LiveMetric.DISTANCE,
        LiveMetric.ACTIVE_CALORIES,
        LiveMetric.CALORIES,
        LiveMetric.ACTIVE_MINUTES,
        -> "Health Connect aggregate; current device day and provider-local historical days."
        else -> "Latest reported observation per day. Gaps are not interpolated."
    }
