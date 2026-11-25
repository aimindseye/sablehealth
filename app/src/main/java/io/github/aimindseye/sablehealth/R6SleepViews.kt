package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.SleepPlotPolicy
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SECONDS_PER_MINUTE = 60L
private val asleepStageNames = setOf("Asleep (unspecified)", "Light", "Deep", "REM")

@Composable
internal fun LiveSleepPage(
    state: SableAppState,
    snapshot: LiveSnapshot,
) {
    val days =
        snapshot.sleeps
            .map { snapshot.window.day(it.end) }
            .distinct()
            .sortedDescending()
    var dayName by rememberSaveable { mutableStateOf("") }
    val day = days.firstOrNull { it.toString() == dayName } ?: days.firstOrNull()
    val graph = state.modeFor(Section.SLEEP) == ViewMode.GRAPHS
    R6ModeHeading(state, period = {
        if (day != null) {
            val label = day.format(DateTimeFormatter.ofPattern("MMM d"))
            R6Choice("Sleep date", label, days.map(LocalDate::toString), { dayName = it })
            if (graph) SleepGraphChoice(state.r6)
        }
    })
    if (day == null) {
        R6EmptyCard(state, "No sleep sessions yet", "If Health Connect has no sessions, check Google Health export.")
    } else if (graph && state.r6.sleepGraph == "Stacked") {
        PeriodChoice(state.r6.period(Section.SLEEP)) { state.r6.choosePeriod(Section.SLEEP, it) }
        R6SleepHistoryBars(snapshot, state.r6.period(Section.SLEEP))
    } else {
        snapshot.sleeps.filter { snapshot.window.day(it.end) == day }.forEach { session ->
            SleepSessionCard(session, snapshot.window.zone, graph, state.r6)
        }
    }
    if (!graph || state.r6.sleepGraph != "Stacked") {
        HistoryPanel(state, snapshot, listOf(LiveMetric.SLEEP_DURATION), LiveMetric.SLEEP_DURATION) {}
    }
}

@Composable
private fun SleepGraphChoice(preferences: R6Preferences) {
    R6Choice(
        "Sleep graph type",
        preferences.sleepGraph,
        listOf("Timeline", "Donut", "Stacked"),
        preferences::chooseSleepGraph,
    )
}

@Composable
private fun SleepSessionCard(
    session: LiveSleep,
    zone: ZoneId,
    graph: Boolean,
    preferences: R6Preferences,
) {
    val seconds = SleepPlotPolicy.secondsByStage(session)
    val asleep = seconds.filterKeys { it in asleepStageNames }.values.sum()
    R6Card(color = r7MetricSurface(LiveMetric.SLEEP_DURATION), verticalPadding = 2.dp) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 32.dp).testTag("r7-sleep-summary"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val title =
                if (seconds.keys.any { it in asleepStageNames }) {
                    "${r7HoursMinutes(asleep)} asleep"
                } else {
                    "${r7HoursMinutes(Duration.between(session.start, session.end).seconds)} session"
                }
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            Text(
                sleepRange(session, zone),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
            )
        }
        if (graph) {
            R6SleepSessionGraph(session, zone, preferences)
        } else {
            if (seconds.isEmpty()) Text("No stages exported")
            R6SleepJournal(session, zone)
        }
        SleepTotals(session, seconds, asleep)
    }
}

@Composable
private fun SleepTotals(
    session: LiveSleep,
    seconds: Map<String, Long>,
    asleep: Long,
) {
    val duration = Duration.between(session.start, session.end).seconds
    Text("Session: ${r7HoursMinutes(duration)}", style = MaterialTheme.typography.labelMedium)
    if (seconds.isNotEmpty()) {
        if (seconds.keys.any { it in asleepStageNames }) {
            Text("Recorded asleep: ${asleep / SECONDS_PER_MINUTE} min · gaps unknown")
        }
        val gap = (duration - seconds.values.sum()).coerceAtLeast(0)
        if (gap > 0) Text("Unrecorded: ${r7HoursMinutes(gap)}", style = MaterialTheme.typography.labelSmall)
        seconds.forEach { (name, stageSeconds) -> Text("$name · ${stageSeconds / SECONDS_PER_MINUTE} min") }
    }
}
