package io.github.aimindseye.sablehealth.core

/** Metadata-only, bounded Health Connect discovery. Never contains biometric values. */
data class HealthMetricCoverage(
    val type: String,
    val permission: String,
    val state: CoverageState,
    val recordCount: Int = 0,
    val firstObservedUtc: String? = null,
    val lastObservedUtc: String? = null,
    val originPackages: List<String> = emptyList(),
    val countIsLowerBound: Boolean = false,
)

enum class CoverageState {
    NOT_GRANTED,
    NO_RECORDS,
    RECORDS_FOUND,
    READ_ERROR,
}

object HealthCoverageContract {
    const val WINDOW_DAYS = 30L
    const val MAX_RECORDS_PER_TYPE = 500
    private const val THREE_DAY_WINDOW = 3L
    private const val WEEK_WINDOW = 7L
    private const val TWO_WEEK_WINDOW = 14L
    val SCAN_WINDOWS = listOf(1L, THREE_DAY_WINDOW, WEEK_WINDOW, TWO_WEEK_WINDOW, WINDOW_DAYS)

    fun status(coverage: HealthMetricCoverage): String =
        when (coverage.state) {
            CoverageState.NOT_GRANTED -> "Read access not granted"
            CoverageState.NO_RECORDS -> "Allowed · no records in scan window"
            CoverageState.READ_ERROR -> "Scan unavailable · retry or check Health Connect"
            CoverageState.RECORDS_FOUND ->
                if (coverage.countIsLowerBound) {
                    "At least ${coverage.recordCount} records · scan cap reached"
                } else {
                    "${coverage.recordCount} records found"
                }
        }

    /** User-initiated share text. No values, record IDs, account IDs or device IDs. */
    fun redactedReport(
        rows: List<HealthMetricCoverage>,
        scannedAtUtc: String,
        windowDays: Long = WINDOW_DAYS,
    ): String =
        buildString {
            appendLine("SABLE_HEALTH_M1A_COVERAGE_V1")
            appendLine("SCOPE=Recent $windowDays-day Health Connect window")
            appendLine("SCAN_TIME_UTC=$scannedAtUtc")
            appendLine("RECORD_MEASUREMENT_VALUES_INCLUDED=false")
            appendLine("IDENTIFIERS_INCLUDED=false")
            appendLine("RECORD_IDS_INCLUDED=false")
            appendLine("RAW_RECORDS_PERSISTED=false")
            appendLine("COUNTS_ARE_RECORDS_NOT_SAMPLES=true")
            rows.forEach { row ->
                appendLine("CATEGORY=${row.type}")
                appendLine("STATE=${row.state}")
                appendLine("GRANTED=${row.state != CoverageState.NOT_GRANTED}")
                appendLine("RECORD_COUNT=${row.recordCount}")
                appendLine("COUNT_IS_LOWER_BOUND=${row.countIsLowerBound}")
                appendLine("FIRST_OBSERVED_UTC=${row.firstObservedUtc ?: "NONE"}")
                appendLine("LAST_OBSERVED_UTC=${row.lastObservedUtc ?: "NONE"}")
                appendLine("WRITER_PACKAGES=${row.originPackages.joinToString(",")}")
            }
            appendLine("END_OF_REPORT")
        }
}
