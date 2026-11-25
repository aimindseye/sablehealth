package io.github.aimindseye.sablehealth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun R6BottomTabs(
    active: Section,
    onClick: (Section) -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        Section.entries.forEach { tab ->
            R6Tab(tab, tab == active, { onClick(tab) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun R6Tab(
    tab: Section,
    active: Boolean,
    action: () -> Unit,
    modifier: Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val icon =
        when (tab) {
            Section.DASHBOARD -> R.drawable.ic_r6_home
            Section.SLEEP -> R.drawable.ic_r6_sleep
            Section.MOVE -> R.drawable.ic_r6_move
            Section.VITALS -> R.drawable.ic_r6_vitals
            Section.TRENDS -> R.drawable.ic_r6_trends
        }
    Column(
        modifier
            .testTag("r7-tab-${tab.name}")
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .then(r7FocusOutline(focused, colors.primary, MaterialTheme.shapes.small))
            .clickable(onClick = action)
            .focusable()
            .semantics { selected = active },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (active) r7MetricSurface(null) else colors.surface,
        ) {
            Icon(
                painterResource(icon),
                null,
                Modifier.padding(horizontal = 8.dp, vertical = 2.dp).size(20.dp),
                tint = if (active) colors.primary else colors.onSurfaceVariant,
            )
        }
        Text(
            tab.title,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (active) colors.primary else colors.onSurfaceVariant,
        )
    }
}
