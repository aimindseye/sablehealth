package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.SetupEntry

@Composable
internal fun SettingsPage(
    state: SableAppState,
    profile: DeviceProfile,
) {
    R7aGroup("Personalization") {
        R7aAppearance(state.appearance, state::chooseAppearance)
        R7aRule()
        R7aRow("Dashboard widgets", "${state.dashboard.widgets.size} selected", action = {
            state.widgetBrowsing = false
            state.page = Page.WIDGETS
        })
        if (state.live) {
            R7aRule()
            R6GoalSetting(state)
        }
    }
    R7aGroup("Health data") {
        R7aDataMode(state.dataMode, state::chooseDataMode)
        if (state.live) {
            R7aRule()
            R7aRefreshChoice(state)
        }
        R7aRule()
        R7aRow("Permissions & setup", action = { state.page = Page.SETTINGS_SETUP })
    }
    R7aGroup("Device & privacy") {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                R7aRow("Phone & keyboard", detail = profile.label, action = { state.page = Page.PHONE_INPUT })
            }
            Box(Modifier.weight(1f)) {
                R7aRow("Privacy & about", action = { state.page = Page.SETTINGS_PRIVACY })
            }
        }
    }
}

@Composable
internal fun R7aAppearance(
    current: String,
    choose: (String) -> Unit,
) {
    val options = mapOf("system" to "System", "light" to "Light", "dark" to "Dark")
    R7aChoiceRow("Appearance", options[current] ?: "System", options.values.toList()) { value ->
        choose(options.entries.first { it.value == value }.key)
    }
}

@Composable
internal fun R7aDataMode(
    current: String,
    choose: (String) -> Unit,
) {
    val options = mapOf("real" to "Real data", "demo" to "Demo", "empty" to "No records")
    R7aChoiceRow("Data mode", options[current] ?: "No records", options.values.toList()) { value ->
        choose(options.entries.first { it.value == value }.key)
    }
}

@Composable
internal fun R7aRefreshChoice(state: SableAppState) {
    val labels = refreshChoices.map { if (it == 0) "Off" else "$it min" }
    R7aChoiceRow("Foreground refresh", labels[refreshChoices.indexOf(state.refreshMinutes)], labels) { value ->
        state.chooseRefreshMinutes(refreshChoices[labels.indexOf(value)])
    }
}

@Composable
internal fun R7aSetupSettings(state: SableAppState) {
    R7aGroup("Health Connect", r7aStatusColor(state.connect.granted.isNotEmpty())) {
        R7aRow(state.connect.label, detail = setupAccessSummary(state.connect))
        R7aNote("Review your access at any time.")
    }
    R7aGroup("Setup") {
        R7aRow("Check permissions", action = { state.setupAction(SetupEntry.PERMISSIONS_ONLY) })
        R7aRule()
        R7aRow("Run complete setup", action = { state.setupAction(SetupEntry.REVIEW_ALL) })
        R7aRule()
        R7aRow("Choose setup steps", action = { state.setupAction(SetupEntry.SELECTED_STEPS) })
    }
    if (state.live) LiveRefreshSettings(state)
    R7aNote("Setup changes never reset existing widgets. No permissions requested automatically.")
}

@Composable
internal fun PhoneInputPage(
    state: SableAppState,
    profile: DeviceProfile,
) {
    R7aGroup("") { R7aRow("Current device", profile.label) }
    R7aGroup("Device profile") {
        R7aRow("Automatic detection", chosen = state.profileOverride == "auto", action = {
            state.chooseProfile("auto")
        })
        DeviceProfile.entries.filter { it in primaryPhoneProfiles }.forEach { option ->
            R7aRule()
            R7aRow(option.label, chosen = state.profileOverride == option.name, action = {
                state.chooseProfile(option.name)
            })
        }
    }
    R7aGroup("") {
        R7aRow("Keyboard controls", detail = "Trackpad, shortcuts and navigation", action = { state.page = Page.HELP })
    }
    R7aGroup("Other profiles") {
        DeviceProfile.entries.filterNot { it in primaryPhoneProfiles }.forEach { option ->
            R7aRow(option.label, chosen = state.profileOverride == option.name, action = {
                state.chooseProfile(option.name)
            })
        }
    }
    R7aNote("Custom key remapping is not available.")
}

@Composable
internal fun SettingsPrivacyPage() {
    R7aGroup("Sable Health") {
        R7aRow("Processing", "On-device")
        R7aRule()
        R7aRow("Saved locally", "Preferences")
        R7aRule()
        R7aRow("Health measurements", "Memory only")
    }
    R7aGroup("Data & access") {
        R7aRow("Real data", "Health Connect")
        R7aRule()
        R7aRow("Demo / No records", "No live reads")
        R7aRule()
        R7aRow("Permissions", "Optional · read only")
    }
    R7aNote("Manage or revoke access in Android settings. No measurements are saved or uploaded.")
    R7aNote("Appearance: System · Light · Dark")
}

private val primaryPhoneProfiles = setOf(DeviceProfile.Q25, DeviceProfile.TITAN2, DeviceProfile.P26)
