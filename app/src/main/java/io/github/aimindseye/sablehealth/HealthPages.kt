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
internal fun SourcesPage(
    status: ConnectStatus,
    refresh: () -> Unit,
    back: () -> Unit,
) {
    SectionHeading("Data sources", "Read-only availability and access diagnostic")
    Notice("Health Connect: ${status.label}", status.detail)
    Notice(
        "Granted permissions",
        "${status.grantedCount?.toString() ?: "Unknown"} · individual metric permissions are not requested by M0",
    )
    Notice("Data imported", "None · no health measurements are read or stored in this milestone")
    Notice("Your privacy", "No backend, telemetry, login, network sync or direct Fitbit Bluetooth pairing")
    FocusButton("Check again", refresh)
    FocusButton("Back", back)
}

@Composable
internal fun SettingsPage(
    state: SableAppState,
    profile: DeviceProfile,
) {
    SectionHeading("Phone & input", "Detected/selected profile: ${profile.label}")
    Text("Device profile", style = MaterialTheme.typography.titleMedium)
    FocusButton(
        "Automatic detection",
        { state.chooseProfile("auto") },
        selected = state.profileOverride == "auto",
    )
    DeviceProfile.entries.forEach { candidate ->
        FocusButton(
            candidate.label,
            { state.chooseProfile(candidate.name) },
            selected = state.profileOverride == candidate.name,
        )
    }
    Text("Appearance", style = MaterialTheme.typography.titleMedium)
    listOf("system", "light", "dark").forEach { value ->
        FocusButton(
            value.replaceFirstChar(Char::uppercaseChar),
            { state.chooseAppearance(value) },
            selected = state.appearance == value,
        )
    }
    Text("Data mode", style = MaterialTheme.typography.titleMedium)
    FocusButton("Synthetic demonstration", { state.chooseDemo(true) }, selected = state.demo)
    FocusButton("No records (live placeholder)", { state.chooseDemo(false) }, selected = !state.demo)
    Notice("Sable Blue", "Dark #2E9BD6 · light surface #1A6FA0 · exact Sable Games tokens")
    Notice(
        "Remappable keys",
        "M0 establishes per-profile defaults. Custom remapping editor is planned as a follow-up; " +
            "no silent reassignment.",
    )
    FocusButton("Back", { state.page = Page.MAIN })
}

@Composable
internal fun HelpPage(
    profile: DeviceProfile,
    back: () -> Unit,
) {
    SectionHeading("Keyboard controls", profile.label)
    Notice(
        "Move focus",
        if (profile.letterNavigation) "I / J / K / L or DPAD arrows" else "Trackpad / DPAD arrows",
    )
    Notice("Open / return", "Enter, Space or trackpad click · Escape / Back returns")
    Notice("Global shortcuts", "T next tab · R source diagnostic · ? help")
    Notice("Text input", "Input fields retain native letter keys and system shortcuts")
    FocusButton("Back", back)
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
