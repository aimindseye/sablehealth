package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
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
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.github.aimindseye.sablehealth.core.LiveHealthPolicy
import io.github.aimindseye.sablehealth.core.LiveHour
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSeries
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStage
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import kotlin.reflect.KClass

private const val PAGE_SIZE = 200
private const val MAX_PAGES = 20
private const val METRIC_TIMEOUT_MS = 20_000L

/** Dedicated read-only boundary for live projections. No record persistence or logs. */
internal class LiveHealthReader(
    private val queries: LiveQueries,
    private val access: ConnectStatus,
    private val window: LiveWindow,
) {
    private val results = mutableMapOf<LiveMetric, LiveSeries>()

    suspend fun load(): LiveSnapshot {
        activity()
        vitals()
        val sleeps = sleep()
        exerciseSessions()
        val hours = stepHours()
        return LiveSnapshot(window, results.toMap(), sleeps, hours)
    }

    private suspend fun activity() {
        aggregate(LiveMetric.STEPS, StepsRecord::class, StepsRecord.COUNT_TOTAL) { it.toDouble() }
        aggregate(LiveMetric.DISTANCE, DistanceRecord::class, DistanceRecord.DISTANCE_TOTAL) { it.inKilometers }
        aggregate(
            LiveMetric.ACTIVE_CALORIES,
            ActiveCaloriesBurnedRecord::class,
            ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
        ) { it.inKilocalories }
        aggregate(LiveMetric.CALORIES, TotalCaloriesBurnedRecord::class, TotalCaloriesBurnedRecord.ENERGY_TOTAL) {
            it.inKilocalories
        }
        aggregate(
            LiveMetric.ACTIVE_MINUTES,
            ExerciseSessionRecord::class,
            ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
        ) { it.toMinutes().toDouble() }
    }

    private suspend fun vitals() {
        samples(LiveMetric.HEART_RATE, HeartRateRecord::class) { record ->
            record.samples.map { it.time to it.beatsPerMinute.toDouble() }
        }
        samples(LiveMetric.RESTING_HR, RestingHeartRateRecord::class) {
            listOf(it.time to it.beatsPerMinute.toDouble())
        }
        samples(LiveMetric.HRV, HeartRateVariabilityRmssdRecord::class) {
            listOf(it.time to it.heartRateVariabilityMillis)
        }
        samples(LiveMetric.RESPIRATORY_RATE, RespiratoryRateRecord::class) { listOf(it.time to it.rate) }
        samples(LiveMetric.SPO2, OxygenSaturationRecord::class) { listOf(it.time to it.percentage.value) }
        samples(LiveMetric.BODY_TEMPERATURE, BodyTemperatureRecord::class) {
            listOf(it.time to it.temperature.inCelsius)
        }
        samples(LiveMetric.SKIN_TEMPERATURE, SkinTemperatureRecord::class) { record ->
            record.deltas.map { it.time to it.delta.inCelsius }
        }
    }

    private fun unavailable(type: KClass<out Record>): LiveStatus? {
        val permission = HealthPermission.getReadPermission(type)
        return when {
            permission !in access.supported -> LiveStatus.UNSUPPORTED
            permission !in access.granted -> LiveStatus.DENIED
            else -> null
        }
    }

    private suspend fun series(
        metric: LiveMetric,
        type: KClass<out Record>,
        read: suspend () -> LiveSeries,
    ) {
        results[metric] = unavailable(type)?.let { LiveSeries(it) } ?: try {
            withTimeoutOrNull(METRIC_TIMEOUT_MS) { read() } ?: LiveSeries(LiveStatus.ERROR)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            LiveSeries(LiveStatus.DENIED)
        } catch (_: Exception) {
            LiveSeries(LiveStatus.ERROR)
        }
    }

    private suspend fun <T : Any> aggregate(
        metric: LiveMetric,
        type: KClass<out Record>,
        key: AggregateMetric<T>,
        convert: (T) -> Double,
    ) = series(metric, type) { queries.daily(key, window, convert) }

    private suspend fun stepHours(): List<LiveHour> =
        if (unavailable(StepsRecord::class) != null) {
            emptyList()
        } else {
            try {
                withTimeoutOrNull(METRIC_TIMEOUT_MS) { queries.stepHours(window) }.orEmpty()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }

    private suspend fun <T : Record> samples(
        metric: LiveMetric,
        type: KClass<T>,
        values: (T) -> List<Pair<Instant, Double>>,
    ) = series(metric, type) {
        val points = mutableMapOf<java.time.LocalDate, LivePoint>()
        val partial =
            readPages(queries, window, type, window.sleepStart) { record ->
                values(record)
                    .filter { (at, value) -> at >= window.start && at < window.now && value.isFinite() }
                    .forEach { (at, value) ->
                        val day = window.day(at)
                        val point = LivePoint(day, value, at, setOf(record.metadata.dataOrigin.packageName))
                        points[day] = LiveHealthPolicy.latestPerDay(listOfNotNull(points[day], point)).first()
                    }
            }
        val latest = points.values.sortedBy { it.day }
        LiveSeries(LiveHealthPolicy.status(latest.isNotEmpty(), partial), latest)
    }

    private suspend fun exerciseSessions() =
        series(LiveMetric.EXERCISE_SESSIONS, ExerciseSessionRecord::class) {
            val sessions = mutableListOf<LiveSleep>()
            val partial =
                readPages(queries, window, ExerciseSessionRecord::class, window.sleepStart) {
                    sessions.add(LiveSleep(it.startTime, it.endTime, it.metadata.dataOrigin.packageName, emptyList()))
                }
            val points =
                LiveHealthPolicy
                    .reconcileSleep(sessions, window)
                    .groupBy { window.day(it.end) }
                    .map { (day, records) ->
                        LivePoint(
                            day,
                            records.size.toDouble(),
                            records.maxOf { it.end },
                            records.map { it.writer }.toSet(),
                        )
                    }
            LiveSeries(LiveHealthPolicy.status(points.isNotEmpty(), partial), points)
        }

    private suspend fun sleep(): List<LiveSleep> {
        var selected = emptyList<LiveSleep>()
        series(LiveMetric.SLEEP_DURATION, SleepSessionRecord::class) {
            val sessions = mutableListOf<LiveSleep>()
            val partial =
                readPages(queries, window, SleepSessionRecord::class, window.sleepStart) { record ->
                    val stages = record.stages.map { LiveStage(it.startTime, it.endTime, stageName(it.stage)) }
                    sessions.add(
                        LiveSleep(record.startTime, record.endTime, record.metadata.dataOrigin.packageName, stages),
                    )
                }
            selected = LiveHealthPolicy.reconcileSleep(sessions, window)
            val points = LiveHealthPolicy.sleepPoints(selected, window)
            LiveSeries(LiveHealthPolicy.status(points.isNotEmpty(), partial), points)
        }
        return selected
    }
}

private suspend fun <T : Record> readPages(
    queries: LiveQueries,
    window: LiveWindow,
    type: KClass<T>,
    from: Instant,
    consume: (T) -> Unit,
): Boolean {
    var token: String? = null
    repeat(MAX_PAGES) {
        val response =
            queries.readPage(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(from, window.now),
                    ascendingOrder = false,
                    pageSize = PAGE_SIZE,
                    pageToken = token,
                ),
            )
        response.records.forEach(consume)
        token = response.pageToken
        if (token.isNullOrEmpty()) return false
    }
    return true
}

internal fun stageName(stage: Int): String =
    when (stage) {
        SleepSessionRecord.STAGE_TYPE_AWAKE -> "Awake"
        SleepSessionRecord.STAGE_TYPE_SLEEPING -> "Asleep (unspecified)"
        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "Out of bed"
        SleepSessionRecord.STAGE_TYPE_LIGHT -> "Light"
        SleepSessionRecord.STAGE_TYPE_DEEP -> "Deep"
        SleepSessionRecord.STAGE_TYPE_REM -> "REM"
        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> "Awake in bed"
        else -> "Unknown"
    }

internal suspend fun loadLiveHealth(
    context: Context,
    access: ConnectStatus,
): LiveSnapshot {
    val window = LiveWindow(Instant.now(), ZoneId.systemDefault())
    val client = HealthConnectClient.getOrCreate(context)
    val granted = client.permissionController.getGrantedPermissions().intersect(access.supported)
    val current = access.copy(granted = granted)
    val snapshot = LiveHealthReader(PlatformLiveQueries(client), current, window).load()
    check(client.permissionController.getGrantedPermissions().intersect(access.supported) == granted) {
        "Permissions changed during refresh"
    }
    return snapshot
}
