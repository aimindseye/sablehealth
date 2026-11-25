package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
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
import io.github.aimindseye.sablehealth.core.CoverageState
import io.github.aimindseye.sablehealth.core.HealthCoverageContract
import io.github.aimindseye.sablehealth.core.HealthMetricCoverage
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * Foreground, user-triggered, read-only coverage check. Reads are transient:
 * only record count, UTC timestamps and writer package names reach Compose state.
 * Never access measurement values, IDs, device identifiers or record payload logs.
 */
internal object M1HealthConnectProbe {
    private const val RECORDS_PER_PAGE = 100

    internal data class RecordBounds(
        val start: Instant,
        val end: Instant,
        val origin: String,
    )

    internal data class MetadataPage(
        val records: List<RecordBounds>,
        val pageToken: String?,
    )

    internal interface Reader {
        suspend fun granted(): Set<String>

        suspend fun <T : Record> read(
            request: ReadRecordsRequest<T>,
            times: (T) -> Pair<Instant, Instant>,
        ): MetadataPage
    }

    private class PlatformReader(
        private val client: HealthConnectClient,
    ) : Reader {
        override suspend fun granted(): Set<String> = client.permissionController.getGrantedPermissions()

        override suspend fun <T : Record> read(
            request: ReadRecordsRequest<T>,
            times: (T) -> Pair<Instant, Instant>,
        ): MetadataPage {
            val response = client.readRecords(request)
            return MetadataPage(
                response.records.map { record ->
                    val (start, end) = times(record)
                    RecordBounds(start, end, record.metadata.dataOrigin.packageName)
                },
                response.pageToken,
            )
        }
    }

    internal class Metric(
        val name: String,
        val permission: String,
        val inspect: suspend (Reader, Instant, Long) -> HealthMetricCoverage,
    )

    private fun <T : Record> metric(
        label: String,
        type: KClass<T>,
        times: (T) -> Pair<Instant, Instant>,
    ): Metric {
        val permission = HealthPermission.getReadPermission(type)
        return Metric(label, permission) { client, time, days ->
            readCoverage(client, type, label, ScanPeriod(time, days), times)
        }
    }

    val metrics: List<Metric> =
        listOf(
            metric("Steps", StepsRecord::class) { it.startTime to it.endTime },
            metric("Sleep sessions", SleepSessionRecord::class) { it.startTime to it.endTime },
            metric("Heart rate", HeartRateRecord::class) { it.startTime to it.endTime },
            metric("HRV (RMSSD)", HeartRateVariabilityRmssdRecord::class) { it.time to it.time },
            metric("Resting heart rate", RestingHeartRateRecord::class) { it.time to it.time },
            metric("Exercise sessions", ExerciseSessionRecord::class) { it.startTime to it.endTime },
            metric("Respiratory rate", RespiratoryRateRecord::class) { it.time to it.time },
            metric("Oxygen saturation", OxygenSaturationRecord::class) { it.time to it.time },
            metric("Body temperature", BodyTemperatureRecord::class) { it.time to it.time },
            metric("Skin temperature", SkinTemperatureRecord::class) { it.startTime to it.endTime },
            metric("Active calories", ActiveCaloriesBurnedRecord::class) { it.startTime to it.endTime },
            metric("Distance", DistanceRecord::class) { it.startTime to it.endTime },
            metric("Total calories", TotalCaloriesBurnedRecord::class) { it.startTime to it.endTime },
        )

    private val availableStatus = HealthConnectClient.SDK_AVAILABLE

    fun platformAvailable(context: Context): Boolean = HealthConnectClient.getSdkStatus(context) == availableStatus

    suspend fun scan(
        context: Context,
        at: Instant = Instant.now(),
        windowDays: Long = HealthCoverageContract.WINDOW_DAYS,
    ): List<HealthMetricCoverage> {
        check(platformAvailable(context)) { "Health Connect unavailable" }
        return scan(PlatformReader(HealthConnectClient.getOrCreate(context)), at, windowDays)
    }

    internal suspend fun scan(
        client: Reader,
        at: Instant,
        windowDays: Long = HealthCoverageContract.WINDOW_DAYS,
    ): List<HealthMetricCoverage> {
        require(windowDays in HealthCoverageContract.SCAN_WINDOWS)
        val granted = client.granted()
        return metrics.map { metric ->
            if (metric.permission !in granted) {
                HealthMetricCoverage(metric.name, metric.permission, CoverageState.NOT_GRANTED)
            } else {
                try {
                    metric.inspect(client, at, windowDays)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    HealthMetricCoverage(metric.name, metric.permission, CoverageState.NOT_GRANTED)
                } catch (_: Exception) {
                    HealthMetricCoverage(metric.name, metric.permission, CoverageState.READ_ERROR)
                }
            }
        }
    }

    private data class ScanPeriod(
        val now: Instant,
        val days: Long,
    )

    private suspend fun <T : Record> readCoverage(
        client: Reader,
        type: KClass<T>,
        label: String,
        period: ScanPeriod,
        times: (T) -> Pair<Instant, Instant>,
    ): HealthMetricCoverage {
        val now = period.now
        val from = now.minus(period.days, ChronoUnit.DAYS)
        var token: String? = null
        var count = 0
        var first: Instant? = null
        var last: Instant? = null
        var limited = false
        val origins = sortedSetOf<String>()
        do {
            val response =
                client.read(
                    ReadRecordsRequest(
                        recordType = type,
                        timeRangeFilter = TimeRangeFilter.between(from, now),
                        ascendingOrder = false,
                        pageSize = min(RECORDS_PER_PAGE, HealthCoverageContract.MAX_RECORDS_PER_TYPE - count),
                        pageToken = token,
                    ),
                    times,
                )
            for (record in response.records) {
                val (start, end, origin) = record
                if (first == null || start.isBefore(first)) first = start
                if (last == null || end.isAfter(last)) last = end
                origins.add(origin)
                count++
            }
            token = response.pageToken
            if (count >= HealthCoverageContract.MAX_RECORDS_PER_TYPE) {
                limited = !token.isNullOrEmpty()
                break
            }
        } while (!token.isNullOrEmpty())

        return HealthMetricCoverage(
            type = label,
            permission = HealthPermission.getReadPermission(type),
            state = if (count == 0) CoverageState.NO_RECORDS else CoverageState.RECORDS_FOUND,
            recordCount = count,
            firstObservedUtc = first?.toString(),
            lastObservedUtc = last?.toString(),
            originPackages = origins.toList(),
            countIsLowerBound = limited,
        )
    }
}
