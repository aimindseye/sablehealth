package io.github.aimindseye.sablehealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.SetupEntry

@Composable
internal fun SettingsPage(
    state: SableAppState,
    profile: DeviceProfile,
) {
    CompactPageTitle("Customize experience", "LOCAL")
    CompactLinkRow("Check permissions", state.connect.label, { state.setupAction(SetupEntry.PERMISSIONS_ONLY) })
    CompactLinkRow("Run complete setup", "Review all setup stages", { state.setupAction(SetupEntry.REVIEW_ALL) })
    CompactLinkRow("Choose setup steps", "Repeat selected stages", { state.setupAction(SetupEntry.SELECTED_STEPS) })
    CompactLinkRow(
        "Dashboard",
        "Customize widgets · ${state.dashboard.widgets.size} selected",
        { state.page = Page.WIDGETS },
    )
    CompactChoiceRow(
        title = "Appearance",
        options = listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
        selected = state.appearance,
        onSelect = state::chooseAppearance,
    )
    CompactChoiceRow(
        title = "Data mode · presentation only",
        options = listOf("demo" to "Demo", "empty" to "No records"),
        selected = if (state.demo) "demo" else "empty",
        onSelect = { state.chooseDemo(it == "demo") },
    )
    CompactLinkRow(
        "Phone & keyboard",
        "${profile.label} · profile and key controls",
        { state.page = Page.PHONE_INPUT },
    )
    CompactLinkRow("Privacy & about", "Local-only M0 · details", { state.page = Page.SETTINGS_PRIVACY })
}

@Composable
internal fun PhoneInputPage(
    state: SableAppState,
    profile: DeviceProfile,
) {
    SectionHeading("Phone & keyboard", "Current profile: ${profile.label}")
    Text("Device profile", style = MaterialTheme.typography.titleMedium)
    CompactLinkRow(
        "Automatic detection",
        if (state.profileOverride == "auto") "Selected" else "Use device-detected defaults",
        { state.chooseProfile("auto") },
    )
    DeviceProfile.entries.forEach { option ->
        CompactLinkRow(
            option.label,
            if (state.profileOverride == option.name) "Selected" else "Use this keyboard profile",
            { state.chooseProfile(option.name) },
        )
    }
    CompactLinkRow("Keyboard controls", "DPAD, trackpad, I/J/K/L and shortcuts", { state.page = Page.HELP })
    Text("Custom key remapping is planned, not available in M0.", style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun SettingsPrivacyPage() {
    SectionHeading("Privacy & about", "Sable Health · M0 foundation")
    Notice("Local first", "Settings and user-confirmed companion setup are stored on this phone.")
    Notice("Your records", "No health measurements are currently imported or sent anywhere.")
    Notice("Data mode", "Demo numbers are synthetic. No records is an empty-data preview, not a sync switch.")
    Notice("Sable theme", "Dark Sable Blue #2E9BD6 · light accent #1A6FA0 · system default.")
    Notice("Permissions and access", "Health Connect read-only data access begins in M1 after permission review.")
}
