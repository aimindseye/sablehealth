package io.github.aimindseye.sablehealth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import io.github.aimindseye.sablehealth.core.HealthCoverageContract
import io.github.aimindseye.sablehealth.core.SetupPlan
import io.github.aimindseye.sablehealth.core.SourceOverview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private val recommendedNames = setOf("Steps", "Sleep sessions", "Heart rate", "Resting heart rate", "Exercise sessions")

internal fun setupAccessSummary(status: ConnectStatus): String =
    if (status.grantedCount == null) {
        "Access unknown · recheck"
    } else {
        val missing = status.supported.minus(status.granted).size
        "${status.granted.size}/${status.supported.size} allowed · $missing not granted"
    }

@Composable
internal fun SetupPermissions(
    state: SableAppState,
    inspect: suspend (Context) -> ConnectStatus = ::inspectHealthConnect,
    requestOverride: ((Set<String>) -> Unit)? = null,
) {
    val recommended =
        M1HealthConnectProbe.metrics
            .filter { it.name in recommendedNames }
            .map { it.permission }
            .toSet()
    var selection by rememberSaveable { mutableStateOf(recommended.joinToString(",")) }
    val selected = selection.split(",").filter { it.isNotEmpty() }.toSet()
    var expanded by rememberSaveable { mutableStateOf(false) }
    R7aGroup("", r7MetricSurface(null)) {
        Text("Connect health data", style = MaterialTheme.typography.titleMedium)
        R7aNote("Allow read-only access to selected health measurements.")
    }
    R7aGroup("Health Connect") {
        R7aRow("Availability", state.connect.label, detail = setupAccessSummary(state.connect))
        R7aRule()
        R7aRow("Review permissions", action = { expanded = !expanded }, chosen = expanded)
        R7aRule()
        R7aRow("Recheck permissions", action = state::recheckPermissions)
    }
    if (expanded) {
        R7aGroup("Read categories") {
            val current =
                when {
                    selected == recommended -> "Recommended"
                    selected.isNotEmpty() && selected == state.connect.supported -> "All supported"
                    else -> "Custom"
                }
            R7aChoiceRow("Selection", current, listOf("Recommended", "All supported")) { value ->
                selection = (if (value == "Recommended") recommended else state.connect.supported).joinToString(",")
            }
        }
        SetupRequestControls(state, selected, inspect, requestOverride)
        ManageAndroidPermissions()
        SetupMetricRows(state.connect, selected) {
            selection = (if (it in selected) selected - it else selected + it).joinToString(",")
        }
    }
    R7aNote("Android requests access only after your action.")
}

@Composable
private fun SetupRequestControls(
    state: SableAppState,
    selected: Set<String>,
    inspect: suspend (Context) -> ConnectStatus,
    requestOverride: ((Set<String>) -> Unit)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val request = remember { SetupRequestState() }
    val launcher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            request.finished()
            state.recheckPermissions()
        }
    R7aAction(
        "Grant missing selected",
        action = {
            if (!request.busy) {
                request.busy = true
                scope.launch {
                    request.request(state, selected, { inspect(context) }) { missing ->
                        if (requestOverride == null) {
                            launcher.launch(missing)
                        } else {
                            requestOverride(missing)
                            request.finished()
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (request.busy) Text("Waiting for Android permissions")
    if (request.message.isNotEmpty()) Text(request.message, style = MaterialTheme.typography.bodySmall)
}

internal class SetupRequestState {
    var busy by mutableStateOf(false)
    var message by mutableStateOf("")
        private set

    fun finished() {
        busy = false
        message = "Permission screen closed. Checking actual access."
    }

    suspend fun request(
        state: SableAppState,
        selected: Set<String>,
        inspect: suspend () -> ConnectStatus,
        launch: (Set<String>) -> Unit,
    ) {
        try {
            val actual = inspect()
            currentCoroutineContext().ensureActive()
            state.connect = actual
            val missing = SetupPlan.missing(selected, actual.supported, actual.granted)
            if (missing.isEmpty()) {
                message = "No missing supported selection. Check access or select categories."
                busy = false
            } else {
                launch(missing)
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

@Composable
private fun SetupMetricRows(
    status: ConnectStatus,
    selected: Set<String>,
    toggle: (String) -> Unit,
) {
    M1HealthConnectProbe.metrics.groupBy { SourceOverview.category(it.name) }.forEach { (group, metrics) ->
        R7aGroup(group) {
            metrics.forEach { metric ->
                val permission = metric.permission
                R7aRow(
                    metric.name,
                    value = metricAccess(status, permission),
                    chosen = permission in selected,
                    action = { toggle(permission) },
                )
                R7aRule()
            }
        }
    }
}

private fun metricAccess(
    status: ConnectStatus,
    permission: String,
): String =
    when {
        status.grantedCount == null -> "Unknown"
        permission !in status.supported -> "Unsupported"
        permission in status.granted -> "Allowed"
        else -> "Not granted"
    }

@Composable
private fun ManageAndroidPermissions() {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    R7aAction(
        "Manage Android permissions",
        action = {
            message = openHealthConnectSettings(context)
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (message.isNotEmpty()) Text(message, style = MaterialTheme.typography.bodySmall)
}

internal fun openHealthConnectSettings(context: Context): String =
    try {
        context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
        ""
    } catch (_: ActivityNotFoundException) {
        "Health Connect settings unavailable."
    } catch (_: SecurityException) {
        "Health Connect settings access blocked."
    }

@Composable
internal fun SetupVerify(
    state: SableAppState,
    discovery: DiscoveryState = state.discovery,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    R7aGroup("Read access") {
        R7aRow("Allowed categories", detail = setupAccessSummary(state.connect))
        R7aRow("Recheck permissions", action = state::recheckPermissions)
        if (state.connect.granted.isNotEmpty()) {
            R7aRow("Scan allowed records", action = { scope.launch { discovery.scan(context) } })
        } else {
            R7aNote("Grant read access before scanning.")
        }
        R7aNote(discovery.message)
    }
    val rows = SourceOverview.visibleRows(discovery.results, state.connect.granted)
    rows.groupBy { SourceOverview.category(it.type) }.forEach { (category, entries) ->
        R7aGroup(category) {
            entries.forEach { coverage ->
                R7aRow(coverage.type, detail = HealthCoverageContract.status(coverage))
                coverage.lastObservedUtc?.let { R7aNote("Last observed (UTC): $it") }
                if (coverage.originPackages.isNotEmpty()) {
                    R7aNote("Writer apps: ${coverage.originPackages.joinToString()}")
                }
                R7aRule()
            }
        }
    }
    Text("Record coverage is not proof of wearable pairing or current sync.")
}
