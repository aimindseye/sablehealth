package io.github.aimindseye.sablehealth.core

import java.time.Duration
import java.time.Instant

/** UI preferences and transient plot geometry; never store health readings. */
enum class ChartKind(
    val label: String,
) {
    BAR("Bar"),
    LINE("Line"),
    AREA("Area"),
    POINTS("Points"),
}

object ChartPolicy {
    private val activity =
        setOf(
            LiveMetric.STEPS,
            LiveMetric.DISTANCE,
            LiveMetric.ACTIVE_CALORIES,
            LiveMetric.CALORIES,
            LiveMetric.ACTIVE_MINUTES,
            LiveMetric.EXERCISE_SESSIONS,
            LiveMetric.SLEEP_DURATION,
        )

    fun options(metric: LiveMetric): List<ChartKind> =
        if (metric in activity) {
            listOf(ChartKind.BAR, ChartKind.LINE, ChartKind.AREA, ChartKind.POINTS)
        } else if (metric == LiveMetric.SKIN_TEMPERATURE) {
            listOf(ChartKind.BAR, ChartKind.LINE, ChartKind.POINTS)
        } else {
            listOf(ChartKind.LINE, ChartKind.POINTS)
        }

    fun restore(
        metric: LiveMetric,
        saved: String?,
    ): ChartKind = options(metric).firstOrNull { it.name == saved } ?: options(metric).first()

    /** Bar/area scales include zero, and constant series have a nonzero extent. */
    fun bounds(
        values: List<Double?>,
        kind: ChartKind,
    ): ClosedFloatingPointRange<Double> {
        val observed = values.filterNotNull().filter(Double::isFinite)
        if (observed.isEmpty()) return 0.0..1.0
        val low = observed.min()
        val high = observed.max()
        val includeZero = kind == ChartKind.BAR || kind == ChartKind.AREA
        val bottom = if (includeZero) minOf(low, 0.0) else low
        val top = if (includeZero) maxOf(high, 0.0) else high
        return if (bottom == top) bottom..(top + 1.0) else bottom..top
    }

    fun indexAt(
        fraction: Float,
        count: Int,
    ): Int {
        require(count > 0)
        return (fraction.coerceIn(0f, 1f) * count).toInt().coerceAtMost(count - 1)
    }
}

/** Reject ambiguous overlapping stages rather than invent a transition or double-count time. */
object SleepPlotPolicy {
    fun intervals(session: LiveSleep): List<LiveStage> {
        val valid =
            session.stages
                .filter { it.start >= session.start && it.end <= session.end && it.start < it.end }
                .sortedBy { it.start }
        return valid.filter { candidate ->
            valid.count { it.start < candidate.end && candidate.start < it.end } == 1
        }
    }

    fun fraction(
        session: LiveSleep,
        instant: Instant,
    ): Float {
        val total = Duration.between(session.start, session.end).toMillis().coerceAtLeast(1)
        return (Duration.between(session.start, instant).toMillis().toDouble() / total).toFloat().coerceIn(0f, 1f)
    }

    fun contiguous(
        previous: LiveStage,
        next: LiveStage,
    ): Boolean = previous.end == next.start

    fun secondsByStage(session: LiveSleep): Map<String, Long> =
        intervals(session).groupBy { it.type }.mapValues { (_, stages) ->
            stages.sumOf { Duration.between(it.start, it.end).seconds }
        }
}
