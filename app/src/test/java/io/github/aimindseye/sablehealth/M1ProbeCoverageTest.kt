package io.github.aimindseye.sablehealth

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import io.github.aimindseye.sablehealth.core.CoverageState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class M1ProbeCoverageTest {
    private val now = Instant.parse("2026-11-23T23:00:00Z")
    private val steps = M1HealthConnectProbe.metrics.first()
    private val reader = SyntheticReader()

    @Test
    fun deniedThenAllowedThenRevoked() =
        runBlocking {
            assertTrue(M1HealthConnectProbe.scan(reader, now).all { it.state == CoverageState.NOT_GRANTED })
            reader.permissions = setOf(steps.permission)
            val rows = M1HealthConnectProbe.scan(reader, now)
            assertEquals(CoverageState.NO_RECORDS, rows.first().state)
            assertTrue(rows.drop(1).all { it.state == CoverageState.NOT_GRANTED })
            reader.permissions = emptySet()
            assertEquals(CoverageState.NOT_GRANTED, M1HealthConnectProbe.scan(reader, now).first().state)
        }

    @Test
    @Config(sdk = [30])
    fun platformWithoutProviderRejectsScan() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication()
            assertFalse(M1HealthConnectProbe.platformAvailable(context))
            val failure = runCatching { M1HealthConnectProbe.scan(context, now) }
            assertTrue(failure.exceptionOrNull() is IllegalStateException)
        }

    @Test
    fun allAllowedEmptyTypesRemainDistinct() =
        runBlocking {
            reader.permissions = M1HealthConnectProbe.metrics.map { it.permission }.toSet()
            val rows = M1HealthConnectProbe.scan(reader, now)
            assertEquals(13, rows.map { it.type }.distinct().size)
            assertTrue(rows.all { it.state == CoverageState.NO_RECORDS })
        }

    @Test
    fun paginationOriginsAndCapAreMetadataOnly() =
        runBlocking {
            reader.permissions = setOf(steps.permission)
            reader.records = (1..101).map(::bounds)
            val initial = M1HealthConnectProbe.scan(reader, now).first()
            assertEquals(101, initial.recordCount)
            assertFalse(initial.countIsLowerBound)
            assertEquals(now.minusSeconds(202).toString(), initial.firstObservedUtc)
            reader.records = (1..502).map(::bounds)
            val capped = M1HealthConnectProbe.scan(reader, now).first()
            assertEquals(500, capped.recordCount)
            assertTrue(capped.countIsLowerBound)
            assertEquals(listOf("com.example.second", "com.example.writer"), capped.originPackages)
        }

    @Test
    fun securityAndReadFailureAreDistinct() =
        runBlocking {
            reader.permissions = setOf(steps.permission)
            reader.failure = SecurityException()
            assertEquals(CoverageState.NOT_GRANTED, M1HealthConnectProbe.scan(reader, now).first().state)
            reader.failure = IllegalStateException()
            assertEquals(CoverageState.READ_ERROR, M1HealthConnectProbe.scan(reader, now).first().state)
        }

    @Test
    fun cancellationIsPropagated() =
        runBlocking {
            reader.permissions = setOf(steps.permission)
            reader.failure = CancellationException()
            val failure = runCatching { M1HealthConnectProbe.scan(reader, now) }
            assertTrue(failure.exceptionOrNull() is CancellationException)
        }

    private fun bounds(index: Int) =
        M1HealthConnectProbe.RecordBounds(
            now.minusSeconds(index * 2L),
            now.minusSeconds(index * 2L - 1),
            if (index > 101) "com.example.second" else "com.example.writer",
        )

    private class SyntheticReader : M1HealthConnectProbe.Reader {
        var permissions = emptySet<String>()
        var records = emptyList<M1HealthConnectProbe.RecordBounds>()
        var failure: Exception? = null

        override suspend fun granted(): Set<String> = permissions

        override suspend fun <T : Record> read(
            request: ReadRecordsRequest<T>,
            times: (T) -> Pair<Instant, Instant>,
        ): M1HealthConnectProbe.MetadataPage {
            failure?.let { throw it }
            val offset = request.pageToken?.toInt() ?: 0
            val end = minOf(offset + request.pageSize, records.size)
            return M1HealthConnectProbe.MetadataPage(
                records.subList(offset, end),
                if (end < records.size) end.toString() else null,
            )
        }
    }
}
