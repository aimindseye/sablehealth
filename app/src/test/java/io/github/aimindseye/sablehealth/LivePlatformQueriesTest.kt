package io.github.aimindseye.sablehealth

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.testing.stubs.buildStub
import androidx.health.connect.client.testing.stubs.stub
import androidx.health.connect.client.time.TimeRangeFilter
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LivePlatformQueriesTest {
    private val window = LiveWindow(Instant.parse("2026-11-24T21:00:00Z"), ZoneId.of("America/New_York"))
    private val key = StepsRecord.COUNT_TOTAL
    private val writer = DataOrigin("fixture.writer")

    @Test
    fun dailyAggregatesKeepRealZeroAndGapsAndUseCorrectWindows() =
        runBlocking {
            val fake = FakeHealthConnectClient()
            fake.overrides.aggregate =
                buildStub {
                    defaultHandler = { request ->
                        assertEquals(
                            AggregateRequest(setOf(key), TimeRangeFilter.between(window.todayStart, window.now)),
                            request,
                        )
                        aggregate(0)
                    }
                }
            val from = window.firstDay.atStartOfDay()
            val until = window.today.atStartOfDay()
            fake.overrides.aggregateGroupByPeriod =
                buildStub {
                    defaultHandler = { request ->
                        assertEquals(
                            AggregateGroupByPeriodRequest(
                                metrics = setOf(key),
                                timeRangeFilter = TimeRangeFilter.between(from, until),
                                timeRangeSlicer = Period.ofDays(1),
                            ),
                            request,
                        )
                        listOf(period(from, 300), period(from.plusDays(1), null))
                    }
                }
            val series = PlatformLiveQueries(fake).daily(key, window, Long::toDouble)
            check(series.status == LiveStatus.AVAILABLE)
            check(series.points.size == 2)
            check(series.on(window.today)?.value == 0.0)
            check(series.on(window.firstDay)?.value == 300.0)
            check(series.on(window.firstDay.plusDays(1)) == null)
            check(series.points.all { it.writers == setOf(writer.packageName) && it.observedAt == null })
        }

    @Test
    fun independentFailuresAndEmptyResultsPreserveMeaning() =
        runBlocking {
            for (failed in listOf("aggregate", "aggregateGroupByPeriod", "both", "none")) {
                val fake = FakeHealthConnectClient()
                fake.overrides.aggregate =
                    stub {
                        if (failed == "aggregate" || failed == "both") error("fixture")
                        aggregate(null)
                    }
                fake.overrides.aggregateGroupByPeriod =
                    stub {
                        if (failed == "aggregateGroupByPeriod" || failed == "both") error("fixture")
                        emptyList<AggregationResultGroupedByPeriod>()
                    }
                val expected =
                    when (failed) {
                        "both" -> LiveStatus.ERROR
                        "none" -> LiveStatus.NO_RECORDS
                        else -> LiveStatus.PARTIAL
                    }
                val series = PlatformLiveQueries(fake).daily(key, window, Long::toDouble)
                check(series.status == expected && series.points.isEmpty())
            }
            for (failure in listOf(SecurityException(), CancellationException())) {
                val fake = FakeHealthConnectClient()
                fake.overrides.aggregate = stub { throw failure }
                val queries = PlatformLiveQueries(fake)
                check(runCatching { queries.daily(key, window, Long::toDouble) }.exceptionOrNull() === failure)
            }
        }

    @Test
    fun hourlyBucketsAndRecordPagePreserveProviderPayload() =
        runBlocking {
            val start = window.todayStart
            val fake = FakeHealthConnectClient()
            // Only the official in-memory fake receives synthetic records.
            fake.insertRecords(listOf(steps(start, 20), steps(start.plusSeconds(120), 30)))
            val queries = PlatformLiveQueries(fake)
            val range = TimeRangeFilter.between(start, window.now)
            val first = queries.readPage(ReadRecordsRequest(StepsRecord::class, range, pageSize = 1))
            val token = requireNotNull(first.pageToken)
            val second =
                queries.readPage(ReadRecordsRequest(StepsRecord::class, range, pageSize = 1, pageToken = token))
            assertEquals(1, first.records.size)
            assertEquals(1, second.records.size)
            assertEquals(setOf(20L, 30L), (first.records + second.records).map { it.count }.toSet())
            assertEquals(null, second.pageToken)
            fake.overrides.aggregateGroupByDuration =
                buildStub {
                    defaultHandler = { request ->
                        assertEquals(
                            AggregateGroupByDurationRequest(
                                metrics = setOf(key),
                                timeRangeFilter = range,
                                timeRangeSlicer = Duration.ofHours(1),
                            ),
                            request,
                        )
                        listOf(hour(start, 0), hour(start.plusSeconds(3600), null))
                    }
                }
            val hours = queries.stepHours(window)
            check(hours.single().start == start && hours.single().count == 0L)
        }

    @Test
    fun exactMidnightDoesNotSubmitEmptyInterval() =
        runBlocking {
            val midnight = window.copy(now = window.todayStart)
            val fake = FakeHealthConnectClient()
            fake.overrides.aggregate = stub { throw AssertionError("Empty daily request") }
            fake.overrides.aggregateGroupByDuration = stub { throw AssertionError("Empty hourly request") }
            fake.overrides.aggregateGroupByPeriod =
                stub { listOf(period(midnight.firstDay.atStartOfDay(), 120)) }
            val queries = PlatformLiveQueries(fake)
            check(queries.stepHours(midnight).isEmpty())
            val series = queries.daily(key, midnight, Long::toDouble)
            check(series.on(midnight.today) == null && series.points.single().value == 120.0)
        }

    private fun steps(
        start: Instant,
        count: Long,
    ): StepsRecord =
        StepsRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = start.plusSeconds(60),
            endZoneOffset = ZoneOffset.UTC,
            count = count,
            metadata = Metadata.manualEntry(),
        )

    private fun aggregate(value: Long?): AggregationResult =
        androidx.health.connect.client.testing.AggregationResult(
            dataOrigins = setOf(writer),
            metrics = if (value == null) emptyMap() else mapOf(key to value),
        )

    private fun period(
        start: LocalDateTime,
        value: Long?,
    ): AggregationResultGroupedByPeriod = AggregationResultGroupedByPeriod(aggregate(value), start, start.plusDays(1))

    private fun hour(
        start: Instant,
        value: Long?,
    ): AggregationResultGroupedByDuration =
        AggregationResultGroupedByDuration(aggregate(value), start, start.plusSeconds(3600), ZoneOffset.UTC)
}
