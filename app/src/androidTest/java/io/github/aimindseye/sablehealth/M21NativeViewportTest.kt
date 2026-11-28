package io.github.aimindseye.sablehealth

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.WindowCompat
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
        DashboardWidget.entries
            .filter {
                it in listOf(DashboardWidget.STEPS, DashboardWidget.HEART_RATE, DashboardWidget.SLEEP_STAGES, DashboardWidget.HRV)
            }.forEach { full(tag("r7-widget-${it.name}")) }
        capture(appearance, "dashboard")
        tab(Section.VITALS)
        listOf(LiveMetric.HEART_RATE, LiveMetric.RESTING_HR, LiveMetric.HRV, LiveMetric.RESPIRATORY_RATE).forEach {
            full(tag("r7-metric-${it.name}"))
        }
        capture(appearance, "vitals")
        history(appearance, LiveMetric.HRV)
        history(appearance, LiveMetric.RESTING_HR)
        tab(Section.MOVE)
        full(tag("r7-metric-STEPS"))
        full(tag("r7-metric-DISTANCE"))
        full(tag("r7-metric-CALORIES"))
        full(tag("r7-metric-ACTIVE_CALORIES"))
        capture(appearance, "move")
        clickIcon("Next metrics")
        full(tag("r7-metric-EXERCISE_SESSIONS"))
        full(tag("r7a-row-Heart-rate zones"))
        capture(appearance, "move-more")
        tab(Section.TRENDS)
        listOf(LiveMetric.STEPS, LiveMetric.DISTANCE, LiveMetric.ACTIVE_CALORIES, LiveMetric.CALORIES).forEach {
            full(tag("r7-metric-${it.name}"))
        }
        capture(appearance, "trends")
        clickIcon("Next metrics")
        clickIcon("Next metrics")
        clickIcon("Next metrics")
        full(tag("r7-metric-SKIN_TEMPERATURE"))
        tab(Section.SLEEP)
        full(tag("r7-sleep-summary"))
        full(compose.onNodeWithContentDescription("Sleep stage transitions with recorded times"))
        listOf("Previous stage", "Next stage", "Sleep details").forEach { full(tag("r7-icon-$it")) }
        full(tag("r7-choice-Transition labels"))
        capture(appearance, "sleep")
        clickIcon("Sleep details")
        tag("r7a-row-Sleep efficiency").performScrollTo().assertIsDisplayed()
        clickIcon("Sleep details")
        clickIcon("Text")
        full(tag("r7-sleep-summary"))
        full(tag("r7a-row-Sleep efficiency"))
        full(tag("r7a-row-Recorded stages"))
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
        listOf("All metrics", "Text", "Graphs", "Previous day", "Next day").forEach { full(tag("r7-icon-$it")) }
        listOf("Historical metric", "History period", "Graph type").forEach { full(tag("r7-choice-$it")) }
        val methods = tag("r7a-row-Method and sources")
        full(methods)
        val methodBounds = methods.fetchSemanticsNode().boundsInRoot
        val dayBounds = tag("r7-icon-Previous day").fetchSemanticsNode().boundsInRoot
        check(methodBounds.bottom <= dayBounds.top + 1f) { "Methods must remain in the history heading" }
        full(compose.onNodeWithText("2/7 days observed"))
        full(compose.onNodeWithText("Observed mean (excl. today):", substring = true))
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
        if (label == "All metrics" || label == "Sleep details") node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun full(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val measured = node.fetchSemanticsNode()
        check(abs(measured.boundsInRoot.width - measured.size.width) <= 1f) {
            "Horizontally clipped: ${measured.config}"
        }
        check(abs(measured.boundsInRoot.height - measured.size.height) <= 1f) {
            "Vertically clipped: ${measured.config}; " +
                "visible=${measured.boundsInRoot.height}, full=${measured.size.height}"
        }
    }

    private fun capture(
        appearance: String,
        name: String,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.runOnIdle {
            val window = compose.activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            check(controller.isAppearanceLightStatusBars == (appearance == "light"))
            check(controller.isAppearanceLightNavigationBars == (appearance == "light"))
        }
        val directory = File(instrumentation.targetContext.filesDir, "m21-native")
        check(directory.isDirectory || directory.mkdirs())
        val rejected = File(directory, "$appearance-$name-rejected.png")
        val image = renderedFrame(compose.activity.window.decorView, appearance, rejected)
        try {
            saveFrame(image, File(directory, "$appearance-$name.png"))
        } finally {
            image.recycle()
        }
    }

    private fun saveFrame(
        image: Bitmap,
        destination: File,
    ) {
        destination.outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    private fun awaitDisplayFrame(decor: View) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        // Compose idleness alone does not include the emulator's display compositor and navigation ripples.
        SystemClock.sleep(250)
        val drawn = CountDownLatch(1)
        instrumentation.runOnMainSync {
            decor.postOnAnimation {
                decor.postOnAnimation { drawn.countDown() }
            }
        }
        check(drawn.await(5, TimeUnit.SECONDS)) { "Frame synchronization timed out" }
    }

    private fun renderedFrame(
        decor: View,
        appearance: String,
        rejected: File,
    ): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val diagnostics = mutableListOf<String>()
        var previous: Bitmap? = null
        var previousValid = false
        try {
            repeat(16) { attempt ->
                awaitDisplayFrame(decor)
                val image = instrumentation.uiAutomation.takeScreenshot()
                if (image == null) {
                    diagnostics.add("attempt=${attempt + 1} null screenshot")
                    previousValid = false
                } else {
                    val valid = M21RenderedFrame.valid(image, appearance == "light")
                    val earlier = previous
                    val stable = valid && previousValid && earlier != null && M21RenderedFrame.sameBody(earlier, image)
                    diagnostics.add("attempt=${attempt + 1} valid=$valid stable=$stable ${M21RenderedFrame.describe(image)}")
                    if (stable) return image
                    previous?.recycle()
                    previous = image
                    previousValid = valid
                }
            }
            previous?.let { saveFrame(it, rejected) }
            error("HOLD_RENDERED_SCREENSHOT: ${rejected.name}; ${diagnostics.joinToString("; ")}")
        } finally {
            previous?.recycle()
        }
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
        val sleep =
            LiveSleep(
                start,
                end,
                "synthetic.fixture",
                listOf(LiveStage(start, middle, "Deep"), LiveStage(middle, end, "Awake in bed")),
            )
        val metrics =
            LiveMetric.entries.associateWith { metric ->
                val value =
                    when (metric) {
                        LiveMetric.HEART_RATE -> 74.0
                        LiveMetric.RESTING_HR -> 62.0
                        LiveMetric.HRV -> 28.0
                        else -> 42.0
                    }
                val points =
                    listOf(
                        LivePoint(window.today.minusDays(2), value, window.now.minusSeconds(172800), setOf("synthetic.fixture")),
                        LivePoint(window.today, value, window.now, setOf("synthetic.fixture")),
                    )
                LiveSeries(LiveStatus.PARTIAL, points)
            } + (LiveMetric.SLEEP_DURATION to LiveSeries(LiveStatus.AVAILABLE, LiveHealthPolicy.sleepPoints(listOf(sleep), window)))
        return LiveSnapshot(window, metrics, listOf(sleep))
    }
}
