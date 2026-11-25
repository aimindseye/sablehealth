package io.github.aimindseye.sablehealth

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveMeasurementRecordsTest {
    private val window = LiveWindow(Instant.parse("2026-11-24T21:00:00Z"), ZoneId.of("America/New_York"))
    private val catalog = M1HealthConnectProbe.metrics.map { it.permission }.toSet()
    private val access = ConnectStatus("Available", "fixture", catalog.size, catalog, catalog)
    private val at = window.now.minusSeconds(1800)
    private val zone = ZoneOffset.ofHours(-5)

    @Test
    fun realRecordConversionsLatestSampleAndAggregates() =
        runBlocking {
            val queries = RecordsFixture(records())
            val data = LiveHealthReader(queries, access, window).load()
            val expected =
                mapOf(
                    LiveMetric.STEPS to 200.0,
                    LiveMetric.DISTANCE to 2.5,
                    LiveMetric.ACTIVE_CALORIES to 120.0,
                    LiveMetric.CALORIES to 300.0,
                    LiveMetric.ACTIVE_MINUTES to 45.0,
                    LiveMetric.EXERCISE_SESSIONS to 1.0,
                    LiveMetric.HEART_RATE to 78.0,
                    LiveMetric.RESTING_HR to 62.0,
                    LiveMetric.HRV to 40.0,
                    LiveMetric.RESPIRATORY_RATE to 14.0,
                    LiveMetric.SPO2 to 97.0,
                    LiveMetric.BODY_TEMPERATURE to 36.5,
                    LiveMetric.SKIN_TEMPERATURE to -0.5,
                )
            expected.forEach { (metric, value) ->
                val series = checkNotNull(data.metrics[metric])
                check(series.status == LiveStatus.AVAILABLE) { "$metric: ${series.status}" }
                val point = checkNotNull(series.on(window.today)) { "Missing $metric" }
                check(kotlin.math.abs(point.value - value) < 0.0001) { "$metric: ${point.value}" }
            }
            check(data.metrics[LiveMetric.HEART_RATE]?.on(window.today)?.observedAt == at)
            check(data.metrics[LiveMetric.SLEEP_DURATION]?.status == LiveStatus.NO_RECORDS)
        }

    @Test
    fun hourlyFailureDoesNotDiscardDailyValuesAndCancellationPropagates() =
        runBlocking {
            val queries = RecordsFixture(emptyList())
            queries.hourFailure = IllegalStateException("fixture")
            val data = LiveHealthReader(queries, access, window).load()
            check(data.stepHours.isEmpty())
            check(data.metrics[LiveMetric.STEPS]?.on(window.today)?.value == 200.0)
            queries.hourFailure = CancellationException()
            val failure = runCatching { LiveHealthReader(queries, access, window).load() }
            check(failure.exceptionOrNull() is CancellationException)
        }

    private fun records(): List<Record> =
        listOf(
            HeartRateRecord(
                startTime = at.minusSeconds(600),
                startZoneOffset = zone,
                endTime = at.plusSeconds(60),
                endZoneOffset = zone,
                samples = listOf(HeartRateRecord.Sample(at.minusSeconds(120), 70), HeartRateRecord.Sample(at, 78)),
                metadata = Metadata.manualEntry(),
            ),
            RestingHeartRateRecord(
                time = at,
                zoneOffset = zone,
                beatsPerMinute = 62,
                metadata = Metadata.manualEntry(),
            ),
            HeartRateVariabilityRmssdRecord(
                time = at,
                zoneOffset = zone,
                heartRateVariabilityMillis = 40.0,
                metadata = Metadata.manualEntry(),
            ),
            RespiratoryRateRecord(time = at, zoneOffset = zone, rate = 14.0, metadata = Metadata.manualEntry()),
            OxygenSaturationRecord(
                time = at,
                zoneOffset = zone,
                percentage = Percentage(97.0),
                metadata = Metadata.manualEntry(),
            ),
            BodyTemperatureRecord(
                time = at,
                zoneOffset = zone,
                temperature = Temperature.celsius(36.5),
                metadata = Metadata.manualEntry(),
            ),
            SkinTemperatureRecord(
                startTime = at.minusSeconds(600),
                startZoneOffset = zone,
                endTime = at.plusSeconds(60),
                endZoneOffset = zone,
                deltas = listOf(SkinTemperatureRecord.Delta(at, TemperatureDelta.celsius(-0.5))),
                metadata = Metadata.manualEntry(),
            ),
            exercise(at.minusSeconds(2700), at),
            exercise(at.minusSeconds(1200), at.minusSeconds(600)),
        )

    private fun exercise(
        start: Instant,
        end: Instant,
    ): ExerciseSessionRecord =
        ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zone,
            endTime = end,
            endZoneOffset = zone,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            metadata = Metadata.manualEntry(),
        )

    private class RecordsFixture(
        private val records: List<Record>,
    ) : LiveQueries {
        var hourFailure: Exception? = null

        override suspend fun <T : Record> readPage(request: ReadRecordsRequest<T>): LiveRecordPage<T> {
            val matching = records.filter { it::class == request.recordType }
            @Suppress("UNCHECKED_CAST") // Record class equality checked above.
            return LiveRecordPage(matching as List<T>, null)
        }

        override suspend fun <T : Any> daily(
            key: AggregateMetric<T>,
            window: LiveWindow,
            convert: (T) -> Double,
        ): LiveSeries {
            val value =
                when (key) {
                    StepsRecord.COUNT_TOTAL -> 200L
                    DistanceRecord.DISTANCE_TOTAL -> Length.kilometers(2.5)
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL -> Energy.kilocalories(120.0)
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL -> Energy.kilocalories(300.0)
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL -> Duration.ofMinutes(45)
                    else -> error("Unexpected aggregate")
                }

            @Suppress("UNCHECKED_CAST") // Each value matches its exact aggregate key.
            val converted = convert(value as T)
            val point = LivePoint(window.today, converted, null, setOf("fixture.writer"))
            return LiveSeries(LiveStatus.AVAILABLE, listOf(point))
        }

        override suspend fun stepHours(window: LiveWindow): List<LiveHour> {
            hourFailure?.let { throw it }
            return listOf(LiveHour(window.todayStart, 200))
        }
    }
}
