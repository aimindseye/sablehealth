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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SkinTemperatureRecord
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.KeyboardMapping
import io.github.aimindseye.sablehealth.core.Press
import io.github.aimindseye.sablehealth.core.SetupEntry
import io.github.aimindseye.sablehealth.core.UiAction
import kotlinx.coroutines.CancellationException

internal enum class Section(
    val title: String,
) {
    DASHBOARD("Dashboard"),
    SLEEP("Sleep"),
    MOVE("Move"),
    VITALS("Vitals"),
    TRENDS("Trends"),
}

internal enum class Page {
    MAIN,
    SOURCES,
    SOURCE_DETAIL,
    SOURCE_COMPANION,
    SOURCE_DIAGNOSTICS,
    SLEEP_DIAGNOSTICS,
    SOURCE_COVERAGE,
    SOURCE_REPORT,
    SETTINGS_SETUP,
    SOURCE_PRIVACY,
    M1_DISCOVERY,
    SETTINGS,
    WIDGETS,
    PHONE_INPUT,
    SETTINGS_PRIVACY,
    HELP,
}

internal data class ConnectStatus(
    val label: String,
    val detail: String,
    val grantedCount: Int? = null,
    val granted: Set<String> = emptySet(),
    val supported: Set<String> = emptySet(),
)

class MainActivity : ComponentActivity() {
    private var resumeVersion by mutableStateOf(0)
    private var foreground by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        resumeVersion++
        foreground = true
    }

    override fun onPause() {
        foreground = false
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SableApp(resumeVersion, foreground = foreground) }
    }
}

@Composable
internal fun SableApp(
    resumeVersion: Int = 0,
    foreground: Boolean = true,
    inspect: suspend (Context) -> ConnectStatus = ::inspectHealthConnect,
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("sable_health_m0", Context.MODE_PRIVATE) }
    var entry by rememberSaveable {
        mutableStateOf(if (preferences.getBoolean("onboarded", false)) "" else SetupEntry.FIRST_RUN.name)
    }
    var returnPage by rememberSaveable { mutableStateOf(Page.SETTINGS.name) }
    val state = remember(preferences) { SableAppState(preferences) }
    state.setupAction = { mode ->
        returnPage = state.page.name
        entry = mode.name
        state.recheckPermissions()
    }
    val configuration = LocalConfiguration.current
    val detected = remember(configuration) { detectDevice(configuration) }
    val profile = state.selectedProfile(detected)
    val focusManager = LocalFocusManager.current
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(entry) {
        rootFocus.requestFocus()
    }
    LaunchedEffect(state.diagnosticsVersion, resumeVersion) {
        state.connect = ConnectStatus("Checking", "Checking Health Connect permissions")
        state.connect = inspect(context)
    }

    LiveRefreshEffect(state, context, foreground, entry.isNotEmpty(), inspect)

    SableHealthTheme(state.appearance) {
        BackHandler(enabled = entry.isEmpty() && (state.page != Page.MAIN || state.section != Section.DASHBOARD)) {
            state.back()
        }
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(rootFocus)
                    .onPreviewKeyEvent { handleDeviceKey(it, state, profile, focusManager, entry.isNotEmpty()) }
                    .focusable(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (entry.isEmpty()) {
                HealthAppContent(state, profile)
            } else {
                val mode = SetupEntry.valueOf(entry)
                SetupWizard(state, profile, mode) { finished ->
                    if (mode == SetupEntry.FIRST_RUN && finished) {
                        state.completeOnboarding(state.demo)
                    }
                    state.page = if (mode == SetupEntry.FIRST_RUN) Page.MAIN else Page.valueOf(returnPage)
                    entry = ""
                }
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

internal fun handleDeviceKey(
    event: KeyEvent,
    state: SableAppState,
    profile: DeviceProfile,
    focusManager: FocusManager,
    setupActive: Boolean,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
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
    return if (setupActive && action !in setOf(UiAction.UP, UiAction.DOWN, UiAction.LEFT, UiAction.RIGHT)) {
        false
    } else {
        when (action) {
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
                refreshFromKeyboard(state)
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
}

private fun refreshFromKeyboard(state: SableAppState) {
    if (state.live && state.page == Page.MAIN) {
        state.refreshLive()
    } else {
        state.refreshSources()
    }
}

/** Provider and permission inspection only; never reads records. */
internal suspend fun inspectHealthConnect(context: Context): ConnectStatus =
    inspectHealthConnect({ HealthConnectClient.getSdkStatus(context) }) {
        val client = HealthConnectClient.getOrCreate(context)
        val skinAvailable =
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE) ==
                HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        PlatformReadAccess(skinAvailable, client.permissionController.getGrantedPermissions())
    }

internal data class PlatformReadAccess(
    val skinAvailable: Boolean,
    val granted: Set<String>,
)

/** Keep status handling and the read-only allowlist independent of the Android service. */
internal suspend fun inspectHealthConnect(
    sdkStatus: () -> Int,
    readAccess: suspend () -> PlatformReadAccess,
): ConnectStatus =
    try {
        when (sdkStatus()) {
            HealthConnectClient.SDK_AVAILABLE -> availableReadAccess(readAccess())
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                ConnectStatus("Update required", "Install or update Health Connect from its provider")
            else -> ConnectStatus("Unavailable", "Health Connect is not available on this device")
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ConnectStatus("Diagnostic error", "Could not inspect permissions; no data was read")
    }

private fun availableReadAccess(access: PlatformReadAccess): ConnectStatus {
    val skin = HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    val supported =
        M1HealthConnectProbe.metrics
            .map { it.permission }
            .filter { it != skin || access.skinAvailable }
            .toSet()
    val granted = access.granted.intersect(supported)
    return ConnectStatus("Available", "Read access checked · no records queried", granted.size, granted, supported)
}
