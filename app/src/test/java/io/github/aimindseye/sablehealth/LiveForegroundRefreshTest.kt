package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveForegroundRefreshTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun foregroundAccessRecheckInvalidatesSnapshotAndBackgroundStopsWork() {
        val context = compose.activity
        val preferences = context.getSharedPreferences("r4-foreground", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val state = SableAppState(preferences)
        val granted = ConnectStatus("Available", "fixture", 1, setOf("read.steps"), setOf("read.steps"))
        val revoked = ConnectStatus("Unavailable", "fixture access unavailable")
        val foreground = mutableStateOf(false)
        val setup = mutableStateOf(false)
        val checks = AtomicInteger(0)
        state.chooseDataMode("real")
        state.connect = granted
        compose.setContent {
            MaterialTheme {
                Text(state.connect.label)
                LiveRefreshEffect(state, context, foreground.value, setup.value) {
                    checks.incrementAndGet()
                    revoked
                }
            }
        }
        compose.runOnIdle { check(checks.get() == 0) }
        compose.runOnIdle {
            val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("UTC"))
            val snapshot = LiveSnapshot(window, emptyMap(), emptyList())
            runBlocking { state.liveHealth.load(granted) { snapshot } }
            check(state.liveHealth.snapshot == snapshot)
            foreground.value = true
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Unavailable").assertIsDisplayed()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.runOnIdle {
            check(checks.get() >= 2)
            check(state.connect == revoked)
            check(state.liveHealth.snapshot == null)
            foreground.value = false
        }
        compose.waitForIdle()
        val stopped = checks.get()
        compose.runOnIdle { state.refreshLive() }
        compose.waitForIdle()
        check(checks.get() == stopped)
        compose.runOnIdle {
            setup.value = true
            foreground.value = true
        }
        compose.waitForIdle()
        check(checks.get() == stopped)
        compose.runOnIdle { state.chooseDataMode("demo") }
        compose.runOnIdle { setup.value = false }
        compose.waitForIdle()
        check(checks.get() == stopped)
    }

    @Test
    fun compactChromeRoutesRealSettingsAndSourcesWithoutLosingPreviewChoices() {
        val preferences = compose.activity.getSharedPreferences("r4-navigation", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val state = SableAppState(preferences)
        compose.setContent {
            MaterialTheme { HealthAppContent(state, DeviceProfile.Q25) }
        }
        compose.onNodeWithText("DASHBOARD · DEMO").assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Real data").performScrollTo().performClick()
        compose.onNodeWithText("Foreground refresh").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            check(state.live)
            state.connect = ConnectStatus("Available", "fixture", 0)
            state.page = Page.MAIN
        }
        compose.onNodeWithText("Real data · Not loaded").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sources").assertIsDisplayed().performClick()
        compose.onNodeWithText("Sleep data diagnostics").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            state.chooseDataMode("empty")
            state.page = Page.MAIN
        }
        compose.onNodeWithText("DASHBOARD · NO RECORDS").assertIsDisplayed()
    }

    @Test
    fun refreshRequestBlockedWhileLoadingAndInPreviewMode() {
        val preferences = compose.activity.getSharedPreferences("r4-refresh-guard", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val state = SableAppState(preferences)
        val access = ConnectStatus("Available", "fixture", 1, setOf("read.steps"), setOf("read.steps"))
        state.connect = access
        state.refreshLive()
        check(state.liveRefreshVersion == 0)
        state.chooseDataMode("real")
        state.refreshLive()
        check(state.liveRefreshVersion == 0)
        state.connect = access
        val window = LiveWindow(Instant.parse("2026-11-24T20:00:00Z"), ZoneId.of("UTC"))
        val snapshot = LiveSnapshot(window, emptyMap(), emptyList())
        runBlocking {
            state.liveHealth.load(access) {
                state.refreshLive()
                check(state.liveRefreshVersion == 0)
                snapshot
            }
        }
        state.refreshLive()
        check(state.liveRefreshVersion == 1)
        state.chooseDataMode("empty")
        check(state.liveHealth.snapshot == null)
        state.refreshLive()
        check(state.liveRefreshVersion == 1)
    }
}
