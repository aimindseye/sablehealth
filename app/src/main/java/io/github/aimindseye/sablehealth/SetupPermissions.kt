package io.github.aimindseye.sablehealth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import io.github.aimindseye.sablehealth.core.HealthCoverageContract
import io.github.aimindseye.sablehealth.core.SetupPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val recommendedNames = setOf("Steps", "Sleep sessions", "Heart rate", "Resting heart rate", "Exercise sessions")

internal fun setupAccessSummary(status: ConnectStatus): String =
    if (status.grantedCount == null) {
        "Access unknown · recheck"
    } else {
        "${status.granted.size}/${status.supported.size} allowed · ${status.supported.minus(status.granted).size} not granted"
    }

@Composable
internal fun SetupPermissions(state: SableAppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recommended = M1HealthConnectProbe.metrics.filter { it.name in recommendedNames }.map { it.permission }.toSet()
    var selection by rememberSaveable { mutableStateOf(recommended.joinToString(",")) }
    var message by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            busy = false
            message = "Permission screen closed. Checking actual access."
            state.recheckPermissions()
        }
    val selected = selection.split(",").filter { it.isNotEmpty() }.toSet()
    Text(setupAccessSummary(state.connect), style = MaterialTheme.typography.bodyMedium)
    FocusButton(
        "Grant missing selected",
        onClick = {
            if (!busy) {
                busy = true
                scope.launch {
                    try {
                        val actual = inspectHealthConnect(context)
                        state.connect = actual
                        val missing = SetupPlan.missing(selected, actual.supported, actual.granted)
                        if (missing.isEmpty()) {
                            message = "No missing supported selection. Check access or select categories."
                            busy = false
                        } else {
                            launcher.launch(missing)
                        }
                    } catch (cancelled: CancellationException) {
                        busy = false
                        throw cancelled
                    } catch (_: Exception) {
                        busy = false
                        message = "Permission screen unavailable. Recheck and retry."
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    FocusButton("Recheck permissions", state::recheckPermissions, modifier = Modifier.fillMaxWidth())
    if (busy) Text("Waiting for Android permissions")
    if (message.isNotEmpty()) Text(message, style = MaterialTheme.typography.bodySmall)
    CompactChoiceRow(
        "Select read categories",
        listOf("recommended" to "Recommended", "all" to "All supported"),
        if (selected == recommended) "recommended" else if (selected == state.connect.supported) "all" else "",
        { selection = (if (it == "recommended") recommended else state.connect.supported).joinToString(",") },
    )
    SetupMetricRows(state.connect, selected) {
        selection = (if (it in selected) selected - it else selected + it).joinToString(",")
    }
}

@Composable
private fun SetupMetricRows(
    status: ConnectStatus,
    selected: Set<String>,
    toggle: (String) -> Unit,
) {
    M1HealthConnectProbe.metrics.forEach { metric ->
        val permission = metric.permission
        val access =
            when {
                status.grantedCount == null -> "Unknown"
                permission !in status.supported -> "Unsupported"
                permission in status.granted -> "Allowed"
                else -> "Not granted"
            }
        CompactLinkRow(
            metric.name,
            (if (permission in selected) "Selected" else "Not selected") + " · " + access,
            { toggle(permission) },
        )
    }
}

@Composable
internal fun SetupVerify(state: SableAppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discovery = remember(state.connect.granted) { DiscoveryState() }
    Notice("Read access", setupAccessSummary(state.connect))
    FocusButton("Recheck permissions", state::recheckPermissions, modifier = Modifier.fillMaxWidth())
    FocusButton(
        "Scan allowed records",
        onClick = {
            if (state.connect.granted.isNotEmpty()) scope.launch { discovery.scan(context) }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(discovery.message, style = MaterialTheme.typography.bodySmall)
    discovery.results.forEach { coverage ->
        CompactSummary(coverage.type, HealthCoverageContract.status(coverage))
        if (coverage.originPackages.isNotEmpty()) {
            Text("Writer apps: ${coverage.originPackages.joinToString()}", style = MaterialTheme.typography.bodySmall)
        }
    }
    Text("Record coverage is not proof of wearable pairing or current sync.")
}
