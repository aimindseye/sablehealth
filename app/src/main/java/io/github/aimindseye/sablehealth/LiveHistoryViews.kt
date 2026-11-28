package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.ChartPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import java.time.LocalDate

private const val METHOD_HEADER_MIN_WIDTH = 360
private const val METHOD_HEADER_MAX_FONT = 1.3f
private const val METHOD_HEADER_BUTTON_WIDTH = 96

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
        HistoryDetail(state, snapshot, metrics, metric)
    }
}

@Composable
private fun HistoryDetail(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
    metric: LiveMetric,
) {
    var methodsExpanded by remember(metric) { mutableStateOf(false) }
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val inlineMethods =
        windowWidth >= METHOD_HEADER_MIN_WIDTH.dp &&
            LocalConfiguration.current.fontScale <= METHOD_HEADER_MAX_FONT
    R6ModeHeading(
        state,
        onOverview = { state.r6.chooseMetric(state.section, null) },
        titleContent = {
            HistoricalMetricChoice(metrics, metric) { state.r6.chooseMetric(state.section, it) }
        },
        period = {
            if (inlineMethods) {
                FocusButton(
                    "Method and sources",
                    { methodsExpanded = !methodsExpanded },
                    selected = methodsExpanded,
                    modifier = Modifier.width(METHOD_HEADER_BUTTON_WIDTH.dp).testTag("r7a-row-Method and sources"),
                )
            }
        },
    )
    val expanded = if (inlineMethods) methodsExpanded else null
    HistoryPanel(state, snapshot, metrics, metric, expanded)
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
    methodsExpanded: Boolean? = null,
) {
    val days = state.r6.period(state.section)
    val calendar = calendar(snapshot, days)
    val series = snapshot.metrics[metric]
    val points = calendar.map { series?.on(it) }
    val graph = state.modeFor(state.section) == ViewMode.GRAPHS
    val kind = state.r6.chart(metric)
    val options: @Composable () -> Unit = { HistoryFilters(state, metric, days, graph) }
    val hasPoints = points.any { it != null }
    val metricInHeading = state.section != Section.SLEEP
    val combined = graph && metricInHeading && hasPoints
    R6Card(verticalPadding = 2.dp) {
        if (!combined) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!metricInHeading) {
                    Box(Modifier.weight(1f)) {
                        HistoricalMetricChoice(metrics, metric) {}
                    }
                }
                options()
            }
        }
        if (!hasPoints) {
            Text(
                if (graph) "No observations to graph" else "No observations in this period",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(series?.status?.label ?: "No records received", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { state.page = Page.SOURCES }) { Text("View data sources") }
        } else if (graph) {
            R6HistoryChart(calendar, points, metric, kind, options = if (combined) options else null)
        }
        if (methodsExpanded == null) {
            M2HistoryEvidence(metric, series?.status, points, snapshot.window.zone) {
                HistorySummary(points, days, metric, compact = false)
            }
        } else {
            HistorySummary(points, days, metric, compact = true)
            if (methodsExpanded) HistoryEvidenceBody(metric, series?.status, points, snapshot.window.zone)
        }
        if (!graph) {
            calendar.zip(points).asReversed().forEach { (day, point) ->
                CompactSummary(day.toString(), point?.let { displayValue(metric, it) } ?: "No record")
            }
        }
    }
}

@Composable
private fun HistoryFilters(
    state: SableAppState,
    metric: LiveMetric,
    days: Int,
    graph: Boolean,
) {
    PeriodChoice(days) { state.r6.choosePeriod(state.section, it) }
    if (graph) {
        val kind = state.r6.chart(metric)
        R6Choice("Graph type", kind.label, ChartPolicy.options(metric).map { it.label }, { label ->
            state.r6.chooseChart(metric, ChartPolicy.options(metric).first { it.label == label })
        })
    }
}

@Composable
private fun HistoricalMetricChoice(
    metrics: List<LiveMetric>,
    metric: LiveMetric,
    choose: (LiveMetric) -> Unit,
) {
    R6Choice("Historical metric", metric.label, metrics.map { it.label }, { label ->
        choose(metrics.first { it.label == label })
    })
}

@Composable
private fun HistorySummary(
    points: List<LivePoint?>,
    days: Int,
    metric: LiveMetric,
    compact: Boolean,
) {
    val coverage = "${points.count { it != null }}/$days days observed"
    if (compact) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(coverage, style = MaterialTheme.typography.labelSmall)
            Box(Modifier.weight(1f)) {
                ObservedAverage(points.dropLast(1), metric)
            }
        }
    } else {
        Column {
            Text(coverage, style = MaterialTheme.typography.labelSmall)
            ObservedAverage(points.dropLast(1), metric)
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
