package io.github.aimindseye.sablehealth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveHealthPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStatus
import java.time.LocalDate

private const val OVERVIEW_CHART_WEIGHT = 1.25f

@Composable
internal fun MetricOverview(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
    choose: (LiveMetric) -> Unit,
) {
    val days = state.r6.period(state.section)
    val calendar = calendar(snapshot, days)
    if (state.section != Section.MOVE) {
        metrics.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                pair.forEach { metric ->
                    Box(Modifier.weight(1f)) { MetricOverviewCard(state, snapshot, metric, calendar, choose) }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    } else {
        metrics.forEach { metric -> MetricOverviewCard(state, snapshot, metric, calendar, choose) }
    }
}

@Composable
private fun MetricOverviewCard(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metric: LiveMetric,
    calendar: List<LocalDate>,
    choose: (LiveMetric) -> Unit,
) {
    val points = calendar.map { snapshot.metrics[metric]?.on(it) }
    val history = if (state.section == Section.TRENDS) points else null
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("r7-metric-${metric.name}")
                .sizeIn(minHeight = 48.dp)
                .clickable { choose(metric) },
        color = r7MetricSurface(metric),
        shape = MaterialTheme.shapes.large,
    ) {
        if (state.section != Section.MOVE) {
            Column(Modifier.padding(5.dp)) {
                OverviewReading(snapshot, metric, history)
                OverviewPlot(state, metric, calendar, points)
            }
        } else {
            Row(Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { OverviewReading(snapshot, metric, history, compact = true) }
                Column(Modifier.weight(OVERVIEW_CHART_WEIGHT)) { OverviewPlot(state, metric, calendar, points) }
            }
        }
    }
}

@Composable
private fun OverviewReading(
    snapshot: LiveSnapshot,
    metric: LiveMetric,
    history: List<LivePoint?>?,
    compact: Boolean = false,
) {
    Text(
        metric.label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (history == null) {
        val point = LiveHealthPolicy.dashboardPoint(snapshot, metric)
        val series = snapshot.metrics[metric]
        Text(
            point?.let { displayValue(metric, it) } ?: "—",
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
        )
        val detail =
            when {
                point == null -> series?.status?.label ?: "Not loaded"
                series?.status == LiveStatus.PARTIAL -> "${point.day} · partial"
                else -> point.observedAt?.let { localTime(it, snapshot.window.zone) } ?: point.day.toString()
            }
        Text(detail, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    } else {
        val values = history.dropLast(1).mapNotNull { it?.value }
        val mean = if (values.isEmpty()) "—" else "${formatNumber(values.average())} ${metric.unit}".trim()
        Text(mean, style = MaterialTheme.typography.titleLarge)
        Text("Mean · excl. today", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun OverviewPlot(
    state: SableAppState,
    metric: LiveMetric,
    calendar: List<LocalDate>,
    points: List<LivePoint?>,
) {
    if (points.any { it != null }) {
        R6Plot(PlotData(calendar, points.map { it?.value }, metric, state.r6.chart(metric)), compact = true)
    } else {
        Text("Open for details", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun DailyMetricCards(
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
) {
    metrics.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pair.forEach { metric ->
                Box(Modifier.weight(1f)) {
                    R6Card {
                        Text(metric.label, style = MaterialTheme.typography.labelMedium)
                        LiveMetricValue(snapshot, metric)
                    }
                }
            }
            if (pair.size == 1) Box(Modifier.weight(1f))
        }
    }
}
