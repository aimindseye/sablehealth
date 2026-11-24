#!/usr/bin/env python3
"""R5.1 static contract: preserve daily-only Dashboard, four widgets and no imports."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app = root / 'app/src/main/java/io/github/aimindseye/sablehealth'
core = root / 'core/src/main/kotlin/io/github/aimindseye/sablehealth/core'
activity = (app / 'MainActivity.kt').read_text()
state = (app / 'SableAppState.kt').read_text()
home = (app / 'DashboardHome.kt').read_text()
picker = (app / 'WidgetPickerPage.kt').read_text()
widget = (core / 'DashboardWidgets.kt').read_text()
chrome = (app / 'AppChrome.kt').read_text()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()

assert 'DASHBOARD("Dashboard")' in activity
assert 'Section.TODAY' not in '\n'.join(p.read_text() for p in app.glob('*.kt'))
assert 'DashboardHome(' in home
assert 'StepsChart(' not in home and 'RestingHeartRateChart(' not in home
assert 'DashboardHero(' in home and 'DashboardWide(' in home and 'DashboardCompact(' in home
assert 'DashboardDataStatus(state)' in home
assert 'DashboardPreferenceState(preferences)' in state
assert 'dashboard.widgets.v1' in (app / 'DashboardPreferenceState.kt').read_text()
assert 'WidgetStyle' in (app / 'DashboardPreferenceState.kt').read_text()
assert 'preferences.edit()' in (app / 'DashboardPreferenceState.kt').read_text()
assert 'DashboardWidgetSelection.toggle' in picker
assert 'DashboardWidgetSelection.isValid' in picker
assert 'Restore four defaults' in picker and 'Cancel · back to Settings' in picker
assert 'Page.WIDGETS' in chrome and 'Customize widgets' in (app / 'SettingsScreens.kt').read_text()
assert 'val defaults =' in widget and 'listOf(' in widget and 'const val COUNT = 4' in widget
assert 'SPO2(' in widget and 'READINESS(' in widget and 'DATA_FRESHNESS(' in widget
assert 'No live data imported' in home
assert 'Not synced' in (app / 'DashboardVisuals.kt').read_text()
assert 'No demo sample' in (app / 'DashboardVisuals.kt').read_text()
assert 'contentDescription' in (app / 'DashboardVisuals.kt').read_text() and 'compactQ25' in chrome
assert 'getGrantedPermissions' in activity and 'readRecords' not in activity
assert 'android.permission.INTERNET' not in manifest and 'android.permission.BLUETOOTH' not in manifest
print('PASS_R51: dashboard four-widget selection, no history/imports, fallback states and Q25 input')
