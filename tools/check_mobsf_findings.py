#!/usr/bin/env python3
"""Fail closed on broken MobSF scans/new security warnings, retain inherited findings.

Known findings are *not* marked fixed or risk-accepted. This is a CI noise
ratchet anchored to a reviewed mainline SARIF and issue-based risk review.
"""

import argparse
from collections import Counter
import json
from pathlib import Path
import sys


class ScanError(ValueError):
    """A missing, malformed or newly risky MobSF scan cannot qualify."""


def read_json(path: Path) -> dict:
    if not path.is_file():
        raise ScanError(f"Missing required file: {path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise ScanError(f"Invalid JSON in {path}: {error}") from error
    if not isinstance(value, dict):
        raise ScanError(f"Expected JSON object: {path}")
    return value


def location(finding: dict) -> str:
    locations = finding.get("locations")
    if not isinstance(locations, list) or not locations:
        raise ScanError("SARIF finding has no location")
    uri = (
        locations[0]
        .get("physicalLocation", {})
        .get("artifactLocation", {})
        .get("uri")
    )
    if not isinstance(uri, str) or not uri:
        raise ScanError("SARIF finding has no file URI")
    if uri == ".":
        return uri
    # Convert the stable GitHub Actions checkout root to a project-relative path.
    # Unknown absolute paths remain unmatched and therefore cannot inherit a waiver.
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
    expected_version = tool.get("version")
    if tool.get("name") != "mobsfscan" or not isinstance(expected_version, str):
        raise ScanError("Baseline must pin the MobSF scanner identity and version")
    allowed = baseline.get("inherited")
    if not isinstance(allowed, list) or not allowed:
        raise ScanError("Baseline lacks a documented inherited finding list")
    limits: Counter = Counter()
    for item in allowed:
        if not isinstance(item, dict) or set(item) != {"ruleId", "level", "path", "limit"}:
            raise ScanError("Malformed inherited finding entry")
        key = (item["ruleId"], item["level"], item["path"])
        if not all(isinstance(v, str) and v for v in key):
            raise ScanError("Missing inherited finding fingerprint")
        if key[1] not in {"error", "warning", "note", "none"}:
            raise ScanError("Invalid inherited severity")
        if type(item["limit"]) is not int or item["limit"] != 1 or key in limits:
            raise ScanError("Baseline requires one nonduplicated instance per exact fingerprint")
        limits[key] = 1
    if report.get("version") != "2.1.0":
        raise ScanError("Unexpected SARIF version")
    runs = report.get("runs")
    if not isinstance(runs, list) or len(runs) != 1 or not isinstance(runs[0], dict):
        raise ScanError("Expected exactly one MobSF SARIF run")
    run = runs[0]
    driver = run.get("tool", {}).get("driver", {})
    if driver.get("name") != "mobsfscan" or driver.get("version") != expected_version:
        raise ScanError("MobSF identity/version differs from documented baseline; review scanner upgrade")
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
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--scan-outcome", required=True)
    parser.add_argument("--summary", type=Path)
    args = parser.parse_args()
    try:
        if args.scan_outcome != "success":
            raise ScanError(f"MobSF scan step did not succeed: {args.scan_outcome}")
        baseline = read_json(args.baseline)
        report = read_json(args.report)
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
        if args.summary:
            with args.summary.open("a", encoding="utf-8") as summary:
                summary.write(
                    "### MobSF risk ratchet\n"
                    f"- Inherited **unresolved** findings: **{len(inherited)}**\n"
                    f"- New informational findings (review required): **{len(new_notes)}**\n"
                    f"- New warning/error findings (blocking): **{len(new_blocking)}**\n"
                    f"- Tracked review: {baseline['review_issue']}\n"
                    "- Full original SARIF is uploaded, including inherited findings.\n"
                )
        if new_blocking:
            raise ScanError("New MobSF warning/error findings; investigate before merge")
        print("RESULT=PASS_MOBSF_BASELINE_AWARE_GATE")
        return 0
    except (OSError, ScanError) as error:
        print(f"HOLD_MOBSF_RISK_GATE: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
