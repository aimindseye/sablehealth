package io.github.aimindseye.sablehealth

import androidx.compose.runtime.Composable
import io.github.aimindseye.sablehealth.core.LiveMetric

@Composable
internal fun LiveRefreshSettings(state: SableAppState) {
    R7aGroup("Health data") {
        R7aRefreshChoice(state)
        LiveRefreshRow(state)
        R7aNote("Launch, resume and date changes refresh automatically. No background collection.")
    }
}

@Composable
internal fun LiveSleepDiagnostic(state: SableAppState) {
    val snapshot = state.liveHealth.snapshot
    val series = snapshot?.metrics?.get(LiveMetric.SLEEP_DURATION)
    R7aGroup("Current read window") {
        R7aRow("Status", series?.status?.label ?: "Not loaded")
        if (snapshot != null) {
            R7aRule()
            R7aRow("Sessions", snapshot.sleeps.size.toString())
            R7aRule()
            R7aRow("Sessions with stages", snapshot.sleeps.count { it.stages.isNotEmpty() }.toString())
            R7aRule()
            val writers =
                snapshot.sleeps
                    .map { it.writer }
                    .distinct()
                    .joinToString()
                    .ifEmpty { "Not observed" }
            R7aRow("Writer", detail = writers)
        }
    }
    if (snapshot != null) {
        R7aGroup("Query details") {
            R7aRow("Time zone", detail = snapshot.window.zone.toString())
            R7aRow("Read window", detail = "${snapshot.window.sleepStart} – ${snapshot.window.now}")
        }
    }
    R7aAction("Refresh measurements", state::refreshLive, primary = true)
    R7aNote("Missing stages are never inferred. No sleep records? Check Health Connect and Google Health export.")
}
