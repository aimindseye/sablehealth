package io.github.aimindseye.sablehealth

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.ChartPolicy
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.SleepPlotPolicy
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val SECONDS_PER_MINUTE = 60L
private const val WEEK = 7
private const val STACK_BAR_FRACTION = .65f
private const val CELL_CENTER = .5f

private data class NightHistory(
    val dates: List<LocalDate>,
    val sessions: List<List<LiveSleep>>,
    val totals: List<Long>,
) {
    val days: Int get() = dates.size
    val maximum: Long get() = totals.maxOrNull()?.coerceAtLeast(1) ?: 1
}

@Composable
internal fun R6SleepHistoryBars(
    snapshot: LiveSnapshot,
    days: Int,
) {
    val dates = (days - 1 downTo 0).map { snapshot.window.today.minusDays(it.toLong()) }
    val sessions = dates.map { day -> snapshot.sleeps.filter { snapshot.window.day(it.end) == day } }
    val totals = sessions.map { records -> records.sumOf { Duration.between(it.start, it.end).seconds } }
    val history = NightHistory(dates, sessions, totals)
    var selected by remember(days) { mutableIntStateOf(dates.lastIndex) }
    NightHistoryPlot(history) { selected = it }
    val nightLabel =
        if (sessions[selected].isEmpty()) {
            "${dates[selected]} · No record"
        } else {
            "${dates[selected]} · ${totals[selected] / SECONDS_PER_MINUTE} min session"
        }
    Text(nightLabel, style = MaterialTheme.typography.bodyMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        R6IconButton("Previous night", R.drawable.ic_r6_back, { selected = (selected - 1).coerceAtLeast(0) })
        Text("${sessions.count { it.isNotEmpty() }}/$days nights", Modifier.weight(1f))
        R6IconButton("Next night", R.drawable.ic_r6_next, { selected = (selected + 1).coerceAtMost(dates.lastIndex) })
    }
    NightStageSummary(sessions[selected])
}

@Composable
private fun NightStageSummary(sessions: List<LiveSleep>) {
    if (sessions.isNotEmpty()) {
        stageLabels.forEach { label ->
            val seconds =
                sessions.sumOf { session ->
                    SleepPlotPolicy
                        .secondsByStage(session)
                        .filterKeys { plotStage(it) == label }
                        .values
                        .sum()
                }
            if (seconds > 0) {
                val duration = seconds / SECONDS_PER_MINUTE
                Text("$label · $duration min recorded", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private data class NightBarLayout(
    val cell: Float,
    val height: Float,
    val maximum: Long,
)

private data class NightInk(
    val palette: Map<String, Color>,
    val missing: Color,
)

@Composable
private fun NightHistoryPlot(
    history: NightHistory,
    choose: (Int) -> Unit,
) {
    val days = history.days
    val palette = stagePalette()
    val colors = MaterialTheme.colorScheme
    val text = rememberTextMeasurer()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .semantics { contentDescription = "Sleep stage composition by night" }
            .pointerInput(days) {
                detectTapGestures { offset ->
                    choose(ChartPolicy.indexAt(offset.x / size.width, days))
                }
            },
    ) {
        val layout = NightBarLayout(size.width / days, size.height - 18.dp.toPx(), history.maximum)
        val ink = NightInk(palette, colors.outlineVariant)
        history.sessions.forEachIndexed { index, records ->
            drawNightBar(records, history.totals[index], index, layout, ink)
            if (days == WEEK || index % WEEK == 0 || index == history.dates.lastIndex) {
                sleepLabel(
                    text,
                    history.dates[index].dayOfMonth.toString(),
                    (index + CELL_CENTER) * layout.cell,
                    layout.height + 3.dp.toPx(),
                    colors.onSurfaceVariant,
                )
            }
        }
    }
}

private fun DrawScope.drawNightBar(
    records: List<LiveSleep>,
    total: Long,
    index: Int,
    layout: NightBarLayout,
    ink: NightInk,
) {
    val byStage = records.flatMap { SleepPlotPolicy.secondsByStage(it).entries }.groupBy { plotStage(it.key) }
    var bottom = layout.height
    stageLabels.forEach { label ->
        val seconds = byStage[label]?.sumOf { it.value } ?: 0
        val length = seconds.toFloat() / layout.maximum * layout.height
        if (length > 0) {
            drawRect(
                ink.palette.getValue(label),
                Offset(index * layout.cell, bottom - length),
                Size(layout.cell * STACK_BAR_FRACTION, length),
            )
        }
        bottom -= length
    }
    val missing = (total.toFloat() / layout.maximum * layout.height - (layout.height - bottom)).coerceAtLeast(0f)
    if (missing > 0) {
        drawRect(
            ink.missing,
            Offset(index * layout.cell, bottom - missing),
            Size(layout.cell * STACK_BAR_FRACTION, missing),
        )
    }
}

@Composable
internal fun R6SleepJournal(
    session: LiveSleep,
    zone: ZoneId,
) {
    val pattern = if (DateFormat.is24HourFormat(LocalContext.current)) "MMM d HH:mm" else "MMM d h:mm a"
    val formatter = DateTimeFormatter.ofPattern(pattern)
    SleepPlotPolicy.intervals(session).forEach { stage ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stage.type, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${formatter.format(stage.start.atZone(zone))} – ${formatter.format(stage.end.atZone(zone))}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text("${Duration.between(stage.start, stage.end).seconds / SECONDS_PER_MINUTE} min")
        }
    }
}
