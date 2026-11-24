package io.github.aimindseye.sablehealth

import android.content.Context
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import io.github.aimindseye.sablehealth.core.LiveWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveStateCoverageTest {
    private val zone = ZoneId.of("America/New_York")
    private val now = Instant.parse("2026-11-24T20:00:00Z")
    private val permission = "read.steps"
    private val access = ConnectStatus("Available", "fixture", 1, setOf(permission), setOf(permission))
    private val snapshot = LiveSnapshot(LiveWindow(now, zone), emptyMap(), emptyList())

    @Test
    fun refreshClockHandlesIntervalMidnightTimezoneAndClockRollback() {
        check(!refreshDue(now, now.plusSeconds(299), zone, zone, 5))
        check(refreshDue(now, now.plusSeconds(300), zone, zone, 5))
        check(!refreshDue(now, now.plusSeconds(300), zone, zone, 0))
        check(refreshDue(now, now.plusSeconds(1), zone, ZoneId.of("UTC"), 0))
        check(refreshDue(now, now.minusSeconds(1), zone, zone, 0))
        check(refreshDue(now, Instant.parse("2026-11-25T05:00:00Z"), zone, zone, 0))
        check(validRefreshMinutes(-1) == 5)
        check(validRefreshMinutes(30) == 30)
    }

    @Test
    fun loadConsolidatesConcurrentRequestsAndInvalidatesStaleCompletion() =
        runBlocking {
            val state = LiveHealthState()
            val entered = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            val job =
                launch {
                    state.load(access) {
                        entered.complete(Unit)
                        finish.await()
                        snapshot
                    }
                }
            entered.await()
            check(state.loading)
            state.load(access) { error("Concurrent read not allowed") }
            state.clear()
            finish.complete(Unit)
            job.join()
            check(state.snapshot == null && !state.loading)
            state.load(access) { snapshot }
            check(state.snapshot == snapshot && !state.failed)
            state.clear()
            state.load(access) { throw IllegalStateException() }
            check(state.failed && !state.loading)
            val cancellation = runCatching { state.load(access) { throw CancellationException() } }
            check(cancellation.exceptionOrNull() is CancellationException)
        }

    @Test
    fun modesAndPermissionChangesClearRealStateWithoutChangingWidgets() =
        runBlocking {
            val context: Context = RuntimeEnvironment.getApplication()
            val preferences = context.getSharedPreferences("live-state", Context.MODE_PRIVATE)
            preferences
                .edit()
                .clear()
                .putBoolean("demo", false)
                .commit()
            val state = SableAppState(preferences)
            val widgets = state.dashboard.widgets.toList()
            check(state.dataMode == "empty")
            state.chooseDataMode("real")
            state.connect = access
            check(state.live && !state.demo)
            state.liveHealth.load(access) { snapshot }
            state.refreshLive()
            check(state.liveRefreshVersion == 1)
            state.connect = access.copy(granted = emptySet(), grantedCount = 0)
            check(state.liveHealth.snapshot == null)
            state.chooseRefreshMinutes(15)
            check(SableAppState(preferences).refreshMinutes == 15)
            check(SableAppState(preferences).live)
            state.chooseDemo(true)
            check(state.dataMode == "demo" && state.liveHealth.snapshot == null)
            state.chooseDemo(false)
            check(state.dataMode == "empty" && state.dashboard.widgets == widgets)
        }
}
