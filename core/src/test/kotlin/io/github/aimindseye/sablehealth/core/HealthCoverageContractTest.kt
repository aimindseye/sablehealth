package io.github.aimindseye.sablehealth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCoverageContractTest {
    @Test
    fun permissionDenialIsNotAZeroHealthMeasurement() {
        val denied = HealthMetricCoverage("Steps", "read.steps", CoverageState.NOT_GRANTED)
        assertEquals("Read access not granted", HealthCoverageContract.status(denied))
        assertFalse(HealthCoverageContract.status(denied).contains("0 steps"))
    }

    @Test
    fun noRecordsAndReadErrorsAreDifferentStates() {
        val empty = HealthMetricCoverage("Sleep sessions", "read.sleep", CoverageState.NO_RECORDS)
        val failed = empty.copy(state = CoverageState.READ_ERROR)
        assertTrue(HealthCoverageContract.status(empty).contains("no records"))
        assertTrue(HealthCoverageContract.status(failed).contains("Scan unavailable"))
    }

    @Test
    fun cappedRecordCountIsAnExplicitLowerBound() {
        val capped =
            HealthMetricCoverage(
                type = "Heart rate",
                permission = "read.heart",
                state = CoverageState.RECORDS_FOUND,
                recordCount = HealthCoverageContract.MAX_RECORDS_PER_TYPE,
                countIsLowerBound = true,
            )
        assertTrue(HealthCoverageContract.status(capped).startsWith("At least"))
        val report =
            HealthCoverageContract.redactedReport(
                listOf(capped),
                "2026-11-23T23:00:00Z",
            )
        assertTrue(report.contains("COUNT_IS_LOWER_BOUND=true"))
    }

    @Test
    fun uncappedCountAndEmptyReportRemainDistinct() {
        val row = HealthMetricCoverage("Steps", "read.steps", CoverageState.RECORDS_FOUND, recordCount = 2)
        assertEquals("2 records found", HealthCoverageContract.status(row))
        val report = HealthCoverageContract.redactedReport(listOf(row), "2026-11-23T23:00:00Z")
        assertTrue(report.contains("COUNT_IS_LOWER_BOUND=false"))
        assertTrue(report.contains("FIRST_OBSERVED_UTC=NONE"))
        assertTrue(report.contains("LAST_OBSERVED_UTC=NONE"))
        assertTrue(HealthCoverageContract.redactedReport(emptyList(), "now").endsWith("END_OF_REPORT\n"))
    }

    @Test
    fun exportContainsOnlyProvenanceMetadataNotMeasurements() {
        val row =
            HealthMetricCoverage(
                type = "HRV (RMSSD)",
                permission = "android.permission.health.READ_HEART_RATE_VARIABILITY",
                state = CoverageState.RECORDS_FOUND,
                recordCount = 3,
                firstObservedUtc = "2026-11-20T01:00:00Z",
                lastObservedUtc = "2026-11-22T01:00:00Z",
                originPackages = listOf("com.example.writer"),
            )
        val report = HealthCoverageContract.redactedReport(listOf(row), "2026-11-23T23:00:00Z")
        assertTrue(report.contains("HRV (RMSSD)"))
        assertTrue(report.contains("RECORD_COUNT=3"))
        assertTrue(report.contains("WRITER_PACKAGES=com.example.writer"))
        assertTrue(report.contains("RECORD_MEASUREMENT_VALUES_INCLUDED=false"))
        assertFalse(report.contains("heartRateVariabilityMillis"))
        assertFalse(report.contains("healthDataValue"))
        assertFalse(report.contains("DEVICE_SERIAL"))
    }
}
