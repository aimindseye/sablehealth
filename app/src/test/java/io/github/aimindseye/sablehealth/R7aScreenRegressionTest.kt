package io.github.aimindseye.sablehealth

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.CoverageState
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.HealthMetricCoverage
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStage
import io.github.aimindseye.sablehealth.core.LiveWindow
import io.github.aimindseye.sablehealth.core.SetupEntry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h411dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class R7aScreenRegressionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun darkSourcesSettingsAndSetup() = exercise("dark")

    @Test
    fun lightSourcesSettingsAndSetup() = exercise("light")

    private fun exercise(appearance: String) {
        val state = fixture(appearance)
        val setup = mutableStateOf<SetupEntry?>(null)
        state.setupAction = { setup.value = it }
        compose.setContent {
            SableHealthTheme(appearance) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) {
                        val entry = setup.value
                        if (entry == null) {
                            HealthAppContent(state, DeviceProfile.Q25)
                        } else {
                            SetupWizard(state, DeviceProfile.Q25, entry) { setup.value = null }
                        }
                    }
                }
            }
        }
        sources(state, appearance)
        settings(state, appearance)
        wizard(setup, appearance)
    }

    private fun sources(
        state: SableAppState,
        appearance: String,
    ) {
        page(state, Page.SOURCES)
        fullTarget("r7a-row-Health Connect")
        capture(appearance, "sources")
        clickRow("Health Connect")
        capture(appearance, "connection")
        back()
        clickRow("Google Health / Fitbit")
        capture(appearance, "companion")
        back()
        clickRow("Check data availability")
        fullTarget("r7a-action-Scan allowed records")
        capture(appearance, "coverage")
        clickRow("Sleep")
        capture(appearance, "coverage-sleep")
        back()
        clickRow("Activity")
        capture(appearance, "coverage-activity")
        back()
        clickRow("Vitals")
        capture(appearance, "coverage-vitals")
        back()
        back()
        clickRow("Sync diagnostics")
        capture(appearance, "sync-diagnostics")
        clickRow("Review coverage report")
        capture(appearance, "report")
        back()
        clickRow("Sleep data diagnostics")
        capture(appearance, "sleep-diagnostics")
        populatedSleepDiagnostics(state, appearance)
        back()
        clickRow("Privacy & diagnostics")
        capture(appearance, "source-privacy")
    }

    private fun populatedSleepDiagnostics(
        state: SableAppState,
        appearance: String,
    ) {
        val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("UTC"))
        val start = window.now.minusSeconds(3600)
        val sleep = LiveSleep(start, window.now, "fixture.writer", listOf(LiveStage(start, window.now, "Deep")))
        compose.runOnIdle {
            runBlocking { state.liveHealth.load(state.connect) { LiveSnapshot(window, emptyMap(), listOf(sleep)) } }
        }
        compose.onNodeWithText("fixture.writer").performScrollTo().assertIsDisplayed()
        capture(appearance, "sleep-diagnostics-populated")
        compose.runOnIdle { state.liveHealth.clear() }
    }

    private fun settings(
        state: SableAppState,
        appearance: String,
    ) {
        page(state, Page.SETTINGS)
        capture(appearance, "settings")
        clickRow("Dashboard widgets")
        fullTarget("r7a-action-Save changes")
        capture(appearance, "widgets")
        fullTarget("r7a-action-Cancel · back to Settings", scroll = true)
        fullTarget("r7a-action-Save changes")
        capture(appearance, "widgets-scrolled")
        clickAction("Browse available widgets")
        capture(appearance, "widget-catalog")
        back()
        back()
        clickRow("Permissions & setup")
        capture(appearance, "setup-settings")
        back()
        clickRow("Phone & keyboard")
        fullTarget("r7a-row-Keyboard controls")
        capture(appearance, "phone-keyboard")
        fullTarget("r7a-row-Touch phone", scroll = true)
        capture(appearance, "phone-other-profiles")
        clickRow("Keyboard controls")
        capture(appearance, "keyboard-help")
        back()
        back()
        fullTarget("r7a-row-Privacy & about", scroll = true)
        capture(appearance, "settings-device-privacy")
        clickRow("Privacy & about")
        capture(appearance, "settings-privacy")
    }

    private fun wizard(
        setup: androidx.compose.runtime.MutableState<SetupEntry?>,
        appearance: String,
    ) {
        compose.runOnIdle { setup.value = SetupEntry.REVIEW_ALL }
        listOf("Welcome", "Sources", "Read permissions", "Verify", "Personalize", "Review").forEachIndexed { i, title ->
            compose.onNodeWithText(title).assertIsDisplayed()
            val last = title == "Review"
            if (last) fullTarget("r7a-row-Appearance", minHeight = 30f)
            val action = if (last) "Finish" else "Next"
            fullTarget("r7a-action-$action")
            capture(appearance, "setup-${i + 1}")
            compose.onNodeWithTag("r7a-action-$action").performClick()
        }
        compose.runOnIdle { setup.value = SetupEntry.SELECTED_STEPS }
        compose.onNodeWithText("Choose setup steps").assertIsDisplayed()
        capture(appearance, "setup-selected")
        compose.onNodeWithTag("r7a-action-Skip").performClick()
        compose.runOnIdle { setup.value = SetupEntry.PERMISSIONS_ONLY }
        capture(appearance, "setup-permissions-only")
        clickRow("Review permissions")
        capture(appearance, "setup-permissions-expanded")
        fullTarget("r7a-action-Manage Android permissions", scroll = true)
        capture(appearance, "setup-permissions-actions")
        compose.onNodeWithTag("r7a-action-Next").performClick()
        capture(appearance, "setup-verify-only")
        compose.onNodeWithTag("r7a-action-Next").performClick()
        fullTarget("r7a-row-Appearance", minHeight = 30f)
        capture(appearance, "setup-review-only")
        compose.onNodeWithTag("r7a-action-Finish").performClick()
    }

    private fun page(
        state: SableAppState,
        page: Page,
    ) {
        compose.runOnIdle { state.page = page }
        compose.waitForIdle()
    }

    private fun clickRow(label: String) {
        val tag = "r7a-row-$label"
        fullTarget(tag, scroll = true)
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun clickAction(label: String) {
        val tag = "r7a-action-$label"
        fullTarget(tag, scroll = true)
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun back() {
        fullTarget("r7-icon-Back")
        compose.onNodeWithTag("r7-icon-Back").performClick()
        compose.waitForIdle()
    }

    private fun fullTarget(
        tag: String,
        scroll: Boolean = false,
        minHeight: Float = 48f,
    ) {
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed()
        val measured = node.fetchSemanticsNode()
        val bounds = measured.boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        check(bounds.height >= minHeight * density - 1f) { "Touch target too small: $tag" }
        check(bounds.width >= 48 * density - 1f) { "Touch target too narrow: $tag" }
        check(abs(bounds.height - measured.size.height) <= 1f) { "Vertically clipped: $tag" }
        check(abs(bounds.width - measured.size.width) <= 1f) { "Horizontally clipped: $tag" }
    }

    private fun capture(
        appearance: String,
        screen: String,
    ) {
        compose.waitForIdle()
        val bounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val image = compose.runOnIdle { renderR6Bitmap(compose.activity, bounds) }
        val out = File("build/reports/r7a-visual")
        check(out.isDirectory || out.mkdirs())
        File(out, "$appearance-$screen.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    private fun fixture(appearance: String): SableAppState {
        val store = compose.activity.getSharedPreferences("r7a-visual-$appearance", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val state = SableAppState(store)
        state.chooseDataMode("real")
        val permissions = M1HealthConnectProbe.metrics.map { it.permission }.toSet()
        state.connect = ConnectStatus("Available", "Synthetic fixture", permissions.size, permissions, permissions)
        state.companion.chooseConfirmation(true)
        state.discovery.scannedAt = Instant.now().toString()
        state.discovery.results =
            M1HealthConnectProbe.metrics.map { metric ->
                HealthMetricCoverage(
                    metric.name,
                    metric.permission,
                    CoverageState.RECORDS_FOUND,
                    recordCount = 1,
                    lastObservedUtc = "2026-11-24T12:05:00Z",
                    originPackages = listOf("fixture.writer"),
                )
            }
        state.page = Page.SOURCES
        return state
    }
}
