package io.github.aimindseye.sablehealth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablehealth.core.DeviceProfile
import io.github.aimindseye.sablehealth.core.SetupEntry
import io.github.aimindseye.sablehealth.core.SetupPlan
import io.github.aimindseye.sablehealth.core.SetupStage

@Composable
internal fun SetupWizard(
    state: SableAppState,
    profile: DeviceProfile,
    entry: SetupEntry,
    close: (Boolean) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(SetupStage.entries.joinToString(",") { it.name }) }
    var choosing by rememberSaveable { mutableStateOf(entry == SetupEntry.SELECTED_STEPS) }
    var index by rememberSaveable { mutableStateOf(0) }
    val stages = SetupPlan.stages(entry, SetupStage.entries.filter { it.name in selected.split(",") }.toSet())
    val stage = stages[index.coerceIn(stages.indices)]
    val back = {
        when {
            choosing -> close(false)
            index > 0 -> index = SetupPlan.previous(index)
            entry == SetupEntry.SELECTED_STEPS -> choosing = true
            entry != SetupEntry.FIRST_RUN -> close(false)
        }
    }
    BackHandler { back() }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompactPageTitle("Sable Health", if (choosing) "SETUP" else "${index + 1}/${stages.size}")
        key(choosing, stage) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (choosing) {
                    SetupStagePicker(selected) { selected = it }
                } else {
                    Text(stage.title, style = MaterialTheme.typography.titleLarge)
                    SetupStageContent(stage, state, profile)
                }
            }
        }
        SetupFooter(
            back = back,
            nextLabel = if (!choosing && stage == SetupStage.REVIEW) "Finish" else "Next",
            next = {
                when {
                    choosing -> choosing = false
                    stage == SetupStage.REVIEW -> close(true)
                    else -> index = SetupPlan.next(index, stages.size)
                }
            },
            skip = {
                if (choosing || entry != SetupEntry.FIRST_RUN && index == 0) close(false)
                else if (stage == SetupStage.REVIEW) close(true)
                else index = SetupPlan.next(index, stages.size)
            },
        )
    }
}

@Composable
private fun SetupStagePicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text("Choose setup steps", style = MaterialTheme.typography.titleLarge)
    SetupStage.entries.filter { it != SetupStage.REVIEW }.forEach { stage ->
        val names = selected.split(",").toSet()
        CompactLinkRow(
            stage.title,
            if (stage.name in names) "Selected" else "Not selected",
            {
                val next = if (stage.name in names) names - stage.name else names + stage.name
                onSelect(next.joinToString(","))
            },
        )
    }
    Text("Review is always included.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SetupFooter(
    back: () -> Unit,
    nextLabel: String,
    next: () -> Unit,
    skip: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FocusButton("Back", back, modifier = Modifier.weight(1f))
        FocusButton("Skip", skip, modifier = Modifier.weight(1f))
        FocusButton(nextLabel, next, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SetupStageContent(
    stage: SetupStage,
    state: SableAppState,
    profile: DeviceProfile,
) {
    when (stage) {
        SetupStage.WELCOME -> {
            Notice("Local-first wellness", "No cloud, account or background access.")
            Notice("Phone detected", profile.label)
        }
        SetupStage.SOURCES -> SetupSources(state)
        SetupStage.PERMISSIONS -> SetupPermissions(state)
        SetupStage.VERIFY -> SetupVerify(state)
        SetupStage.PERSONALIZE -> SetupPersonalize(state)
        SetupStage.REVIEW -> {
            Notice("Health Connect", state.connect.label)
            Notice("Read access", setupAccessSummary(state.connect))
            Notice("Dashboard", "Setup does not import measurements. Demo remains synthetic.")
            Text("Finish even if access is missing; return from Settings at any time.")
        }
    }
}

@Composable
private fun SetupSources(state: SableAppState) {
    Notice("Health Connect", state.connect.detail)
    FocusButton("Check availability", state::recheckPermissions, modifier = Modifier.fillMaxWidth())
    Notice("Fitbit Air", "Pair and sync in Google Health. Sable Health cannot verify pairing.")
    CompactLinkRow(
        "Companion setup",
        if (state.companion.confirmed) "User confirmed" else "Not confirmed",
        { state.companion.chooseConfirmation(!state.companion.confirmed) },
    )
}

@Composable
private fun SetupPersonalize(state: SableAppState) {
    CompactChoiceRow(
        "Appearance",
        listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
        state.appearance,
        state::chooseAppearance,
    )
    CompactChoiceRow(
        "Data mode",
        listOf("demo" to "Demo", "empty" to "No records"),
        if (state.demo) "demo" else "empty",
        { state.chooseDemo(it == "demo") },
    )
    Text("Choices save immediately. Existing dashboard widgets are preserved.")
}
