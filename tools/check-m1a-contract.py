#!/usr/bin/env python3
"""M1-A fail-closed static contract; Android compilation remains a separate gate."""
from pathlib import Path
import re
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
        "READ_DISTANCE",
        "READ_HEART_RATE",
        "READ_HEART_RATE_VARIABILITY",
        "READ_OXYGEN_SATURATION",
        "READ_RESPIRATORY_RATE",
        "READ_RESTING_HEART_RATE",
        "READ_SKIN_TEMPERATURE",
        "READ_SLEEP",
        "READ_STEPS",
        "READ_TOTAL_CALORIES_BURNED",
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
live = (APP / "LiveHealthReader.kt").read_text()
adapter = (APP / "LiveQueries.kt").read_text()
# Match Kotlin call tokens rather than line layout: ktlint may wrap a receiver chain.
def has_call(content, name, receiver=None):
    prefix = r"\b" if receiver is None else r"\b" + re.escape(receiver) + r"\s*\.\s*"
    if receiver == "*":
        prefix = r"\.\s*"
    return re.search(prefix + re.escape(name) + r"\s*\(", content) is not None


read_calls = ("readRecords", "aggregate", "aggregateGroupByPeriod", "aggregateGroupByDuration")
for call in read_calls:
    assert has_call(adapter, call, "client"), f"Missing authorized adapter call: {call}"
assert "getGrantedPermissions" in live and "MAX_PAGES" in live
write_calls = ("insertRecords", "updateRecords", "deleteRecords", "deleteRecordsByTimeRange")
for file in APP.rglob("*.kt"):
    content = file.read_text()
    for forbidden in write_calls:
        assert not has_call(content, forbidden), f"Health Connect write outside allowed scope: {file.name}"
    if file not in {APP / "M1HealthConnectProbe.kt", APP / "LiveQueries.kt"}:
        for call in read_calls:
            # Reject record/aggregate calls even when the receiver has a different name.
            receiver = "*" if call == "aggregate" else None
            assert not has_call(content, call, receiver), f"Record read outside authorized boundary: {file.name}"
for forbidden in ("Log.", "println(", "WRITE_", "SharedPreferences", "FileOutputStream", "ACTION_SEND"):
    assert forbidden not in live + adapter, f"Forbidden live reader operation: {forbidden}"
ui = (APP / "M1DiscoveryPage.kt").read_text()
assert "permissionLauncher.launch(it)" in ui
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
