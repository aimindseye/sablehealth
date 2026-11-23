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
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DeviceProfile

@Composable
internal fun HealthAppContent(state: SableAppState, profile: DeviceProfile) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        HealthHeader(
            title = if (state.page == Page.MAIN) "Sable Health" else
                state.page.name.lowercase().replaceFirstChar(Char::uppercaseChar),
            subtitle = if (state.demo) "DEMO DATA · ${profile.label}" else
                "NO RECORDS IMPORTED · ${profile.label}",
            compactQ25 = profile == DeviceProfile.Q25,
            onSources = { state.page = Page.SOURCES },
            onSettings = { state.page = Page.SETTINGS },
        )
        Box(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = if (profile == DeviceProfile.Q25) 2.dp else 4.dp),
                verticalArrangement = Arrangement.spacedBy(
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
private fun HealthPage(state: SableAppState, profile: DeviceProfile) {
    when (state.page) {
        Page.MAIN -> Dashboard(state, profile == DeviceProfile.Q25) { state.page = Page.SOURCES }
        Page.SOURCES -> SourcesPage(state.connect, state::refreshSources) { state.page = Page.MAIN }
        Page.SETTINGS -> SettingsPage(state, profile)
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
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FocusButton("Sources", onSources)
            FocusButton("Settings", onSettings)
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
        modifier = modifier
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

@Composable
private fun BottomTabs(active: Section, compactQ25: Boolean, onClick: (Section) -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    BoxWithConstraints {
        val compact = maxWidth < 310.dp
        val modifier = if (compact) {
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
