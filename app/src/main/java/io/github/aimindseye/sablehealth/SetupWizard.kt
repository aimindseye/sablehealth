package io.github.aimindseye.sablehealth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
    val draft =
        rememberSaveable(saver = SetupDraft.saver) {
            SetupDraft(state.appearance, state.demo, state.companion.confirmed)
        }
    val holder = rememberSaveableStateHolder()
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
            else -> Unit
        }
    }
    val advance = {
        when {
            choosing -> choosing = false
            stage == SetupStage.REVIEW -> {
                draft.applyTo(state)
                close(true)
            }
            else -> index = SetupPlan.next(index, stages.size)
        }
    }
    val actions =
        SetupActions(
            back = back,
            nextLabel = if (!choosing && stage == SetupStage.REVIEW) "Finish" else "Next",
            next = advance,
            skip = { if (choosing) close(false) else advance() },
        )
    SetupFrame(if (choosing) "SETUP" else "${index + 1}/${stages.size}", actions) {
        holder.SaveableStateProvider(if (choosing) "picker" else stage.name) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (choosing) {
                    SetupStagePicker(selected) { selected = it }
                } else {
                    Text(stage.title, style = MaterialTheme.typography.titleLarge)
                    SetupStageContent(stage, state, profile, draft)
                }
            }
        }
    }
}

private data class SetupActions(
    val back: () -> Unit,
    val nextLabel: String,
    val next: () -> Unit,
    val skip: () -> Unit,
)

@Composable
private fun SetupFrame(
    progress: String,
    actions: SetupActions,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler { actions.back() }
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(8.dp)
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
                    actions.back()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompactPageTitle("Sable Health", progress)
        content()
        SetupFooter(actions)
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
private fun SetupFooter(actions: SetupActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FocusButton("Back", actions.back, modifier = Modifier.weight(1f))
        FocusButton("Skip", actions.skip, modifier = Modifier.weight(1f))
        FocusButton(actions.nextLabel, actions.next, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SetupStageContent(
    stage: SetupStage,
    state: SableAppState,
    profile: DeviceProfile,
    draft: SetupDraft,
) {
    when (stage) {
        SetupStage.WELCOME -> {
            Notice("Local-first wellness", "No cloud, account or background access.")
            Notice("Phone detected", profile.label)
        }
        SetupStage.SOURCES -> SetupSources(state, draft)
        SetupStage.PERMISSIONS -> SetupPermissions(state)
        SetupStage.VERIFY -> SetupVerify(state)
        SetupStage.PERSONALIZE -> SetupPersonalize(draft)
        SetupStage.REVIEW -> {
            Notice("Health Connect", state.connect.label)
            Notice("Read access", setupAccessSummary(state.connect))
            Notice("Dashboard", if (draft.demo) "Synthetic demo" else "No records imported")
            Notice("Appearance", draft.appearance)
            Notice("Companion", if (draft.confirmed) "User confirmed" else "Not confirmed")
            Text("Finish even if access is missing; return from Settings at any time.")
        }
    }
}

@Composable
private fun SetupSources(
    state: SableAppState,
    draft: SetupDraft,
) {
    Notice("Health Connect", state.connect.detail)
    FocusButton("Check availability", state::recheckPermissions, modifier = Modifier.fillMaxWidth())
    Notice("Fitbit Air", "Pair and sync in Google Health. Sable Health cannot verify pairing.")
    CompactLinkRow(
        "Companion setup",
        if (draft.confirmed) "User confirmed" else "Not confirmed",
        { draft.confirmed = !draft.confirmed },
    )
}

@Composable
private fun SetupPersonalize(draft: SetupDraft) {
    CompactChoiceRow(
        "Appearance",
        listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
        draft.appearance,
        { draft.appearance = it },
    )
    CompactChoiceRow(
        "Data mode",
        listOf("demo" to "Demo", "empty" to "No records"),
        if (draft.demo) "demo" else "empty",
        { draft.demo = it == "demo" },
    )
    Text("Choices save on Finish. Existing dashboard widgets are preserved.")
}

/** Only pending non-health preferences. Android grants are never persisted here. */
internal class SetupDraft(
    appearance: String,
    demo: Boolean,
    confirmed: Boolean,
) {
    var appearance by mutableStateOf(appearance)
    var demo by mutableStateOf(demo)
    var confirmed by mutableStateOf(confirmed)

    fun applyTo(state: SableAppState) {
        if (appearance != state.appearance) state.chooseAppearance(appearance)
        if (demo != state.demo) state.chooseDemo(demo)
        if (confirmed != state.companion.confirmed) state.companion.chooseConfirmation(confirmed)
    }

    companion object {
        val saver =
            listSaver<SetupDraft, String>(
                save = { listOf(it.appearance, it.demo.toString(), it.confirmed.toString()) },
                restore = { SetupDraft(it[0], it[1].toBoolean(), it[2].toBoolean()) },
            )
    }
}
