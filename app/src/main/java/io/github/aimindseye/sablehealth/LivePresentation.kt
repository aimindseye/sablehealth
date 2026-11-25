package io.github.aimindseye.sablehealth

import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSleep
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun widgetMetric(widget: DashboardWidget): LiveMetric? =
    when (widget) {
        DashboardWidget.SLEEP_STAGES, DashboardWidget.SLEEP_SCHEDULE -> LiveMetric.SLEEP_DURATION
        else -> LiveMetric.entries.firstOrNull { it.name == widget.name }
    }

internal fun widgetTitle(widget: DashboardWidget): String =
    when (widget) {
        DashboardWidget.SLEEP_STAGES, DashboardWidget.SLEEP_SCHEDULE -> widget.label
        else -> widgetMetric(widget)?.label ?: widget.label
    }

internal fun localTime(
    at: Instant,
    zone: ZoneId,
): String = DateTimeFormatter.ofPattern("MMM d HH:mm").format(at.atZone(zone))

internal fun sleepRange(
    sleep: LiveSleep,
    zone: ZoneId,
): String = "${localTime(sleep.start, zone)} – ${localTime(sleep.end, zone)}"

internal fun formatNumber(value: Double): String {
    val formatter = NumberFormat.getNumberInstance()
    formatter.maximumFractionDigits = 1
    return formatter.format(value)
}

internal fun displayValue(
    metric: LiveMetric,
    point: LivePoint,
): String = "${formatNumber(point.value)} ${metric.unit}".trim()

internal fun observation(
    point: LivePoint,
    zone: ZoneId,
): String {
    val date = point.observedAt?.let { localTime(it, zone) } ?: point.day.toString()
    val writers =
        point.writers
            .sorted()
            .joinToString()
            .ifEmpty { "Writer unknown" }
    return "$date · $writers"
}
