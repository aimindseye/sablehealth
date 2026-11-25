package io.github.aimindseye.sablehealth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val SHORT_LABEL = 3
private const val MEDIUM_LABEL = 8

@Composable
internal fun R6IconButton(
    label: String,
    icon: Int,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = action,
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .onFocusChanged { focused = it.isFocused }
                .border(if (focused) 2.dp else 0.dp, colors.primary, MaterialTheme.shapes.small)
                .semantics { selected = active },
    ) {
        Icon(
            painterResource(icon),
            label,
            Modifier.size(22.dp),
            tint = if (active || focused) colors.primary else colors.onSurfaceVariant,
        )
    }
}

@Composable
internal fun R6Choice(
    label: String,
    current: String,
    options: List<String>,
    choose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val width =
        when {
            options.all { it.length <= SHORT_LABEL } -> 72.dp
            options.all { it.length <= MEDIUM_LABEL } -> 108.dp
            else -> 190.dp
        }
    Column(modifier.widthIn(max = width)) {
        TextButton(
            onClick = { open = true },
            contentPadding = PaddingValues(horizontal = 6.dp),
            modifier =
                Modifier
                    .sizeIn(minHeight = 48.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .border(if (focused) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                    .semantics { contentDescription = label },
        ) {
            Text(current, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(painterResource(R.drawable.ic_r6_down), null, Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    choose(option)
                    open = false
                })
            }
        }
    }
}

@Composable
internal fun R6ModeHeading(
    state: SableAppState,
    onOverview: (() -> Unit)? = null,
    period: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (onOverview != null) R6IconButton("All metrics", R.drawable.ic_r6_back, onOverview)
        Text(
            state.section.title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        period?.invoke()
        R6IconButton(
            "Text",
            R.drawable.ic_r6_list,
            { state.chooseViewMode(state.section, ViewMode.TEXT) },
            active = state.modeFor(state.section) == ViewMode.TEXT,
        )
        R6IconButton(
            "Graphs",
            R.drawable.ic_r6_graph,
            { state.chooseViewMode(state.section, ViewMode.GRAPHS) },
            active = state.modeFor(state.section) == ViewMode.GRAPHS,
        )
    }
}
