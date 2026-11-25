package io.github.aimindseye.sablehealth.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Transient projections only. No Health Connect record IDs or raw payloads. */
enum class LiveMetric(
    val label: String,
    val unit: String,
) {
    STEPS("Steps", ""),
    DISTANCE("Distance", "km"),
    ACTIVE_CALORIES("Active calories", "kcal"),
    CALORIES("Total calories", "kcal"),
    ACTIVE_MINUTES("Recorded exercise", "min"),
    EXERCISE_SESSIONS("Exercise sessions", ""),
    SLEEP_DURATION("Sleep session time", "min"),
    HEART_RATE("Heart rate", "bpm"),
    RESTING_HR("Resting heart rate", "bpm"),
    HRV("HRV (RMSSD)", "ms"),
    RESPIRATORY_RATE("Respiratory rate", "breaths/min"),
    SPO2("SpO₂", "%"),
    BODY_TEMPERATURE("Body temperature", "°C"),
    SKIN_TEMPERATURE("Skin temperature delta", "°C"),
}

enum class LiveStatus(
    val label: String,
) {
    AVAILABLE("Available"),
    PARTIAL("Partial history"),
    NO_RECORDS("No records in window"),
    DENIED("Read permission needed"),
    UNSUPPORTED("Unsupported on device"),
    ERROR("Read failed · retry"),
}

data class LivePoint(
    val day: LocalDate,
    val value: Double,
    val observedAt: Instant?,
    val writers: Set<String>,
)

data class LiveSeries(
    val status: LiveStatus,
    val points: List<LivePoint> = emptyList(),
) {
    fun on(day: LocalDate): LivePoint? = points.firstOrNull { it.day == day }
}

data class LiveWindow(
    val now: Instant,
    val zone: ZoneId,
) {
    val today: LocalDate get() = now.atZone(zone).toLocalDate()
    val firstDay: LocalDate get() = today.minusDays(HISTORY_DAYS - 1)
    val start: Instant get() = firstDay.atStartOfDay(zone).toInstant()
    val todayStart: Instant get() = today.atStartOfDay(zone).toInstant()
    val sleepStart: Instant get() = maxOf(start.minus(Duration.ofDays(1)), now.minus(Duration.ofDays(HISTORY_DAYS)))

    fun day(at: Instant): LocalDate = at.atZone(zone).toLocalDate()

    companion object {
        const val HISTORY_DAYS = 30L
    }
}

data class LiveStage(
    val start: Instant,
    val end: Instant,
    val type: String,
)

data class LiveSleep(
    val start: Instant,
    val end: Instant,
    val writer: String,
    val stages: List<LiveStage>,
) {
    val minutes: Double get() = Duration.between(start, end).seconds / SECONDS_PER_MINUTE

    companion object {
        private const val SECONDS_PER_MINUTE = 60.0
    }
}

object LiveHealthPolicy {
    private const val RECENT_VITAL_DAYS = 7L

    /** Prefer a longer complete session if writers overlap. Never sum competing intervals. */
    fun reconcileSleep(
        sessions: List<LiveSleep>,
        window: LiveWindow,
    ): List<LiveSleep> {
        val selected = mutableListOf<LiveSleep>()
        sessions
            .filter { it.start < it.end && it.end <= window.now && window.day(it.end) >= window.firstDay }
            .sortedWith(
                compareByDescending<LiveSleep> { it.minutes }
                    .thenBy { it.writer }
                    .thenBy { it.start },
            ).forEach { candidate ->
                if (selected.none { it.start < candidate.end && candidate.start < it.end }) selected.add(candidate)
            }
        return selected.sortedBy { it.end }
    }

    /** Latest observed value per local calendar day. No invented zeroes or interpolated gaps. */
    fun latestPerDay(points: List<LivePoint>): List<LivePoint> =
        points
            .filter { it.value.isFinite() && it.observedAt != null }
            .groupBy { it.day }
            .values
            .mapNotNull { day ->
                day.maxWithOrNull(
                    compareBy<LivePoint> { it.observedAt }
                        .thenBy { it.writers.sorted().joinToString() },
                )
            }.sortedBy { it.day }

    fun status(
        hasRecords: Boolean,
        partial: Boolean,
    ): LiveStatus =
        when {
            partial -> LiveStatus.PARTIAL
            hasRecords -> LiveStatus.AVAILABLE
            else -> LiveStatus.NO_RECORDS
        }

    fun sleepPoints(
        sessions: List<LiveSleep>,
        window: LiveWindow,
    ): List<LivePoint> =
        sessions
            .groupBy { window.day(it.end) }
            .map { (day, records) ->
                LivePoint(
                    day,
                    records.sumOf { it.minutes },
                    records.maxOf { it.end },
                    records.map { it.writer }.toSet(),
                )
            }.sortedBy { it.day }

    fun dashboardPoint(
        snapshot: LiveSnapshot,
        metric: LiveMetric,
    ): LivePoint? {
        val series = snapshot.metrics[metric] ?: return null
        val recentDays =
            when (metric) {
                LiveMetric.HEART_RATE -> 1L
                LiveMetric.RESTING_HR, LiveMetric.HRV -> RECENT_VITAL_DAYS
                else -> 0L
            }
        return series.on(snapshot.window.today) ?: series.points.lastOrNull { point ->
            recentDays > 0 &&
                point.observedAt?.let {
                    it >= snapshot.window.now.minus(Duration.ofDays(recentDays))
                } == true
        }
    }
}

data class LiveSnapshot(
    val window: LiveWindow,
    val metrics: Map<LiveMetric, LiveSeries>,
    val sleeps: List<LiveSleep>,
    val stepHours: List<LiveHour> = emptyList(),
)

data class LiveHour(
    val start: Instant,
    val count: Long,
)
