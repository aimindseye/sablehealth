package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DeviceProfile

@Composable
internal fun HelpPage(
    profile: DeviceProfile,
    back: () -> Unit,
) {
    R7aGroup("Navigation · ${profile.label}") {
        R7aRow("Move focus", detail = if (profile.letterNavigation) "I / J / K / L or DPAD" else "Trackpad / DPAD")
        R7aRule()
        R7aRow("Open", "Enter / Space / click")
        R7aRule()
        R7aRow("Return", "Escape / Back")
    }
    R7aGroup("Shortcuts") {
        R7aRow("Next main tab", "T")
        R7aRule()
        R7aRow("Refresh diagnostics", "R")
        R7aRule()
        R7aRow("Help", "?")
    }
    R7aNote("Text input keeps native keys and shortcuts.")
    R7aAction("Back", back)
}

@Composable
internal fun Onboarding(
    profile: DeviceProfile,
    status: ConnectStatus,
    finish: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Sable Health",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SectionHeading("Designed for keyboard phones", "A local-first companion for health records you control")
        Notice("Phone detected", profile.label)
        Notice("Health Connect: ${status.label}", status.detail)
        Notice(
            "Important",
            "Fitbit Air pairs through Google Health. M0 inspects Health Connect availability; " +
                "it does not import records or ask for health permissions.",
        )
        FocusButton("Explore synthetic demo", { finish(true) })
        FocusButton("Continue with no data", { finish(false) })
        Notice(
            "Privacy first",
            "Synthetic demo and local settings only. No account, advertising, telemetry or remote service.",
        )
    }
}
