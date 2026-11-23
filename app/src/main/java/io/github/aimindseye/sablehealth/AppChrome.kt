package io.github.aimindseye.sablehealth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DeviceProfile

@Composable
internal fun HealthAppContent(
    state: SableAppState,
    profile: DeviceProfile,
) {
    val dashboardOnQ25 =
        profile == DeviceProfile.Q25 && state.page == Page.MAIN && state.section == Section.DASHBOARD
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        HealthHeader(
            title =
                if (state.page == Page.MAIN) {
                    "Sable Health"
                } else {
                    state.page.name
                        .lowercase()
                        .replaceFirstChar(Char::uppercaseChar)
                },
            subtitle =
                if (dashboardOnQ25) {
                    if (state.demo) "DASHBOARD · DEMO" else "DASHBOARD · NO RECORDS"
                } else if (state.demo) {
                    "DEMO DATA · ${profile.label}"
                } else {
                    "NO RECORDS IMPORTED · ${profile.label}"
                },
            compactQ25 = profile == DeviceProfile.Q25,
            onSources = { state.page = Page.SOURCES },
            onSettings = { state.page = Page.SETTINGS },
        )
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = if (profile == DeviceProfile.Q25) 2.dp else 4.dp),
                verticalArrangement =
                    Arrangement.spacedBy(
                        if (profile == DeviceProfile.Q25) 4.dp else 7.dp,
                    ),
            ) {
                HealthPage(state, profile)
            }
        }
        if (state.page == Page.MAIN) {
            BottomTabs(state.section, profile == DeviceProfile.Q25) { state.section = it }
        }
    }
}

@Composable
private fun HealthPage(
    state: SableAppState,
    profile: DeviceProfile,
) {
    when (state.page) {
        Page.MAIN -> Dashboard(state, profile == DeviceProfile.Q25)
        Page.SOURCES -> SourcesPage(state.connect, state::refreshSources) { state.page = Page.MAIN }
        Page.SETTINGS -> SettingsPage(state, profile)
        Page.WIDGETS -> WidgetPickerPage(state)
        Page.HELP -> HelpPage(profile) { state.page = Page.MAIN }
    }
}

@Composable
internal fun HealthHeader(
    title: String,
    subtitle: String,
    compactQ25: Boolean,
    onSources: () -> Unit,
    onSettings: () -> Unit,
) {
    val titleStyle = if (compactQ25) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
    val subtitleStyle = if (compactQ25) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    Column(
        Modifier.fillMaxWidth().padding(
            horizontal = 10.dp,
            vertical = if (compactQ25) 0.dp else 4.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = subtitleStyle,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (compactQ25) {
                Q25ChromeButton("Sources", onSources)
                Q25ChromeButton("Settings", onSettings)
            } else {
                FocusButton("Sources", onSources)
                FocusButton("Settings", onSettings)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun FocusButton(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .onFocusChanged { focused = it.isFocused }
                .border(if (focused) 2.dp else 0.dp, colors.primary, MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .focusable(),
        color = if (selected) colors.surfaceVariant else colors.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Box(
            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.primary else colors.onSurface,
            )
        }
    }
}

private const val Q25_TOUCH_HEIGHT_DP = 48
private const val Q25_VISUAL_HEIGHT_DP = 36
private const val Q25_CHROME_TEXT_SP = 12

/** Tighter visual pill with a full 48dp hit target for touch, trackpad and accessibility. */
@Composable
private fun Q25ChromeButton(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = Q25_TOUCH_HEIGHT_DP.dp)
                .onFocusChanged { focused = it.isFocused }
                .clickable(onClick = onClick)
                .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Q25_VISUAL_HEIGHT_DP.dp)
                    .border(if (focused) 2.dp else 0.dp, colors.primary, MaterialTheme.shapes.small),
            shape = MaterialTheme.shapes.small,
            color = if (selected) colors.surfaceVariant else colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Box(Modifier.padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                Text(
                    label,
                    fontSize = Q25_CHROME_TEXT_SP.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) colors.primary else colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun BottomTabs(
    active: Section,
    compactQ25: Boolean,
    onClick: (Section) -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    BoxWithConstraints {
        val compact = maxWidth < 310.dp
        val modifier =
            if (compact) {
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth()
            }
        Row(
            modifier.padding(
                horizontal = if (compactQ25) 1.dp else 4.dp,
                vertical = if (compactQ25) 0.dp else 2.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Section.entries.forEach { tab ->
                if (compactQ25) {
                    Q25ChromeButton(
                        label = tab.title,
                        onClick = { onClick(tab) },
                        selected = tab == active,
                        modifier = if (compact) Modifier else Modifier.weight(1f),
                    )
                } else {
                    FocusButton(
                        label = tab.title,
                        onClick = { onClick(tab) },
                        selected = tab == active,
                        modifier = if (compact) Modifier else Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
