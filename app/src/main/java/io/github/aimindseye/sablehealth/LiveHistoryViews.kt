package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.aimindseye.sablehealth.core.ChartPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import java.time.LocalDate

@Composable
internal fun LiveHistoryPage(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
) {
    val selected = state.r6.detailMetric(state.section)
    val metric = metrics.firstOrNull { it == selected } ?: metrics.first()
    if (selected == null) {
        MetricOverview(state, snapshot, metrics) { state.r6.chooseMetric(state.section, it) }
    } else {
        R6ModeHeading(
            state,
            onOverview = { state.r6.chooseMetric(state.section, null) },
        )
        HistoryPanel(state, snapshot, metrics, metric) { state.r6.chooseMetric(state.section, it) }
    }
}

@Composable
internal fun PeriodChoice(
    days: Int,
    choose: (Int) -> Unit,
) {
    R6Choice("History period", "${days}D", listOf("7D", "30D"), { choose(it.dropLast(1).toInt()) })
}

internal fun calendar(
    snapshot: LiveSnapshot,
    days: Int,
): List<LocalDate> = (days - 1 downTo 0).map { snapshot.window.today.minusDays(it.toLong()) }

@Composable
internal fun HistoryPanel(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
    metric: LiveMetric,
    choose: (LiveMetric) -> Unit,
) {
    val days = state.r6.period(state.section)
    val calendar = calendar(snapshot, days)
    val series = snapshot.metrics[metric]
    val points = calendar.map { series?.on(it) }
    val graph = state.modeFor(state.section) == ViewMode.GRAPHS
    val kind = state.r6.chart(metric)
    R6Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            R6Choice("Historical metric", metric.label, metrics.map { it.label }, { label ->
                choose(metrics.first { it.label == label })
            }, Modifier.weight(1f))
            PeriodChoice(days) { state.r6.choosePeriod(state.section, it) }
            if (graph) {
                R6Choice("Graph type", kind.label, ChartPolicy.options(metric).map { it.label }, { label ->
                    state.r6.chooseChart(metric, ChartPolicy.options(metric).first { it.label == label })
                })
            }
        }
        if (points.none { it != null }) {
            Text(
                if (graph) "No observations to graph" else "No observations in this period",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(series?.status?.label ?: "No records received", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { state.page = Page.SOURCES }) { Text("View data sources") }
        } else if (graph) {
            R6HistoryChart(calendar, points, metric, kind)
        }
        Text("${points.count { it != null }}/$days days observed", style = MaterialTheme.typography.labelSmall)
        ObservedAverage(points.dropLast(1), metric)
        M2HistoryEvidence(metric, series?.status, points, snapshot.window.zone)
        if (!graph) {
            calendar.zip(points).asReversed().forEach { (day, point) ->
                CompactSummary(day.toString(), point?.let { displayValue(metric, it) } ?: "No record")
            }
        }
    }
}

@Composable
private fun ObservedAverage(
    completedDays: List<LivePoint?>,
    metric: LiveMetric,
) {
    val values = completedDays.mapNotNull { it?.value }
    if (values.isNotEmpty()) {
        val mean = formatNumber(values.average())
        Text("Observed mean (excl. today): $mean ${metric.unit}", style = MaterialTheme.typography.labelSmall)
    }
}
