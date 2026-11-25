package io.github.aimindseye.sablehealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.SleepPlotPolicy
import java.time.LocalDate
import java.time.ZoneId

private const val SECONDS_PER_MINUTE = 60L
private val asleepStageNames = setOf("Asleep (unspecified)", "Light", "Deep", "REM")

@Composable
internal fun LiveSleepPage(
    state: SableAppState,
    snapshot: LiveSnapshot,
) {
    R6ModeHeading(state)
    val days =
        snapshot.sleeps
            .map { snapshot.window.day(it.end) }
            .distinct()
            .sortedDescending()
    var dayName by rememberSaveable { mutableStateOf("") }
    val day = days.firstOrNull { it.toString() == dayName } ?: days.firstOrNull()
    val graph = state.modeFor(Section.SLEEP) == ViewMode.GRAPHS
    if (day == null) {
        R6EmptyCard(state, "No sleep sessions yet", "If Health Connect has no sessions, check Google Health export.")
    } else {
        if (graph) {
            R6Choice(
                "Sleep graph type",
                state.r6.sleepGraph,
                listOf("Timeline", "Donut", "Stacked"),
                state.r6::chooseSleepGraph,
            )
        }
        if (graph && state.r6.sleepGraph == "Stacked") {
            PeriodChoice(state.r6.period(Section.SLEEP)) { state.r6.choosePeriod(Section.SLEEP, it) }
            R6SleepHistoryBars(snapshot, state.r6.period(Section.SLEEP))
        } else {
            R6Choice("Sleep date", day.toString(), days.map(LocalDate::toString), { dayName = it })
            snapshot.sleeps.filter { snapshot.window.day(it.end) == day }.forEach { session ->
                SleepSessionCard(session, snapshot.window.zone, graph, state.r6)
            }
        }
    }
    if (!graph || state.r6.sleepGraph != "Stacked") {
        HistoryPanel(state, snapshot, listOf(LiveMetric.SLEEP_DURATION), LiveMetric.SLEEP_DURATION) {}
    }
}

@Composable
private fun SleepSessionCard(
    session: LiveSleep,
    zone: ZoneId,
    graph: Boolean,
    preferences: R6Preferences,
) {
    R6Card {
        Text("${formatNumber(session.minutes)} min session", style = MaterialTheme.typography.headlineSmall)
        Text(sleepRange(session, zone), style = MaterialTheme.typography.labelSmall)
        if (graph) {
            R6SleepSessionGraph(session, zone, preferences)
        } else {
            if (SleepPlotPolicy.intervals(session).isEmpty()) Text("No stages exported")
            R6SleepJournal(session, zone)
        }
        val seconds = SleepPlotPolicy.secondsByStage(session)
        if (seconds.isNotEmpty()) {
            val asleep = seconds.filterKeys { it in asleepStageNames }.values.sum()
            Text("Recorded asleep: ${asleep / SECONDS_PER_MINUTE} min · gaps unknown")
            seconds.forEach { (name, duration) -> Text("$name · ${duration / SECONDS_PER_MINUTE} min") }
        }
    }
}
