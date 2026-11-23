package io.github.aimindseye.sablehealth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
            onSources = { state.page = Page.SOURCES },
            onSettings = { state.page = Page.SETTINGS },
        )
        Box(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HealthPage(state, profile)
            }
        }
        if (state.page == Page.MAIN) BottomTabs(state.section) { state.section = it }
    }
}

@Composable
private fun HealthPage(state: SableAppState, profile: DeviceProfile) {
    when (state.page) {
        Page.MAIN -> Dashboard(state.section, state.demo, state.connect) { state.page = Page.SOURCES }
        Page.SOURCES -> SourcesPage(state.connect, state::refreshSources) { state.page = Page.MAIN }
        Page.SETTINGS -> SettingsPage(state, profile)
        Page.HELP -> HelpPage(profile) { state.page = Page.MAIN }
    }
}

@Composable
internal fun HealthHeader(
    title: String,
    subtitle: String,
    onSources: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
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
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
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
private fun BottomTabs(active: Section, onClick: (Section) -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Section.entries.forEach { tab ->
            FocusButton(
                label = tab.title,
                onClick = { onClick(tab) },
                selected = tab == active,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
