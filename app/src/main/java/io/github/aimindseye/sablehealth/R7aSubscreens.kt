package io.github.aimindseye.sablehealth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DeviceProfile

@Composable
internal fun R7aSubscreen(
    state: SableAppState,
    profile: DeviceProfile,
) {
    Surface(Modifier.fillMaxSize(), color = r7aBackground()) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                R6IconButton("Back", R.drawable.ic_r6_back, state::back)
                Text(r7aPageTitle(state), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
            if (state.page == Page.WIDGETS) {
                WidgetPickerPage(state)
            } else {
                ScrollableHealthPage(state, profile)
            }
        }
    }
}

internal fun r7aPageTitle(state: SableAppState): String =
    when (state.page) {
        Page.SOURCES -> "Sources"
        Page.SOURCE_DETAIL -> "Connection details"
        Page.SOURCE_COMPANION -> "Fitbit companion status"
        Page.SOURCE_DIAGNOSTICS -> "Sync diagnostics"
        Page.SOURCE_PRIVACY -> "Privacy & diagnostics"
        Page.SLEEP_DIAGNOSTICS -> "Sleep data diagnostics"
        Page.M1_DISCOVERY -> "Check data availability"
        Page.SOURCE_COVERAGE -> "Record coverage · ${state.coverageCategory}"
        Page.SOURCE_REPORT -> "Coverage report"
        else -> r7aSettingsTitle(state)
    }

private fun r7aSettingsTitle(state: SableAppState): String =
    when (state.page) {
        Page.SETTINGS -> "Settings"
        Page.SETTINGS_SETUP -> "Permissions & setup"
        Page.WIDGETS -> if (state.widgetBrowsing) "Available widgets" else "Dashboard widgets"
        Page.PHONE_INPUT -> "Phone & keyboard"
        Page.SETTINGS_PRIVACY -> "Privacy & about"
        Page.HELP -> "Keyboard controls"
        Page.MAIN -> "Sable Health"
        else -> error("Not a settings page: ${state.page}")
    }

internal fun SableAppState.openCoverage() {
    coverageReturn = page
    page = Page.M1_DISCOVERY
}

internal fun SableAppState.openCoverageReport() {
    reportReturn = page
    page = Page.SOURCE_REPORT
}

@Composable
internal fun R7aSetupProgress(
    index: Int,
    count: Int,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { position ->
            Box(
                Modifier.weight(1f).height(4.dp).background(
                    if (position <= index) MaterialTheme.colorScheme.primary else r7aGroupColor(),
                    RoundedCornerShape(2.dp),
                ),
            )
        }
    }
}

@Composable
internal fun R7aAdditionalPage(state: SableAppState) {
    when (state.page) {
        Page.SOURCE_COMPANION -> R7aCompanionPage(state)
        Page.SOURCE_DIAGNOSTICS -> R7aSourceDiagnostics(state)
        Page.SLEEP_DIAGNOSTICS -> LiveSleepDiagnostic(state)
        Page.SETTINGS_SETUP -> R7aSetupSettings(state)
        Page.SOURCE_REPORT -> R7aCoverageReport(state)
        Page.SOURCE_COVERAGE -> R7aCoverageCategory(state)
        else -> error("Not an additional R7A page: ${state.page}")
    }
}
