package io.github.aimindseye.sablehealth

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/**
 * Isolated M1-A proof; R5.4 Sources/Settings overview remains unchanged.
 * Nothing is scanned without a deliberate user tap.
 */
@Composable
internal fun M1DiscoveryPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected =
        remember {
            mutableStateMapOf<String, Boolean>().apply {
                M1HealthConnectProbe.metrics.forEachIndexed { index, metric ->
                    this[metric.permission] = index < 3
                }
            }
        }
    var message by remember { mutableStateOf("No scan performed") }
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<HealthMetricCoverage>>(emptyList()) }
    var scannedAt by remember { mutableStateOf<String?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { permissions ->
            message =
                if (permissions.isEmpty()) {
                    "No permissions granted · you can retry"
                } else {
                    "Permission screen closed · select Scan to verify records"
                }
            results = emptyList()
            scannedAt = null
        }
    val platformAvailable =
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
    if (!platformAvailable) {
        Notice("Unavailable", "Health Connect needs to be installed or updated before discovery.")
        return
    }
    Text("Select read categories", style = MaterialTheme.typography.titleSmall)
    M1HealthConnectProbe.metrics.forEach { metric ->
        val enabled = selected[metric.permission] == true
        CompactLinkRow(
            title = (if (enabled) "✓ " else "○ ") + metric.name,
            detail = if (enabled) "Selected · tap to remove" else "Not selected · tap to add",
            onClick = { selected[metric.permission] = !enabled },
        )
    }

    FocusButton(
        "Request selected READ access",
        onClick = {
            val request = selected.filterValues { it }.keys.toSet()
            if (request.isEmpty()) {
                message = "Select at least one category"
            } else {
                try {
                    permissionLauncher.launch(request)
                } catch (_: Exception) {
                    message = "Permission screen unavailable · check Health Connect"
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    FocusButton(
        "Scan allowed records",
        onClick = {
            if (!scanning) {
                scanning = true
                results = emptyList()
                scannedAt = null
                message = "Scanning locally · no measurements retained"
                scope.launch {
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
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)

    if (results.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            results.forEach { coverage ->
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
                "Earliest/latest observed UTC timestamps are available in the optional report. " +
                    "Capped counts describe scanned records, not full history.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        FocusButton(
            "Review and share coverage report",
            onClick = {
                val report = HealthCoverageContract.redactedReport(results, scannedAt.orEmpty())
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
}
