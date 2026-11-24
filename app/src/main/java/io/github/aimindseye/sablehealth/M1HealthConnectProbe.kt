package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
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
    internal class Metric(
        val name: String,
        val permission: String,
        val inspect: suspend (HealthConnectClient, Instant) -> HealthMetricCoverage,
    )

    private fun <T : Record> metric(
        label: String,
        type: KClass<T>,
        times: (T) -> Pair<Instant, Instant>,
    ): Metric {
        val permission = HealthPermission.getReadPermission(type)
        return Metric(label, permission) { client, time ->
            readCoverage(client, type, label, permission, time, times)
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
        )

    fun platformAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun scan(
        context: Context,
        at: Instant = Instant.now(),
    ): List<HealthMetricCoverage> {
        check(platformAvailable(context)) { "Health Connect unavailable" }
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return metrics.map { metric ->
            if (metric.permission !in granted) {
                HealthMetricCoverage(metric.name, metric.permission, CoverageState.NOT_GRANTED)
            } else {
                try {
                    metric.inspect(client, at)
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

    private suspend fun <T : Record> readCoverage(
        client: HealthConnectClient,
        type: KClass<T>,
        label: String,
        permission: String,
        now: Instant,
        times: (T) -> Pair<Instant, Instant>,
    ): HealthMetricCoverage {
        val from = now.minus(HealthCoverageContract.WINDOW_DAYS, ChronoUnit.DAYS)
        var token: String? = null
        var count = 0
        var first: Instant? = null
        var last: Instant? = null
        var limited = false
        val origins = sortedSetOf<String>()
        do {
            val response =
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = type,
                        timeRangeFilter = TimeRangeFilter.between(from, now),
                        ascendingOrder = false,
                        pageSize = min(100, HealthCoverageContract.MAX_RECORDS_PER_TYPE - count),
                        pageToken = token,
                    ),
                )
            for (record in response.records) {
                val (start, end) = times(record)
                if (first == null || start.isBefore(first)) first = start
                if (last == null || end.isAfter(last)) last = end
                origins.add(record.metadata.dataOrigin.packageName)
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
            permission = permission,
            state = if (count == 0) CoverageState.NO_RECORDS else CoverageState.RECORDS_FOUND,
            recordCount = count,
            firstObservedUtc = first?.toString(),
            lastObservedUtc = last?.toString(),
            originPackages = origins.toList(),
            countIsLowerBound = limited,
        )
    }
}
