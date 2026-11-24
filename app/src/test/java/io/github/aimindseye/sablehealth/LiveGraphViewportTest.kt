package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSeries
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h411dp")
class LiveGraphViewportTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("America/New_York"))
    private val access = ConnectStatus("Available", "fixture", 1, setOf("read.steps"), setOf("read.steps"))

    @Test
    fun graphIsVisibleWithoutScrollingAndTextKeepsAllReadings() {
        val state = state()
        state.section = Section.MOVE
        state.chooseViewMode(Section.MOVE, ViewMode.GRAPHS)
        load(state, snapshot())
        show(state)
        visibleChart("Steps", 7)
        compose.onNodeWithText("Exercise sessions").assertDoesNotExist()
        compose.onNodeWithText("30 days").performScrollTo().performClick()
        compose.onNodeWithText("Steps · 30 days").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Text").performScrollTo().performClick()
        compose.onNodeWithText("Exercise sessions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Steps, 30 days; gaps are missing data").assertDoesNotExist()
        compose.runOnIdle { state.chooseViewMode(Section.MOVE, ViewMode.GRAPHS) }
        visibleChart("Steps", 30)
    }

    @Test
    fun changingSectionAfterScrollingRevealsNewChartWithoutScrolling() {
        val state = state()
        state.section = Section.TRENDS
        state.chooseViewMode(Section.TRENDS, ViewMode.TEXT)
        load(state, snapshot())
        show(state)
        compose.onNodeWithText("Skin temperature delta").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            state.section = Section.VITALS
            state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        }
        visibleChart("Heart rate", 7)
        compose.runOnIdle {
            state.section = Section.TRENDS
            state.chooseViewMode(Section.TRENDS, ViewMode.GRAPHS)
        }
        visibleChart("Steps", 7)
    }

    @Test
    fun emptyHistoryShowsExplicitMessageInsteadOfBlankChart() {
        val state = state()
        state.section = Section.MOVE
        state.chooseViewMode(Section.MOVE, ViewMode.GRAPHS)
        load(state, LiveSnapshot(window, emptyMap(), emptyList()))
        show(state)
        compose.onNodeWithText("No observations to graph").assertIsDisplayed()
        compose.onNodeWithContentDescription("Steps, 7 days; gaps are missing data").assertDoesNotExist()
    }

    private fun state(): SableAppState {
        val preferences = compose.activity.getSharedPreferences("graph-viewport", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        return SableAppState(preferences).also {
            it.chooseDataMode("real")
            it.connect = access
        }
    }

    private fun snapshot(): LiveSnapshot {
        val today = LivePoint(window.today, 120.0, null, setOf("fixture.writer"))
        val yesterday = LivePoint(window.today.minusDays(1), 20.0, null, emptySet())
        val series = LiveSeries(LiveStatus.AVAILABLE, listOf(yesterday, today))
        return LiveSnapshot(window, LiveMetric.entries.associateWith { series }, emptyList())
    }

    private fun load(
        state: SableAppState,
        snapshot: LiveSnapshot,
    ) = runBlocking {
        state.liveHealth.load(access) { snapshot }
    }

    private fun show(state: SableAppState) {
        compose.setContent {
            MaterialTheme { HealthAppContent(state, DeviceProfile.Q25) }
        }
    }

    private fun visibleChart(
        metric: String,
        days: Int,
    ) {
        val chart = compose.onNodeWithContentDescription("$metric, $days days; gaps are missing data")
        chart.assertIsDisplayed()
        val density = compose.activity.resources.displayMetrics.density
        check(chart.fetchSemanticsNode().boundsInRoot.height >= 72f * density - 1f) {
            "Chart must be fully visible without a scroll action"
        }
    }
}
