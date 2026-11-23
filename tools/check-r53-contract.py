#!/usr/bin/env python3
"""R5.3: bounded Q25 header, Phosphor steps + branded Android launcher assets."""
from pathlib import Path
from xml.etree import ElementTree as ET

root=Path(__file__).resolve().parents[1]
app=root/'app/src/main'
ui=app/'java/io/github/aimindseye/sablehealth'
chrome=(ui/'AppChrome.kt').read_text()
dashboard=(ui/'DashboardHome.kt').read_text()
manifest=(app/'AndroidManifest.xml').read_text()

assert 'private const val Q25_HEADER_ACTION_WIDTH_DP = 76' in chrome
assert chrome.count('modifier = Modifier.width(Q25_HEADER_ACTION_WIDTH_DP.dp)') == 2
assert 'modifier = Modifier.fillMaxWidth()' in chrome
assert 'Column(Modifier.weight(1f))' in chrome
assert 'Q25ChromeButton(' in chrome
assert '"Sources"' in chrome and '"Settings"' in chrome
assert 'profile == DeviceProfile.Q25 || state.page == Page.MAIN' in chrome
assert 'Q25_TOUCH_HEIGHT_DP = 48' in chrome
assert 'Q25_VISUAL_HEIGHT_DP = 36' in chrome
assert 'HERO_Q25_MIN_HEIGHT_DP = 116' in dashboard
assert 'ic_sable_steps' in dashboard and 'ic_sable_heart' in dashboard
assert 'android:icon="@mipmap/ic_sable_health"' in manifest
assert 'android:roundIcon="@mipmap/ic_sable_health_round"' in manifest
for name in ('ic_sable_health', 'ic_sable_health_round'):
    icon=app/'res/mipmap-anydpi-v26'/f'{name}.xml'
    assert ET.parse(icon).getroot().tag == 'adaptive-icon', name
    text=icon.read_text()
    for layer in ('background','foreground','monochrome'):
        assert f'ic_sable_health_{layer}' in text, name
for name in ('ic_sable_steps','ic_sable_health_background','ic_sable_health_foreground','ic_sable_health_monochrome','ic_sable_health'):
    icon=app/'res/drawable'/f'{name}.xml'
    assert ET.parse(icon).getroot().tag == 'vector', name
step=(app/'res/drawable/ic_sable_steps.xml').read_text()
assert 'viewportWidth="256"' in step and 'M208.06,184H152' in step
assert 'FOOTPRINT' not in step
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.BLUETOOTH' not in manifest
assert (root/'docs/licenses/PHOSPHOR_MIT.txt').exists()
print('PASS_R53: Q25 branded bounded header, 48dp targets, Phosphor icons, adaptive launcher layers')
