#!/usr/bin/env python3
"""Fail-closed static scope check for approved R5.4 Sources & Settings layout."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'app/src/main/java/io/github/aimindseye/sablehealth'
checks = {
    'SourcesScreens.kt': ['internal fun SourcesPage(', 'SourcesUiContract.companionStatus', 'state.companion.chooseConfirmation', 'SourceDetailPage', 'SourcePrivacyPage', 'Check availability again'],
    'SettingsScreens.kt': ['internal fun SettingsPage(', 'CompactChoiceRow(', 'Customize widgets', 'PhoneInputPage', 'SettingsPrivacyPage', 'Data mode'],
    'CompactPreferencesUi.kt': ['internal fun CompactLinkRow(', 'minHeight = 48.dp', '.focusable()', 'onFocusChanged', 'internal fun CompactChoiceRow('],
    'SableAppState.kt': ['CompanionSyncPreferenceState', 'Page.SOURCE_DETAIL', 'Page.SOURCE_PRIVACY', 'Page.PHONE_INPUT', 'Page.SETTINGS_PRIVACY'],
    'AppChrome.kt': ['Page.SOURCE_DETAIL ->', 'Page.SOURCE_PRIVACY ->', 'Page.PHONE_INPUT ->', 'Page.SETTINGS_PRIVACY ->', 'onBack = if (state.page == Page.MAIN)'],
    'MainActivity.kt': ['SOURCE_DETAIL', 'SOURCE_PRIVACY', 'PHONE_INPUT', 'SETTINGS_PRIVACY'],
}
failures=[]
for filename, needles in checks.items():
    content=(APP/filename).read_text()
    failures += [f'{filename}: missing {needle}' for needle in needles if needle not in content]
companion=(APP/'CompanionSyncPreferenceState.kt').read_text()
for needle in ('fitbit.sync.user_confirmed', 'mutableStateOf', 'chooseConfirmation', 'preferences.edit()'):
    if needle not in companion:
        failures.append(f'CompanionSyncPreferenceState: missing {needle}')
core=(ROOT/'core/src/main/kotlin/io/github/aimindseye/sablehealth/core/SourcesUiContract.kt').read_text()
for needle in ('User-confirmed sync', 'Setup not confirmed in this app', 'Health Connect export unverified', 'None · M0'):
    if needle not in core:
        failures.append(f'core SourcesUiContract: missing {needle}')
for filename in ('app/src/main/AndroidManifest.xml', 'app/build.gradle.kts'):
    content=(ROOT/filename).read_text()
    if 'android.permission.INTERNET' in content or 'android.permission.BLUETOOTH' in content:
        failures.append(f'{filename}: M0 must not request network/BLE permissions')
for filename in ('docs/R54_SOURCES_SETTINGS_ACCEPTANCE.md', 'docs/R54_APPROVED_DESIGN.md'):
    if not (ROOT/filename).is_file():
        failures.append(f'missing documentation: {filename}')
if failures:
    raise SystemExit('\n'.join(failures))
print('RESULT=PASS_R54_SOURCES_SETTINGS_CONTRACT')
