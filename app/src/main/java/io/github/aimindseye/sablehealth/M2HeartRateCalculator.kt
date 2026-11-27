package io.github.aimindseye.sablehealth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import io.github.aimindseye.sablehealth.core.HeartRateZone
import io.github.aimindseye.sablehealth.core.heartRateZones

@Composable
internal fun M2HeartRateCalculator() {
    var expanded by remember { mutableStateOf(false) }
    R7aGroup("") {
        R7aRow("Heart-rate zones", "Calculate", action = { expanded = !expanded })
        if (expanded) ZoneInputs()
    }
}

@Composable
private fun ZoneInputs() {
    var maximum by remember { mutableStateOf("") }
    var resting by remember { mutableStateOf("") }
    R7aNote("Manual inputs · not saved. No estimated maximum or exercise prescription.")
    BpmInput("Maximum HR (bpm)", maximum) { maximum = it }
    BpmInput("Resting HR (optional)", resting) { resting = it }
    val zones = manualZones(maximum, resting)
    if (zones.isEmpty()) {
        R7aNote("Enter a whole-number maximum (1–300 bpm); resting must be positive and below maximum.")
    } else {
        ZoneResults(zones, resting.isNotBlank())
    }
    R7aAction(
        "Clear inputs",
        {
            maximum = ""
            resting = ""
        },
    )
}

@Composable
private fun BpmInput(
    title: String,
    value: String,
    change: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        modifier = Modifier.fillMaxWidth().testTag("m2-bpm-$title"),
        label = { Text(title) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun ZoneResults(
    zones: List<HeartRateZone>,
    reserve: Boolean,
) {
    val formula = if (reserve) "Rest + (maximum − rest) × fraction" else "Maximum × fraction"
    R7aNote("Calculate & verify · zones v1")
    R7aNote(formula)
    zones.forEach { zone ->
        R7aRow(
            "${zone.lowerPercent}–${zone.upperPercent}%",
            "${formatNumber(zone.lowerBpm)}–${formatNumber(zone.upperBpm)} bpm",
        )
    }
    R7aNote("Illustrative bands 50–100%; shared boundaries are shown to one decimal place.")
}

internal fun manualZones(
    maximum: String,
    resting: String,
): List<HeartRateZone> {
    val max = maximum.trim().toIntOrNull()
    val rest = resting.trim().toIntOrNull()
    return if (resting.isNotBlank() && rest == null) emptyList() else heartRateZones(max, rest)
}
