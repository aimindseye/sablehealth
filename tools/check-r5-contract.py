#!/usr/bin/env python3
"""No SDK: reject trend graphs on Today and guard the R5 device-mode contract."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/io/github/aimindseye/sablehealth'
sections = (src / 'HealthSections.kt').read_text()
app = (src / 'SableAppState.kt').read_text()
charts = (src / 'MetricCharts.kt').read_text()
chrome = (src / 'AppChrome.kt').read_text()
core = (root / 'core/src/main/kotlin/io/github/aimindseye/sablehealth/core/DemoCharts.kt').read_text()
today = sections.split('internal fun TodaySection(', 1)[1].split('internal fun SleepSection(', 1)[0]
assert 'TodayStepGoalChart()' in today and 'SleepStageChart()' in today
assert 'StepsChart(' not in today and 'RestingHeartRateChart(' not in today
assert 'ViewMode.TEXT' in today and 'ViewMode.GRAPHS' in app
assert 'mutableStateMapOf' in app and 'view.${section.name}' in app
assert 'section == Section.TODAY) ViewMode.TEXT' in app
assert 'compactQ25' in chrome and 'DeviceProfile.Q25' in chrome
assert 'contentDescription' in charts and 'DEMO' in charts
assert 'DemoBand' in core and 'TODAY_GOAL_STEPS' in core
assert 'getGrantedPermissions' in (src / 'MainActivity.kt').read_text()
assert 'readRecords' not in '\n'.join(p.read_text() for p in src.glob('*.kt'))
print('PASS_R5_DAILY_TODAY: daily-only graphs, per-tab mode, Q25 density, demo chart semantics')
