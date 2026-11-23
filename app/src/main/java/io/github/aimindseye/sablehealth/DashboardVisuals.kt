package io.github.aimindseye.sablehealth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DashboardDemoSamples
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DemoCharts

private const val RING_SWEEP_DEGREES = 360f
private const val RING_START_DEGREES = -90f
private const val RING_ALPHA = 0.20f
private const val RING_WIDTH_DP = 5
private const val SLEEP_HIGH = 0.23f
private const val SLEEP_MID = 0.49f
private const val SLEEP_LOW = 0.75f
private const val WAVEPATH_END_FACTOR = 0.93f
private const val WAVE_HEIGHT_DP = 50
private const val WAVE_Q25_HEIGHT_DP = 40
private const val GOAL_BAR_HEIGHT_DP = 10

@Composable
internal fun StepGoalRing(sizeDp: Int) {
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
internal fun SleepMiniTimeline(compact: Boolean) {
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
internal fun DashboardGoalBar() {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(GOAL_BAR_HEIGHT_DP.dp).background(accent.copy(alpha = RING_ALPHA))) {
        Box(Modifier.fillMaxWidth(DemoCharts.todayGoalFraction()).fillMaxHeight().background(accent))
    }
}

@Composable
internal fun WidgetValue(
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
internal fun DashboardCard(modifier: Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = { content() },
    )
}
