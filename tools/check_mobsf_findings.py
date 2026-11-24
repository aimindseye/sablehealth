#!/usr/bin/env python3
"""Preserve inherited MobSF findings and block new high-risk regressions.

Only repository-defined paths are read. The CLI intentionally accepts no
file-system path from a caller or an agent-generated command.
"""

import argparse
from collections import Counter
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
BASELINE_FILE = ROOT / "config/mobsf-known-findings.json"
REPORT_FILE = ROOT / "mobsfscan-results.sarif"


class ScanError(ValueError):
    """A broken scan or unexpected result cannot qualify."""


def parse_json(raw: str, label: str) -> dict:
    try:
        value = json.loads(raw)
    except json.JSONDecodeError as error:
        raise ScanError(f"Malformed {label}: {error}") from error
    if not isinstance(value, dict):
        raise ScanError(f"Expected JSON object for {label}")
    return value


def location(finding: dict) -> str:
    locations = finding.get("locations")
    if not isinstance(locations, list) or not locations:
        raise ScanError("SARIF finding has no location")
    first = locations[0]
    if not isinstance(first, dict):
        raise ScanError("SARIF finding has malformed location")
    physical = first.get("physicalLocation", {})
    if not isinstance(physical, dict):
        raise ScanError("SARIF finding has malformed physical location")
    artifact = physical.get("artifactLocation", {})
    if not isinstance(artifact, dict):
        raise ScanError("SARIF finding has malformed artifact location")
    uri = artifact.get("uri")
    if not isinstance(uri, str) or not uri:
        raise ScanError("SARIF finding has no file URI")
    if uri == ".":
        return uri
    # Normalize only the fixed GitHub Actions checkout root; unexpected absolute
    # locations do not match inherited fingerprints and therefore cannot pass.
    marker = "/sablehealth/sablehealth/"
    if uri.startswith("file://") and marker in uri:
        return uri.split(marker, 1)[1]
    if uri.startswith("./"):
        return uri[2:]
    return uri


def fingerprint(finding: dict) -> tuple[str, str, str]:
    if not isinstance(finding, dict):
        raise ScanError("SARIF result must be an object")
    rule = finding.get("ruleId")
    level = finding.get("level", "warning")
    if not isinstance(rule, str) or not rule:
        raise ScanError("SARIF finding missing ruleId")
    if level not in {"error", "warning", "note", "none"}:
        raise ScanError(f"Unexpected SARIF result level for {rule}: {level}")
    return rule, level, location(finding)


def check(report: dict, baseline: dict) -> tuple[list[tuple[str, str, str]], list[tuple[str, str, str]], list[tuple[str, str, str]]]:
    if baseline.get("schema") != 1:
        raise ScanError("Unknown baseline schema")
    if not baseline.get("source_commit") or not baseline.get("review_issue"):
        raise ScanError("Baseline must pin mainline commit and open security review")
    tool = baseline.get("tool", {})
    if not isinstance(tool, dict):
        raise ScanError("Malformed baseline scanner identity")
    expected_version = tool.get("version")
    if tool.get("name") != "mobsfscan" or not isinstance(expected_version, str) or not expected_version:
        raise ScanError("Baseline must pin MobSF identity and version")
    allowed = baseline.get("inherited")
    if not isinstance(allowed, list) or not allowed:
        raise ScanError("Baseline lacks a documented inherited finding list")
    limits: Counter = Counter()
    for item in allowed:
        if not isinstance(item, dict) or set(item) != {"ruleId", "level", "path", "limit"}:
            raise ScanError("Malformed inherited finding entry")
        key = (item["ruleId"], item["level"], item["path"])
        if not all(isinstance(value, str) and value for value in key):
            raise ScanError("Missing inherited finding fingerprint")
        if key[1] not in {"error", "warning", "note", "none"}:
            raise ScanError("Invalid inherited severity")
        if type(item["limit"]) is not int or item["limit"] != 1 or key in limits:
            raise ScanError("Baseline requires one instance per exact fingerprint")
        limits[key] = 1
    if report.get("version") != "2.1.0":
        raise ScanError("Unexpected SARIF version")
    runs = report.get("runs")
    if not isinstance(runs, list) or len(runs) != 1 or not isinstance(runs[0], dict):
        raise ScanError("Expected exactly one MobSF SARIF run")
    run = runs[0]
    tool_info = run.get("tool", {})
    if not isinstance(tool_info, dict):
        raise ScanError("Malformed SARIF tool")
    driver = tool_info.get("driver", {})
    if not isinstance(driver, dict) or driver.get("name") != "mobsfscan" or driver.get("version") != expected_version:
        raise ScanError("MobSF identity/version differs from documented baseline")
    results = run.get("results")
    if not isinstance(results, list):
        raise ScanError("MobSF SARIF results field missing or invalid")
    seen = Counter()
    inherited, new_notes, new_blocking = [], [], []
    for item in results:
        key = fingerprint(item)
        seen[key] += 1
        if seen[key] <= limits[key]:
            inherited.append(key)
        elif key[1] in {"error", "warning"}:
            new_blocking.append(key)
        else:
            new_notes.append(key)
    return inherited, new_notes, new_blocking


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scan-outcome", required=True)
    args = parser.parse_args()
    try:
        if args.scan_outcome != "success":
            raise ScanError(f"MobSF scan step did not succeed: {args.scan_outcome}")
        # Security boundary: both paths are literals anchored to this source
        # file's repository, NOT constructed from CLI args or outside input.
        baseline = parse_json(BASELINE_FILE.read_text(encoding="utf-8"), "baseline")
        report = parse_json(REPORT_FILE.read_text(encoding="utf-8"), "SARIF")
        inherited, new_notes, new_blocking = check(report, baseline)
        print(f"MOBSF_RESULT_TOTAL={len(inherited) + len(new_notes) + len(new_blocking)}")
        print(f"MOBSF_INHERITED_UNRESOLVED={len(inherited)}")
        print(f"MOBSF_NEW_NOTES={len(new_notes)}")
        print(f"MOBSF_NEW_BLOCKING={len(new_blocking)}")
        for rule, level, path in inherited:
            print(f"MOBSF_INHERITED_REVIEW_REQUIRED {level} {rule} {path}")
        for rule, level, path in new_notes:
            print(f"MOBSF_NEW_REVIEW_REQUIRED {level} {rule} {path}")
        for rule, level, path in new_blocking:
            print(f"MOBSF_NEW_BLOCKING {level} {rule} {path}")
        if new_blocking:
            raise ScanError("New MobSF warning/error findings; investigate before merge")
        print("RESULT=PASS_MOBSF_BASELINE_AWARE_GATE")
        return 0
    except (OSError, UnicodeError, ScanError) as error:
        print(f"HOLD_MOBSF_RISK_GATE: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
