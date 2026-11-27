package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.LiveHealthPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.WidgetStyle

private const val WIDE_THRESHOLD = 310
private const val LARGE_FONT = 1.3f
private val activityMetrics =
    listOf(
        LiveMetric.STEPS,
        LiveMetric.DISTANCE,
        LiveMetric.CALORIES,
        LiveMetric.ACTIVE_CALORIES,
        LiveMetric.ACTIVE_MINUTES,
        LiveMetric.EXERCISE_SESSIONS,
    )
private val vitalMetrics =
    listOf(
        LiveMetric.HEART_RATE,
        LiveMetric.RESTING_HR,
        LiveMetric.HRV,
        LiveMetric.RESPIRATORY_RATE,
        LiveMetric.SPO2,
        LiveMetric.BODY_TEMPERATURE,
        LiveMetric.SKIN_TEMPERATURE,
    )

@Composable
internal fun LiveHealthPage(
    state: SableAppState,
    showRefresh: Boolean = true,
) {
    if (showRefresh) LiveRefreshRow(state)
    val snapshot = state.liveHealth.snapshot
    if (snapshot == null) {
        val message =
            when {
                state.connect.label == "Checking" -> "Checking Health Connect access"
                state.connect.grantedCount == null -> state.connect.detail
                state.connect.granted.isEmpty() -> "Grant read permissions in Sources"
                state.liveHealth.failed -> "Refresh failed. Retry when ready."
                else -> "Loading local measurements"
            }
        Notice("Real data", message)
        if (state.section == Section.MOVE) M2HeartRateCalculator()
        return
    }
    when (state.section) {
        Section.DASHBOARD -> LiveDashboard(state, snapshot)
        Section.SLEEP -> LiveSleepPage(state, snapshot)
        Section.MOVE -> LiveHistoryPage(state, snapshot, activityMetrics)
        Section.VITALS -> LiveHistoryPage(state, snapshot, vitalMetrics)
        Section.TRENDS -> LiveHistoryPage(state, snapshot, activityMetrics + LiveMetric.SLEEP_DURATION + vitalMetrics)
    }
}

@Composable
internal fun LiveRefreshRow(state: SableAppState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(Modifier.weight(1f)) {
            val window = state.liveHealth.snapshot?.window
            Text(
                when {
                    state.liveHealth.loading -> "Refreshing local data"
                    state.liveHealth.failed -> "Refresh failed · retry"
                    window != null -> "Checked ${localTime(window.now, window.zone)}"
                    else -> "Not loaded"
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Health Connect · not a tracker sync", style = MaterialTheme.typography.labelSmall)
        }
        R6IconButton("Refresh", R.drawable.ic_r6_refresh, state::refreshLive)
    }
}

/** Four selected daily slots. No historical charts on Dashboard. */
@Composable
private fun LiveDashboard(
    state: SableAppState,
    snapshot: LiveSnapshot,
) {
    Text("Dashboard", style = MaterialTheme.typography.titleLarge)
    val widgets = state.dashboard.widgets
    BoxWithConstraints {
        if (maxWidth >= WIDE_THRESHOLD.dp && LocalConfiguration.current.fontScale <= LARGE_FONT) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                widgets.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        pair.forEach { widget ->
                            Box(Modifier.weight(1f)) { LiveWidget(state, snapshot, widget, true) }
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                widgets.forEach { LiveWidget(state, snapshot, it, true) }
            }
        }
    }
}

@Composable
private fun LiveWidget(
    state: SableAppState,
    snapshot: LiveSnapshot,
    widget: DashboardWidget,
    hero: Boolean,
) {
    R6Card(
        modifier = Modifier.testTag("r7-widget-${widget.name}"),
        color = r7MetricSurface(widgetMetric(widget)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(widgetTitle(widget), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            if (hero) LiveWidgetIcon(widget)
        }
        WidgetReading(snapshot, widget)
        WidgetGraphic(state, snapshot, widget, state.dashboard.styleFor(widget))
    }
}

@Composable
private fun WidgetReading(
    snapshot: LiveSnapshot,
    widget: DashboardWidget,
) {
    val metric = widgetMetric(widget)
    when {
        widget == DashboardWidget.DATA_FRESHNESS -> Text(localTime(snapshot.window.now, snapshot.window.zone))
        metric == null -> Text("Not available in M1-C", style = MaterialTheme.typography.bodySmall)
        widget == DashboardWidget.SLEEP_STAGES -> R7SleepWidgetReading(snapshot, metric)
        widget == DashboardWidget.SLEEP_SCHEDULE -> {
            val sleep = snapshot.sleeps.lastOrNull { snapshot.window.day(it.end) == snapshot.window.today }
            if (sleep == null) {
                LiveMetricValue(snapshot, metric, prominent = true)
            } else {
                Text(sleepRange(sleep, snapshot.window.zone))
            }
        }
        else -> LiveMetricValue(snapshot, metric, prominent = true)
    }
}

@Composable
private fun WidgetGraphic(
    state: SableAppState,
    snapshot: LiveSnapshot,
    widget: DashboardWidget,
    style: WidgetStyle,
) {
    if (style != WidgetStyle.GRAPH) return
    when (widget) {
        DashboardWidget.STEPS -> {
            val steps = snapshot.metrics[LiveMetric.STEPS]?.on(snapshot.window.today)?.value
            if (steps != null && state.r6.stepGoal > 0) {
                R6StepRing(steps, state.r6.stepGoal)
            } else {
                TodayStepsChart(snapshot)
            }
        }
        DashboardWidget.SLEEP_STAGES -> {
            val sleep = snapshot.sleeps.lastOrNull { snapshot.window.day(it.end) == snapshot.window.today }
            if (sleep != null) {
                R7DashboardSleepStrip(sleep)
            }
        }
        else -> Unit
    }
}

@Composable
private fun LiveWidgetIcon(widget: DashboardWidget) {
    when (widget) {
        DashboardWidget.STEPS -> Icon(painterResource(R.drawable.ic_sable_steps), null, Modifier.size(20.dp))
        DashboardWidget.HEART_RATE, DashboardWidget.RESTING_HR ->
            Icon(painterResource(R.drawable.ic_sable_heart), null, Modifier.size(22.dp), tint = Color.Unspecified)
        else -> Unit
    }
}

@Composable
internal fun LiveMetricValue(
    snapshot: LiveSnapshot,
    metric: LiveMetric,
    prominent: Boolean = false,
) {
    val series = snapshot.metrics[metric]
    val point = LiveHealthPolicy.dashboardPoint(snapshot, metric)
    val style = if (prominent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
    Text(point?.let { displayValue(metric, it) } ?: "—", style = style)
    val detail =
        when {
            point != null -> point.observedAt?.let { localTime(it, snapshot.window.zone) } ?: point.day.toString()
            series?.status == LiveStatus.AVAILABLE -> "No record today"
            else -> series?.status?.label ?: "Not loaded"
        }
    Text(detail, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (point != null && series?.status == LiveStatus.PARTIAL) {
        Text("Partial history", style = MaterialTheme.typography.labelSmall)
    }
}
