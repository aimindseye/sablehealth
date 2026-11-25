package io.github.aimindseye.sablehealth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val DARK_BACKGROUND = 0xFF111922
private const val DARK_GROUP = 0xFF202E3A
private const val DARK_CONNECTED = 0xFF203C39
private const val LIGHT_CONNECTED = 0xFFE2F1E9
private const val DARK_WARNING = 0xFF3B3025
private const val LIGHT_WARNING = 0xFFFFF0D8
private const val DARK_THRESHOLD = .5f

@Composable
internal fun r7aBackground(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < DARK_THRESHOLD) {
        Color(DARK_BACKGROUND)
    } else {
        MaterialTheme.colorScheme.background
    }

@Composable
internal fun r7aGroupColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < DARK_THRESHOLD) {
        Color(DARK_GROUP)
    } else {
        MaterialTheme.colorScheme.surface
    }

@Composable
internal fun r7aStatusColor(ready: Boolean): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_THRESHOLD
    return Color(
        if (ready) {
            if (dark) DARK_CONNECTED else LIGHT_CONNECTED
        } else {
            if (dark) DARK_WARNING else LIGHT_WARNING
        },
    )
}

@Composable
internal fun R7aGroup(
    title: String,
    color: Color = r7aGroupColor(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 2.dp)) {
            if (title.isNotEmpty()) {
                Text(
                    title.uppercase(),
                    modifier = Modifier.padding(top = 2.dp, bottom = 1.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            content()
        }
    }
}

@Composable
internal fun R7aRule() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .22f))
}

@Composable
internal fun R7aRow(
    title: String,
    value: String = "",
    detail: String = "",
    action: (() -> Unit)? = null,
    chosen: Boolean? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val control =
        if (action == null) {
            Modifier
        } else {
            Modifier
                .onFocusChanged { focused = it.isFocused }
                .then(r7FocusOutline(focused, colors.primary, MaterialTheme.shapes.small))
                .clickable(role = Role.Button, onClick = action)
        }
    Row(
        Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = if (action != null) 48.dp else 30.dp)
            .testTag("r7a-row-$title")
            .then(control)
            .semantics(mergeDescendants = true) { chosen?.let { selected = it } }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (action != null) R7aPreferencesUi.SourceGlyph(title)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        if (value.isNotEmpty()) {
            Text(
                value,
                modifier = Modifier.widthIn(max = 160.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        if (chosen != null || action != null) {
            Text(
                if (chosen != null) {
                    if (chosen) "✓" else "○"
                } else {
                    "›"
                },
                color = colors.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
internal fun R7aChoiceRow(
    title: String,
    current: String,
    options: List<String>,
    choose: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        R6Choice(title, current, options, choose)
    }
}

@Composable
internal fun R7aNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

internal object R7aPreferencesUi {
    data class ActionLabel(
        val text: String,
        val description: String = text,
    )

    @Composable
    fun SourceGlyph(title: String) {
        val icon =
            when (title) {
                "Health permissions" -> R.drawable.ic_r6_sources
                "Check data availability" -> R.drawable.ic_r6_refresh
                "Sync diagnostics" -> R.drawable.ic_r6_vitals
                else -> null
            }
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        } else if (title == "Health Connect") {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@Composable
internal fun R7aAction(
    label: String,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    R7aAction(R7aPreferencesUi.ActionLabel(label), action, modifier, primary, enabled)
}

@Composable
internal fun R7aAction(
    label: R7aPreferencesUi.ActionLabel,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val target =
        modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .testTag("r7a-action-${label.text}")
            .semantics { contentDescription = label.description }
            .onFocusChanged { focused = it.isFocused }
            .then(r7FocusOutline(focused, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small))
    if (primary) {
        Button(onClick = action, modifier = target, enabled = enabled) { Text(label.text) }
    } else {
        androidx.compose.material3.TextButton(onClick = action, modifier = target, enabled = enabled) {
            Text(label.text)
        }
    }
}
