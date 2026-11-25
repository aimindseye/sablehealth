package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun R6Card(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    verticalPadding: Dp = 6.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { content() }
    }
}

@Composable
internal fun R6EmptyCard(
    state: SableAppState,
    title: String,
    message: String,
) {
    R6Card {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { state.page = Page.SOURCES }) { Text("View data sources") }
    }
}
