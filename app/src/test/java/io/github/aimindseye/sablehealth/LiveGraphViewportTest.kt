package io.github.aimindseye.sablehealth

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
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
        compose.onNodeWithContentDescription("Previous day").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next day").assertIsDisplayed()
        compose.onNodeWithTag("r7a-row-Method and sources").assertIsDisplayed()
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
        compose.onNodeWithContentDescription("All metrics").performScrollTo().performClick()
        compose.onNodeWithTag("r7-metric-RESTING_HR").assertIsDisplayed().performClick()
        chart("Resting heart rate", 7).assertIsDisplayed()
        compose.onNodeWithContentDescription("Text").performClick()
        compose.onNodeWithTag("r7a-row-Method and sources").performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.RESTING_HR)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("r7a-row-Method and sources").performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.RESTING_HR)).assertDoesNotExist()
        compose.onNodeWithContentDescription("All metrics").performScrollTo().performClick()
        compose.runOnIdle {
            state.section = Section.TRENDS
            state.r6.chooseMetric(Section.TRENDS, null)
            state.chooseViewMode(Section.TRENDS, ViewMode.GRAPHS)
        }
        chart("Steps", 7).assertIsDisplayed()
    }

    @Test
    fun compactPagesShowRemainingMetricsAndCalculator() {
        val state = state()
        state.section = Section.MOVE
        state.chooseViewMode(Section.MOVE, ViewMode.TEXT)
        load(state, snapshot())
        show(state)
        compose.onNodeWithContentDescription("Previous metrics").performClick()
        compose.onNodeWithText("Metrics 1/2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next metrics").performClick()
        compose.onNodeWithContentDescription("Next metrics").performClick()
        compose.onNodeWithText("Metrics 2/2").assertIsDisplayed()
        compose.onNodeWithTag("r7-metric-EXERCISE_SESSIONS").assertIsDisplayed()
        compose.onNodeWithTag("r7a-row-Heart-rate zones").assertIsDisplayed().performClick()
        compose.onNodeWithTag("m2-bpm-Maximum HR (bpm)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous metrics").performScrollTo().performClick()
        compose.onNodeWithText("Metrics 1/2").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w280dp-h600dp")
    fun narrowWindowAllowsAccessibleUnpagedOverview() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.TEXT)
        load(state, snapshot())
        show(state)
        compose.onNodeWithContentDescription("Next metrics").assertDoesNotExist()
        compose.onNodeWithTag("r7-metric-SKIN_TEMPERATURE").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w600dp-h700dp")
    fun tallWindowDoesNotForceMetricPagination() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        load(state, snapshot())
        show(state)
        compose.onNodeWithContentDescription("Next metrics").assertDoesNotExist()
        compose.onNodeWithTag("r7-metric-SKIN_TEMPERATURE").performScrollTo().assertIsDisplayed()
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

    @Test
    fun noRecordsModeKeepsManualCalculatorAvailableWithoutDemoMeasurements() {
        val state = state()
        state.chooseDataMode("empty")
        state.section = Section.MOVE
        show(state)
        compose.onNodeWithText("No records imported").assertIsDisplayed()
        compose
            .onNodeWithText("No records mode · Choose Real data in Settings to read permitted Health Connect records.")
            .assertIsDisplayed()
        compose
            .onNodeWithTag("r7a-row-Heart-rate zones")
            .performScrollTo()
            .performClick()
        compose.onNodeWithTag("m2-bpm-Maximum HR (bpm)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("r7-metric-STEPS").assertDoesNotExist()
        compose.runOnIdle { check(state.liveHealth.snapshot == null && !state.demo && !state.live) }
    }

    @Test
    fun textHistoryReportsDeniedAccessAndOffersSourcesWithoutAChart() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.TEXT)
        val denied = mapOf(LiveMetric.HRV to LiveSeries(LiveStatus.DENIED))
        load(state, LiveSnapshot(window, denied, emptyList()))
        show(state)
        compose.onNodeWithTag("r7-metric-HRV").performClick()
        compose.onNodeWithText("No observations in this period").assertIsDisplayed()
        compose.onNodeWithText(LiveStatus.DENIED.label).assertIsDisplayed()
        chart("HRV (RMSSD)", 7).assertDoesNotExist()
        compose.onNodeWithText("View data sources").performClick()
        compose.runOnIdle { check(state.page == Page.SOURCES) }
    }

    @Test
    @Config(qualifiers = "w600dp-h700dp")
    fun unpagedOverviewPeriodUpdatesItsCharts() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        load(state, snapshot())
        show(state)
        compose.onNodeWithText("7D").performClick()
        compose.onNodeWithText("30D").performClick()
        chart("HRV (RMSSD)", 30).assertIsDisplayed()
        chart("HRV (RMSSD)", 7).assertDoesNotExist()
        compose.runOnIdle { check(state.r6.period(Section.VITALS) == 30) }
    }

    @Test
    @Config(qualifiers = "w280dp-h600dp")
    fun narrowHistoryKeepsFiltersAndMethodsReachable() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        load(state, snapshot())
        show(state)
        compose.onNodeWithTag("r7-metric-HRV").performScrollTo().performClick()
        compose.onNodeWithContentDescription("History period").performScrollTo().performClick()
        compose.onNodeWithText("30D").performClick()
        chart("HRV (RMSSD)", 30).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous day").performScrollTo().performClick()
        compose.onNodeWithTag("r7a-row-Method and sources").performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.HRV)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun largeFontUsesScrollableMethodsAndNoForcedPagination() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        load(state, snapshot())
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { fontScale = 1.5f }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                MaterialTheme { HealthAppContent(state, DeviceProfile.Q25) }
            }
        }
        compose.onNodeWithContentDescription("Next metrics").assertDoesNotExist()
        compose.onNodeWithTag("r7-metric-HRV").performScrollTo().performClick()
        chart("HRV (RMSSD)", 7).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("History period").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous day").performScrollTo().assertIsDisplayed()
        val method = compose.onNodeWithTag("r7a-row-Method and sources")
        method.performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.HRV)).performScrollTo().assertIsDisplayed()
        method.performScrollTo().performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.HRV)).assertDoesNotExist()
    }

    @Test
    fun inlineMethodsShowMissingHistoryWithoutFabricatingAnAverage() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        load(state, LiveSnapshot(window, emptyMap(), emptyList()))
        show(state)
        compose.onNodeWithTag("r7-metric-RESTING_HR").performClick()
        compose.onNodeWithText("No observations to graph").assertIsDisplayed()
        val method = compose.onNodeWithTag("r7a-row-Method and sources")
        method.performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.RESTING_HR)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Coverage: Not loaded · method v1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No observations in this period.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Observed mean (excl. today):", substring = true).assertDoesNotExist()
        method.performScrollTo().performClick()
        compose.onNodeWithText("No observations in this period.").assertDoesNotExist()
    }

    @Test
    fun trendsWithoutCompletedDaysShowMissingMeanThenUpdateAfterRefresh() {
        val state = state()
        state.section = Section.TRENDS
        state.chooseViewMode(Section.TRENDS, ViewMode.TEXT)
        val point = LivePoint(window.today, 120.0, window.now, setOf("fixture.writer"))
        val series = LiveSeries(LiveStatus.AVAILABLE, listOf(point))
        load(state, LiveSnapshot(window, mapOf(LiveMetric.STEPS to series), emptyList()))
        show(state)
        val steps = compose.onNodeWithTag("r7-metric-STEPS")
        steps.assert(hasText("—"))
        steps.performClick()
        compose.onNodeWithText("1/7 days observed").assertIsDisplayed()
        compose.onNodeWithText("Observed mean (excl. today):", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("r7a-row-Method and sources").performClick()
        compose.onNodeWithText(historyMethod(LiveMetric.STEPS)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Coverage: Available · method v1").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { load(state, snapshot()) }
        compose.onNodeWithText("2/7 days observed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Observed mean (excl. today):", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("All metrics").performScrollTo().performClick()
        steps.assert(hasText("20", substring = true))
    }

    @Test
    fun unavailableMetricOverviewUpdatesToPartialTimestampedReading() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.TEXT)
        val denied = LiveSeries(LiveStatus.DENIED)
        load(state, LiveSnapshot(window, mapOf(LiveMetric.HRV to denied), emptyList()))
        show(state)
        val hrv = compose.onNodeWithTag("r7-metric-HRV")
        hrv.assert(hasText(LiveStatus.DENIED.label))
        val observed = LivePoint(window.today, 28.0, window.now, setOf("fixture.writer"))
        val series = LiveSeries(LiveStatus.PARTIAL, listOf(observed))
        compose.runOnIdle { load(state, LiveSnapshot(window, mapOf(LiveMetric.HRV to series), emptyList())) }
        hrv.assert(hasText("28", substring = true))
        hrv.assert(hasText("${window.today} · partial"))
        hrv.performClick()
        compose.onNodeWithTag("r7a-row-Method and sources").performClick()
        compose
            .onNodeWithText("Coverage: ${LiveStatus.PARTIAL.label} · method v1")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(historyMethod(LiveMetric.HRV)).performScrollTo().assertIsDisplayed()
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
