package io.github.aimindseye.sablehealth

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.github.aimindseye.sablehealth.core.LiveHealthPolicy
import io.github.aimindseye.sablehealth.core.LiveHour
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSeries
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Period

internal data class LiveRecordPage<T : Record>(
    val records: List<T>,
    val pageToken: String?,
)

/** Test seam. Only the production adapter performs Android record or aggregate requests. */
internal interface LiveQueries {
    suspend fun <T : Record> readPage(request: ReadRecordsRequest<T>): LiveRecordPage<T>

    suspend fun <T : Any> daily(
        key: AggregateMetric<T>,
        window: LiveWindow,
        convert: (T) -> Double,
    ): LiveSeries

    suspend fun stepHours(window: LiveWindow): List<LiveHour>
}

internal class PlatformLiveQueries(
    private val client: HealthConnectClient,
) : LiveQueries {
    override suspend fun <T : Record> readPage(request: ReadRecordsRequest<T>): LiveRecordPage<T> {
        val response = client.readRecords(request)
        return LiveRecordPage(response.records, response.pageToken)
    }

    override suspend fun <T : Any> daily(
        key: AggregateMetric<T>,
        window: LiveWindow,
        convert: (T) -> Double,
    ): LiveSeries {
        // History follows provider-local periods; the current day uses exact device midnight.
        val today = attempt { today(key, window, convert) }
        val history = attempt { history(key, window, convert) }
        if (today == null && history == null) return LiveSeries(LiveStatus.ERROR)
        val points = history.orEmpty() + today.orEmpty()
        return LiveSeries(LiveHealthPolicy.status(points.isNotEmpty(), today == null || history == null), points)
    }

    private suspend fun <T : Any> today(
        key: AggregateMetric<T>,
        window: LiveWindow,
        convert: (T) -> Double,
    ): List<LivePoint> {
        if (window.todayStart >= window.now) return emptyList()
        val result =
            client.aggregate(
                AggregateRequest(setOf(key), TimeRangeFilter.between(window.todayStart, window.now)),
            )
        return listOfNotNull(
            result[key]?.let {
                LivePoint(
                    window.today,
                    convert(it),
                    null,
                    result.dataOrigins.map { origin -> origin.packageName }.toSet(),
                )
            },
        )
    }

    private suspend fun <T : Any> history(
        key: AggregateMetric<T>,
        window: LiveWindow,
        convert: (T) -> Double,
    ): List<LivePoint> =
        client
            .aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(key),
                    timeRangeFilter =
                        TimeRangeFilter.between(
                            window.firstDay.atStartOfDay(),
                            window.today.atStartOfDay(),
                        ),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            ).mapNotNull { row ->
                row.result[key]?.let {
                    LivePoint(
                        row.startTime.toLocalDate(),
                        convert(it),
                        null,
                        row.result.dataOrigins
                            .map { origin -> origin.packageName }
                            .toSet(),
                    )
                }
            }

    override suspend fun stepHours(window: LiveWindow): List<LiveHour> {
        if (window.todayStart >= window.now) return emptyList()
        return client
            .aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(window.todayStart, window.now),
                    timeRangeSlicer = Duration.ofHours(1),
                ),
            ).mapNotNull { row -> row.result[StepsRecord.COUNT_TOTAL]?.let { LiveHour(row.startTime, it) } }
    }
}

private suspend fun <T> attempt(read: suspend () -> T): T? =
    try {
        read()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (denied: SecurityException) {
        throw denied
    } catch (_: Exception) {
        null
    }
