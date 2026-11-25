package io.github.aimindseye.sablehealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import io.github.aimindseye.sablehealth.core.SetupEntry
import io.github.aimindseye.sablehealth.core.SourceOverview
import io.github.aimindseye.sablehealth.core.SourcesUiContract
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
internal fun SourcesPage(state: SableAppState) {
    val rows = SourceOverview.visibleRows(state.discovery.results, state.connect.granted)
    val permissionSummary =
        SourceOverview.permissionSummary(
            state.connect.supported,
            state.connect.granted,
            state.connect.grantedCount != null,
        )
    val coverage =
        SourceOverview.recordSummary(
            rows,
            state.discovery.scannedAt,
            state.discovery.scanning,
            Instant.now(),
        )
    val ready = state.connect.label == "Available" && state.connect.granted.isNotEmpty()
    R7aGroup("", r7aStatusColor(ready)) {
        R7aRow("Health Connect", value = state.connect.label, detail = permissionSummary, action = {
            state.page = Page.SOURCE_DETAIL
        })
    }
    R7aGroup("Data sources") {
        R7aRow("Google Health / Fitbit", detail = coverage, action = { state.page = Page.SOURCE_COMPANION })
        val writer = "com.fitbit.FitbitMobile" in SourceOverview.writerApps(rows)
        R7aNote(if (writer) "Fitbit writer observed" else "Fitbit export unverified")
        R7aNote(SourcesUiContract.companionStatus(state.companion.confirmed))
    }
    R7aGroup("") {
        R7aRow("Health permissions", action = { state.setupAction(SetupEntry.PERMISSIONS_ONLY) })
        R7aRule()
        R7aRow("Check data availability", action = { state.openCoverage() })
        R7aRule()
        R7aRow("Sync diagnostics", action = { state.page = Page.SOURCE_DIAGNOSTICS })
    }
    R7aNote("Private · Read-only · Local data")
}

@Composable
internal fun SourceDetailPage(state: SableAppState) {
    R7aGroup("", r7aStatusColor(state.connect.granted.isNotEmpty())) {
        R7aRow("Health Connect", state.connect.label)
        R7aRule()
        R7aRow("Read access", value = setupAccessSummary(state.connect))
    }
    R7aGroup("Record coverage") {
        listOf("Activity", "Sleep", "Vitals").forEach { category ->
            val summary = r7aCategorySummary(state, category)
            R7aRow(category, value = summary)
            R7aRule()
        }
    }
    R7aGroup("") {
        R7aRow("Manage permissions", action = { state.setupAction(SetupEntry.PERMISSIONS_ONLY) })
        R7aRule()
        R7aRow("Check availability again", action = state::recheckPermissions)
    }
    R7aGroup("") { R7aRow("Record coverage", action = state::openCoverage) }
    R7aNote(state.connect.detail)
}

@Composable
internal fun R7aCompanionPage(state: SableAppState) {
    val rows = SourceOverview.visibleRows(state.discovery.results, state.connect.granted)
    val writers = SourceOverview.writerApps(rows)
    R7aGroup("Google Health / Fitbit Air") {
        R7aNote("Pair and sync in the official companion. Sable Health does not verify pairing.")
    }
    R7aGroup("Companion setup") {
        R7aRow(
            "Paired and synced",
            detail = if (state.companion.confirmed) "User confirmed" else "Not confirmed",
            chosen = state.companion.confirmed,
            action = {
                state.companion.chooseConfirmation(!state.companion.confirmed)
            },
        )
        R7aRule()
        R7aRow("Health Connect export", if ("com.fitbit.FitbitMobile" in writers) "Writer observed" else "Unverified")
    }
    R7aGroup("Record source") {
        R7aRow("Writer apps", detail = writers.joinToString().ifEmpty { "Not observed" })
        R7aRow("Last observed (UTC)", detail = SourceOverview.lastObserved(rows) ?: "Not observed")
        R7aRow("Record coverage", action = { state.openCoverage() })
    }
    R7aNote("Writer app is not proof of wearable identity.")
}

@Composable
internal fun R7aSourceDiagnostics(state: SableAppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    R7aGroup("Health Connect") {
        R7aRow("Availability", state.connect.label)
        R7aRule()
        R7aRow("Read permissions", detail = setupAccessSummary(state.connect))
        R7aRule()
        R7aRow("Last scan (UTC)", detail = state.discovery.scannedAt ?: "Not scanned")
        R7aRow("Refresh local records", action = {
            if (state.connect.granted.isEmpty()) {
                state.setupAction(SetupEntry.PERMISSIONS_ONLY)
            } else {
                scope.launch { state.discovery.scan(context) }
            }
        })
        R7aNote(state.discovery.message)
    }
    R7aGroup("Tools") {
        if (state.live) {
            R7aRow("Sleep data diagnostics", action = { state.page = Page.SLEEP_DIAGNOSTICS })
            R7aRule()
        }
        R7aRow("Review coverage report", action = {
            state.openCoverageReport()
        })
        R7aRule()
        R7aRow("Privacy & diagnostics", action = { state.page = Page.SOURCE_PRIVACY })
    }
    R7aNote("Check companion export if records are missing. Refreshing here does not force Fitbit sync.")
}

@Composable
internal fun SourcePrivacyPage() {
    R7aGroup("On this phone") {
        R7aRow("Coverage scans", "Metadata only")
        R7aRule()
        R7aRow("Measurements", "Memory only")
        R7aRule()
        R7aRow("Diagnostic report", "Review first")
    }
    R7aGroup("Network & accounts") {
        R7aRow("Sable Health account", "None")
        R7aRule()
        R7aRow("Analytics / telemetry", "None")
        R7aRule()
        R7aRow("Direct Fitbit pairing", "Not supported")
    }
    R7aNote("Scan metadata is transient. Review the report before explicitly sharing it.")
    Text("Companion sync is not proof of export.", style = MaterialTheme.typography.bodySmall)
    R7aNote("No measurements are saved or uploaded.")
}
