package io.github.aimindseye.sablehealth

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.LiveHealthPolicy
import io.github.aimindseye.sablehealth.core.LiveHour
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
class R7VisualRegressionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("America/New_York"))
    private val access = ConnectStatus("Available", "fixture", 1, setOf("read.steps"), setOf("read.steps"))

    @Test
    fun darkSquareScreenShowsApprovedContentWithoutScrolling() = exercise("dark", SablePalette.blue)

    @Test
    fun lightSquareScreenShowsApprovedContentWithoutScrolling() = exercise("light", SablePalette.blueLightSurface)

    private fun exercise(
        appearance: String,
        primary: Color,
    ) {
        val state = setup(appearance)
        compose.setContent {
            SableHealthTheme(appearance) {
                Surface(Modifier.fillMaxSize()) {
                    // Reserve system chrome in addition to the app's own header and tabs.
                    Box(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 12.dp)) {
                        HealthAppContent(state, DeviceProfile.Q25)
                    }
                }
            }
        }
        dashboard(primary)
        capture(appearance, "dashboard")
        section(state, Section.MOVE)
        metrics(listOf(LiveMetric.STEPS, LiveMetric.DISTANCE, LiveMetric.CALORIES, LiveMetric.ACTIVE_CALORIES))
        full(compose.onNodeWithTag("r7-icon-Previous metrics"), 48f)
        full(compose.onNodeWithTag("r7-icon-Next metrics"), 48f)
        capture(appearance, "move")
        compose.onNodeWithTag("r7-metric-STEPS").performClick()
        capture(appearance, "steps-detail")
        section(state, Section.VITALS)
        metrics(listOf(LiveMetric.HEART_RATE, LiveMetric.RESTING_HR, LiveMetric.HRV, LiveMetric.RESPIRATORY_RATE))
        capture(appearance, "vitals")
        compose.onNodeWithTag("r7-metric-HEART_RATE").performClick()
        capture(appearance, "heart-detail")
        section(state, Section.TRENDS)
        metrics(listOf(LiveMetric.STEPS, LiveMetric.DISTANCE, LiveMetric.ACTIVE_CALORIES, LiveMetric.CALORIES))
        check(compose.onAllNodesWithText("Mean · excl. today").fetchSemanticsNodes().size >= 4)
        capture(appearance, "trends")
        section(state, Section.SLEEP)
        full(compose.onNodeWithTag("r7-sleep-summary", useUnmergedTree = true), 30f)
        full(compose.onNodeWithContentDescription("Sleep stage transitions with recorded times"), 124f)
        compose.onNodeWithText("0h 40m asleep").assertIsDisplayed()
        full(compose.onNodeWithTag("r7-icon-Previous stage"), 48f)
        full(compose.onNodeWithTag("r7-icon-Next stage"), 48f)
        capture(appearance, "sleep")
    }

    private fun dashboard(primary: Color) {
        val widgets =
            listOf(
                DashboardWidget.STEPS,
                DashboardWidget.HEART_RATE,
                DashboardWidget.SLEEP_STAGES,
                DashboardWidget.HRV,
            )
        val images =
            widgets.map { widget ->
                val node = compose.onNodeWithTag("r7-widget-${widget.name}", useUnmergedTree = true)
                full(node, 48f)
                val bounds = node.fetchSemanticsNode().boundsInRoot
                compose.runOnIdle { renderR6Bitmap(compose.activity, bounds) }
            }
        val swatches = images.map { image -> image.getPixel(image.width / 2, image.height - 3) }
        check(swatches.distinct().size >= 3) { "Dashboard needs distinct category surfaces" }
        listOf("Refresh", "Sources", "Settings").forEach { label ->
            idleChrome(compose.onNodeWithTag("r7-icon-$label", useUnmergedTree = true), primary)
        }
        Section.entries.forEach { tab ->
            idleChrome(compose.onNodeWithTag("r7-tab-${tab.name}", useUnmergedTree = true), primary)
        }
    }

    private fun idleChrome(
        node: SemanticsNodeInteraction,
        primary: Color,
    ) {
        full(node, 48f)
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val image = compose.runOnIdle { renderR6Bitmap(compose.activity, bounds) }
        val edge =
            image.countInk(primary.toArgb(), 0, 2) +
                image.countInk(primary.toArgb(), image.width - 2, image.width)
        check(edge < image.height / 2) { "Idle chrome must not show a permanent focus outline" }
    }

    private fun metrics(items: List<LiveMetric>) {
        items.forEach { metric ->
            full(compose.onNodeWithTag("r7-metric-${metric.name}", useUnmergedTree = true), 48f)
            val description = "${metric.label}, 7 days; gaps are missing data"
            full(compose.onNodeWithContentDescription(description, useUnmergedTree = true), 32f)
        }
    }

    private fun full(
        node: SemanticsNodeInteraction,
        minHeight: Float,
    ) {
        node.assertIsDisplayed()
        val measured = node.fetchSemanticsNode()
        val density = compose.activity.resources.displayMetrics.density
        val bounds = measured.boundsInRoot
        check(bounds.height >= minHeight * density - 1f)
        check(abs(bounds.height - measured.size.height) <= 1f) {
            "Vertically clipped: ${measured.config}; visible=${bounds.height}, measured=${measured.size.height}"
        }
        check(abs(bounds.width - measured.size.width) <= 1f) {
            "Horizontally clipped: ${measured.config}; visible=${bounds.width}, measured=${measured.size.width}"
        }
    }

    private fun section(
        state: SableAppState,
        section: Section,
    ) {
        compose.runOnIdle {
            state.section = section
            state.chooseViewMode(section, ViewMode.GRAPHS)
        }
        compose.waitForIdle()
    }

    private fun capture(
        appearance: String,
        screen: String,
    ) {
        val bounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val image = compose.runOnIdle { renderR6Bitmap(compose.activity, bounds) }
        val out = File("build/reports/r7-visual")
        check(out.isDirectory || out.mkdirs())
        File(out, "$appearance-$screen.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    private fun setup(appearance: String): SableAppState {
        val store = compose.activity.getSharedPreferences("r7-visual-$appearance", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val state = SableAppState(store)
        state.chooseDataMode("real")
        state.connect = access
        state.r6.chooseStepGoal(8000)
        val start = Instant.parse("2026-11-24T04:00:00Z")
        val sleep =
            LiveSleep(
                start,
                start.plusSeconds(3600),
                "fixture",
                listOf(
                    LiveStage(start, start.plusSeconds(600), "Light"),
                    LiveStage(start.plusSeconds(600), start.plusSeconds(1200), "Deep"),
                    LiveStage(start.plusSeconds(1200), start.plusSeconds(1800), "Awake"),
                    LiveStage(start.plusSeconds(2400), start.plusSeconds(3600), "REM"),
                ),
            )
        val sleepSeries = LiveSeries(LiveStatus.AVAILABLE, LiveHealthPolicy.sleepPoints(listOf(sleep), window))
        val samples =
            mapOf(
                LiveMetric.STEPS to 4800.0,
                LiveMetric.DISTANCE to 3.1,
                LiveMetric.CALORIES to 1860.0,
                LiveMetric.ACTIVE_CALORIES to 170.0,
                LiveMetric.HEART_RATE to 69.0,
                LiveMetric.RESTING_HR to 67.0,
                LiveMetric.HRV to 24.0,
                LiveMetric.RESPIRATORY_RATE to 11.6,
            )
        val metrics =
            samples.mapValues { (_, reading) ->
                val today = LivePoint(window.today, reading, window.now, setOf("fixture"))
                val yesterday = LivePoint(window.today.minusDays(1), reading * .9, null, emptySet())
                LiveSeries(LiveStatus.AVAILABLE, listOf(yesterday, today))
            } + (LiveMetric.SLEEP_DURATION to sleepSeries)
        val snapshot = LiveSnapshot(window, metrics, listOf(sleep), listOf(LiveHour(window.todayStart, 40)))
        runBlocking { state.liveHealth.load(access) { snapshot } }
        return state
    }
}
