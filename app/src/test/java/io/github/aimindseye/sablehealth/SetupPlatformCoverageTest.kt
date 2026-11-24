package io.github.aimindseye.sablehealth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SkinTemperatureRecord
import io.github.aimindseye.sablehealth.core.DeviceProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.view.KeyEvent as AndroidKeyEvent

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetupPlatformCoverageTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val state get() = SableAppState(context.getSharedPreferences("platform-test", Context.MODE_PRIVATE))

    @Test
    fun unsupportedProviderDoesNotReadPermissions() =
        runBlocking {
            val unsupported = inspectHealthConnect({ HealthConnectClient.SDK_UNAVAILABLE }) { error("must not read") }
            assertEquals("Unavailable", unsupported.label)
            val update =
                inspectHealthConnect({ HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED }) {
                    error("must not read")
                }
            assertEquals("Update required", update.label)
            assertTrue(update.granted.isEmpty())
            assertEquals(null, update.grantedCount)
        }

    @Test
    fun actualReadCatalogFiltersUnsupportedAndUnrelatedGrants() =
        runBlocking {
            val catalog = M1HealthConnectProbe.metrics.map { it.permission }.toSet()
            val skin = HealthPermission.getReadPermission(SkinTemperatureRecord::class)
            val granted = catalog + "android.permission.health.WRITE_STEPS"
            val all = inspectHealthConnect({ HealthConnectClient.SDK_AVAILABLE }) { PlatformReadAccess(true, granted) }
            assertEquals(catalog, all.granted)
            assertEquals(catalog.size, all.grantedCount)
            val limited =
                inspectHealthConnect({ HealthConnectClient.SDK_AVAILABLE }) { PlatformReadAccess(false, granted) }
            assertEquals(catalog - skin, limited.supported)
            assertEquals(catalog - skin, limited.granted)
            val revoked =
                inspectHealthConnect({ HealthConnectClient.SDK_AVAILABLE }) { PlatformReadAccess(true, emptySet()) }
            assertTrue(revoked.granted.isEmpty())
            assertEquals(0, revoked.grantedCount)
        }

    @Test
    fun providerFailureIsUnknownAndCancellationPropagates() =
        runBlocking {
            val status = inspectHealthConnect({ throw SecurityException() }) { error("must not read") }
            assertEquals("Diagnostic error", status.label)
            assertEquals(null, status.grantedCount)
            val failure =
                runCatching {
                    inspectHealthConnect({ HealthConnectClient.SDK_AVAILABLE }) { throw CancellationException() }
                }
            assertTrue(failure.exceptionOrNull() is CancellationException)
        }

    @Test
    @Config(sdk = [30])
    fun realPlatformInspectionWithoutProvider() =
        runBlocking {
            val status = inspectHealthConnect(context)
            assertTrue(status.label in setOf("Unavailable", "Update required"))
            assertTrue(status.granted.isEmpty())
        }

    @Test
    fun keyboardDirectionAndCommandRoutingIsolatedDuringSetup() {
        val app = state
        val focus = RecordingFocus()
        val directions =
            mapOf(
                AndroidKeyEvent.KEYCODE_DPAD_UP to FocusDirection.Up,
                AndroidKeyEvent.KEYCODE_DPAD_DOWN to FocusDirection.Down,
                AndroidKeyEvent.KEYCODE_DPAD_LEFT to FocusDirection.Left,
                AndroidKeyEvent.KEYCODE_DPAD_RIGHT to FocusDirection.Right,
            )
        for ((key, direction) in directions) {
            assertTrue(dispatch(key, app, focus, true))
            assertEquals(direction, focus.direction)
        }
        app.page = Page.SETTINGS
        for (key in listOf(AndroidKeyEvent.KEYCODE_T, AndroidKeyEvent.KEYCODE_R, AndroidKeyEvent.KEYCODE_SLASH)) {
            assertFalse(dispatch(key, app, focus, true))
            assertEquals(Page.SETTINGS, app.page)
        }
        assertFalse(dispatch(AndroidKeyEvent.KEYCODE_ESCAPE, app, focus, true))
        assertFalse(dispatch(AndroidKeyEvent.KEYCODE_ENTER, app, focus, true))
        assertTrue(dispatch(AndroidKeyEvent.KEYCODE_ESCAPE, app, focus, false))
        assertEquals(Page.MAIN, app.page)
        assertTrue(dispatch(AndroidKeyEvent.KEYCODE_T, app, focus, false))
        assertEquals(Section.SLEEP, app.section)
        assertTrue(dispatch(AndroidKeyEvent.KEYCODE_R, app, focus, false))
        assertEquals(Page.SOURCES, app.page)
        assertTrue(dispatch(AndroidKeyEvent.KEYCODE_SLASH, app, focus, false))
        assertEquals(Page.HELP, app.page)
        assertFalse(dispatch(AndroidKeyEvent.KEYCODE_ENTER, app, focus, false))
        assertFalse(dispatch(AndroidKeyEvent.KEYCODE_A, app, focus, false))
        val up = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_UP, AndroidKeyEvent.KEYCODE_DPAD_UP))
        assertFalse(handleDeviceKey(up, app, DeviceProfile.Q25, focus, false))
    }

    @Test
    fun requestCancellationAndSettingsFailureDoNotClaimAccess() =
        runBlocking {
            val request = SetupRequestState()
            val app = state
            request.busy = true
            val result =
                runCatching {
                    request.request(app, setOf("read.steps"), { throw CancellationException() }) {
                        error("must not launch")
                    }
                }
            assertTrue(result.exceptionOrNull() is CancellationException)
            assertFalse(request.busy)
            val missing =
                object : ContextWrapper(context) {
                    override fun startActivity(intent: Intent) {
                        throw ActivityNotFoundException()
                    }
                }
            assertEquals("Health Connect settings unavailable.", openHealthConnectSettings(missing))
            val blocked =
                object : ContextWrapper(context) {
                    override fun startActivity(intent: Intent) {
                        throw SecurityException()
                    }
                }
            assertEquals("Health Connect settings access blocked.", openHealthConnectSettings(blocked))
        }

    private fun dispatch(
        key: Int,
        app: SableAppState,
        focus: FocusManager,
        setup: Boolean,
    ): Boolean {
        val event = KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, key))
        return handleDeviceKey(event, app, DeviceProfile.Q25, focus, setup)
    }

    private class RecordingFocus : FocusManager {
        var direction: FocusDirection? = null

        override fun clearFocus(force: Boolean) = Unit

        override fun moveFocus(focusDirection: FocusDirection): Boolean {
            direction = focusDirection
            return true
        }
    }
}
