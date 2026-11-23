package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.SourcesUiContract

/** The only provider state verified by M0 is Health Connect platform/permission availability. */
@Composable
internal fun SourcesPage(state: SableAppState) {
    CompactPageTitle("Data connections", "M0 DIAGNOSTIC")
    val dark = MaterialTheme.colorScheme.background == SablePalette.darkBase
    val availableColor = if (dark) ChartColors.darkGood else ChartColors.lightGood
    val cautionColor = if (dark) ChartColors.darkCaution else ChartColors.lightCaution
    CompactLinkRow(
        title = "Health Connect · ${state.connect.label}",
        detail = "Record read access not requested · M0",
        onClick = { state.page = Page.SOURCE_DETAIL },
        statusColor = if (state.connect.label == "Available") availableColor else cautionColor,
    )
    val companionStatus = SourcesUiContract.companionStatus(state.companion.confirmed)
    CompactLinkRow(
        title = "Google Health / Fitbit Air",
        detail = "$companionStatus · export unverified",
        onClick = { state.page = Page.SOURCE_DETAIL },
        statusColor = cautionColor,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.weight(1f)) {
            CompactSummary("Permissions", state.connect.grantedCount?.let { "$it granted" } ?: "Unknown")
        }
        Box(Modifier.weight(1f)) {
            CompactSummary("Records imported", SourcesUiContract.IMPORTED_M0)
        }
    }
    CompactLinkRow("Privacy & diagnostics", "Local only · details", { state.page = Page.SOURCE_PRIVACY })
    FocusButton("Check availability again", state::refreshSources, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun SourceDetailPage(state: SableAppState) {
    SectionHeading("Connection details", "Diagnostic results · no health measurements queried")
    Notice("Health Connect", "${state.connect.label} · ${state.connect.detail}")
    Notice(
        "Metric permission access",
        "${state.connect.grantedCount?.toString() ?: "Unknown"} granted to Sable Health. " +
            "M0 does not request or read heart rate, sleep or activity records.",
    )
    Notice("Imported records", "None. M0 has no data ingestion, regardless of companion syncing.")
    Notice(
        "Google Health / Fitbit Air",
        if (state.companion.confirmed) {
            "You marked the official companion as paired and synced. " +
                "Sable Health cannot verify its connection or Health Connect exports yet."
        } else {
            "Not confirmed within Sable Health. Pair and sync in Google Health first; " +
                "this app does not connect to Fitbit over Bluetooth."
        },
    )
    Text("Companion setup confirmation", style = MaterialTheme.typography.titleMedium)
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
    Notice(
        "Next: M1 record verification",
        "Request read-only permissions, inspect record categories, provenance and freshness. " +
            "No Fitbit-only metric is assumed to be exported.",
    )
    FocusButton("Check availability again", state::refreshSources, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun SourcePrivacyPage() {
    SectionHeading("Privacy & diagnostics", "Local foundation · M0")
    Notice("Health data", "No health readings are read, imported, stored or sent by M0.")
    Notice("Network and accounts", "No Sable Health account, backend, analytics, telemetry or direct Fitbit Bluetooth.")
    Notice("Diagnostics", "Provider availability and granted permissions only; no biometric values.")
    Notice("Export status", "Google Health sync is not proof of Health Connect export to Sable Health.")
}
