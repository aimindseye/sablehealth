package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SourceOverviewTest {
    private val now = Instant.parse("2026-11-24T12:00:00Z")
    private val record =
        HealthMetricCoverage(
            "Steps",
            "read.steps",
            CoverageState.RECORDS_FOUND,
            recordCount = 2,
            lastObservedUtc = "2026-11-24T11:00:00Z",
            originPackages = listOf("com.example.writer"),
        )

    @Test
    fun permissionCountsNeverInferGrantsOrCountUnrelatedPermissions() {
        assertEquals("Access unknown", SourceOverview.permissionSummary(setOf("a"), setOf("a"), false))
        assertEquals("1/2 allowed", SourceOverview.permissionSummary(setOf("a", "b"), setOf("a", "other"), true))
        assertEquals("0/0 allowed", SourceOverview.permissionSummary(emptySet(), emptySet(), true))
    }

    @Test
    fun scanStatesAreIndependentOfPermissionCounts() {
        assertEquals("Scanning", SourceOverview.recordSummary(emptyList(), null, true, now))
        assertEquals("Not verified", SourceOverview.recordSummary(emptyList(), null, false, now))
        assertEquals("No records observed", SourceOverview.recordSummary(emptyList(), now.toString(), false, now))
        assertEquals("Types found: 1", SourceOverview.recordSummary(listOf(record), now.toString(), false, now))
        for (state in listOf(CoverageState.NOT_GRANTED, CoverageState.READ_ERROR)) {
            val rows = listOf(record, record.copy(type = "Heart rate", state = state))
            assertEquals("Action needed", SourceOverview.recordSummary(rows, now.toString(), false, now))
        }
    }

    @Test
    fun staleInvalidAndFutureScanTimesNeverLookCurrent() {
        for (timestamp in listOf("bad", "2026-11-23T12:00:00Z", "2026-11-24T12:00:01Z")) {
            assertTrue(SourceOverview.scanExpired(timestamp, now))
            assertEquals("Scan stale · refresh", SourceOverview.recordSummary(listOf(record), timestamp, false, now))
        }
        assertFalse(SourceOverview.scanExpired("2026-11-23T12:00:01Z", now))
    }

    @Test
    fun revokedRowsAndFailedReadsNeverContributeWriterOrTime() {
        val other = record.copy(permission = "read.sleep", state = CoverageState.READ_ERROR)
        val rows = listOf(record, other)
        assertEquals(listOf(other), SourceOverview.visibleRows(rows, setOf("read.sleep")))
        assertEquals(emptyList<String>(), SourceOverview.writerApps(listOf(other)))
        assertEquals(null, SourceOverview.lastObserved(listOf(other)))
        assertEquals(listOf("com.example.writer"), SourceOverview.writerApps(rows + record))
        assertEquals(record.lastObservedUtc, SourceOverview.lastObserved(rows))
        assertEquals(null, SourceOverview.lastObserved(listOf(record.copy(lastObservedUtc = "invalid"))))
        assertEquals(null, SourceOverview.lastObserved(listOf(record.copy(lastObservedUtc = null))))
    }

    @Test
    fun groupsFollowExistingMetricCatalog() {
        for (name in listOf("Steps", "Exercise sessions", "Active calories")) {
            assertEquals("Activity", SourceOverview.category(name))
        }
        assertEquals("Sleep", SourceOverview.category("Sleep sessions"))
        assertEquals("Vitals", SourceOverview.category("Heart rate"))
    }
}
