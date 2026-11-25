package io.github.aimindseye.sablehealth

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object R7TimelineLabels {
    data class Label(
        val text: String,
        val center: Float,
    )
}

/** Return a clamped start only if the measured rectangle fits after the previous label. */
internal fun r7LabelLeft(
    center: Float,
    width: Float,
    viewport: Float,
    minimum: Float,
): Float? {
    if (width > viewport || width <= 0f) return null
    val left = (center - width / 2f).coerceIn(0f, viewport - width)
    return left.takeIf { it >= minimum }
}

internal fun DrawScope.drawR7TimeLabels(
    labels: List<R7TimelineLabels.Label>,
    text: TextMeasurer,
    color: Color,
) {
    var minimum = 0f
    val style = TextStyle(color = color, fontSize = 9.sp)
    labels.sortedBy { it.center }.forEach { label ->
        val measured = text.measure(label.text, style = style)
        val width = measured.size.width.toFloat()
        val left = r7LabelLeft(label.center, width, size.width, minimum)
        if (left != null) {
            drawText(measured, topLeft = Offset(left, 0f))
            minimum = left + width + 4.dp.toPx()
        }
    }
}
