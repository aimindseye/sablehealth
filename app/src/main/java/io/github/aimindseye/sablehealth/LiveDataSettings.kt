package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.aimindseye.sablehealth.core.LiveMetric

@Composable
internal fun LiveRefreshSettings(state: SableAppState) {
    CompactLinkRow(
        "Foreground refresh",
        if (state.refreshMinutes == 0) "Off · tap to change" else "Every ${state.refreshMinutes} min · tap to change",
        {
            val index = refreshChoices.indexOf(state.refreshMinutes)
            state.chooseRefreshMinutes(refreshChoices[(index + 1) % refreshChoices.size])
        },
    )
    LiveRefreshRow(state)
    Text(
        "Launch, resume and date changes refresh automatically. No background collection.",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun LiveSleepDiagnostic(state: SableAppState) {
    val snapshot = state.liveHealth.snapshot
    val series = snapshot?.metrics?.get(LiveMetric.SLEEP_DURATION)
    Notice("Sleep data diagnostics", series?.status?.label ?: "Not loaded")
    if (snapshot != null) {
        Text(
            "Window: ${snapshot.window.sleepStart} – ${snapshot.window.now}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Sessions: ${snapshot.sleeps.size} · Zone: ${snapshot.window.zone}",
            style = MaterialTheme.typography.bodySmall,
        )
        val writers =
            snapshot.sleeps
                .map { it.writer }
                .distinct()
                .joinToString()
                .ifEmpty { "Not observed" }
        Text("Writers: $writers", style = MaterialTheme.typography.bodySmall)
        val withStages = snapshot.sleeps.count { it.stages.isNotEmpty() }
        Text("Sessions with stages: $withStages", style = MaterialTheme.typography.bodySmall)
    }
    Text("No sleep records? Check Health Connect and Google Health export.", style = MaterialTheme.typography.bodySmall)
    FocusButton("Refresh measurements", state::refreshLive, modifier = Modifier.fillMaxWidth())
}
