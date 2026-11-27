package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DashboardWidget
import io.github.aimindseye.sablehealth.core.LiveHour
import io.github.aimindseye.sablehealth.core.LiveMetric
import io.github.aimindseye.sablehealth.core.LivePoint
import io.github.aimindseye.sablehealth.core.LiveSeries
import io.github.aimindseye.sablehealth.core.LiveSleep
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveStage
import io.github.aimindseye.sablehealth.core.LiveStatus
import io.github.aimindseye.sablehealth.core.LiveWindow
import io.github.aimindseye.sablehealth.core.WidgetStyle
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
class LiveRichViewsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("America/New_York"))
    private val permission = "read.steps"
    private val access = ConnectStatus("Available", "fixture", 1, setOf(permission), setOf(permission))
    private val start = Instant.parse("2026-11-24T04:30:00Z")
    private val end = start.plusSeconds(3600)
    private val sleep = LiveSleep(start, end, "fixture.writer", listOf(LiveStage(start, end, "Deep")))
    private val nap = LiveSleep(end.plusSeconds(3600), end.plusSeconds(7200), "fixture.writer", emptyList())

    @Test
    fun refreshSettingsCycleAndSleepDiagnosticsReflectNewSnapshot() {
        val state = state()
        show {
            LiveRefreshSettings(state)
            LiveSleepDiagnostic(state)
        }
        visible("CURRENT READ WINDOW")
        visible("5 min")
        for (minutes in listOf(15, 30, 0, 1, 5)) {
            compose.onNodeWithContentDescription("Foreground refresh").performScrollTo().performClick()
            compose.onNodeWithText(if (minutes == 0) "Off" else "$minutes min").performClick()
            compose.runOnIdle { check(state.refreshMinutes == minutes) }
        }
        compose.runOnIdle { load(state, LiveSnapshot(window, emptyMap(), emptyList())) }
        visible("Not observed")
        compose.runOnIdle { load(state, richSnapshot()) }
        visible("fixture.writer")
        visible("Sessions with stages")
        val row = compose.onNodeWithTag("r7a-row-Sessions with stages")
        row.performScrollTo().assertIsDisplayed().assert(hasText("1"))
        click("Refresh measurements")
        compose.runOnIdle { check(state.liveRefreshVersion == 1) }
    }

    @Test
    fun sleepStagesAndUnstagedSessionsRenderInGraphAndTextModes() {
        val state = state()
        state.section = Section.SLEEP
        state.chooseViewMode(Section.SLEEP, ViewMode.GRAPHS)
        load(state, richSnapshot())
        show { LiveHealthPage(state) }
        visible("Recorded asleep: 60 min · gaps unknown")
        visible("No stages exported")
        visible("Deep · 60 min")
        check(compose.onAllNodesWithTag("r7a-row-Sleep efficiency").fetchSemanticsNodes().size == 2)
        compose.runOnIdle { state.chooseViewMode(Section.SLEEP, ViewMode.TEXT) }
        visible("No stages exported")
        visible("Deep · 60 min")
        click("7D")
        compose.onNodeWithText("30D").performClick()
        visible("2/30 days observed", substring = true)
        compose.runOnIdle {
            val old = richSnapshot()
            load(state, old.copy(sleeps = emptyList(), metrics = old.metrics - LiveMetric.SLEEP_DURATION))
        }
        visible("If Health Connect has no sessions", substring = true)
    }

    @Test
    fun vitalsHistoryAndTrendsRetainGapsAndObservedAverage() {
        val state = state()
        state.section = Section.VITALS
        state.chooseViewMode(Section.VITALS, ViewMode.GRAPHS)
        load(state, richSnapshot())
        show { LiveHealthPage(state) }
        click("Heart rate")
        visible("Observed mean (excl. today): 20 bpm")
        visible("2/7 days observed", substring = true)
        click("Heart rate")
        click("Resting heart rate")
        visible("Observed mean (excl. today): 20 bpm")
        click("7D")
        click("30D")
        visible("2/30 days observed", substring = true)
        compose.runOnIdle { state.chooseViewMode(Section.VITALS, ViewMode.TEXT) }
        visible("2/30 days observed", substring = true)
        compose.runOnIdle { state.section = Section.TRENDS }
        visible("Steps")
        compose.runOnIdle {
            state.section = Section.MOVE
            state.chooseViewMode(Section.MOVE, ViewMode.TEXT)
        }
        visible("Exercise sessions")
        visible("Method and sources")
    }

    @Test
    fun narrowDashboardShowsRealGraphsAndUnsupportedWidgetStates() {
        val state = state()
        val widgets =
            listOf(
                DashboardWidget.STEPS,
                DashboardWidget.RESTING_HR,
                DashboardWidget.SLEEP_STAGES,
                DashboardWidget.SLEEP_SCHEDULE,
            )
        state.dashboard.save(
            widgets,
            mapOf(DashboardWidget.STEPS to WidgetStyle.GRAPH, DashboardWidget.SLEEP_STAGES to WidgetStyle.GRAPH),
        )
        load(state, richSnapshot())
        show { Column(Modifier.width(280.dp)) { LiveHealthPage(state) } }
        compose
            .onNodeWithContentDescription("Today's observed hourly steps; gaps unknown")
            .performScrollTo()
            .assertIsDisplayed()
        visible("Latest session", substring = true)
        compose.runOnIdle {
            state.dashboard.save(
                listOf(
                    DashboardWidget.DATA_FRESHNESS,
                    DashboardWidget.READINESS,
                    DashboardWidget.HRV,
                    DashboardWidget.SLEEP_SCHEDULE,
                ),
                emptyMap(),
            )
        }
        visible("Not available in M1-C")
        compose.runOnIdle { load(state, richSnapshot().copy(sleeps = emptyList())) }
        visible("120 min")
    }

    @Test
    fun missingPermissionAndReadFailureNeverShowDemoValues() {
        val state = state()
        state.connect = ConnectStatus("Checking", "fixture")
        show { LiveHealthPage(state) }
        visible("Checking Health Connect access")
        compose.runOnIdle { state.connect = ConnectStatus("Unavailable", "Provider unavailable") }
        visible("Provider unavailable")
        compose.runOnIdle { state.connect = access.copy(grantedCount = 0, granted = emptySet()) }
        visible("Grant read permissions in Sources")
        compose.runOnIdle { state.connect = access }
        visible("Loading local measurements")
        compose.runOnIdle { runBlocking { state.liveHealth.load(access) { throw IllegalStateException() } } }
        visible("Refresh failed. Retry when ready.")
        visible("Refresh failed · retry")
    }

    private fun state(): SableAppState {
        val preferences = compose.activity.getSharedPreferences("r4-rich", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        return SableAppState(preferences).also {
            it.chooseDataMode("real")
            it.connect = access
        }
    }

    private fun richSnapshot(): LiveSnapshot {
        val current = LivePoint(window.today, 120.0, window.now.minusSeconds(60), setOf("fixture.writer"))
        val previous = LivePoint(window.today.minusDays(1), 20.0, null, emptySet())
        val series = LiveSeries(LiveStatus.AVAILABLE, listOf(previous, current))
        val metrics = LiveMetric.entries.associateWith { series }.toMutableMap()
        metrics[LiveMetric.HRV] = series.copy(status = LiveStatus.PARTIAL)
        return LiveSnapshot(window, metrics, listOf(sleep, nap), listOf(LiveHour(window.todayStart, 10)))
    }

    private fun load(
        state: SableAppState,
        snapshot: LiveSnapshot,
    ) = runBlocking {
        state.liveHealth.load(access) { snapshot }
    }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { content() } }
        }
    }

    private fun visible(
        text: String,
        substring: Boolean = false,
    ) {
        compose.onNodeWithText(text, substring = substring).performScrollTo().assertIsDisplayed()
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }
}
