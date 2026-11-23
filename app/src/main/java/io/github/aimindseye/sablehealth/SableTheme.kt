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

/** Exact Sable Games SableTheme/SableSystemTokens palette; 2026-11-23 approved override. */
object SablePalette {
    val blue = Color(0xFF2E9BD6)
    val blueLightSurface = Color(0xFF1A6FA0)
    val darkBase = Color(0xFF000000)
    val darkSurface = Color(0xFF141414)
    val darkRaised = Color(0xFF262626)
    val darkMuted = Color(0xFFA6A6A6)
    val lightBase = Color(0xFFF5F7F9)
    val lightSurface = Color(0xFFFFFFFF)
    val lightRaised = Color(0xFFE9EDF2)
    val lightText = Color(0xFF12171C)
    val lightMuted = Color(0xFF515B66)
}

/** Never package external font binaries; source-sans is optional on SableOS. */
private val sableFont = FontFamily(Typeface.create("source-sans", Typeface.NORMAL))

private val typography = Typography(
    headlineMedium = TextStyle(fontFamily = sableFont, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = sableFont, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontFamily = sableFont, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = sableFont, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = sableFont, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = sableFont, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = sableFont, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun SableHealthTheme(appearance: String, content: @Composable () -> Unit) {
    val dark = when (appearance) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) darkColorScheme(
        primary = SablePalette.blue, onPrimary = Color.Black,
        background = SablePalette.darkBase, onBackground = Color.White,
        surface = SablePalette.darkSurface, onSurface = Color.White,
        surfaceVariant = SablePalette.darkRaised, onSurfaceVariant = SablePalette.darkMuted,
        outline = SablePalette.blue, outlineVariant = SablePalette.darkRaised,
        secondary = SablePalette.darkMuted, onSecondary = Color.Black,
        error = Color(0xFFE0463C), surfaceTint = Color.Transparent,
    ) else lightColorScheme(
        primary = SablePalette.blueLightSurface, onPrimary = Color.White,
        background = SablePalette.lightBase, onBackground = SablePalette.lightText,
        surface = SablePalette.lightSurface, onSurface = SablePalette.lightText,
        surfaceVariant = SablePalette.lightRaised, onSurfaceVariant = SablePalette.lightMuted,
        outline = SablePalette.blueLightSurface, outlineVariant = Color(0xFFD0D6DD),
        secondary = SablePalette.lightMuted, onSecondary = Color.White,
        error = Color(0xFFBA1A1A), surfaceTint = Color.Transparent,
    )
    MaterialTheme(colorScheme = colors, typography = typography, shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(16.dp),
    ), content = content)
}
