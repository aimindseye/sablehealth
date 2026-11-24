package io.github.aimindseye.sablehealth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aimindseye.sablehealth.core.LiveSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.coroutineContext

private const val CLOCK_CHECK_MS = 15_000L
internal const val DEFAULT_REFRESH_MINUTES = 5
private const val QUARTER_HOUR_MINUTES = 15
private const val HALF_HOUR_MINUTES = 30
internal val refreshChoices = listOf(0, 1, DEFAULT_REFRESH_MINUTES, QUARTER_HOUR_MINUTES, HALF_HOUR_MINUTES)

/** In-memory values are discarded on mode/access/lifecycle changes. No disk writes. */
internal class LiveHealthState {
    var snapshot by mutableStateOf<LiveSnapshot?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    private var generation = 0L

    fun clear() {
        generation++
        snapshot = null
        loading = false
        failed = false
    }

    suspend fun load(
        access: ConnectStatus,
        read: suspend (ConnectStatus) -> LiveSnapshot,
    ) {
        if (loading) return
        val request = ++generation
        loading = true
        failed = false
        try {
            val next = read(access)
            coroutineContext.ensureActive()
            if (generation == request) snapshot = next
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (generation == request) failed = true
        } finally {
            if (generation == request) loading = false
        }
    }
}

internal fun validRefreshMinutes(value: Int): Int = if (value in refreshChoices) value else DEFAULT_REFRESH_MINUTES

/** One shared foreground job, rechecking access before each refresh. */
@Composable
internal fun LiveRefreshEffect(
    state: SableAppState,
    context: Context,
    foreground: Boolean,
    setupActive: Boolean,
    inspect: suspend (Context) -> ConnectStatus,
) {
    val access = state.connect
    LaunchedEffect(state.live, foreground, setupActive, access, state.liveRefreshVersion, state.refreshMinutes) {
        state.liveHealth.clear()
        val active = foreground && !setupActive
        if (state.live && active && access.label != "Checking") {
            refreshWithAccess(state, context, inspect)
        }
    }
}

private suspend fun refreshWithAccess(
    state: SableAppState,
    context: Context,
    inspect: suspend (Context) -> ConnectStatus,
) {
    val checkedAccess = inspect(context)
    if (checkedAccess != state.connect) {
        state.connect = checkedAccess
        return // effect restarts with the updated grants
    }
    if (checkedAccess.grantedCount != null) {
        state.liveHealth.load(checkedAccess) { loadLiveHealth(context, it) }
    }
    val checked = Instant.now()
    val zone = ZoneId.systemDefault()
    while (true) {
        delay(CLOCK_CHECK_MS)
        if (refreshDue(checked, Instant.now(), zone, ZoneId.systemDefault(), state.refreshMinutes)) {
            state.refreshLive()
            break
        }
    }
}

internal fun refreshDue(
    checked: Instant,
    now: Instant,
    zone: ZoneId,
    currentZone: ZoneId,
    minutes: Int,
): Boolean =
    zone != currentZone ||
        now.atZone(currentZone).toLocalDate() != checked.atZone(zone).toLocalDate() ||
        now < checked ||
        (minutes > 0 && Duration.between(checked, now) >= Duration.ofMinutes(minutes.toLong()))
