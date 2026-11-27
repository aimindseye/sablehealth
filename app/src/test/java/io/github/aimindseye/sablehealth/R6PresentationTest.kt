package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.aimindseye.sablehealth.core.ChartKind
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
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class R6PresentationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun presentationPreferencesPersistWithoutSavingMeasurements() {
        val store = compose.activity.getSharedPreferences("r6-prefs", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val preferences = R6Preferences(store)
        preferences.chooseChart(LiveMetric.STEPS, ChartKind.AREA)
        preferences.chooseChart(LiveMetric.HRV, ChartKind.POINTS)
        preferences.choosePeriod(Section.VITALS, 30)
        preferences.chooseStepGoal(8000)
        preferences.chooseSleepGraph("Donut")
        preferences.chooseTransitionLabels("All times")
        val restored = R6Preferences(store)
        check(restored.chart(LiveMetric.STEPS) == ChartKind.AREA)
        check(restored.chart(LiveMetric.HRV) == ChartKind.POINTS)
        check(restored.chart(LiveMetric.DISTANCE) == ChartKind.BAR)
        check(restored.period(Section.VITALS) == 30 && restored.period(Section.MOVE) == 7)
        check(restored.stepGoal == 8000 && restored.sleepGraph == "Donut" && restored.transitionLabels == "All times")
        check(store.all.keys.all { it.startsWith("r6.") })
    }

    @Test
    fun sleepGraphShowsRecordedTransitionsAndAlternativeViews() {
        val store = compose.activity.getSharedPreferences("r6-sleep", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val state = SableAppState(store)
        state.section = Section.SLEEP
        state.chooseViewMode(Section.SLEEP, ViewMode.GRAPHS)
        state.chooseDataMode("real")
        val access = ConnectStatus("Available", "fixture", 1, setOf("read.sleep"), setOf("read.sleep"))
        state.connect = access
        val start = Instant.parse("2026-11-24T04:00:00Z")
        val end = start.plusSeconds(3600)
        val sleep =
            LiveSleep(
                start,
                end,
                "fixture",
                listOf(
                    LiveStage(start, start.plusSeconds(600), "Light"),
                    LiveStage(start.plusSeconds(600), start.plusSeconds(1800), "Deep"),
                    LiveStage(start.plusSeconds(2400), end, "REM"),
                ),
            )
        val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("America/New_York"))
        val point = LivePoint(window.today, 60.0, end, setOf("fixture"))
        val snapshot =
            LiveSnapshot(
                window,
                mapOf(LiveMetric.SLEEP_DURATION to LiveSeries(LiveStatus.AVAILABLE, listOf(point))),
                listOf(sleep),
            )
        runBlocking { state.liveHealth.load(access) { snapshot } }
        compose.setContent {
            MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { LiveHealthPage(state) } }
        }
        compose.onNodeWithContentDescription("Sleep details").performScrollTo().performClick()
        visible("Recorded asleep: 50 min · gaps unknown")
        compose.onNodeWithContentDescription("Sleep details").performScrollTo().performClick()
        compose
            .onNodeWithContentDescription("Sleep stage transitions with recorded times")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Next stage").performScrollTo().performClick()
        visible("Light → Deep → End / gap")
        click("Key times")
        compose.onNodeWithText("All times").performClick()
        compose.runOnIdle { check(state.r6.transitionLabels == "All times") }
        click("Timeline")
        compose.onNodeWithText("Donut").performClick()
        visible("Unrecorded: 10 min")
        compose.onNodeWithContentDescription("Sleep details").performScrollTo().performClick()
        visible("Unrecorded: 0h 10m")
        compose.onNodeWithContentDescription("Sleep details").performScrollTo().performClick()
        click("Donut")
        compose.onNodeWithText("Stacked").performClick()
        compose
            .onNodeWithContentDescription("Sleep stage composition by night")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous night").performScrollTo().performClick()
        visible("2026-11-23 · No record")
        compose.onNodeWithContentDescription("Text").performScrollTo().performClick()
        visible("Deep · 20 min")
    }

    private fun visible(text: String) {
        compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }
}
