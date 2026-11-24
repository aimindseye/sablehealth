#!/usr/bin/env python3
"""M1-A fail-closed static contract; Android compilation remains a separate gate."""
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/io/github/aimindseye/sablehealth"
CORE = ROOT / "core/src/main/kotlin/io/github/aimindseye/sablehealth/core"
manifest = ROOT / "app/src/main/AndroidManifest.xml"
tree = ET.parse(manifest)
permissions = {
    element.attrib.get("{http://schemas.android.com/apk/res/android}name")
    for element in tree.getroot().findall("uses-permission")
}
expected = {
    "android.permission.health." + name
    for name in (
        "READ_ACTIVE_CALORIES_BURNED",
        "READ_BODY_TEMPERATURE",
        "READ_EXERCISE",
        "READ_HEART_RATE",
        "READ_HEART_RATE_VARIABILITY",
        "READ_OXYGEN_SATURATION",
        "READ_RESPIRATORY_RATE",
        "READ_RESTING_HEART_RATE",
        "READ_SKIN_TEMPERATURE",
        "READ_SLEEP",
        "READ_STEPS",
    )
}
assert permissions == expected, f"Unexpected permissions: {permissions.symmetric_difference(expected)}"
assert all("WRITE_" not in permission for permission in permissions)
application = tree.getroot().find("application")
assert application is not None
assert any(element.tag == "activity-alias" for element in application)
rationale = (APP / "M1PermissionRationaleActivity.kt").read_text()
assert "Health Connect privacy" in rationale and "revoked" in rationale
probe = (APP / "M1HealthConnectProbe.kt").read_text()
assert "readRecords(" in probe
assert "TimeRangeFilter.between(from, now)" in probe
assert "getGrantedPermissions" in probe
for forbidden in ("insertRecords(", "updateRecords(", "deleteRecords(", "Log.", "println(", "WRITE_"):
    assert forbidden not in probe, f"Forbidden M1-A probe operation: {forbidden}"
for file in APP.glob("*.kt"):
    if file.name != "M1HealthConnectProbe.kt":
        assert "readRecords(" not in file.read_text(), f"Record read outside M1 probe: {file.name}"
ui = (APP / "M1DiscoveryPage.kt").read_text()
assert "permissionLauncher.launch(request)" in ui
assert "M1HealthConnectProbe.scan(context, now)" in ui
assert "ACTION_SEND" in ui and "createChooser" in ui
assert "M1_DISCOVERY" in (APP / "AppChrome.kt").read_text()
assert "M1_DISCOVERY" in (APP / "SableAppState.kt").read_text()
report = (CORE / "HealthCoverageContract.kt").read_text()
for marker in ("MAX_RECORDS_PER_TYPE = 500", "RECORD_MEASUREMENT_VALUES_INCLUDED=false", "COUNT_IS_LOWER_BOUND"):
    assert marker in report
for forbidden in ("android.permission.INTERNET", "android.permission.BLUETOOTH"):
    assert forbidden not in manifest.read_text()
print("RESULT=PASS_M1A_READ_ONLY_CONTRACT")
