package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.WidgetStyle

private const val NARROW_TWO_UP_DP = 310
private const val ACCESSIBLE_FONT_THRESHOLD = 1.3f
private const val HERO_VERTICAL_DP = 7
private const val HERO_SIDE_DP = 7
private const val HERO_GAP_DP = 7
private const val COMPACT_RING_DP = 44
private const val REGULAR_RING_DP = 48
private const val STEPS_ICON_Q25_DP = 25
private const val STEPS_ICON_STANDARD_DP = 30
private const val HEART_ICON_Q25_DP = 37
private const val HEART_ICON_STANDARD_DP = 43
private const val COMPACT_WIDGET_SLOT_INDEX = 3
private const val HERO_MIN_HEIGHT_DP = 127
private const val HERO_Q25_MIN_HEIGHT_DP = 116
private const val WIDE_VERTICAL_DP = 8

/** Four fixed slots: two hero cards, one wide visual, and one compact secondary tile. */
@Composable
internal fun DashboardHome(
    state: SableAppState,
    compactQ25: Boolean,
) {
    if (!compactQ25) {
        SectionHeading("Dashboard", "Your health at a glance · no weekly trends")
    }
    val selected = state.dashboard.widgets
    BoxWithConstraints {
        val pair =
            maxWidth >= NARROW_TWO_UP_DP.dp &&
                LocalConfiguration.current.fontScale <= ACCESSIBLE_FONT_THRESHOLD
        if (pair) {
            Row(horizontalArrangement = Arrangement.spacedBy(HERO_GAP_DP.dp)) {
                selected.take(2).forEach { widget ->
                    Box(Modifier.weight(1f)) {
                        DashboardHero(widget, state.demo, compactQ25, state.dashboard.styleFor(widget))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(HERO_GAP_DP.dp)) {
                selected.take(2).forEach { widget ->
                    DashboardHero(widget, state.demo, compactQ25, state.dashboard.styleFor(widget))
                }
            }
        }
    }
    DashboardWide(selected[2], state.demo, compactQ25, state.dashboard.styleFor(selected[2]))
    DashboardCompact(selected[COMPACT_WIDGET_SLOT_INDEX], state.demo)
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
                .padding(horizontal = HERO_SIDE_DP.dp, vertical = HERO_VERTICAL_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                widget.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (demo) {
                HeroIcon(widget, style, compact)
            } else {
                Text("—", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            WidgetValue(widget, demo, prominent = true, compact = compact)
        }
    }
}

@Composable
private fun HeroIcon(
    widget: DashboardWidget,
    style: WidgetStyle,
    compact: Boolean,
) {
    when {
        widget == DashboardWidget.STEPS && style == WidgetStyle.GRAPH -> {
            Box(contentAlignment = Alignment.Center) {
                StepGoalRing(if (compact) COMPACT_RING_DP else REGULAR_RING_DP)
                Icon(
                    painter = painterResource(R.drawable.ic_sable_steps),
                    contentDescription = null, // Decorative within the labelled Steps widget.
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((if (compact) STEPS_ICON_Q25_DP else STEPS_ICON_STANDARD_DP).dp),
                )
            }
        }
        widget == DashboardWidget.HEART_RATE || widget == DashboardWidget.RESTING_HR ->
            Icon(
                painter = painterResource(R.drawable.ic_sable_heart),
                contentDescription = null, // Card label already identifies the metric.
                tint = Color.Unspecified, // Red identifies the heart, not a clinical warning.
                modifier = Modifier.size((if (compact) HEART_ICON_Q25_DP else HEART_ICON_STANDARD_DP).dp),
            )
        else ->
            Text("●", color = MaterialTheme.colorScheme.primary, fontSize = if (compact) 17.sp else 23.sp)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(widget.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (demo) {
                    Text(
                        "DEMO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
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
private fun DashboardCompact(
    widget: DashboardWidget,
    demo: Boolean,
) {
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
