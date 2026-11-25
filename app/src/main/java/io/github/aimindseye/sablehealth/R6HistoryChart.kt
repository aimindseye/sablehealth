package io.github.aimindseye.sablehealth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aimindseye.sablehealth.core.ChartKind
import io.github.aimindseye.sablehealth.core.ChartPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import java.time.LocalDate
import kotlin.math.abs

private const val AXIS_LEFT = 43f
private const val AXIS_RIGHT = 12f
private const val AXIS_TOP = 18f
private const val AXIS_BOTTOM = 19f
private const val BAR_FRACTION = .64f
private const val WEEK = 7
private const val FULL_CHART_DP = 112
private const val MINI_CHART_DP = 78
private const val CELL_CENTER = .5f
private const val SELECTION_ALPHA = .35f
private const val AREA_ALPHA = .18f

@Composable
internal fun R6HistoryChart(
    days: List<LocalDate>,
    points: List<LivePoint?>,
    metric: LiveMetric,
    kind: ChartKind,
) {
    var selected by remember(metric, days) { mutableIntStateOf(days.lastIndex) }
    val point = points[selected]
    val title = point?.let { displayValue(metric, it) } ?: "No record"
    val dateLabel = "${days[selected]}${if (selected == days.lastIndex) " · Today" else ""}"
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Text(dateLabel, style = MaterialTheme.typography.labelSmall)
        }
        R6Plot(
            PlotData(days, points.map { it?.value }, metric, kind),
            selected,
            { selected = it },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            R6IconButton("Previous day", R.drawable.ic_r6_back, { selected = (selected - 1).coerceAtLeast(0) })
            R6IconButton("Next day", R.drawable.ic_r6_next, { selected = (selected + 1).coerceAtMost(days.lastIndex) })
        }
    }
}

internal data class PlotData(
    val days: List<LocalDate>,
    val values: List<Double?>,
    val metric: LiveMetric,
    val kind: ChartKind,
)

@Composable
internal fun R6Plot(
    data: PlotData,
    selected: Int = -1,
    select: (Int) -> Unit = {},
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val text = rememberTextMeasurer()
    val description = "${data.metric.label}, ${data.days.size} days; gaps are missing data"
    val accent =
        if (data.metric == LiveMetric.HEART_RATE || data.metric == LiveMetric.RESTING_HR) {
            colors.tertiary
        } else {
            colors.primary
        }
    val interactive =
        if (compact) {
            Modifier
        } else {
            Modifier
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                select((selected - 1).coerceAtLeast(0))
                                true
                            }
                            Key.DirectionRight -> {
                                select((selected + 1).coerceAtMost(data.days.lastIndex))
                                true
                            }
                            else -> false
                        }
                    }
                }.focusable()
                .pointerInput(data.metric, data.days) {
                    detectTapGestures { offset ->
                        val width = (size.width - AXIS_LEFT.dp.toPx() - AXIS_RIGHT.dp.toPx()).coerceAtLeast(1f)
                        select(ChartPolicy.indexAt((offset.x - AXIS_LEFT.dp.toPx()) / width, data.days.size))
                    }
                }
        }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(if (compact) MINI_CHART_DP.dp else FULL_CHART_DP.dp)
            .semantics { contentDescription = description }
            .then(interactive),
    ) {
        val layout =
            PlotLayout(
                AXIS_LEFT.dp.toPx(),
                AXIS_TOP.dp.toPx(),
                (size.width - AXIS_LEFT.dp.toPx() - AXIS_RIGHT.dp.toPx()).coerceAtLeast(1f),
                (size.height - AXIS_TOP.dp.toPx() - AXIS_BOTTOM.dp.toPx()).coerceAtLeast(1f),
                ChartPolicy.bounds(data.values, data.kind),
                data.days.size,
            )
        plotAxes(layout, text, colors.onSurfaceVariant, colors.outlineVariant)
        plotValues(data, layout, PlotInk(accent, colors.onSurface, text), selected)
    }
}

private data class PlotLayout(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val range: ClosedFloatingPointRange<Double>,
    val count: Int,
) {
    val cell: Float get() = width / count.coerceAtLeast(1)

    fun x(index: Int): Float = left + (index + CELL_CENTER) * cell

    fun y(value: Double): Float {
        val fraction = (range.endInclusive - value) / (range.endInclusive - range.start)
        return top + fraction.toFloat() * height
    }

    val bottom: Float get() = top + height
}

private data class PlotInk(
    val accent: Color,
    val label: Color,
    val text: TextMeasurer,
)

private fun DrawScope.plotAxes(
    layout: PlotLayout,
    text: TextMeasurer,
    label: Color,
    grid: Color,
) {
    val midpoint = (layout.range.start + layout.range.endInclusive) / 2
    val ticks = listOf(layout.range.endInclusive, midpoint, layout.range.start)
    ticks.forEach { value ->
        val y = layout.y(value)
        drawLine(grid, Offset(layout.left, y), Offset(layout.left + layout.width, y), 1.dp.toPx())
        drawText(
            text,
            formatNumber(value),
            topLeft = Offset(0f, (y - 6.dp.toPx()).coerceAtLeast(0f)),
            style = TextStyle(color = label, fontSize = 9.sp),
        )
    }
}

private fun DrawScope.plotValues(
    data: PlotData,
    layout: PlotLayout,
    ink: PlotInk,
    selected: Int,
) {
    data.values.forEachIndexed { index, raw ->
        val value = raw?.takeIf(Double::isFinite)
        val x = layout.x(index)
        if (index == selected) {
            drawLine(
                ink.label.copy(alpha = SELECTION_ALPHA),
                Offset(x, layout.top),
                Offset(x, layout.bottom),
                1.dp.toPx(),
            )
        }
        if (value != null) {
            val previous = data.values.getOrNull(index - 1)?.takeIf(Double::isFinite)
            val here = Offset(x, layout.y(value))
            val before = previous?.let { Offset(layout.x(index - 1), layout.y(it)) }
            drawSample(data.kind, layout, ink.accent, here, before)
            val labelsFit = data.days.size <= WEEK && layout.cell > 38.dp.toPx()
            if (index == selected || labelsFit) {
                val measured =
                    ink.text.measure(
                        formatNumber(value),
                        style = TextStyle(color = ink.label, fontSize = 9.sp),
                    )
                val left =
                    (x - measured.size.width / 2f).coerceIn(
                        0f,
                        (size.width - measured.size.width).coerceAtLeast(0f),
                    )
                drawText(measured, topLeft = Offset(left, (here.y - measured.size.height).coerceAtLeast(0f)))
            }
        }
        val weeklyTick = index % WEEK == 0 && index < data.days.lastIndex - 2
        if (data.days.size <= WEEK || index == data.days.lastIndex || weeklyTick) {
            val label = data.days[index].dayOfMonth.toString()
            drawText(
                ink.text,
                label,
                topLeft = Offset((x - 6.dp.toPx()).coerceAtLeast(0f), layout.bottom + 3.dp.toPx()),
                style = TextStyle(color = ink.label, fontSize = 9.sp),
            )
        }
    }
}

private fun DrawScope.drawSample(
    kind: ChartKind,
    layout: PlotLayout,
    color: Color,
    here: Offset,
    before: Offset?,
) {
    if (kind == ChartKind.BAR) {
        val zero = layout.y(0.0)
        drawRoundRect(
            color,
            Offset(here.x - layout.cell * BAR_FRACTION / 2, minOf(here.y, zero)),
            Size(layout.cell * BAR_FRACTION, abs(zero - here.y).coerceAtLeast(1f)),
            CornerRadius(3.dp.toPx()),
        )
        return
    }
    if (before != null && kind != ChartKind.POINTS) {
        if (kind == ChartKind.AREA) {
            val area =
                Path().apply {
                    moveTo(before.x, layout.y(0.0))
                    lineTo(before.x, before.y)
                    lineTo(here.x, here.y)
                    lineTo(here.x, layout.y(0.0))
                    close()
                }
            drawPath(area, color.copy(alpha = AREA_ALPHA))
        }
        drawLine(color, before, here, 2.dp.toPx())
    }
    drawCircle(color, radius = 2.5.dp.toPx(), center = here)
}
