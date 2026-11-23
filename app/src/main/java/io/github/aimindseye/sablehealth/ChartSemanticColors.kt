package io.github.aimindseye.sablehealth

import androidx.compose.ui.graphics.Color
import io.github.aimindseye.sablehealth.core.DemoBand

/** Sable semantic tokens. These express illustrative goals, not clinical normality. */
internal object ChartColors {
    private const val DARK_GOOD = 0xFF4ADE80
    private const val LIGHT_GOOD = 0xFF1A7F45
    private const val DARK_CAUTION = 0xFFFBBF24
    private const val LIGHT_CAUTION = 0xFF8A5A00
    val darkGood = Color(DARK_GOOD.toInt())
    val lightGood = Color(LIGHT_GOOD.toInt())
    val darkCaution = Color(DARK_CAUTION.toInt())
    val lightCaution = Color(LIGHT_CAUTION.toInt())
}

internal enum class DemoBandColor { INFO, GOOD, CAUTION, OUTLIER }

internal fun demoBandColor(band: DemoBand, dark: Boolean): Color = when (band) {
    DemoBand.INFORMATIONAL -> if (dark) SablePalette.blue else SablePalette.blueLightSurface
    DemoBand.GOAL_MET -> if (dark) ChartColors.darkGood else ChartColors.lightGood
    DemoBand.BELOW_PLAN -> if (dark) ChartColors.darkCaution else ChartColors.lightCaution
    DemoBand.SYNTHETIC_OUTLIER -> if (dark) SablePalette.darkError else SablePalette.lightError
}

internal fun demoLegendColor(band: DemoBandColor, dark: Boolean): Color = when (band) {
    DemoBandColor.INFO -> demoBandColor(DemoBand.INFORMATIONAL, dark)
    DemoBandColor.GOOD -> demoBandColor(DemoBand.GOAL_MET, dark)
    DemoBandColor.CAUTION -> demoBandColor(DemoBand.BELOW_PLAN, dark)
    DemoBandColor.OUTLIER -> demoBandColor(DemoBand.SYNTHETIC_OUTLIER, dark)
}
