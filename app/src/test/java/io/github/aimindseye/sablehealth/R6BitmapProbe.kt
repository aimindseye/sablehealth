package io.github.aimindseye.sablehealth

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

/** Same direct-view rendering path as the existing native LiveChartRenderingTest. */
internal fun renderR6Bitmap(
    activity: ComponentActivity,
    bounds: Rect,
): Bitmap {
    val root = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val image = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    root.draw(Canvas(image))
    val left = bounds.left.roundToInt().coerceIn(0, image.width - 1)
    val top = bounds.top.roundToInt().coerceIn(0, image.height - 1)
    val width = bounds.width.roundToInt().coerceIn(1, image.width - left)
    val height = bounds.height.roundToInt().coerceIn(1, image.height - top)
    return Bitmap.createBitmap(image, left, top, width, height)
}

internal fun Bitmap.countInk(
    color: Int,
    left: Int = 0,
    right: Int = width,
): Int {
    val start = left.coerceIn(0, width - 1)
    val span = (right - start).coerceIn(1, width - start)
    val pixels = IntArray(span * height)
    getPixels(pixels, 0, span, start, 0, span, height)
    return pixels.count { it == color }
}
