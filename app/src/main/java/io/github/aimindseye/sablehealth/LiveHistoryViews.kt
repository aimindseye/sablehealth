package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import java.time.Duration
import java.time.ZoneId

private const val WEEK = 7
private const val MONTH = 30
private const val SECONDS_PER_MINUTE = 60L

@Composable
internal fun LiveSleepPage(
    state: SableAppState,
    snapshot: LiveSnapshot,
) {
    ViewModeHeading(Section.SLEEP, "Sessions by wake-up date · not time asleep", state)
    val series = snapshot.metrics[LiveMetric.SLEEP_DURATION]
    if (series?.on(snapshot.window.today) == null) {
        Notice(
            "Sleep",
            if (series?.points.isNullOrEmpty()) series?.status?.label ?: "Not loaded" else "No session today",
        )
        Text("If Health Connect has no sessions, check Google Health export. Refresh cannot force tracker sync.")
    }
    snapshot.sleeps.filter { snapshot.window.day(it.end) == snapshot.window.today }.forEach { session ->
        SleepSessionCard(session, snapshot.window.zone, state.modeFor(Section.SLEEP) == ViewMode.GRAPHS)
    }
    HistoryPanel(state, snapshot, listOf(LiveMetric.SLEEP_DURATION))
}

@Composable
private fun SleepSessionCard(
    session: LiveSleep,
    zone: ZoneId,
    graph: Boolean,
) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Text(sleepRange(session, zone))
            Text("Session: ${formatNumber(session.minutes)} min · ${session.writer}")
            if (graph) {
                SleepTimeline(session)
            } else if (session.stages.isEmpty()) {
                Text("No stages exported")
            }
            if (session.stages.isNotEmpty()) {
                val seconds =
                    session.stages.groupBy { it.type }.mapValues { (_, stages) ->
                        stages.sumOf { Duration.between(it.start, it.end).seconds }
                    }
                val asleep = seconds.filterKeys { it in asleepStageNames }.values.sum()
                Text("Recorded asleep: ${asleep / SECONDS_PER_MINUTE} min · gaps unknown")
                seconds.forEach { (name, duration) -> Text("$name · ${duration / SECONDS_PER_MINUTE} min") }
            }
        }
    }
}

private val asleepStageNames = setOf("Asleep (unspecified)", "Light", "Deep", "REM")

@Composable
internal fun LiveHistoryPage(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
) {
    ViewModeHeading(state.section, "Today's readings and observed history", state)
    metrics.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pair.forEach { metric ->
                Box(Modifier.weight(1f)) {
                    DashboardCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(6.dp)) {
                            Text(metric.label, style = MaterialTheme.typography.titleSmall)
                            LiveMetricValue(snapshot, metric)
                        }
                    }
                }
            }
            if (pair.size == 1) Box(Modifier.weight(1f))
        }
    }
    HistoryPanel(state, snapshot, metrics)
}

@Composable
private fun HistoryPanel(
    state: SableAppState,
    snapshot: LiveSnapshot,
    metrics: List<LiveMetric>,
) {
    var days by rememberSaveable { mutableStateOf(WEEK) }
    var name by rememberSaveable { mutableStateOf(metrics.first().name) }
    val metric = metrics.firstOrNull { it.name == name } ?: metrics.first()
    CompactChoiceRow(
        "History",
        listOf(WEEK.toString() to "7 days", MONTH.toString() to "30 days"),
        days.toString(),
        { days = it.toInt() },
    )
    CompactLinkRow(
        "Historical metric",
        metric.label,
        { name = metrics[(metrics.indexOf(metric) + 1) % metrics.size].name },
    )
    val calendar = (days - 1 downTo 0).map { snapshot.window.today.minusDays(it.toLong()) }
    val series = snapshot.metrics[metric]
    val points = calendar.map { series?.on(it) }
    Text("${points.count { it != null }}/$days days observed · ${series?.status?.label ?: "Not loaded"}")
    ObservedAverage(points.dropLast(1), metric)
    if (state.modeFor(state.section) == ViewMode.GRAPHS) HistoryChart(calendar, points, metric)
    calendar.zip(points).asReversed().forEach { (day, point) ->
        CompactSummary(day.toString(), point?.let { displayValue(metric, it) } ?: "No record")
        point?.let { Text(observation(it, snapshot.window.zone), style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun ObservedAverage(
    completedDays: List<LivePoint?>,
    metric: LiveMetric,
) {
    val values = completedDays.mapNotNull { it?.value }
    if (values.isNotEmpty()) {
        Text("Observed mean (excl. today): ${formatNumber(values.average())} ${metric.unit}")
    }
}
