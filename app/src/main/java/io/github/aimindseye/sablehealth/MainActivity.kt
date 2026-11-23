package io.github.aimindseye.sablehealth

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import io.github.aimindseye.sablehealth.core.DemoHealth
import io.github.aimindseye.sablehealth.core.DemoMetric
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.KeyboardMapping
import io.github.aimindseye.sablehealth.core.Press
import io.github.aimindseye.sablehealth.core.UiAction

private enum class Section(val title: String) {
    TODAY("Today"), SLEEP("Sleep"), MOVE("Move"), VITALS("Vitals"), TRENDS("Trends")
}

private enum class Page { MAIN, SOURCES, SETTINGS, HELP }

private data class ConnectStatus(val label: String, val detail: String, val grantedCount: Int? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SableApp() }
    }
}

@Composable
private fun SableApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sable_health_m0", 0) }
    val config = context.resources.configuration
    val detected = remember(config) {
        DeviceProfile.detect(
            Build.MANUFACTURER.orEmpty(), Build.MODEL.orEmpty(), Build.DEVICE.orEmpty(),
            config.keyboard == Configuration.KEYBOARD_QWERTY,
            config.navigation == Configuration.NAVIGATION_DPAD || config.navigation == Configuration.NAVIGATION_TRACKBALL,
        )
    }
    var override by remember { mutableStateOf(prefs.getString("profile", "auto") ?: "auto") }
    val profile = DeviceProfile.entries.firstOrNull { it.name == override } ?: detected
    var appearance by remember { mutableStateOf(prefs.getString("appearance", "system") ?: "system") }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var demo by remember { mutableStateOf(prefs.getBoolean("demo", true)) }
    var section by remember { mutableStateOf(Section.TODAY) }
    var page by remember { mutableStateOf(Page.MAIN) }
    var diagnosticsVersion by remember { mutableStateOf(0) }
    var connect by remember { mutableStateOf(ConnectStatus("Checking", "Inspecting Health Connect availability")) }
    val focus = LocalFocusManager.current
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(onboarded) { if (onboarded) rootFocus.requestFocus() }

    LaunchedEffect(diagnosticsVersion) {
        connect = try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    val permissions = HealthConnectClient.getOrCreate(context)
                        .permissionController.getGrantedPermissions()
                    ConnectStatus("Available", "Health Connect available · no records queried in M0", permissions.size)
                }
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    ConnectStatus("Update required", "Install or update Health Connect from its provider")
                else -> ConnectStatus("Unavailable", "Health Connect is not available on this device")
            }
        } catch (_: Exception) {
            ConnectStatus("Diagnostic error", "Could not inspect permissions; no data was read")
        }
    }

    fun back() { if (page != Page.MAIN) page = Page.MAIN else section = Section.TODAY }

    SableHealthTheme(appearance) {
        BackHandler(enabled = page != Page.MAIN || section != Section.TODAY) { back() }
        Surface(
            modifier = Modifier.fillMaxSize().focusRequester(rootFocus).onPreviewKeyEvent { event ->
                if (!onboarded || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val native = event.nativeKeyEvent
                val action = KeyboardMapping.action(
                    profile, Press(native.keyCode, native.repeatCount,
                        native.isAltPressed, native.isCtrlPressed, native.isMetaPressed)
                )
                when (action) {
                    UiAction.UP -> focus.moveFocus(FocusDirection.Up)
                    UiAction.DOWN -> focus.moveFocus(FocusDirection.Down)
                    UiAction.LEFT -> focus.moveFocus(FocusDirection.Left)
                    UiAction.RIGHT -> focus.moveFocus(FocusDirection.Right)
                    UiAction.BACK -> { back(); true }
                    UiAction.NEXT_TAB -> { page = Page.MAIN; section = Section.entries[(section.ordinal + 1) % Section.entries.size]; true }
                    UiAction.REFRESH -> { diagnosticsVersion++; page = Page.SOURCES; true }
                    UiAction.HELP -> { page = Page.HELP; true }
                    // Allow the Compose focused clickable to process Enter/Space.
                    UiAction.CONFIRM, null -> false
                }
            }.focusable(), color = MaterialTheme.colorScheme.background,
        ) {
            if (!onboarded) {
                Onboarding(profile, connect) { useDemo ->
                    demo = useDemo
                    prefs.edit().putBoolean("onboarded", true).putBoolean("demo", useDemo).apply()
                    onboarded = true
                }
            } else {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Header(
                        title = if (page == Page.MAIN) "Sable Health" else page.name.lowercase().replaceFirstChar(Char::uppercaseChar),
                        subtitle = if (demo) "DEMO DATA · ${profile.label}" else "NO RECORDS IMPORTED · ${profile.label}",
                        onSources = { page = Page.SOURCES }, onSettings = { page = Page.SETTINGS },
                    )
                    Box(Modifier.weight(1f)) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (page) {
                                Page.MAIN -> Dashboard(section, demo, connect, onSources = { page = Page.SOURCES })
                                Page.SOURCES -> SourcesPage(connect, { diagnosticsVersion++ }, { page = Page.MAIN })
                                Page.SETTINGS -> SettingsPage(profile, override, appearance,
                                    onProfile = { override = it; prefs.edit().putString("profile", it).apply() },
                                    onAppearance = { appearance = it; prefs.edit().putString("appearance", it).apply() },
                                    onDemo = { demo = it; prefs.edit().putBoolean("demo", it).apply() },
                                    demo = demo, onClose = { page = Page.MAIN })
                                Page.HELP -> HelpPage(profile, { page = Page.MAIN })
                            }
                        }
                    }
                    if (page == Page.MAIN) BottomTabs(section) { section = it }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, onSources: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            FocusButton("Sources", onSources)
            FocusButton("Settings", onSettings)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FocusButton(label: String, onClick: () -> Unit, selected: Boolean = false, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 0.dp, color.primary, MaterialTheme.shapes.small)
            .clickable(onClick = onClick).focusable(),
        color = if (selected) color.surfaceVariant else color.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color.outlineVariant),
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = if (selected) color.primary else color.onSurface)
        }
    }
}

@Composable
private fun BottomTabs(active: Section, onClick: (Section) -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Section.entries.forEach { tab ->
            FocusButton(tab.title, { onClick(tab) }, selected = tab == active, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun Dashboard(section: Section, demo: Boolean, status: ConnectStatus, onSources: () -> Unit) {
    when (section) {
        Section.TODAY -> {
            SectionHeading("Today", if (demo) "Your day at a glance · synthetic demonstration" else "Connect a source to see your readings")
            if (demo) {
                TwoUp(DemoHealth.today)
                Notice("Finding your usual", "Personal baselines appear only after sufficient valid history. No recovery score is fabricated.")
            } else {
                Notice("No measurements yet", "M0 does not import health records. Health Connect integration follows in M1.")
            }
            Notice("Source status: ${status.label}", status.detail, onClick = onSources)
        }
        Section.SLEEP -> {
            SectionHeading("Sleep", "Last night · sleep sessions and stages")
            if (demo) Metrics(DemoHealth.sleep) else EmptyState()
            Notice("Sleep quality", "Metrics will include sources, local wake date and coverage. Never treat missing sleep as zero.")
        }
        Section.MOVE -> {
            SectionHeading("Move", "Activity and workouts")
            if (demo) Metrics(DemoHealth.move) else EmptyState()
            Notice("Workout history", "M2 will reconcile repeated events across sources without double counting.")
        }
        Section.VITALS -> {
            SectionHeading("Vitals", "Recorded values, not medical conclusions")
            if (demo) Metrics(DemoHealth.vitals) else EmptyState()
            Notice("Reading freshness", "The data view will distinguish the last measurement from the last successful import.")
        }
        Section.TRENDS -> {
            SectionHeading("Trends", "7 days · 30 days · 90 days")
            Notice("Not enough history", "Time-series charts and personal comparisons are withheld until the needed coverage is present.")
            if (demo) Notice("Example · 7 days", "Steps  6.9k  ·  Sleep  7h 23m  ·  values are synthetic")
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TwoUp(items: List<DemoMetric>) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 300.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { metric -> Box(Modifier.weight(1f)) { MetricCard(metric) } }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Metrics(items: List<DemoMetric>) {
    items.forEach { MetricCard(it) }
}

@Composable
private fun MetricCard(metric: DemoMetric) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(metric.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(metric.value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(metric.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Notice(title: String, detail: String, onClick: (() -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.onFocusChanged { focused = it.isFocused }
                .border(if (focused) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick).focusable() else Modifier
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyState() {
    Notice("No records imported", "This is a foundation build. Actual Health Connect import is scheduled for M1.")
}

@Composable
private fun SourcesPage(status: ConnectStatus, refresh: () -> Unit, back: () -> Unit) {
    SectionHeading("Data sources", "Read-only availability and access diagnostic")
    Notice("Health Connect: ${status.label}", status.detail)
    Notice("Granted permissions", "${status.grantedCount?.toString() ?: "Unknown"} · individual metric permissions are not requested by M0")
    Notice("Data imported", "None · no health measurements are read or stored in this milestone")
    Notice("Your privacy", "No backend, telemetry, login, network sync or direct Fitbit Bluetooth pairing")
    FocusButton("Check again", refresh)
    FocusButton("Back", back)
}

@Composable
private fun SettingsPage(
    profile: DeviceProfile,
    override: String,
    appearance: String,
    onProfile: (String) -> Unit,
    onAppearance: (String) -> Unit,
    onDemo: (Boolean) -> Unit,
    demo: Boolean,
    onClose: () -> Unit,
) {
    SectionHeading("Phone & input", "Detected/selected profile: ${profile.label}")
    Text("Device profile", style = MaterialTheme.typography.titleMedium)
    FocusButton("Automatic detection", { onProfile("auto") }, selected = override == "auto")
    DeviceProfile.entries.forEach { candidate ->
        FocusButton(candidate.label, { onProfile(candidate.name) }, selected = override == candidate.name)
    }
    Text("Appearance", style = MaterialTheme.typography.titleMedium)
    listOf("system", "light", "dark").forEach { value ->
        FocusButton(value.replaceFirstChar(Char::uppercaseChar), { onAppearance(value) }, selected = appearance == value)
    }
    Text("Data mode", style = MaterialTheme.typography.titleMedium)
    FocusButton("Synthetic demonstration", { onDemo(true) }, selected = demo)
    FocusButton("No records (live placeholder)", { onDemo(false) }, selected = !demo)
    Notice("Sable Blue", "Dark #2E9BD6 · light surface #1A6FA0 · exact Sable Games tokens")
    Notice("Remappable keys", "M0 establishes per-profile defaults. Custom remapping editor is planned as a follow-up; no silent reassignment.")
    FocusButton("Back", onClose)
}

@Composable
private fun HelpPage(profile: DeviceProfile, back: () -> Unit) {
    SectionHeading("Keyboard controls", profile.label)
    Notice("Move focus", if (profile.letterNavigation) "I / J / K / L or DPAD arrows" else "Trackpad / DPAD arrows")
    Notice("Open / return", "Enter, Space or trackpad click · Escape / Back returns")
    Notice("Global shortcuts", "T next tab · R source diagnostic · ? help")
    Notice("Text input", "Input fields retain native letter keys and system shortcuts")
    FocusButton("Back", back)
}

@Composable
private fun Onboarding(profile: DeviceProfile, status: ConnectStatus, finish: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Sable Health", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        SectionHeading("Designed for keyboard phones", "A local-first companion for health records you control")
        Notice("Phone detected", profile.label)
        Notice("Health Connect: ${status.label}", status.detail)
        Notice("Important", "Fitbit Air pairs through Google Health. M0 inspects Health Connect availability; it does not import records or ask for health permissions.")
        FocusButton("Explore synthetic demo", { finish(true) })
        FocusButton("Continue with no data", { finish(false) })
        Notice("Privacy first", "Synthetic demo and local settings only. No account, advertising, telemetry or remote service.")
    }
}
