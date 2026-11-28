package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
private const val STAGE_COLUMN_MIN_WIDTH = 320
private const val STAGE_MAX_FONT_SCALE = 1.3f
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
    var details by rememberSaveable(dayName) { mutableStateOf(false) }
    val stacked = graph && state.r6.sleepGraph == "Stacked"
    val showDetails = !graph || details
    R6ModeHeading(
        state,
        titleContent = {
            if (day != null) {
                val label = day.format(DateTimeFormatter.ofPattern("MMM d"))
                R6Choice("Sleep date", label, days.map(LocalDate::toString), { dayName = it })
            } else {
                Text("Sleep", style = MaterialTheme.typography.titleLarge)
            }
        },
        period = {
            if (graph && day != null) {
                SleepGraphControls(state.r6, details) { details = !details }
            }
        },
    )
    if (day == null) {
        R6EmptyCard(state, "No sleep sessions yet", "If Health Connect has no sessions, check Google Health export.")
    } else if (stacked) {
        PeriodChoice(state.r6.period(Section.SLEEP)) { state.r6.choosePeriod(Section.SLEEP, it) }
        val period = state.r6.period(Section.SLEEP)
        R6SleepHistoryBars(snapshot, period)
        val series = snapshot.metrics[LiveMetric.SLEEP_DURATION]
        val points = calendar(snapshot, period).map { series?.on(it) }
        M2HistoryEvidence(LiveMetric.SLEEP_DURATION, series?.status, points, snapshot.window.zone)
    } else {
        SelectedSleepSession(state, snapshot, day, !showDetails)
    }
    if (day == null || (showDetails && !stacked)) SleepHistoryDisclosure(state, snapshot)
}

@Composable
private fun SleepGraphControls(
    preferences: R6Preferences,
    details: Boolean,
    toggle: () -> Unit,
) {
    SleepGraphChoice(preferences)
    if (preferences.sleepGraph != "Stacked") {
        R6IconButton(
            "Sleep details",
            if (details) R.drawable.ic_r6_back else R.drawable.ic_r6_next,
            toggle,
            active = details,
        )
    }
}

@Composable
private fun SelectedSleepSession(
    state: SableAppState,
    snapshot: LiveSnapshot,
    day: LocalDate,
    graph: Boolean,
) {
    val sessions = snapshot.sleeps.filter { snapshot.window.day(it.end) == day }
    var index by rememberSaveable(day) { mutableIntStateOf(0) }
    val selected = index.coerceIn(sessions.indices)
    if (sessions.size > 1) {
        val options = sessions.indices.map { "Session ${it + 1}" }
        R6Choice("Sleep session", options[selected], options, { index = options.indexOf(it) })
    }
    SleepSessionCard(sessions[selected], snapshot.window.zone, graph, state.r6)
}

@Composable
private fun SleepHistoryDisclosure(
    state: SableAppState,
    snapshot: LiveSnapshot,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    R7aGroup("") {
        R7aRow("Sleep history", if (expanded) "Hide" else "Open", action = { expanded = !expanded })
    }
    if (expanded) HistoryPanel(state, snapshot, listOf(LiveMetric.SLEEP_DURATION), LiveMetric.SLEEP_DURATION)
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
        } else if (seconds.isEmpty()) {
            Text("No stages exported")
        }
        if (!graph) {
            SleepTotals(session, seconds, asleep)
            M2SleepDetails(session)
            if (seconds.isNotEmpty()) {
                var journal by rememberSaveable(session.start.toString(), session.end.toString()) {
                    mutableStateOf(false)
                }
                R7aRow("Recorded stages", if (journal) "Hide" else "Open", action = { journal = !journal })
                if (journal) R6SleepJournal(session, zone)
            }
        }
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
        StageTotalRows(seconds)
    }
}

@Composable
private fun StageTotalRows(seconds: Map<String, Long>) {
    BoxWithConstraints {
        val columns =
            if (maxWidth >= STAGE_COLUMN_MIN_WIDTH.dp && LocalConfiguration.current.fontScale <= STAGE_MAX_FONT_SCALE) {
                2
            } else {
                1
            }
        Column {
            seconds.entries.chunked(columns).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pair.forEach { (name, stageSeconds) ->
                        Text(
                            "$name · ${stageSeconds / SECONDS_PER_MINUTE} min",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (pair.size < columns) Box(Modifier.weight(1f))
                }
            }
        }
    }
}
