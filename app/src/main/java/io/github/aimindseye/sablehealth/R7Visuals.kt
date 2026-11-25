package io.github.aimindseye.sablehealth

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.LiveMetric

private const val DARK_THRESHOLD = .5f
private const val DARK_TINT = .19f
private const val LIGHT_TINT = .10f
private const val MOVEMENT_TINT = 0xFF409B96
private const val HEART_TINT = 0xFFC86D85
private const val SLEEP_TINT = 0xFF8675C8
private const val VITAL_TINT = 0xFF4C91BA
private const val ENERGY_TINT = 0xFFB99964

/** A zero-width Compose border means hairline, not no border. */
internal fun r7FocusOutline(
    focused: Boolean,
    color: Color,
    shape: Shape,
): Modifier = if (focused) Modifier.border(2.dp, color, shape) else Modifier

@Composable
internal fun r7MetricSurface(metric: LiveMetric?): Color {
    val base = MaterialTheme.colorScheme.surface
    val tint =
        when (metric) {
            LiveMetric.STEPS, LiveMetric.DISTANCE, LiveMetric.ACTIVE_MINUTES, LiveMetric.EXERCISE_SESSIONS ->
                Color(MOVEMENT_TINT)
            LiveMetric.HEART_RATE, LiveMetric.RESTING_HR -> Color(HEART_TINT)
            LiveMetric.SLEEP_DURATION -> Color(SLEEP_TINT)
            LiveMetric.CALORIES, LiveMetric.ACTIVE_CALORIES -> Color(ENERGY_TINT)
            null -> MaterialTheme.colorScheme.primary
            else -> Color(VITAL_TINT)
        }
    return lerp(base, tint, if (base.luminance() < DARK_THRESHOLD) DARK_TINT else LIGHT_TINT)
}

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L

internal fun r7HoursMinutes(seconds: Long): String {
    val minutes = seconds.coerceAtLeast(0) / SECONDS_PER_MINUTE
    return "${minutes / MINUTES_PER_HOUR}h ${minutes % MINUTES_PER_HOUR}m"
}
