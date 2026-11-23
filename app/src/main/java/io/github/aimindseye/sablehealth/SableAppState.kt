package io.github.aimindseye.sablehealth

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DashboardWidgetSelection
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.WidgetStyle

internal enum class ViewMode { TEXT, GRAPHS }

/** A single state owner keeps callbacks and preference writes out of Compose screen functions. */
internal class SableAppState(
    private val preferences: SharedPreferences,
) {
    var dashboardWidgets by mutableStateOf(DashboardWidgetSelection.restore(preferences.getString("dashboard.widgets.v1", null)))
        private set
    private val dashboardStyles =
        mutableStateMapOf<DashboardWidget, WidgetStyle>().apply {
            DashboardWidget.entries.forEach { widget ->
                this[widget] = DashboardWidgetSelection.styleFor(
                    widget,
                    preferences.getString("dashboard.style.${widget.name}", null),
                )
            }
        }

    fun styleFor(widget: DashboardWidget): WidgetStyle =
        dashboardStyles[widget] ?: DashboardWidgetSelection.styleFor(widget, null)

    fun saveDashboard(widgets: List<DashboardWidget>, styles: Map<DashboardWidget, WidgetStyle>) {
        require(DashboardWidgetSelection.isValid(widgets)) { "Select exactly four distinct widgets" }
        dashboardWidgets = widgets.toList()
        val edit = preferences.edit().putString("dashboard.widgets.v1", DashboardWidgetSelection.serialize(widgets))
        widgets.forEach { widget ->
            val style = DashboardWidgetSelection.styleFor(widget, styles[widget]?.name)
            dashboardStyles[widget] = style
            edit.putString("dashboard.style.${widget.name}", style.name)
        }
        edit.apply()
    }

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
        page = when (page) {
            Page.WIDGETS -> Page.SETTINGS
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

    fun refreshSources() {
        diagnosticsVersion++
        page = Page.SOURCES
    }
}
