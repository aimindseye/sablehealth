package io.github.aimindseye.sablehealth

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.DeviceProfile

/** A single state owner keeps callbacks and preference writes out of Compose screen functions. */
internal class SableAppState(private val preferences: SharedPreferences) {
    var profileOverride by mutableStateOf(preferences.getString("profile", "auto") ?: "auto")
        private set
    var appearance by mutableStateOf(preferences.getString("appearance", "system") ?: "system")
        private set
    var onboarded by mutableStateOf(preferences.getBoolean("onboarded", false))
        private set
    var demo by mutableStateOf(preferences.getBoolean("demo", true))
        private set
    var section by mutableStateOf(Section.TODAY)
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
        if (page != Page.MAIN) page = Page.MAIN else section = Section.TODAY
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

