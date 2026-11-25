package io.github.aimindseye.sablehealth

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.aimindseye.sablehealth.core.HealthCoverageContract
import io.github.aimindseye.sablehealth.core.SetupEntry
import io.github.aimindseye.sablehealth.core.SourceOverview
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
internal fun R7aCoveragePage(app: SableAppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = app.discovery
    val rows = SourceOverview.visibleRows(state.results, app.connect.granted)
    R7aGroup("Local record scan") {
        val windows = HealthCoverageContract.SCAN_WINDOWS
        R7aChoiceRow("Scan period", "Last ${state.windowDays} days", windows.map { "Last $it days" }) { selected ->
            state.chooseWindow(windows.first { "Last $it days" == selected })
        }
        R7aRule()
        R7aRow("Read access", value = setupAccessSummary(app.connect))
        R7aRule()
        val summary = SourceOverview.recordSummary(rows, state.scannedAt, state.scanning, Instant.now())
        R7aRow("Scan status", value = summary)
        state.scannedAt?.let { scannedAt ->
            runCatching { Instant.parse(scannedAt) }.getOrNull()?.let { end ->
                val start = end.minus(state.windowDays, java.time.temporal.ChronoUnit.DAYS)
                R7aNote("UTC: $start – $end")
            }
        }
    }
    if (app.connect.granted.isNotEmpty()) {
        R7aAction(
            "Scan allowed records",
            { scope.launch { state.scan(context) } },
            modifier = Modifier.fillMaxWidth(),
            primary = true,
            enabled = !state.scanning,
        )
    }
    R7aGroup("Record coverage") {
        listOf("Activity", "Sleep", "Vitals").forEach { category ->
            val summary = r7aCategorySummary(app, category)
            R7aRow(category, detail = summary, action = {
                app.coverageCategory = category
                app.page = Page.SOURCE_COVERAGE
            })
            R7aRule()
        }
        R7aRow("Manage permissions", action = { app.setupAction(SetupEntry.PERMISSIONS_ONLY) })
    }
    R7aNote(state.message)
    if (rows.isNotEmpty()) R7aAction("Review coverage report", app::openCoverageReport)
    R7aNote("Metadata only · not a Fitbit sync command. Counts may be capped.")
}

@Composable
internal fun R7aCoverageCategory(app: SableAppState) {
    val discovery = app.discovery
    val rows = SourceOverview.visibleRows(discovery.results, app.connect.granted)
    val entries = M1HealthConnectProbe.metrics.filter { SourceOverview.category(it.name) == app.coverageCategory }
    R7aGroup(app.coverageCategory) {
        entries.forEach { metric ->
            val coverage = rows.firstOrNull { it.permission == metric.permission }
            val status =
                when {
                    app.connect.grantedCount == null -> "Access unknown"
                    coverage != null -> HealthCoverageContract.status(coverage)
                    metric.permission !in app.connect.supported -> "Unsupported"
                    metric.permission !in app.connect.granted -> "Not granted"
                    else -> "Allowed · not scanned"
                }
            R7aRow(metric.name, detail = status)
            coverage?.lastObservedUtc?.let { R7aNote("Last observed (UTC): $it") }
            coverage?.originPackages?.takeIf { it.isNotEmpty() }?.let {
                R7aNote("Writer apps: ${it.joinToString()}")
            }
            R7aRule()
        }
    }
    R7aGroup("Observed (UTC)") {
        R7aRow("Scan completed", detail = discovery.scannedAt ?: "Not scanned")
        val scannedAt = discovery.scannedAt
        if (scannedAt != null && SourceOverview.scanExpired(scannedAt, Instant.now())) {
            R7aNote("Scan stale · refresh")
        }
        R7aRow("Manage permissions", action = { app.setupAction(SetupEntry.PERMISSIONS_ONLY) })
        if (rows.isNotEmpty()) R7aRow("Review coverage report", action = app::openCoverageReport)
    }
}

@Composable
internal fun R7aCoverageReport(app: SableAppState) {
    val context = LocalContext.current
    val discovery = app.discovery
    val rows = SourceOverview.visibleRows(discovery.results, app.connect.granted)
    val scannedAt = discovery.scannedAt
    R7aGroup("Review before sharing") {
        R7aRow("Scope", "${discovery.windowDays}-day scan")
        R7aRule()
        R7aRow("Report content", "Metadata only")
    }
    if (rows.isEmpty() || scannedAt == null) {
        R7aNote("No current report. Scan allowed records first.")
    } else {
        val report = HealthCoverageContract.redactedReport(rows, scannedAt, discovery.windowDays)
        R7aGroup("") { Text(report, style = MaterialTheme.typography.bodySmall) }
        R7aNote("Counts may be capped. This includes timestamps and writer app names. Share only if you choose.")
        R7aAction("Share reviewed report", {
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Sable Health coverage")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
            context.startActivity(Intent.createChooser(intent, "Share metadata-only coverage"))
        }, modifier = Modifier.fillMaxWidth(), primary = true)
    }
}

internal fun r7aCategorySummary(
    app: SableAppState,
    category: String,
): String {
    val permissions =
        M1HealthConnectProbe.metrics
            .filter { SourceOverview.category(it.name) == category }
            .map { it.permission }
            .toSet()
    val supported = permissions.intersect(app.connect.supported)
    val granted = supported.intersect(app.connect.granted)
    return when {
        app.connect.grantedCount == null -> "Access unknown"
        supported.isEmpty() -> "Unavailable"
        granted.isEmpty() -> "Not granted"
        else -> {
            val discovery = app.discovery
            val rows = SourceOverview.visibleRows(discovery.results, granted)
            SourceOverview.recordSummary(rows, discovery.scannedAt, discovery.scanning, Instant.now())
        }
    }
}
