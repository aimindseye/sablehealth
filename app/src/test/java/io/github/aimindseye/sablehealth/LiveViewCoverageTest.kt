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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
@Config(sdk = [34])
class LiveViewCoverageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptySleepDoesNotPreventOtherTabsAndRefresh() {
        val preferences = compose.activity.getSharedPreferences("live-view", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val state = SableAppState(preferences)
        val access = ConnectStatus("Available", "fixture", 1, setOf("read.steps"), setOf("read.steps"))
        state.chooseDataMode("real")
        state.connect = access
        val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("America/New_York"))
        val steps = LivePoint(window.today, 123.0, null, setOf("example.writer"))
        val snapshot =
            LiveSnapshot(
                window,
                mapOf(
                    LiveMetric.STEPS to LiveSeries(LiveStatus.AVAILABLE, listOf(steps)),
                    LiveMetric.SLEEP_DURATION to LiveSeries(LiveStatus.NO_RECORDS),
                ),
                emptyList(),
            )
        runBlocking { state.liveHealth.load(access) { snapshot } }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { LiveHealthPage(state) }
            }
        }
        compose.onNodeWithContentDescription("Refresh").assertIsDisplayed().performClick()
        compose.runOnIdle {
            check(state.liveRefreshVersion == 1)
            state.section = Section.SLEEP
        }
        compose
            .onNodeWithText("If Health Connect has no sessions", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("r7a-row-Sleep history").performScrollTo().performClick()
        compose.onNodeWithText("7D").performScrollTo().performClick()
        compose.onNodeWithText("30D").performClick()
        compose.runOnIdle { state.section = Section.MOVE }
        compose.onNodeWithText("Steps").performScrollTo().assertIsDisplayed()
    }
}
