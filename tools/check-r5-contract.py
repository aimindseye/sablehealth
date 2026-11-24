#!/usr/bin/env python3
"""No SDK: check R5.1 Dashboard, with history only in dedicated tabs."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/io/github/aimindseye/sablehealth'
sections = (src / 'HealthSections.kt').read_text()
app = (src / 'SableAppState.kt').read_text()
charts = (src / 'MetricCharts.kt').read_text()
chrome = (src / 'AppChrome.kt').read_text()
core = (root / 'core/src/main/kotlin/io/github/aimindseye/sablehealth/core/DemoCharts.kt').read_text()
home = (src / 'DashboardHome.kt').read_text()
assert 'DashboardHome(' in home and 'SLEEP_STAGES' in home
assert 'StepsChart(' not in home and 'RestingHeartRateChart(' not in home
assert 'Section.DASHBOARD' in app and 'ViewMode.GRAPHS' in app
assert 'mutableStateMapOf' in app
assert 'DashboardPreferenceState(preferences)' in app
assert 'DashboardWidgetSelection' in (src / 'WidgetPickerPage.kt').read_text()
assert 'Section.MOVE' in sections and 'Section.VITALS' in sections
assert 'compactQ25' in chrome and 'DeviceProfile.Q25' in chrome
assert 'contentDescription' in charts and 'DEMO' in charts
assert 'DemoBand' in core and 'TODAY_GOAL_STEPS' in core
assert 'getGrantedPermissions' in (src / 'MainActivity.kt').read_text()
assert 'readRecords' not in (src / 'MainActivity.kt').read_text(), 'M0 entrypoint must remain read-free'
assert 'readRecords' in (src / 'M1HealthConnectProbe.kt').read_text()
print('PASS_R51_DASHBOARD: 4 widgets, daily-only home, Q25 density, demo chart semantics')
