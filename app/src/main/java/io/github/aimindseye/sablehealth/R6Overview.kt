package io.github.aimindseye.sablehealth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveHealthPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStatus
import java.time.LocalDate

private const val TWO_COLUMN_WIDTH = 320
private const val MAX_COMPACT_FONT = 1.3f
private const val COMPACT_SCREEN_HEIGHT = 500
private const val METRICS_PER_PAGE = 4

@Composable
internal fun MetricOverview(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
    choose: (LiveMetric) -> Unit,
) {
    val days = state.r6.period(state.section)
    val calendar = calendar(snapshot, days)
    val configuration = LocalConfiguration.current
    var pageIndex by rememberSaveable(state.section) { mutableIntStateOf(0) }
    BoxWithConstraints {
        val twoColumns = maxWidth >= TWO_COLUMN_WIDTH.dp && configuration.fontScale <= MAX_COMPACT_FONT
        val paged = twoColumns && configuration.screenHeightDp <= COMPACT_SCREEN_HEIGHT
        val pages = if (paged) metrics.chunked(METRICS_PER_PAGE) else listOf(metrics)
        val current = pageIndex.coerceIn(pages.indices)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (pages.size > 1) {
                MetricPageNavigation(current, pages.size) { pageIndex = it }
            }
            pages[current].chunked(if (twoColumns) 2 else 1).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    pair.forEach { metric ->
                        Box(Modifier.weight(1f)) { MetricOverviewCard(state, snapshot, metric, calendar, choose) }
                    }
                    if (twoColumns && pair.size == 1) Box(Modifier.weight(1f))
                }
            }
            if (state.section == Section.MOVE && current == pages.lastIndex) M2HeartRateCalculator()
        }
    }
}

@Composable
private fun MetricPageNavigation(
    current: Int,
    count: Int,
    choose: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        R6IconButton("Previous metrics", R.drawable.ic_r6_back, { choose((current - 1).coerceAtLeast(0)) })
        Text("Metrics ${current + 1}/$count", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        R6IconButton("Next metrics", R.drawable.ic_r6_next, { choose((current + 1).coerceAtMost(count - 1)) })
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
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("r7-metric-${metric.name}")
                .sizeIn(minHeight = 48.dp)
                .onFocusChanged { focused = it.isFocused }
                .then(r7FocusOutline(focused, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large))
                .clickable { choose(metric) },
        color = r7MetricSurface(metric),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) {
            OverviewReading(snapshot, metric, history)
            if (state.modeFor(state.section) == ViewMode.GRAPHS) OverviewPlot(state, metric, calendar, points)
        }
    }
}

@Composable
private fun OverviewReading(
    snapshot: LiveSnapshot,
    metric: LiveMetric,
    history: List<LivePoint?>?,
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
            style = MaterialTheme.typography.titleMedium,
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
        Text(mean, style = MaterialTheme.typography.titleMedium)
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
