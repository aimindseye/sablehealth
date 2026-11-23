package io.github.aimindseye.sablehealth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aimindseye.sablehealth.core.DashboardDemoSamples
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DemoCharts
import io.github.aimindseye.sablehealth.core.WidgetStyle

private const val NARROW_TWO_UP_DP = 310
private const val ACCESSIBLE_FONT_THRESHOLD = 1.3f
private const val HERO_VERTICAL_DP = 7
private const val HERO_MIN_HEIGHT_DP = 127
private const val HERO_Q25_MIN_HEIGHT_DP = 101
private const val WIDE_VERTICAL_DP = 8
private const val WAVE_HEIGHT_DP = 50
private const val WAVE_Q25_HEIGHT_DP = 40
private const val RING_SWEEP_DEGREES = 360f
private const val RING_START_DEGREES = -90f
private const val RING_ALPHA = 0.20f
private const val RING_WIDTH_DP = 5
private const val SLEEP_HIGH = 0.23f
private const val SLEEP_MID = 0.49f
private const val SLEEP_LOW = 0.75f
private const val GOAL_BAR_HEIGHT_DP = 10
private const val WAVEPATH_END_FACTOR = 0.93f

/** Four fixed slots: two hero cards, one wide visual, and one compact secondary tile. */
@Composable
internal fun DashboardHome(
    state: SableAppState,
    compactQ25: Boolean,
) {
    SectionHeading("Dashboard", "Your health at a glance · no weekly trends")
    val selected = state.dashboardWidgets
    BoxWithConstraints {
        val pair = maxWidth >= NARROW_TWO_UP_DP.dp &&
            LocalConfiguration.current.fontScale <= ACCESSIBLE_FONT_THRESHOLD
        if (pair) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                selected.take(2).forEach { widget ->
                    Box(Modifier.weight(1f)) {
                        DashboardHero(widget, state.demo, compactQ25, state.styleFor(widget))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                selected.take(2).forEach { widget ->
                    DashboardHero(widget, state.demo, compactQ25, state.styleFor(widget))
                }
            }
        }
    }
    DashboardWide(selected[2], state.demo, compactQ25, state.styleFor(selected[2]))
    DashboardCompact(selected[3], state.demo)
    DashboardDataStatus(state)
}

@Composable
private fun DashboardHero(
    widget: DashboardWidget,
    demo: Boolean,
    compact: Boolean,
    style: WidgetStyle,
) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .height((if (compact) HERO_Q25_MIN_HEIGHT_DP else HERO_MIN_HEIGHT_DP).dp)
                .padding(horizontal = 7.dp, vertical = HERO_VERTICAL_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(widget.label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (demo) HeroIcon(widget, style, compact)
            else Text("—", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            WidgetValue(widget, demo, prominent = true, compact = compact)
        }
    }
}

@Composable
private fun HeroIcon(widget: DashboardWidget, style: WidgetStyle, compact: Boolean) {
    when {
        widget == DashboardWidget.STEPS && style == WidgetStyle.GRAPH -> {
            Box(contentAlignment = Alignment.Center) {
                StepGoalRing(if (compact) 30 else 40)
                Text("↟", color = MaterialTheme.colorScheme.primary, fontSize = if (compact) 17.sp else 23.sp)
            }
        }
        widget == DashboardWidget.HEART_RATE || widget == DashboardWidget.RESTING_HR ->
            Text("♥", color = MaterialTheme.colorScheme.primary, fontSize = if (compact) 25.sp else 32.sp)
        else ->
            Text("●", color = MaterialTheme.colorScheme.primary, fontSize = if (compact) 17.sp else 23.sp)
    }
}

@Composable
private fun StepGoalRing(sizeDp: Int) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(sizeDp.dp).semantics { contentDescription = "Synthetic steps toward daily goal" }) {
        val stroke = Stroke(width = RING_WIDTH_DP.dp.toPx(), cap = StrokeCap.Round)
        drawArc(accent.copy(alpha = RING_ALPHA), RING_START_DEGREES, RING_SWEEP_DEGREES, false, style = stroke)
        drawArc(
            accent,
            RING_START_DEGREES,
            RING_SWEEP_DEGREES * DemoCharts.todayGoalFraction(),
            false,
            style = stroke,
        )
    }
}

@Composable
private fun DashboardWide(
    widget: DashboardWidget,
    demo: Boolean,
    compact: Boolean,
    style: WidgetStyle,
) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = WIDE_VERTICAL_DP.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(widget.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (demo) Text("DEMO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (demo && widget == DashboardWidget.SLEEP_STAGES && style == WidgetStyle.GRAPH) {
                SleepMiniTimeline(compact)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("11:15 PM", style = MaterialTheme.typography.bodySmall)
                    Text("7h 42m · Demo", style = MaterialTheme.typography.bodySmall)
                    Text("7:18 AM", style = MaterialTheme.typography.bodySmall)
                }
            } else if (demo && widget == DashboardWidget.STEPS && style == WidgetStyle.GRAPH) {
                DashboardGoalBar()
                WidgetValue(widget, demo)
            } else {
                WidgetValue(widget, demo)
            }
        }
    }
}

@Composable
private fun SleepMiniTimeline(compact: Boolean) {
    val violet = SablePalette.blue // A category graphic, not a normal/abnormal reading.
    Canvas(
        Modifier.fillMaxWidth().height((if (compact) WAVE_Q25_HEIGHT_DP else WAVE_HEIGHT_DP).dp)
            .semantics { contentDescription = "Synthetic illustrative sleep stages overnight, not an actual sensor waveform" },
    ) {
        val points = listOf(SLEEP_MID, SLEEP_HIGH, SLEEP_LOW, SLEEP_HIGH, SLEEP_LOW,
            SLEEP_HIGH, SLEEP_LOW, SLEEP_MID, SLEEP_LOW, SLEEP_HIGH, SLEEP_MID, SLEEP_HIGH)
        val path = Path()
        val dx = size.width / points.lastIndex
        points.forEachIndexed { index, y ->
            val xx = index * dx
            val yy = y * size.height * WAVEPATH_END_FACTOR
            if (index == 0) path.moveTo(xx, yy) else path.lineTo(xx, yy)
        }
        drawLine(violet.copy(alpha = RING_ALPHA), Offset(0f, size.height), Offset(size.width, size.height))
        drawPath(path, color = violet, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun DashboardGoalBar() {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(GOAL_BAR_HEIGHT_DP.dp).background(accent.copy(alpha = RING_ALPHA))) {
        Box(Modifier.fillMaxWidth(DemoCharts.todayGoalFraction()).fillMaxHeight().background(accent))
    }
}

@Composable
private fun DashboardCompact(widget: DashboardWidget, demo: Boolean) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(widget.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            WidgetValue(widget, demo)
        }
    }
}

@Composable
private fun WidgetValue(
    widget: DashboardWidget,
    demo: Boolean,
    prominent: Boolean = false,
    compact: Boolean = false,
) {
    val sample = if (demo) DashboardDemoSamples.forWidget(widget) else null
    val value = sample?.value ?: "—"
    val detail = sample?.detail ?: if (demo) "No demo sample" else "Not synced"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = if (prominent && !compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (sample == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (prominent) Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DashboardDataStatus(state: SableAppState) {
    val message = if (state.demo) "DEMO · No live data imported (M0)" else "No data · Never imported"
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DashboardCard(modifier: Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = { content() },
    )
}
