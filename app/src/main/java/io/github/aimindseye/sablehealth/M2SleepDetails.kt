package io.github.aimindseye.sablehealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.EfficiencyStatus
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.sleepEfficiency

private const val SECONDS_PER_MINUTE = 60.0

@Composable
internal fun M2SleepDetails(session: LiveSleep) {
    var expanded by remember(session) { mutableStateOf(false) }
    val result = remember(session) { sleepEfficiency(session) }
    val efficiency = result.inBedPercent?.let { "${formatNumber(it)}%" } ?: "Not available"
    R7aRow(
        "Sleep efficiency",
        efficiency,
        detail = "Stage-derived estimate",
        action = { expanded = !expanded },
    )
    if (expanded) {
        Text("Calculate & verify · sleep v1", style = MaterialTheme.typography.labelMedium)
        Text("Writer: ${session.writer.ifBlank { "Unknown" }}", style = MaterialTheme.typography.bodySmall)
        Text("Asleep / recorded in-bed time × 100", style = MaterialTheme.typography.bodySmall)
        R7aNote("Stage-derived estimate, not an independent time-in-bed measurement.")
        R7aNote(efficiencyReason(result.status))
        result.sessionPercent?.let {
            R7aRow("Asleep / session", "${formatNumber(it)}%")
            R7aRow("Recorded asleep", "${formatNumber(result.asleepSeconds / SECONDS_PER_MINUTE)} min")
        }
        result.inBedSeconds?.let { R7aRow("Recorded in bed", "${formatNumber(it / SECONDS_PER_MINUTE)} min") }
    }
}

internal fun efficiencyReason(status: EfficiencyStatus): String =
    when (status) {
        EfficiencyStatus.COMPLETE -> "Complete classified stages; out-of-bed time excluded."
        EfficiencyStatus.BED_LOCATION_UNKNOWN -> "Awake location unknown. Only the session ratio is available."
        EfficiencyStatus.NO_IN_BED_TIME -> "No recorded in-bed time; division is undefined."
        EfficiencyStatus.INCOMPLETE_STAGES -> "Missing, unknown, invalid or overlapping stages. No estimate."
    }
