package io.github.aimindseye.sablehealth

import android.graphics.Bitmap
import android.graphics.Color

/** Reject empty or wrong-theme display captures. This is not a pixel golden comparator. */
internal object M21RenderedFrame {
    private const val SIDE = 720
    private const val STEP = 3
    private const val DARK_LIMIT = 110
    private const val LIGHT_LIMIT = 175

    fun valid(
        image: Bitmap,
        light: Boolean,
    ): Boolean {
        if (image.width != SIDE || image.height != SIDE) return false
        val upper = sample(image, 60, 300)
        val lower = sample(image, 300, 590)
        val status = sample(image, 5, 35)
        return upper.matches(light) && lower.matches(light) && status.statusMatches(light)
    }

    fun describe(image: Bitmap): String {
        if (image.width != SIDE || image.height != SIDE) return "geometry=${image.width}x${image.height}"
        return "upper=${sample(image, 60, 300)} lower=${sample(image, 300, 590)} status=${sample(image, 5, 35)}"
    }

    fun sameBody(
        first: Bitmap,
        second: Bitmap,
    ): Boolean {
        for (y in 60 until 590 step STEP) {
            for (x in 12 until SIDE - 12 step STEP) {
                if (first.getPixel(x, y) != second.getPixel(x, y)) return false
            }
        }
        return true
    }

    private fun sample(
        image: Bitmap,
        top: Int,
        bottom: Int,
    ): Ink {
        var dark = 0
        var light = 0
        for (y in top until bottom step STEP) {
            for (x in 12 until SIDE - 12 step STEP) {
                val pixel = image.getPixel(x, y)
                val value = (Color.red(pixel) * 3 + Color.green(pixel) * 6 + Color.blue(pixel)) / 10
                if (value < DARK_LIMIT) dark++
                if (value > LIGHT_LIMIT) light++
            }
        }
        return Ink(dark, light)
    }

    private data class Ink(
        val dark: Int,
        val light: Int,
    ) {
        fun statusMatches(lightTheme: Boolean): Boolean =
            if (lightTheme) {
                light >= 500 && dark >= 15
            } else {
                dark >= 500 && light >= 15
            }

        fun matches(lightTheme: Boolean): Boolean =
            if (lightTheme) {
                light >= 2000 && dark >= 25
            } else {
                dark >= 2000 && light >= 25
            }
    }
}
