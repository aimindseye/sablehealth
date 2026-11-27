package io.github.aimindseye.sablehealth

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.LiveHealthPolicy
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSeries
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStage
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs

/** Native synthetic-only viewport exercise. Never starts MainActivity or reads Health Connect. */
class M21NativeViewportTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val window = LiveWindow(Instant.parse("2026-11-26T20:00:00Z"), ZoneOffset.UTC)

    @Test
    fun lightQ25() = exercise("light")

    @Test
    fun darkQ25() = exercise("dark")

    private fun exercise(appearance: String) {
        val state = fixtureState(appearance)
        compose.setContent {
            SableHealthTheme(appearance) {
                Surface(Modifier.fillMaxSize()) {
                    Box { HealthAppContent(state, DeviceProfile.Q25) }
                }
            }
        }
        DashboardWidget.entries.filter {
            it in listOf(DashboardWidget.STEPS, DashboardWidget.HEART_RATE, DashboardWidget.SLEEP_STAGES, DashboardWidget.HRV)
        }.forEach { full(tag("r7-widget-${it.name}")) }
        capture(appearance, "dashboard")
        tab(Section.VITALS)
        full(tag("r7-metric-HRV"))
        full(tag("r7-metric-RESTING_HR"))
        capture(appearance, "vitals")
        history(appearance, LiveMetric.HRV)
        history(appearance, LiveMetric.RESTING_HR)
        tab(Section.MOVE)
        full(tag("r7-metric-STEPS"))
        full(tag("r7-metric-DISTANCE"))
        capture(appearance, "move")
        clickIcon("Next metrics")
        full(tag("r7-metric-EXERCISE_SESSIONS"))
        full(tag("r7a-row-Heart-rate zones"))
        capture(appearance, "move-more")
        tab(Section.TRENDS)
        full(tag("r7-metric-STEPS"))
        capture(appearance, "trends")
        clickIcon("Next metrics")
        clickIcon("Next metrics")
        clickIcon("Next metrics")
        full(tag("r7-metric-SKIN_TEMPERATURE"))
        tab(Section.SLEEP)
        full(tag("r7-sleep-summary"))
        full(compose.onNodeWithContentDescription("Sleep stage transitions with recorded times"))
        capture(appearance, "sleep")
        clickIcon("Text")
        full(tag("r7-sleep-summary"))
        tag("r7a-row-Recorded stages").performScrollTo().assertIsDisplayed()
        capture(appearance, "sleep-text")
        clickIcon("Sources")
        compose.onNodeWithText("Health Connect").assertIsDisplayed()
        capture(appearance, "sources")
        clickIcon("Back")
        clickIcon("Settings")
        compose.onNodeWithText("Dashboard widgets").assertIsDisplayed()
        capture(appearance, "settings")
    }

    private fun history(
        appearance: String,
        metric: LiveMetric,
    ) {
        clickIcon("Graphs")
        tag("r7-metric-${metric.name}").performClick()
        val chart = compose.onNodeWithContentDescription("${metric.label}, 7 days; gaps are missing data")
        full(chart)
        capture(appearance, metric.name.lowercase())
        clickIcon("Text")
        chart.assertDoesNotExist()
        tag("r7a-row-Method and sources").performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(metric)).performScrollTo().assertIsDisplayed()
        clickIcon("All metrics")
        clickIcon("Graphs")
    }

    private fun tab(section: Section) {
        tag("r7-tab-${section.name}").performClick()
        compose.waitForIdle()
    }

    private fun tag(value: String): SemanticsNodeInteraction = compose.onNodeWithTag(value, useUnmergedTree = true)

    private fun clickIcon(label: String) {
        val node = compose.onNodeWithContentDescription(label)
        if (label == "All metrics") node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun full(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val measured = node.fetchSemanticsNode()
        check(abs(measured.boundsInRoot.width - measured.size.width) <= 1f)
        check(abs(measured.boundsInRoot.height - measured.size.height) <= 1f)
    }

    private fun capture(
        appearance: String,
        name: String,
    ) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        check(image.width == 720 && image.height == 720) { "Expected native Q25 viewport" }
        val directory = File(instrumentation.targetContext.filesDir, "m21-native")
        check(directory.isDirectory || directory.mkdirs())
        File(directory, "$appearance-$name.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        image.recycle()
    }

    private fun fixtureState(appearance: String): SableAppState {
        val preferences = compose.activity.getSharedPreferences("m21-native-fixture-$appearance", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val state = SableAppState(preferences)
        state.chooseDataMode("real")
        val access = ConnectStatus("Available", "Synthetic fixture only", 1, setOf("fixture.read"), setOf("fixture.read"))
        state.connect = access
        runBlocking { state.liveHealth.load(access) { fixtureSnapshot() } }
        return state
    }

    private fun fixtureSnapshot(): LiveSnapshot {
        val start = Instant.parse("2026-11-26T04:00:00Z")
        val middle = start.plusSeconds(3600)
        val end = middle.plusSeconds(900)
        val sleep = LiveSleep(start, end, "synthetic.fixture", listOf(LiveStage(start, middle, "Deep"), LiveStage(middle, end, "Awake in bed")))
        val metrics = LiveMetric.entries.associateWith { metric ->
            val value = when (metric) {
                LiveMetric.HEART_RATE -> 74.0
                LiveMetric.RESTING_HR -> 62.0
                LiveMetric.HRV -> 28.0
                else -> 42.0
            }
            val points = listOf(
                LivePoint(window.today.minusDays(2), value, window.now.minusSeconds(172800), setOf("synthetic.fixture")),
                LivePoint(window.today, value, window.now, setOf("synthetic.fixture")),
            )
            LiveSeries(LiveStatus.PARTIAL, points)
        } + (LiveMetric.SLEEP_DURATION to LiveSeries(LiveStatus.AVAILABLE, LiveHealthPolicy.sleepPoints(listOf(sleep), window)))
        return LiveSnapshot(window, metrics, listOf(sleep))
    }
}
