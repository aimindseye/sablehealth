package io.github.aimindseye.sablehealth

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** SableOS supplies Source Sans; the Android system falls back to a sans-serif font. */
private val sableFont = FontFamily(Typeface.create("source-sans", Typeface.NORMAL))

private val sableTypography =
    Typography(
        headlineMedium =
            TextStyle(
                fontFamily = sableFont,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        titleLarge =
            TextStyle(
                fontFamily = sableFont,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium,
            ),
        titleMedium =
            TextStyle(
                fontFamily = sableFont,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyLarge = TextStyle(fontFamily = sableFont, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = sableFont, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = sableFont, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge =
            TextStyle(
                fontFamily = sableFont,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            ),
    )

private val sableShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )

@Composable
internal fun SableHealthTheme(
    appearance: String,
    content: @Composable () -> Unit,
) {
    val dark =
        when (appearance) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
    val colors = if (dark) sableDarkColors() else sableLightColors()
    MaterialTheme(
        colorScheme = colors,
        typography = sableTypography,
        shapes = sableShapes,
        content = content,
    )
}

private fun sableDarkColors() =
    darkColorScheme(
        primary = SablePalette.blue,
        onPrimary = Color.Black,
        background = SablePalette.darkBase,
        onBackground = Color.White,
        surface = SablePalette.darkSurface,
        onSurface = Color.White,
        surfaceVariant = SablePalette.darkRaised,
        onSurfaceVariant = SablePalette.darkMuted,
        outline = SablePalette.blue,
        outlineVariant = SablePalette.darkRaised,
        secondary = SablePalette.darkMuted,
        onSecondary = Color.Black,
        error = SablePalette.darkError,
        surfaceTint = Color.Transparent,
    )

private fun sableLightColors() =
    lightColorScheme(
        primary = SablePalette.blueLightSurface,
        onPrimary = Color.White,
        background = SablePalette.lightBase,
        onBackground = SablePalette.lightText,
        surface = SablePalette.lightSurface,
        onSurface = SablePalette.lightText,
        surfaceVariant = SablePalette.lightRaised,
        onSurfaceVariant = SablePalette.lightMuted,
        outline = SablePalette.blueLightSurface,
        outlineVariant = SablePalette.lightOutline,
        secondary = SablePalette.lightMuted,
        onSecondary = Color.White,
        error = SablePalette.lightError,
        surfaceTint = Color.Transparent,
    )
