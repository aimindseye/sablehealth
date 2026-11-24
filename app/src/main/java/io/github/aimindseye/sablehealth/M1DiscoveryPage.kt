package io.github.aimindseye.sablehealth

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import io.github.aimindseye.sablehealth.core.HealthCoverageContract
import io.github.aimindseye.sablehealth.core.HealthMetricCoverage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

private const val DEFAULT_SELECTED_CATEGORIES = 3

/** Only transient Compose state. No record values are persisted. */
private class DiscoveryState {
    val selected =
        mutableStateMapOf<String, Boolean>().apply {
            M1HealthConnectProbe.metrics.forEachIndexed { index, metric ->
                this[metric.permission] = index < DEFAULT_SELECTED_CATEGORIES
            }
        }
    var message by mutableStateOf("No scan performed")
    var scanning by mutableStateOf(false)
    var results by mutableStateOf<List<HealthMetricCoverage>>(emptyList())
    var scannedAt by mutableStateOf<String?>(null)
    var reportVisible by mutableStateOf(false)

    fun clear() {
        results = emptyList()
        scannedAt = null
        reportVisible = false
    }

    fun permissionResult(permissions: Set<String>) {
        message =
            if (permissions.isEmpty()) {
                "No permissions granted · you can retry"
            } else {
                "Permission screen closed · select Scan to verify records"
            }
        clear()
    }

    suspend fun scan(context: Context) {
        if (scanning) return
        scanning = true
        clear()
        message = "Scanning locally · no measurements retained"
        try {
            val now = Instant.now()
            results = M1HealthConnectProbe.scan(context, now)
            scannedAt = now.toString()
            message = "Finished · record counts are not step or sample totals"
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            message = "Scan unavailable · check Health Connect and retry"
        } finally {
            scanning = false
        }
    }
}

/** M1-A is a separate opt-in proof screen, not a redesign of R5.4 Sources. */
@Composable
internal fun M1DiscoveryPage() {
    val context = LocalContext.current
    val state = remember { DiscoveryState() }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { granted -> state.permissionResult(granted) }
    val available =
        remember(context) {
            try {
                M1HealthConnectProbe.platformAvailable(context)
            } catch (_: Exception) {
                false
            }
        }

    CompactPageTitle("Record coverage", "M1-A · READ ONLY")
    Text(
        "Health Connect on this phone. Choose categories, grant access, then scan the last 30 days.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        "Nothing is uploaded. This does not pair with your Fitbit or import measurements into Dashboard.",
        style = MaterialTheme.typography.bodySmall,
    )
    if (!available) {
        Notice("Unavailable", "Health Connect needs to be installed or updated before discovery.")
        return
    }
    M1MetricSelector(state)
    M1ScanActions(state, context) { permissionLauncher.launch(it) }
    Text(state.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    if (state.results.isNotEmpty()) {
        M1ScanResults(state, context)
    }
}

@Composable
private fun M1MetricSelector(state: DiscoveryState) {
    Text("Select read categories", style = MaterialTheme.typography.titleSmall)
    M1HealthConnectProbe.metrics.forEach { metric ->
        val enabled = state.selected[metric.permission] == true
        CompactLinkRow(
            title = (if (enabled) "✓ " else "○ ") + metric.name,
            detail = if (enabled) "Selected · tap to remove" else "Not selected · tap to add",
            onClick = { state.selected[metric.permission] = !enabled },
        )
    }
}

@Composable
private fun M1ScanActions(
    state: DiscoveryState,
    context: Context,
    requestPermissions: (Set<String>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    FocusButton(
        "Request selected READ access",
        onClick = {
            val request =
                state.selected
                    .filterValues { it }
                    .keys
                    .toSet()
            if (request.isEmpty()) {
                state.message = "Select at least one category"
            } else {
                try {
                    requestPermissions(request)
                } catch (_: Exception) {
                    state.message = "Permission screen unavailable · check Health Connect"
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    FocusButton(
        "Scan allowed records",
        onClick = { if (!state.scanning) scope.launch { state.scan(context) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun M1ScanResults(
    state: DiscoveryState,
    context: Context,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.results.forEach { coverage ->
            CompactSummary(
                coverage.type,
                HealthCoverageContract.status(coverage),
                modifier = Modifier.fillMaxWidth(),
            )
            if (coverage.originPackages.isNotEmpty()) {
                Text(
                    "Writer apps: ${coverage.originPackages.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(
            "Observed UTC bounds are available in the report. Capped counts describe " +
                "scanned records, not full history.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    FocusButton(
        "Review coverage report",
        onClick = { state.reportVisible = !state.reportVisible },
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.reportVisible) {
        M1ReportPreview(state, context)
    }
}

@Composable
private fun M1ReportPreview(
    state: DiscoveryState,
    context: Context,
) {
    val report = HealthCoverageContract.redactedReport(state.results, state.scannedAt.orEmpty())
    Text(report, style = MaterialTheme.typography.bodySmall)
    Text(
        "This includes timestamps and writer app names. Share only if you choose.",
        style = MaterialTheme.typography.bodySmall,
    )
    FocusButton(
        "Share reviewed report",
        onClick = {
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Sable Health M1-A coverage")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
            context.startActivity(Intent.createChooser(intent, "Share metadata-only coverage"))
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
