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
    private val newDiscovery: () -> DiscoveryState = { DiscoveryState() },
) {
    val dashboard = DashboardPreferenceState(preferences)
    val r6 = R6Preferences(preferences)

    val companion = CompanionSyncPreferenceState(preferences)

    var profileOverride by mutableStateOf(preferences.getString("profile", "auto") ?: "auto")
        private set
    var appearance by mutableStateOf(preferences.getString("appearance", "system") ?: "system")
        private set
    var onboarded by mutableStateOf(preferences.getBoolean("onboarded", false))
        private set
    var demo by mutableStateOf(preferences.getBoolean("demo", true))
        private set
    var live by mutableStateOf(preferences.getBoolean("live_data", false) && !demo)
        private set
    val liveHealth = LiveHealthState()
    var liveRefreshVersion by mutableStateOf(0)
        private set
    var refreshMinutes by mutableStateOf(
        validRefreshMinutes(preferences.getInt("refresh_minutes", DEFAULT_REFRESH_MINUTES)),
    )
        private set
    val dataMode: String get() =
        if (live) {
            "real"
        } else if (demo) {
            "demo"
        } else {
            "empty"
        }
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
    var widgetBrowsing by mutableStateOf(false)
    var coverageCategory by mutableStateOf("Sleep")
    var coverageReturn by mutableStateOf(Page.SOURCES)
    var reportReturn by mutableStateOf(Page.M1_DISCOVERY)
    var page by mutableStateOf(Page.MAIN)
    var diagnosticsVersion by mutableStateOf(0)
    private var connection by mutableStateOf(ConnectStatus("Checking", "Inspecting Health Connect availability"))
    var discovery by mutableStateOf(newDiscovery())
        private set
    var connect: ConnectStatus
        get() = connection
        set(value) {
            val accessChanged = connection.granted != value.granted || connection.supported != value.supported
            if (accessChanged || value.grantedCount == null) {
                discovery = newDiscovery()
                liveHealth.clear()
            }
            connection = value
        }

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
        chooseDataMode(if (value) "demo" else "empty")
    }

    fun chooseDataMode(value: String) {
        require(value in setOf("real", "demo", "empty"))
        liveHealth.clear()
        demo = value == "demo"
        live = value == "real"
        preferences
            .edit()
            .putBoolean("demo", demo)
            .putBoolean("live_data", live)
            .apply()
        if (live) recheckPermissions()
    }

    fun chooseRefreshMinutes(value: Int) {
        refreshMinutes = validRefreshMinutes(value)
        preferences.edit().putInt("refresh_minutes", refreshMinutes).apply()
    }

    fun refreshLive() {
        if (live && !liveHealth.loading && connect.label != "Checking") liveRefreshVersion++
    }

    fun completeOnboarding(useDemo: Boolean) {
        if (useDemo != demo) chooseDemo(useDemo)
        preferences.edit().putBoolean("onboarded", true).apply()
        onboarded = true
    }

    var setupAction: (SetupEntry) -> Unit = {}
}

internal fun SableAppState.back() {
    if (page == Page.WIDGETS && widgetBrowsing) {
        widgetBrowsing = false
        return
    }
    page =
        when (page) {
            Page.WIDGETS, Page.PHONE_INPUT, Page.SETTINGS_PRIVACY, Page.SETTINGS_SETUP -> Page.SETTINGS
            Page.HELP -> Page.PHONE_INPUT
            Page.SOURCE_DETAIL, Page.SOURCE_COMPANION, Page.SOURCE_DIAGNOSTICS -> Page.SOURCES
            Page.SOURCE_PRIVACY, Page.SLEEP_DIAGNOSTICS -> Page.SOURCE_DIAGNOSTICS
            Page.M1_DISCOVERY -> coverageReturn
            Page.SOURCE_COVERAGE -> Page.M1_DISCOVERY
            Page.SOURCE_REPORT -> reportReturn
            Page.MAIN -> {
                if (live && r6.detailMetric(section) != null) {
                    r6.chooseMetric(section, null)
                } else {
                    section = Section.DASHBOARD
                }
                Page.MAIN
            }
            else -> Page.MAIN
        }
}

internal fun SableAppState.nextTab() {
    page = Page.MAIN
    section = Section.entries[(section.ordinal + 1) % Section.entries.size]
}

internal fun SableAppState.refreshSources() {
    recheckPermissions()
    page = Page.SOURCES
}

internal fun SableAppState.recheckPermissions() {
    connect = ConnectStatus("Checking", "Checking Health Connect platform access")
    diagnosticsVersion++
}
