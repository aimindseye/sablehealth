package io.github.aimindseye.sablehealth.core

import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LiveHealthTest {
    private val zone = ZoneId.of("America/New_York")
    private val window = LiveWindow(Instant.parse("2026-11-24T21:02:17Z"), zone)

    @Test
    fun localCalendarWindowAndDst() {
        check(window.today == LocalDate.parse("2026-11-24"))
        check(window.firstDay == LocalDate.parse("2026-10-26"))
        check(window.todayStart == Instant.parse("2026-11-24T05:00:00Z"))
        val autumn = LiveWindow(Instant.parse("2026-11-02T00:00:00Z"), zone)
        val spring = LiveWindow(Instant.parse("2026-03-09T00:00:00Z"), zone)
        val autumnNext =
            autumn.today
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
        check(Duration.between(autumn.todayStart, autumnNext).toHours() == 25L)
        val springNext =
            spring.today
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
        check(Duration.between(spring.todayStart, springNext).toHours() == 23L)
        check(window.sleepStart >= window.now.minus(Duration.ofDays(30)))
    }

    @Test
    fun overnightSessionsUseWakeDateAndDoNotDoubleCount() {
        val overnight = sleep("2026-11-24T03:00:00Z", "2026-11-24T11:00:00Z", "writer.a")
        val duplicate = sleep("2026-11-24T04:00:00Z", "2026-11-24T11:00:00Z", "writer.b")
        val nap = sleep("2026-11-24T16:00:00Z", "2026-11-24T16:30:00Z", "writer.a")
        val future = sleep("2026-11-24T21:00:00Z", "2026-11-24T22:00:00Z", "writer.a")
        val old = sleep("2026-10-20T02:00:00Z", "2026-10-20T10:00:00Z", "writer.a")
        val invalid = sleep("2026-11-24T18:00:00Z", "2026-11-24T17:00:00Z", "writer.a")
        val selected = LiveHealthPolicy.reconcileSleep(listOf(nap, future, duplicate, overnight, old, invalid), window)
        check(selected == listOf(overnight, nap))
        val points = LiveHealthPolicy.sleepPoints(selected, window)
        check(points.single().day == window.today)
        check(points.single().value == 510.0)
        check(points.single().writers == setOf("writer.a"))
    }

    @Test
    fun adjoiningSessionsAndDeterministicOverlapTie() {
        val first = sleep("2026-11-24T02:00:00Z", "2026-11-24T05:00:00Z", "a")
        val other = first.copy(writer = "z")
        val next = sleep("2026-11-24T05:00:00Z", "2026-11-24T07:00:00Z", "b")
        check(LiveHealthPolicy.reconcileSleep(listOf(other, next, first), window) == listOf(first, next))
        check(LiveHealthPolicy.reconcileSleep(emptyList(), window).isEmpty())
        check(LiveHealthPolicy.sleepPoints(emptyList(), window).isEmpty())
    }

    @Test
    fun latestObservedValueAndMissingDayAreDistinct() {
        val early = point("2026-11-24T10:00:00Z", 61.0)
        val late = point("2026-11-24T20:00:00Z", 74.0)
        val old = point("2026-11-22T20:00:00Z", 71.0)
        val bad = late.copy(value = Double.NaN)
        val absent = late.copy(observedAt = null)
        val results = LiveHealthPolicy.latestPerDay(listOf(late, old, early, bad, absent))
        check(results == listOf(old, late))
        val series = LiveSeries(LiveStatus.AVAILABLE, results)
        check(series.on(window.today) == late)
        check(series.on(window.today.minusDays(1)) == null)
        check(LiveHealthPolicy.status(true, false) == LiveStatus.AVAILABLE)
        check(LiveHealthPolicy.status(false, false) == LiveStatus.NO_RECORDS)
        check(LiveHealthPolicy.status(false, true) == LiveStatus.PARTIAL)
    }

    @Test
    fun dashboardNeverReusesYesterdaysStepsAndLabelsRecentVitals() {
        val prior = point("2026-11-23T22:00:00Z", 42.0)
        val old = point("2026-11-17T19:00:00Z", 45.0)
        val latest = LiveSeries(LiveStatus.AVAILABLE, listOf(prior))
        val snapshot =
            LiveSnapshot(
                window,
                mapOf(LiveMetric.STEPS to latest, LiveMetric.HEART_RATE to latest, LiveMetric.HRV to latest),
                emptyList(),
            )
        check(LiveHealthPolicy.dashboardPoint(snapshot, LiveMetric.STEPS) == null)
        check(LiveHealthPolicy.dashboardPoint(snapshot, LiveMetric.HEART_RATE) == prior)
        check(LiveHealthPolicy.dashboardPoint(snapshot, LiveMetric.HRV) == prior)
        check(LiveHealthPolicy.dashboardPoint(snapshot, LiveMetric.DISTANCE) == null)
        val stale = snapshot.copy(metrics = mapOf(LiveMetric.HRV to LiveSeries(LiveStatus.AVAILABLE, listOf(old))))
        check(LiveHealthPolicy.dashboardPoint(stale, LiveMetric.HRV) == null)
    }

    private fun sleep(
        start: String,
        end: String,
        writer: String,
    ): LiveSleep = LiveSleep(Instant.parse(start), Instant.parse(end), writer, emptyList())

    private fun point(
        at: String,
        value: Double,
    ): LivePoint {
        val instant = Instant.parse(at)
        return LivePoint(window.day(instant), value, instant, setOf("writer"))
    }
}
