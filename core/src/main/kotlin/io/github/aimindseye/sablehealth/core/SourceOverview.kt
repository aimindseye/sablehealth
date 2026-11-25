package io.github.aimindseye.sablehealth.core

import java.time.Instant
import java.time.temporal.ChronoUnit

/** UI policy for transient, permission-scoped metadata. Not a clinical freshness rule. */
object SourceOverview {
    private const val SCAN_VALID_HOURS = 24L

    fun permissionSummary(
        supported: Set<String>,
        granted: Set<String>,
        known: Boolean,
    ): String = if (known) "${granted.intersect(supported).size}/${supported.size} allowed" else "Access unknown"

    fun visibleRows(
        rows: List<HealthMetricCoverage>,
        granted: Set<String>,
    ): List<HealthMetricCoverage> = rows.filter { it.permission in granted }

    fun recordSummary(
        rows: List<HealthMetricCoverage>,
        scannedAt: String?,
        scanning: Boolean,
        now: Instant,
    ): String =
        when {
            scanning -> "Scanning"
            scannedAt == null -> "Not verified"
            scanExpired(scannedAt, now) -> "Scan stale · refresh"
            rows.any { it.state == CoverageState.READ_ERROR || it.state == CoverageState.NOT_GRANTED } ->
                "Action needed"
            rows.none { it.state == CoverageState.RECORDS_FOUND } -> "No records observed"
            else -> "Types found: ${rows.count { it.state == CoverageState.RECORDS_FOUND }}"
        }

    fun scanExpired(
        scannedAt: String,
        now: Instant,
    ): Boolean {
        val timestamp = runCatching { Instant.parse(scannedAt) }.getOrNull() ?: return true
        return timestamp.isAfter(now) || !timestamp.plus(SCAN_VALID_HOURS, ChronoUnit.HOURS).isAfter(now)
    }

    fun writerApps(rows: List<HealthMetricCoverage>): List<String> =
        rows
            .filter { it.state == CoverageState.RECORDS_FOUND }
            .flatMap { it.originPackages }
            .distinct()
            .sorted()

    fun lastObserved(rows: List<HealthMetricCoverage>): String? =
        rows
            .filter { it.state == CoverageState.RECORDS_FOUND }
            .mapNotNull { row -> row.lastObservedUtc?.let { runCatching { Instant.parse(it) }.getOrNull() } }
            .maxOrNull()
            ?.toString()

    fun category(type: String): String =
        when (type) {
            "Steps", "Exercise sessions", "Active calories", "Distance", "Total calories" -> "Activity"
            "Sleep sessions" -> "Sleep"
            else -> "Vitals"
        }
}
