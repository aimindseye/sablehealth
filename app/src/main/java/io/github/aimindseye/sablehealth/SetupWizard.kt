package io.github.aimindseye.sablehealth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
            SetupDraft(state.appearance, state.demo, state.companion.confirmed, state.live)
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
            nextLabel = setupNextLabel(choosing, stage),
            next = advance,
            skip = { if (choosing) close(false) else advance() },
        )
    SetupFrame(SetupFrameProgress(setupProgress(choosing, index, stages.size), index, stages.size, choosing), actions) {
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

private fun setupNextLabel(
    choosing: Boolean,
    stage: SetupStage,
): String = if (!choosing && stage == SetupStage.REVIEW) "Finish" else "Next"

private fun setupProgress(
    choosing: Boolean,
    index: Int,
    size: Int,
): String = if (choosing) "SETUP" else "${index + 1}/$size"

private data class SetupActions(
    val back: () -> Unit,
    val nextLabel: String,
    val next: () -> Unit,
    val skip: () -> Unit,
)

private data class SetupFrameProgress(
    val label: String,
    val index: Int,
    val count: Int,
    val choosing: Boolean,
)

@Composable
private fun SetupFrame(
    progress: SetupFrameProgress,
    actions: SetupActions,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler { actions.back() }
    Column(
        Modifier
            .fillMaxSize()
            .background(r7aBackground())
            .safeDrawingPadding()
            .padding(8.dp)
            .onPreviewKeyEvent {
                val escape = it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE
                if (it.type == KeyEventType.KeyDown && escape) {
                    actions.back()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompactPageTitle("Sable Health setup", progress.label)
        if (!progress.choosing) R7aSetupProgress(progress.index, progress.count)
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
    R7aGroup("Select steps") {
        SetupStage.entries.filter { it != SetupStage.REVIEW }.forEach { stage ->
            val names = selected.split(",").toSet()
            R7aRow(stage.title, chosen = stage.name in names, action = {
                val next = if (stage.name in names) names - stage.name else names + stage.name
                onSelect(next.joinToString(","))
            })
            R7aRule()
        }
    }
    R7aNote("Review is always included.")
}

@Composable
private fun SetupFooter(actions: SetupActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        R7aAction("Back", actions.back, modifier = Modifier.weight(1f))
        if (actions.nextLabel != "Finish") {
            R7aAction("Skip", actions.skip, modifier = Modifier.weight(1f))
        }
        R7aAction(actions.nextLabel, actions.next, modifier = Modifier.weight(1f), primary = true)
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
            R7aGroup("", r7MetricSurface(null)) {
                Text("Local-first wellness", style = MaterialTheme.typography.titleMedium)
                R7aNote("No cloud, account or background access. Use your health records on this phone.")
            }
            R7aGroup("Phone detected") { R7aRow("Current device", profile.label) }
            R7aNote("Permissions are optional.")
        }
        SetupStage.SOURCES -> SetupSources(state, draft)
        SetupStage.PERMISSIONS -> SetupPermissions(state)
        SetupStage.VERIFY -> SetupVerify(state)
        SetupStage.PERSONALIZE -> SetupPersonalize(draft)
        SetupStage.REVIEW -> SetupReview(state, draft)
    }
}

@Composable
private fun SetupReview(
    state: SableAppState,
    draft: SetupDraft,
) {
    R7aGroup("Health data") {
        R7aRow("Health Connect", state.connect.label)
        R7aRule()
        R7aRow("Read access", value = setupAccessSummary(state.connect))
        R7aRule()
        R7aRow("Companion", if (draft.confirmed) "User confirmed" else "Not confirmed")
    }
    R7aGroup("Preferences") {
        val mode =
            when (draft.dataMode) {
                "real" -> "Real data"
                "demo" -> "Demo"
                else -> "No records"
            }
        R7aRow("Dashboard", mode)
        R7aRule()
        R7aRow("Appearance", draft.appearance.replaceFirstChar(Char::uppercaseChar))
    }
    R7aNote("Finish even if access is missing; return from Settings at any time.")
}

@Composable
private fun SetupSources(
    state: SableAppState,
    draft: SetupDraft,
) {
    R7aGroup("Health Connect") {
        R7aRow("Availability", state.connect.label, detail = state.connect.detail)
        R7aRule()
        R7aRow("Check availability", action = state::recheckPermissions)
    }
    R7aGroup("Google Health / Fitbit Air") {
        R7aRow("Companion setup", chosen = draft.confirmed, action = { draft.confirmed = !draft.confirmed })
        R7aNote("Pair and sync in Google Health. Sable Health cannot verify pairing.")
    }
}

@Composable
private fun SetupPersonalize(draft: SetupDraft) {
    R7aGroup("", r7MetricSurface(null)) {
        Text("Make it yours", style = MaterialTheme.typography.titleMedium)
        R7aNote("Choose how Sable Health looks and which data mode it uses.")
    }
    R7aGroup("Preferences") {
        R7aAppearance(draft.appearance) { draft.appearance = it }
        R7aRule()
        R7aDataMode(draft.dataMode, draft::chooseDataMode)
    }
    R7aNote("Choices save on Finish. Existing dashboard widgets are preserved.")
}

/** Only pending non-health preferences. Android grants are never persisted here. */
internal class SetupDraft(
    appearance: String,
    demo: Boolean,
    confirmed: Boolean,
    live: Boolean = false,
) {
    var appearance by mutableStateOf(appearance)
    var demo by mutableStateOf(demo)
    var live by mutableStateOf(live && !demo)
    val dataMode: String get() =
        if (live) {
            "real"
        } else if (demo) {
            "demo"
        } else {
            "empty"
        }

    fun chooseDataMode(value: String) {
        demo = value == "demo"
        live = value == "real"
    }

    var confirmed by mutableStateOf(confirmed)

    fun applyTo(state: SableAppState) {
        if (appearance != state.appearance) state.chooseAppearance(appearance)
        if (dataMode != state.dataMode) state.chooseDataMode(dataMode)
        if (confirmed != state.companion.confirmed) state.companion.chooseConfirmation(confirmed)
    }

    companion object {
        val saver =
            listSaver<SetupDraft, String>(
                save = { listOf(it.appearance, it.demo.toString(), it.confirmed.toString(), it.live.toString()) },
                restore = { SetupDraft(it[0], it[1].toBoolean(), it[2].toBoolean(), it.getOrNull(3).toBoolean()) },
            )
    }
}
