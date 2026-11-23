#!/usr/bin/env python3
"""R5.2 Q25 design-contract checks: vector hero artwork, compact chrome, preserved privacy."""
from pathlib import Path
from xml.etree import ElementTree

root = Path(__file__).resolve().parents[1]
app = root / 'app/src/main/java/io/github/aimindseye/sablehealth'
res = root / 'app/src/main/res/drawable'
home = (app / 'DashboardHome.kt').read_text()
chrome = (app / 'AppChrome.kt').read_text()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()

assert 'SectionHeading("Dashboard"' in home and 'if (!compactQ25)' in home
assert 'DASHBOARD · DEMO' in chrome and 'DASHBOARD · NO RECORDS' in chrome
assert 'Q25ChromeButton("Sources", onSources)' in chrome
assert 'Q25ChromeButton("Settings", onSettings)' in chrome
assert 'Q25_TOUCH_HEIGHT_DP = 48' in chrome
assert 'Q25_VISUAL_HEIGHT_DP = 36' in chrome
assert 'if (compactQ25)' in chrome and 'Q25ChromeButton(' in chrome
assert 'sizeIn(minWidth = 48.dp, minHeight = Q25_TOUCH_HEIGHT_DP.dp)' in chrome
assert 'COMPACT_RING_DP = 44' in home and 'REGULAR_RING_DP = 48' in home
assert 'HERO_Q25_MIN_HEIGHT_DP = 116' in home
assert 'ic_sable_steps' in home and 'ic_sable_heart' in home
assert 'Text("♥"' not in home and 'Text("↟"' not in home
assert 'Color.Unspecified' in home and 'Red identifies the heart' in home
assert 'DashboardWide(' in home and 'DashboardCompact(' in home
assert 'StepsChart(' not in home and 'RestingHeartRateChart(' not in home
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.BLUETOOTH' not in manifest
for icon_name in ('ic_sable_steps', 'ic_sable_heart'):
    path = res / (icon_name + '.xml')
    elem = ElementTree.parse(path).getroot()
    assert elem.tag == 'vector' and len(elem) >= 1
    assert all('pathData' in str(child.attrib) for child in elem)
assert '#FFE25560' in (res / 'ic_sable_heart.xml').read_text()
print('PASS_R52_Q25: unsquished vector icons, red heart, compact visible chrome, >=48dp hit targets')
