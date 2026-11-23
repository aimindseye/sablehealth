package io.github.aimindseye.sablehealth

import androidx.compose.ui.graphics.Color

/** Exact Sable Games / SableOS 2.0 colors. Centralized to prevent palette drift. */
internal object SablePalette {
    private const val BLUE_ARGB = 0xFF2E9BD6
    private const val BLUE_LIGHT_ARGB = 0xFF1A6FA0
    private const val DARK_BASE_ARGB = 0xFF000000
    private const val DARK_SURFACE_ARGB = 0xFF141414
    private const val DARK_RAISED_ARGB = 0xFF262626
    private const val DARK_MUTED_ARGB = 0xFFA6A6A6
    private const val LIGHT_BASE_ARGB = 0xFFF5F7F9
    private const val LIGHT_SURFACE_ARGB = 0xFFFFFFFF
    private const val LIGHT_RAISED_ARGB = 0xFFE9EDF2
    private const val LIGHT_TEXT_ARGB = 0xFF12171C
    private const val LIGHT_MUTED_ARGB = 0xFF515B66
    private const val DARK_ERROR_ARGB = 0xFFE0463C
    private const val LIGHT_ERROR_ARGB = 0xFFBA1A1A
    private const val LIGHT_OUTLINE_ARGB = 0xFFD0D6DD

    val blue = Color(BLUE_ARGB)
    val blueLightSurface = Color(BLUE_LIGHT_ARGB)
    val darkBase = Color(DARK_BASE_ARGB)
    val darkSurface = Color(DARK_SURFACE_ARGB)
    val darkRaised = Color(DARK_RAISED_ARGB)
    val darkMuted = Color(DARK_MUTED_ARGB)
    val darkError = Color(DARK_ERROR_ARGB)
    val lightBase = Color(LIGHT_BASE_ARGB)
    val lightSurface = Color(LIGHT_SURFACE_ARGB)
    val lightRaised = Color(LIGHT_RAISED_ARGB)
    val lightText = Color(LIGHT_TEXT_ARGB)
    val lightMuted = Color(LIGHT_MUTED_ARGB)
    val lightError = Color(LIGHT_ERROR_ARGB)
    val lightOutline = Color(LIGHT_OUTLINE_ARGB)
}
