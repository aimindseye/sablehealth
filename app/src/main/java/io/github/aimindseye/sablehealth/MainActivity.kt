package io.github.aimindseye.sablehealth

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.health.connect.client.HealthConnectClient
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.KeyboardMapping
import io.github.aimindseye.sablehealth.core.Press
import io.github.aimindseye.sablehealth.core.UiAction

internal enum class Section(
    val title: String,
) {
    TODAY("Today"),
    SLEEP("Sleep"),
    MOVE("Move"),
    VITALS("Vitals"),
    TRENDS("Trends"),
}

internal enum class Page { MAIN, SOURCES, SETTINGS, HELP }

internal data class ConnectStatus(
    val label: String,
    val detail: String,
    val grantedCount: Int? = null,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SableApp() }
    }
}

@Composable
internal fun SableApp() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("sable_health_m0", Context.MODE_PRIVATE) }
    val state = remember(preferences) { SableAppState(preferences) }
    val configuration = LocalConfiguration.current
    val detected = remember(configuration) { detectDevice(configuration) }
    val profile = state.selectedProfile(detected)
    val focusManager = LocalFocusManager.current
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(state.onboarded) {
        if (state.onboarded) rootFocus.requestFocus()
    }
    LaunchedEffect(state.diagnosticsVersion) {
        state.connect = inspectHealthConnect(context)
    }

    SableHealthTheme(state.appearance) {
        BackHandler(enabled = state.page != Page.MAIN || state.section != Section.TODAY) {
            state.back()
        }
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(rootFocus)
                    .onPreviewKeyEvent { handleDeviceKey(it, state, profile, focusManager) }
                    .focusable(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (state.onboarded) {
                HealthAppContent(state, profile)
            } else {
                Onboarding(profile, state.connect, state::completeOnboarding)
            }
        }
    }
}

private fun detectDevice(configuration: Configuration): DeviceProfile =
    DeviceProfile.detect(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        device = Build.DEVICE.orEmpty(),
        hasQwerty = configuration.keyboard == Configuration.KEYBOARD_QWERTY,
        hasDpad =
            configuration.navigation == Configuration.NAVIGATION_DPAD ||
                configuration.navigation == Configuration.NAVIGATION_TRACKBALL,
    )

private fun handleDeviceKey(
    event: KeyEvent,
    state: SableAppState,
    profile: DeviceProfile,
    focusManager: FocusManager,
): Boolean {
    if (!state.onboarded || event.type != KeyEventType.KeyDown) return false
    val native = event.nativeKeyEvent
    val action =
        KeyboardMapping.action(
            profile,
            Press(
                code = native.keyCode,
                repeatCount = native.repeatCount,
                alt = native.isAltPressed,
                ctrl = native.isCtrlPressed,
                meta = native.isMetaPressed,
            ),
        )
    return when (action) {
        UiAction.UP -> focusManager.moveFocus(FocusDirection.Up)
        UiAction.DOWN -> focusManager.moveFocus(FocusDirection.Down)
        UiAction.LEFT -> focusManager.moveFocus(FocusDirection.Left)
        UiAction.RIGHT -> focusManager.moveFocus(FocusDirection.Right)
        UiAction.BACK -> {
            state.back()
            true
        }
        UiAction.NEXT_TAB -> {
            state.nextTab()
            true
        }
        UiAction.REFRESH -> {
            state.refreshSources()
            true
        }
        UiAction.HELP -> {
            state.page = Page.HELP
            true
        }
        // A focused Compose clickable handles Enter / Space without double activation.
        UiAction.CONFIRM, null -> false
    }
}

/** M0 intentionally checks provider state only. It never asks for data read permissions. */
private suspend fun inspectHealthConnect(context: Context): ConnectStatus =
    try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                val count =
                    HealthConnectClient
                        .getOrCreate(context)
                        .permissionController
                        .getGrantedPermissions()
                        .size
                ConnectStatus("Available", "Health Connect available · no records queried in M0", count)
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                ConnectStatus("Update required", "Install or update Health Connect from its provider")
            else -> ConnectStatus("Unavailable", "Health Connect is not available on this device")
        }
    } catch (_: Exception) {
        ConnectStatus("Diagnostic error", "Could not inspect permissions; no data was read")
    }
