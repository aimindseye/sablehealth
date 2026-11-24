package io.github.aimindseye.sablehealth

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import io.github.aimindseye.sablehealth.core.LiveHour
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSeries
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveReaderCoverageTest {
    private val window = LiveWindow(Instant.parse("2026-11-24T21:00:00Z"), ZoneId.of("America/New_York"))
    private val catalog = M1HealthConnectProbe.metrics.map { it.permission }.toSet()
    private val steps = HealthPermission.getReadPermission(StepsRecord::class)
    private val sleep = HealthPermission.getReadPermission(SleepSessionRecord::class)

    @Test
    fun deniedAndUnsupportedNeverCallProvider() =
        runBlocking {
            val queries = TestQueries()
            val deniedAccess = ConnectStatus("Available", "test", 0, emptySet(), setOf(steps))
            val snapshot = LiveHealthReader(queries, deniedAccess, window).load()
            check(snapshot.metrics[LiveMetric.STEPS]?.status == LiveStatus.DENIED)
            check(snapshot.metrics[LiveMetric.SLEEP_DURATION]?.status == LiveStatus.UNSUPPORTED)
            check(queries.reads == 0 && queries.aggregates == 0)
        }

    @Test
    fun aggregatesAndSleepAreIndependentAndReplaceInsteadOfAccumulate() =
        runBlocking {
            val queries = TestQueries()
            val reader = LiveHealthReader(queries, access(setOf(steps, sleep)), window)
            val first = reader.load()
            check(first.metrics[LiveMetric.STEPS]?.on(window.today)?.value == 1234.0)
            check(first.metrics[LiveMetric.SLEEP_DURATION]?.status == LiveStatus.NO_RECORDS)
            check(first.stepHours.single().count == 100L)
            queries.sleeps = listOf(sleepRecord())
            val next = reader.load()
            check(next.metrics[LiveMetric.STEPS]?.on(window.today)?.value == 1234.0)
            check(
                next.sleeps
                    .single()
                    .stages
                    .single()
                    .type == "Deep",
            )
            check(next.metrics[LiveMetric.SLEEP_DURATION]?.on(window.today)?.value == 480.0)
            check(queries.reads == 2)
        }

    @Test
    fun partialAndFailedReadsDoNotInventRecords() =
        runBlocking {
            val queries = TestQueries()
            queries.repeatingPage = true
            val reader = LiveHealthReader(queries, access(setOf(sleep)), window)
            check(reader.load().metrics[LiveMetric.SLEEP_DURATION]?.status == LiveStatus.PARTIAL)
            queries.failure = SecurityException()
            check(reader.load().metrics[LiveMetric.SLEEP_DURATION]?.status == LiveStatus.DENIED)
            queries.failure = IllegalStateException()
            check(reader.load().metrics[LiveMetric.SLEEP_DURATION]?.status == LiveStatus.ERROR)
            queries.failure = CancellationException()
            check(runCatching { reader.load() }.exceptionOrNull() is CancellationException)
        }

    @Test
    fun allTypesAreRepresentedAndStagesHaveDistinctMeanings() =
        runBlocking {
            val data = LiveHealthReader(TestQueries(), access(catalog), window).load()
            check(data.metrics.keys == LiveMetric.entries.toSet())
            check(stageName(SleepSessionRecord.STAGE_TYPE_AWAKE) == "Awake")
            check(stageName(SleepSessionRecord.STAGE_TYPE_SLEEPING) == "Asleep (unspecified)")
            check(stageName(SleepSessionRecord.STAGE_TYPE_OUT_OF_BED) == "Out of bed")
            check(stageName(SleepSessionRecord.STAGE_TYPE_LIGHT) == "Light")
            check(stageName(SleepSessionRecord.STAGE_TYPE_DEEP) == "Deep")
            check(stageName(SleepSessionRecord.STAGE_TYPE_REM) == "REM")
            check(stageName(SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED) == "Awake in bed")
            check(stageName(-1) == "Unknown")
        }

    private fun access(granted: Set<String>): ConnectStatus =
        ConnectStatus(
            "Available",
            "test",
            granted.size,
            granted,
            catalog,
        )

    private fun sleepRecord(): SleepSessionRecord {
        val start = Instant.parse("2026-11-24T03:00:00Z")
        val end = Instant.parse("2026-11-24T11:00:00Z")
        return SleepSessionRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.ofHours(-5),
            endTime = end,
            endZoneOffset = ZoneOffset.ofHours(-5),
            metadata = Metadata.manualEntry(),
            stages = listOf(SleepSessionRecord.Stage(start, end, SleepSessionRecord.STAGE_TYPE_DEEP)),
        )
    }

    private class TestQueries : LiveQueries {
        var reads = 0
        var aggregates = 0
        var sleeps = emptyList<SleepSessionRecord>()
        var repeatingPage = false
        var failure: Exception? = null

        override suspend fun <T : Record> readPage(request: ReadRecordsRequest<T>): LiveRecordPage<T> {
            reads++
            failure?.let { throw it }
            @Suppress("UNCHECKED_CAST") // fixture cast after exact record type check
            val records = if (request.recordType == SleepSessionRecord::class) sleeps as List<T> else emptyList()
            return LiveRecordPage(records, if (repeatingPage) "next" else null)
        }

        override suspend fun <T : Any> daily(
            key: AggregateMetric<T>,
            window: LiveWindow,
            convert: (T) -> Double,
        ): LiveSeries {
            aggregates++
            return if (key == StepsRecord.COUNT_TOTAL) {
                @Suppress("UNCHECKED_CAST") // exact aggregate key is Long
                val value = convert(1234L as T)
                LiveSeries(LiveStatus.AVAILABLE, listOf(LivePoint(window.today, value, null, setOf("fixture.writer"))))
            } else {
                LiveSeries(LiveStatus.NO_RECORDS)
            }
        }

        override suspend fun stepHours(window: LiveWindow): List<LiveHour> = listOf(LiveHour(window.todayStart, 100))
    }
}
