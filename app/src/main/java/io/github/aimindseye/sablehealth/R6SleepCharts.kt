package io.github.aimindseye.sablehealth

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveStage
import io.github.aimindseye.sablehealth.core.SleepPlotPolicy
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val stageLabels = listOf("Awake", "REM", "Light", "Deep", "Other")
private const val SECONDS_PER_MINUTE = 60L
private const val LABEL_WIDTH = 49f
private const val LABEL_TOP = 34f
private const val LABEL_BOTTOM = 18f
private const val ALL_STAGE_WIDTH = 88
private const val FULL_CIRCLE = 360f
private const val MAX_TIMELINE_DP = 5000
private const val TIMELINE_HEIGHT_DP = 124
private const val LABEL_ROW_DP = 16
private const val GUIDE_ALPHA = .48f
private const val MIN_TIMELINE_DP = 340
private const val AWAKE_START_ANGLE = -90f
private const val REM_COLOR = 0xFFAA92D9
private const val DEEP_COLOR = 0xFF7775C8

internal fun plotStage(type: String): String =
    when (type) {
        "Awake", "Awake in bed", "Out of bed" -> "Awake"
        "REM", "Light", "Deep" -> type
        else -> "Other"
    }

@Composable
internal fun stagePalette(): Map<String, Color> {
    val colors = MaterialTheme.colorScheme
    return mapOf(
        "Awake" to colors.tertiary,
        "REM" to Color(REM_COLOR),
        "Light" to colors.primary,
        "Deep" to Color(DEEP_COLOR),
        "Other" to colors.onSurfaceVariant,
    )
}

@Composable
internal fun R6SleepSessionGraph(
    session: LiveSleep,
    zone: ZoneId,
    preferences: R6Preferences,
) {
    val stages = remember(session) { SleepPlotPolicy.intervals(session) }
    if (stages.isEmpty()) {
        Text("No stages exported", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val palette = stagePalette()
    if (preferences.sleepGraph == "Donut") {
        SleepComposition(session, palette)
    } else {
        SleepTransitionChart(session, zone, stages, palette, preferences)
    }
}

@Composable
private fun SleepTransitionChart(
    session: LiveSleep,
    zone: ZoneId,
    stages: List<LiveStage>,
    palette: Map<String, Color>,
    preferences: R6Preferences,
) {
    var selected by remember(session) { mutableIntStateOf(0) }
    val labels = preferences.transitionLabels
    val pattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm a"
    val formatter = DateTimeFormatter.ofPattern(pattern)
    val format: (Instant) -> String = { formatter.format(it.atZone(zone)) }
    val all = labels == "All times"
    val chart: @Composable (Modifier) -> Unit = { modifier ->
        SleepPlot(session, stages, palette, SleepSelection(selected, labels, format) { selected = it }, modifier)
    }
    val stage = stages[selected]
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        R6IconButton("Previous stage", R.drawable.ic_r6_back, { selected = (selected - 1).coerceAtLeast(0) })
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stage.type, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Duration.between(stage.start, stage.end).seconds / SECONDS_PER_MINUTE} min",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text("${format(stage.start)} – ${format(stage.end)}", style = MaterialTheme.typography.bodyMedium)
        }
        R6IconButton("Next stage", R.drawable.ic_r6_next, { selected = (selected + 1).coerceAtMost(stages.lastIndex) })
        R6Choice(
            "Transition labels",
            labels,
            listOf("Key times", "All times", "Selected"),
            preferences::chooseTransitionLabels,
        )
    }
    if (all) {
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            chart(Modifier.width((stages.size * ALL_STAGE_WIDTH).coerceIn(MIN_TIMELINE_DP, MAX_TIMELINE_DP).dp))
        }
    } else {
        chart(Modifier.fillMaxWidth())
    }
    val previous = stages.getOrNull(selected - 1)
    val next = stages.getOrNull(selected + 1)
    val before = previous?.takeIf { SleepPlotPolicy.contiguous(it, stage) }?.type ?: "Start / gap"
    val after = next?.takeIf { SleepPlotPolicy.contiguous(stage, it) }?.type ?: "End / gap"
    Text("$before → ${stage.type} → $after", style = MaterialTheme.typography.labelSmall)
    if (all) {
        Text("Scroll the timeline; select a stage for both exact times.", style = MaterialTheme.typography.labelSmall)
    }
}

private data class SleepSelection(
    val index: Int,
    val labels: String,
    val format: (Instant) -> String,
    val choose: (Int) -> Unit,
)

private data class SleepPlotLayout(
    val left: Float,
    val top: Float,
    val width: Float,
    val row: Float,
    val labels: List<String>,
) {
    fun y(type: String): Float = top + labels.indexOf(plotStage(type)) * row + row / 2

    val bottom: Float get() = top + labels.size * row
}

@Composable
private fun SleepPlot(
    session: LiveSleep,
    stages: List<LiveStage>,
    palette: Map<String, Color>,
    selection: SleepSelection,
    modifier: Modifier,
) {
    val text = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    Canvas(
        modifier
            .height(TIMELINE_HEIGHT_DP.dp)
            .semantics { contentDescription = "Sleep stage transitions with recorded times" }
            .pointerInput(session) {
                detectTapGestures { offset ->
                    val width = (size.width - LABEL_WIDTH.dp.toPx() - 6.dp.toPx()).coerceAtLeast(1f)
                    val position = ((offset.x - LABEL_WIDTH.dp.toPx()) / width).coerceIn(0f, 1f)
                    val index =
                        stages.indexOfFirst {
                            val start = SleepPlotPolicy.fraction(session, it.start)
                            val end = SleepPlotPolicy.fraction(session, it.end)
                            position >= start && position < end
                        }
                    if (index >= 0) selection.choose(index)
                }
            },
    ) {
        val labels = if (stages.any { plotStage(it.type) == "Other" }) stageLabels else stageLabels.dropLast(1)
        val layout =
            SleepPlotLayout(
                LABEL_WIDTH.dp.toPx(),
                LABEL_TOP.dp.toPx(),
                (size.width - LABEL_WIDTH.dp.toPx() - 6.dp.toPx()).coerceAtLeast(1f),
                (size.height - LABEL_TOP.dp.toPx() - LABEL_BOTTOM.dp.toPx()) / labels.size,
                labels,
            )
        val x: (Instant) -> Float = { layout.left + SleepPlotPolicy.fraction(session, it) * layout.width }
        drawSleepGuides(layout, text, colors)
        drawSleepStages(stages, selection.index, layout, x, SleepStageInk(palette, colors.onSurface))
        drawTransitionLabels(stages, selection, x, text, colors.onSurface)
        val midpoint = session.start.plusMillis(Duration.between(session.start, session.end).toMillis() / 2)
        listOf(session.start, midpoint, session.end).forEach { instant ->
            sleepLabel(
                text,
                selection.format(instant),
                x(instant),
                layout.bottom + 4.dp.toPx(),
                colors.onSurfaceVariant,
            )
        }
    }
}

private data class SleepStageInk(
    val palette: Map<String, Color>,
    val marker: Color,
)

private fun DrawScope.drawSleepStages(
    stages: List<LiveStage>,
    selected: Int,
    layout: SleepPlotLayout,
    x: (Instant) -> Float,
    ink: SleepStageInk,
) {
    stages.forEachIndexed { index, stage ->
        val color = ink.palette.getValue(plotStage(stage.type))
        val y = layout.y(stage.type)
        val start = x(stage.start)
        val end = x(stage.end)
        val previous = stages.getOrNull(index - 1)
        if (previous != null && SleepPlotPolicy.contiguous(previous, stage)) {
            drawLine(color, Offset(start, layout.y(previous.type)), Offset(start, y), 1.5.dp.toPx())
        }
        drawRoundRect(
            color,
            Offset(start, y - 3.dp.toPx()),
            Size((end - start).coerceAtLeast(1f), 6.dp.toPx()),
            CornerRadius(2.dp.toPx()),
        )
        if (index == selected) {
            listOf(start, end).forEach { at ->
                drawLine(ink.marker, Offset(at, layout.top), Offset(at, layout.bottom), 1.dp.toPx())
            }
        }
    }
}

private fun DrawScope.drawTransitionLabels(
    stages: List<LiveStage>,
    selection: SleepSelection,
    x: (Instant) -> Float,
    text: TextMeasurer,
    color: Color,
) {
    val chosen = stages[selection.index]
    val range = "${selection.format(chosen.start)} – ${selection.format(chosen.end)}"
    val center = (x(chosen.start) + x(chosen.end)) / 2f
    sleepLabel(text, range, center, LABEL_ROW_DP.dp.toPx(), color)
    val candidates =
        stages.mapIndexedNotNull { index, stage ->
            val key = index == 0 || plotStage(stage.type) == "Awake"
            val show = selection.labels == "All times" || selection.labels == "Key times" && key
            if (show && stage.start != chosen.start && stage.start != chosen.end) {
                R7TimelineLabels.Label(selection.format(stage.start), x(stage.start))
            } else {
                null
            }
        }
    drawR7TimeLabels(candidates, text, color)
}

internal fun DrawScope.sleepLabel(
    text: TextMeasurer,
    label: String,
    x: Float,
    y: Float,
    color: Color,
) {
    val measured = text.measure(label, style = TextStyle(color = color, fontSize = 9.sp))
    val left = (x - measured.size.width / 2f).coerceIn(0f, (size.width - measured.size.width).coerceAtLeast(0f))
    drawText(measured, topLeft = Offset(left, y))
}

@Composable
private fun SleepComposition(
    session: LiveSleep,
    palette: Map<String, Color>,
) {
    val values = SleepPlotPolicy.secondsByStage(session)
    val total = Duration.between(session.start, session.end).seconds.coerceAtLeast(1)
    val gap = (total - values.values.sum()).coerceAtLeast(0)
    val missingColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        Modifier.fillMaxWidth().height(104.dp).semantics {
            contentDescription = "Sleep stage proportions including unrecorded time"
        },
    ) {
        val diameter = (minOf(size.height, size.width) - 14.dp.toPx()).coerceAtLeast(1f)
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val dimensions = Size(diameter, diameter)
        var start = AWAKE_START_ANGLE
        values.forEach { (stage, seconds) ->
            val sweep = seconds.toFloat() / total * FULL_CIRCLE
            drawArc(
                palette.getValue(plotStage(stage)),
                start,
                sweep,
                false,
                topLeft,
                dimensions,
                style = Stroke(12.dp.toPx()),
            )
            start += sweep
        }
        drawArc(
            missingColor,
            start,
            gap.toFloat() / total * FULL_CIRCLE,
            false,
            topLeft,
            dimensions,
            style = Stroke(12.dp.toPx()),
        )
    }
    if (gap > 0) Text("Unrecorded: ${gap / SECONDS_PER_MINUTE} min", style = MaterialTheme.typography.labelSmall)
}

private fun DrawScope.drawSleepGuides(
    layout: SleepPlotLayout,
    text: TextMeasurer,
    colors: ColorScheme,
) {
    layout.labels.forEach { label ->
        val y = layout.y(label)
        drawLine(
            colors.onSurfaceVariant.copy(alpha = GUIDE_ALPHA),
            Offset(layout.left, y),
            Offset(layout.left + layout.width, y),
            1.dp.toPx(),
        )
        drawText(
            text,
            label,
            topLeft = Offset(0f, y - 7.dp.toPx()),
            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp),
        )
    }
}
