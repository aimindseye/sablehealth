"""Synthetic SARIF only; no real app data or credentials."""
import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from check_mobsf_findings import ScanError, check  # noqa: E402

BASELINE = json.loads((ROOT / "config/mobsf-known-findings.json").read_text(encoding="utf-8"))


def result(rule, level, path):
    uri = ("file:///home/runner/work/sablehealth/sablehealth/" + path) if path != "." else "."
    return {
        "ruleId": rule,
        "level": level,
        "message": {"text": "Synthetic finding"},
        "locations": [{"physicalLocation": {"artifactLocation": {"uri": uri}}}],
    }


def sarif(findings=None):
    if findings is None:
        findings = [result(x["ruleId"], x["level"], x["path"]) for x in BASELINE["inherited"]]
    return {
        "version": "2.1.0",
        "runs": [{"tool": {"driver": {"name": "mobsfscan", "version": "0.4.5"}}, "results": findings}],
    }


class MobSFRatchetTest(unittest.TestCase):
    def test_exact_mainline_inherited_findings_pass_without_disappearing(self):
        inherited, notes, blocking = check(sarif(), BASELINE)
        self.assertEqual((7, 0, 0), (len(inherited), len(notes), len(blocking)))

    def test_new_critical_like_error_is_blocking(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("dangerous_intent", "error", "app/new.kt"))
        self.assertEqual(1, len(check(doc, BASELINE)[2]))

    def test_new_warning_is_blocking(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("new_warning", "warning", "app/new.kt"))
        self.assertEqual(1, len(check(doc, BASELINE)[2]))

    def test_new_note_is_recorded_but_nonblocking(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("new_best_practice", "note", "."))
        inherited, notes, blocking = check(doc, BASELINE)
        self.assertEqual((7, 1, 0), (len(inherited), len(notes), len(blocking)))

    def test_same_rule_new_manifest_location_still_blocks(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("android_task_hijacking2", "error", "app/src/main/ExtraManifest.xml"))
        self.assertEqual(1, len(check(doc, BASELINE)[2]))

    def test_duplicate_inherited_error_still_blocks(self):
        doc = sarif()
        doc["runs"][0]["results"].append(copy.deepcopy(doc["runs"][0]["results"][0]))
        self.assertEqual(1, len(check(doc, BASELINE)[2]))

    def test_source_file_disappearing_is_not_a_forced_failure(self):
        inherited, notes, blocking = check(sarif([]), BASELINE)
        self.assertEqual(([], [], []), (inherited, notes, blocking))

    def test_invalid_report_or_scanner_upgrade_fails_closed(self):
        for change in ("version", "name", "noresults"):
            with self.subTest(change=change):
                doc = sarif()
                if change == "version":
                    doc["runs"][0]["tool"]["driver"]["version"] = "1.0.1"
                if change == "name":
                    doc["runs"][0]["tool"]["driver"]["name"] = "unexpected"
                if change == "noresults":
                    del doc["runs"][0]["results"]
                with self.assertRaises(ScanError):
                    check(doc, BASELINE)

    def test_baseline_duplicate_entry_refused(self):
        baseline = copy.deepcopy(BASELINE)
        baseline["inherited"].append(copy.deepcopy(baseline["inherited"][0]))
        with self.assertRaises(ScanError):
            check(sarif(), baseline)

    def test_baseline_larger_allowance_refused(self):
        baseline = copy.deepcopy(BASELINE)
        baseline["inherited"][0]["limit"] = 200
        with self.assertRaises(ScanError):
            check(sarif(), baseline)

    def test_broken_scan_step_refused_even_with_valid_report(self):
        with tempfile.TemporaryDirectory() as temp:
            report = Path(temp) / "report.sarif"
            baseline = Path(temp) / "baseline.json"
            report.write_text(json.dumps(sarif()), encoding="utf-8")
            baseline.write_text(json.dumps(BASELINE), encoding="utf-8")
            cmd = [sys.executable, str(ROOT / "tools/check_mobsf_findings.py"),
                   "--report", str(report), "--baseline", str(baseline)]
            result_failed = subprocess.run(cmd + ["--scan-outcome", "failure"], capture_output=True, text=True)
            self.assertEqual(2, result_failed.returncode)
            self.assertIn("HOLD_MOBSF_RISK_GATE", result_failed.stderr)
            result_pass = subprocess.run(cmd + ["--scan-outcome", "success"], capture_output=True, text=True)
            self.assertEqual(0, result_pass.returncode, result_pass.stderr)
            self.assertIn("MOBSF_INHERITED_UNRESOLVED=7", result_pass.stdout)
            self.assertIn("PASS_MOBSF_BASELINE_AWARE_GATE", result_pass.stdout)

    def test_report_missing_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            baseline = Path(temp) / "baseline.json"
            baseline.write_text(json.dumps(BASELINE), encoding="utf-8")
            proc = subprocess.run([sys.executable, str(ROOT / "tools/check_mobsf_findings.py"),
                                   "--report", str(Path(temp) / "none.sarif"),
                                   "--baseline", str(baseline), "--scan-outcome", "success"],
                                  capture_output=True, text=True)
            self.assertEqual(2, proc.returncode)


if __name__ == "__main__":
    unittest.main()
