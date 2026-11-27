package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import io.github.aimindseye.sablehealth.core.ChartKind
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
    fun overviewAndDetailChartAreVisibleWithoutScrolling() {
        val state = state()
        state.section = Section.MOVE
        state.chooseViewMode(Section.MOVE, ViewMode.GRAPHS)
        load(state, snapshot())
        show(state)
        chart("Steps", 7).assertIsDisplayed()
        compose.onNodeWithText("Steps").performClick()
        chart("Steps", 7).assertIsDisplayed()
        val density = compose.activity.resources.displayMetrics.density
        check(chart("Steps", 7).fetchSemanticsNode().boundsInRoot.height >= 100f * density - 1f)
        compose.onNodeWithText("7D").performClick()
        compose.onNodeWithText("30D").performClick()
        chart("Steps", 30).assertIsDisplayed()
        compose.onNodeWithText("Bar").performClick()
        compose.onNodeWithText("Area").performClick()
        compose.runOnIdle { check(state.r6.chart(LiveMetric.STEPS) == ChartKind.AREA) }
        chart("Steps", 30).performTouchInput { click(center) }
        compose.onNodeWithText("No record").assertIsDisplayed()
        compose.onNodeWithContentDescription("Text").performClick()
        compose.onNodeWithTag("r7a-row-Method and sources").performScrollTo().assertIsDisplayed()
        chart("Steps", 30).assertDoesNotExist()
        compose.runOnIdle { state.chooseViewMode(Section.MOVE, ViewMode.GRAPHS) }
        chart("Steps", 30).assertIsDisplayed()
    }

    @Test
    fun switchingSectionsAfterScrollingShowsNewOverview() {
        val state = state()
        state.section = Section.TRENDS
        state.chooseViewMode(Section.TRENDS, ViewMode.TEXT)
        load(state, snapshot())
        show(state)
        repeat(3) { compose.onNodeWithContentDescription("Next metrics").performClick() }
        compose.onNodeWithText("Skin temperature delta").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous metrics").performClick()
        compose.onNodeWithContentDescription("Next metrics").performClick()
        compose.onNodeWithText("Skin temperature delta").performClick()
        compose.onNodeWithTag("r7a-row-Method and sources").performScrollTo().performClick()
        compose.runOnIdle {
            state.section = Section.VITALS
            state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        }
        chart("Heart rate", 7).assertIsDisplayed()
        compose.onNodeWithTag("r7-metric-HRV").assertIsDisplayed().performClick()
        chart("HRV (RMSSD)", 7).assertIsDisplayed()
        compose.onNodeWithTag("r7a-row-Method and sources").performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.HRV)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("All metrics").performClick()
        compose.onNodeWithTag("r7-metric-RESTING_HR").assertIsDisplayed().performClick()
        chart("Resting heart rate", 7).assertIsDisplayed()
        compose.onNodeWithContentDescription("Text").performClick()
        compose.onNodeWithTag("r7a-row-Method and sources").performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.RESTING_HR)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("All metrics").performClick()
        compose.runOnIdle {
            state.section = Section.TRENDS
            state.r6.chooseMetric(Section.TRENDS, null)
            state.chooseViewMode(Section.TRENDS, ViewMode.GRAPHS)
        }
        chart("Steps", 7).assertIsDisplayed()
    }

    @Test
    fun missingHistoryShowsContextualCardAndNoFalseChart() {
        val state = state()
        state.section = Section.MOVE
        state.chooseViewMode(Section.MOVE, ViewMode.GRAPHS)
        load(state, LiveSnapshot(window, emptyMap(), emptyList()))
        show(state)
        compose.onNodeWithText("Steps").performClick()
        compose.onNodeWithText("No observations to graph").assertIsDisplayed()
        chart("Steps", 7).assertDoesNotExist()
        compose.onNodeWithText("View data sources").performScrollTo().performClick()
        compose.runOnIdle { check(state.page == Page.SOURCES) }
    }

    private fun state(): SableAppState {
        val preferences = compose.activity.getSharedPreferences("r6-viewport", Context.MODE_PRIVATE)
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
        compose.setContent { MaterialTheme { HealthAppContent(state, DeviceProfile.Q25) } }
    }

    private fun chart(
        metric: String,
        days: Int,
    ) = compose.onNodeWithContentDescription(
        "$metric, $days days; gaps are missing data",
        useUnmergedTree = true,
    )
}
