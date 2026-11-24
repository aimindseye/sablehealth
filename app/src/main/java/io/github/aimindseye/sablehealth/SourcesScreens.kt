package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.SetupEntry
import io.github.aimindseye.sablehealth.core.SourceOverview
import io.github.aimindseye.sablehealth.core.SourcesUiContract
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
internal fun SourcesPage(state: SableAppState) {
    CompactPageTitle("Data connections", "LOCAL")
    CompactLinkRow(
        "Health Connect · ${state.connect.label}",
        setupAccessSummary(state.connect),
        { state.page = Page.SOURCE_DETAIL },
    )
    val rows = SourceOverview.visibleRows(state.discovery.results, state.connect.granted)
    val export =
        if ("com.fitbit.FitbitMobile" in SourceOverview.writerApps(rows)) {
            "Fitbit writer observed"
        } else {
            "Export unverified"
        }
    val companionStatus = SourcesUiContract.companionStatus(state.companion.confirmed)
    CompactLinkRow(
        "Google Health / Fitbit Air",
        "$companionStatus · $export",
        { state.page = Page.SOURCE_DETAIL },
    )
    SourceSummaries(state)
    SourceActions(state)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FocusButton("M1-A record coverage", { state.page = Page.M1_DISCOVERY }, modifier = Modifier.weight(1f))
        FocusButton("Privacy & diagnostics", { state.page = Page.SOURCE_PRIVACY }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SourceSummaries(state: SableAppState) {
    val rows = SourceOverview.visibleRows(state.discovery.results, state.connect.granted)
    val summary = SourceOverview.recordSummary(rows, state.discovery.scannedAt, state.discovery.scanning, Instant.now())
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.weight(1f)) {
            CompactSummary(
                "Permissions",
                SourceOverview.permissionSummary(
                    state.connect.supported,
                    state.connect.granted,
                    state.connect.grantedCount != null,
                ),
            )
        }
        Box(Modifier.weight(1f)) {
            CompactSummary("Record coverage", summary)
        }
    }
}

@Composable
private fun SourceActions(state: SableAppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discovery = state.discovery
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FocusButton(
            "Manage permissions",
            { state.setupAction(SetupEntry.PERMISSIONS_ONLY) },
            modifier = Modifier.weight(1f),
        )
        FocusButton(
            "Refresh local records",
            onClick = {
                if (state.connect.granted.isEmpty()) {
                    state.setupAction(SetupEntry.PERMISSIONS_ONLY)
                } else {
                    scope.launch { discovery.scan(context) }
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
    Text(discovery.message, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun SourceDetailPage(state: SableAppState) {
    SectionHeading("Connection details", "Local metadata only")
    Notice("Health Connect", "${state.connect.label} · ${state.connect.detail}")
    Notice("Read permissions", setupAccessSummary(state.connect))
    FocusButton(
        "Manage permissions",
        { state.setupAction(SetupEntry.PERMISSIONS_ONLY) },
        modifier = Modifier.fillMaxWidth(),
    )
    CompanionDetails(state)
    SourceRecordDetails(state)
    FocusButton("Check availability again", state::refreshSources, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CompanionDetails(state: SableAppState) {
    Notice(
        "Google Health / Fitbit Air",
        "Pair and sync in the official companion. A writer app does not identify your physical wearable.",
    )
    val confirmationLabel =
        if (state.companion.confirmed) {
            "✓ Paired and synced in Google Health"
        } else {
            "I paired and synced in Google Health"
        }
    FocusButton(
        label = confirmationLabel,
        onClick = { state.companion.chooseConfirmation(!state.companion.confirmed) },
        selected = state.companion.confirmed,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SourceRecordDetails(state: SableAppState) {
    val rows = SourceOverview.visibleRows(state.discovery.results, state.connect.granted)
    Notice("Writer apps", SourceOverview.writerApps(rows).joinToString().ifEmpty { "Not observed" })
    Notice("Last observed (UTC)", SourceOverview.lastObserved(rows) ?: "Not observed")
    Notice("Scan time (UTC)", state.discovery.scannedAt ?: "Not scanned")
    Notice(
        "Help with sync",
        "If records are missing, check export and write access in your companion. Refresh does not force Fitbit sync.",
    )
    CompactLinkRow("Record coverage details", "Read-only · 30-day window", { state.page = Page.M1_DISCOVERY })
}

@Composable
internal fun SourcePrivacyPage() {
    SectionHeading("Privacy & diagnostics", "Local only")
    Notice(
        "Health data",
        "Explicit local scans inspect counts, writer apps and timestamps. No measurements are retained or imported.",
    )
    Notice("Network and accounts", "No Sable Health account, backend, analytics, telemetry or direct Fitbit Bluetooth.")
    Notice("Diagnostics", "Scan metadata is transient. Review the report before explicitly sharing it.")
    Notice("Export status", "Google Health sync is not proof of Health Connect export to Sable Health.")
}
