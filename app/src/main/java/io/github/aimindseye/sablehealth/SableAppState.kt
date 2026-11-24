package io.github.aimindseye.sablehealth

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.SetupEntry

internal enum class ViewMode { TEXT, GRAPHS }

/** A single state owner keeps callbacks and preference writes out of Compose screen functions. */
internal class SableAppState(
    private val preferences: SharedPreferences,
) {
    val dashboard = DashboardPreferenceState(preferences)

    val companion = CompanionSyncPreferenceState(preferences)

    var profileOverride by mutableStateOf(preferences.getString("profile", "auto") ?: "auto")
        private set
    var appearance by mutableStateOf(preferences.getString("appearance", "system") ?: "system")
        private set
    var onboarded by mutableStateOf(preferences.getBoolean("onboarded", false))
        private set
    var demo by mutableStateOf(preferences.getBoolean("demo", true))
        private set
    private val viewModes =
        mutableStateMapOf<Section, ViewMode>().apply {
            Section.entries.forEach { section ->
                val default = if (section == Section.DASHBOARD) ViewMode.TEXT else ViewMode.GRAPHS
                this[section] = ViewMode.entries.firstOrNull {
                    it.name == preferences.getString("view.${section.name}", default.name)
                } ?: default
            }
        }

    fun modeFor(section: Section): ViewMode = viewModes[section] ?: ViewMode.TEXT

    fun chooseViewMode(
        section: Section,
        mode: ViewMode,
    ) {
        viewModes[section] = mode
        preferences.edit().putString("view.${section.name}", mode.name).apply()
    }

    var section by mutableStateOf(Section.DASHBOARD)
    var page by mutableStateOf(Page.MAIN)
    var diagnosticsVersion by mutableStateOf(0)
    var connect by mutableStateOf(ConnectStatus("Checking", "Inspecting Health Connect availability"))

    fun selectedProfile(detected: DeviceProfile): DeviceProfile =
        DeviceProfile.entries.firstOrNull { it.name == profileOverride } ?: detected

    fun chooseProfile(value: String) {
        profileOverride = value
        preferences.edit().putString("profile", value).apply()
    }

    fun chooseAppearance(value: String) {
        appearance = value
        preferences.edit().putString("appearance", value).apply()
    }

    fun chooseDemo(value: Boolean) {
        demo = value
        preferences.edit().putBoolean("demo", value).apply()
    }

    fun completeOnboarding(useDemo: Boolean) {
        chooseDemo(useDemo)
        preferences.edit().putBoolean("onboarded", true).apply()
        onboarded = true
    }

    fun back() {
        page =
            when (page) {
                Page.WIDGETS, Page.PHONE_INPUT, Page.SETTINGS_PRIVACY -> Page.SETTINGS
                Page.HELP -> Page.PHONE_INPUT
                Page.SOURCE_DETAIL, Page.SOURCE_PRIVACY, Page.M1_DISCOVERY -> Page.SOURCES
                Page.MAIN -> {
                    section = Section.DASHBOARD
                    Page.MAIN
                }
                else -> Page.MAIN
            }
    }

    fun nextTab() {
        page = Page.MAIN
        section = Section.entries[(section.ordinal + 1) % Section.entries.size]
    }

    var setupAction: (SetupEntry) -> Unit = {}

    fun recheckPermissions() {
        connect = ConnectStatus("Checking", "Checking Health Connect platform access")
        diagnosticsVersion++
    }

    fun refreshSources() {
        recheckPermissions()
        page = Page.SOURCES
    }
}
