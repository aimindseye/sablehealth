#!/usr/bin/env python3
"""Static invariant checks that need no Android SDK; CI runs these before any merge."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / 'app/src/main/java/io/github/aimindseye/sablehealth/SableTheme.kt').read_text()
activity = (root / 'app/src/main/java/io/github/aimindseye/sablehealth/MainActivity.kt').read_text()
core = (root / 'core/src/main/kotlin/io/github/aimindseye/sablehealth/core/KeyboardMapping.kt').read_text()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
assert '0xFF2E9BD6' in source and '0xFF1A6FA0' in source, 'not Sable Blue'
for s in ['0xFF000000', '0xFF141414', '0xFFF5F7F9', '0xFFFFFFFF']:
    assert s in source, f'missing Sable token {s}'
for section in ['TODAY', 'SLEEP', 'MOVE', 'VITALS', 'TRENDS']:
    assert section in activity, f'missing tab {section}'
assert 'getSdkStatus' in activity and 'getGrantedPermissions' in activity
assert 'readRecords' not in activity, 'M0 must not ingest health records'
for key in ['UiAction.CONFIRM', 'UiAction.REFRESH', 'editingText', 'repeatCount']:
    assert key in activity + core, f'missing key contract {key}'
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.BLUETOOTH' not in manifest
print('PASS_M0_STATIC_CONTRACT: Sable Blue, tabs, keys, diagnostic-only, no network/BLE permissions')
