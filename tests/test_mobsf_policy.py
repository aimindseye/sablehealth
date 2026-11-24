"""Synthetic SARIF fixtures: no patient records or device identifiers."""
import copy
from contextlib import redirect_stdout, redirect_stderr
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import check_mobsf_findings as policy  # noqa: E402

BASELINE = json.loads((ROOT / "config/mobsf-known-findings.json").read_text(encoding="utf-8"))


def result(rule, level, path):
    uri = ("file:///home/runner/work/sablehealth/sablehealth/" + path) if path != "." else "."
    return {"ruleId": rule, "level": level, "message": {"text": "Synthetic"},
            "locations": [{"physicalLocation": {"artifactLocation": {"uri": uri}}}]}


def sarif(findings=None):
    if findings is None:
        findings = [result(x["ruleId"], x["level"], x["path"]) for x in BASELINE["inherited"]]
    return {"version": "2.1.0", "runs": [{"tool": {"driver": {"name": "mobsfscan", "version": "0.4.5"}}, "results": findings}]}


class MobSFRatchetTest(unittest.TestCase):
    def invoke(self, doc=None, baseline=None, outcome="success", report_present=True,
               baseline_present=True, malformed_report=False, malformed_baseline=False):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            bp, rp = root / "baseline.json", root / "mobsfscan-results.sarif"
            if baseline_present:
                bp.write_text("{broken" if malformed_baseline else json.dumps(BASELINE if baseline is None else baseline), encoding="utf-8")
            if report_present:
                rp.write_text("{broken" if malformed_report else json.dumps(sarif() if doc is None else doc), encoding="utf-8")
            stdout, stderr = io.StringIO(), io.StringIO()
            argv = ["check_mobsf_findings.py", "--scan-outcome", outcome]
            with patch.object(policy, "BASELINE_FILE", bp), patch.object(policy, "REPORT_FILE", rp), \
                 patch.object(sys, "argv", argv), redirect_stdout(stdout), redirect_stderr(stderr):
                status = policy.main()
            return status, stdout.getvalue(), stderr.getvalue()

    def test_exact_mainline_inherited_findings_pass_without_disappearing(self):
        inherited, notes, blocking = policy.check(sarif(), BASELINE)
        self.assertEqual((7, 0, 0), (len(inherited), len(notes), len(blocking)))

    def test_new_critical_like_error_is_blocking(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("dangerous_intent", "error", "app/new.kt"))
        self.assertEqual(1, len(policy.check(doc, BASELINE)[2]))
        status, out, error = self.invoke(doc)
        self.assertEqual(2, status)
        self.assertIn("MOBSF_NEW_BLOCKING=1", out)
        self.assertIn("HOLD_MOBSF_RISK_GATE", error)

    def test_new_warning_is_blocking(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("new_warning", "warning", "app/new.kt"))
        self.assertEqual(1, len(policy.check(doc, BASELINE)[2]))

    def test_new_note_is_recorded_but_nonblocking(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("new_best_practice", "note", "."))
        inherited, notes, blocking = policy.check(doc, BASELINE)
        self.assertEqual((7, 1, 0), (len(inherited), len(notes), len(blocking)))
        status, out, _ = self.invoke(doc)
        self.assertEqual(0, status)
        self.assertIn("MOBSF_NEW_REVIEW_REQUIRED note", out)

    def test_new_none_level_is_recorded_but_nonblocking(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("info_none", "none", "."))
        self.assertEqual(1, len(policy.check(doc, BASELINE)[1]))

    def test_same_rule_new_manifest_location_still_blocks(self):
        doc = sarif()
        doc["runs"][0]["results"].append(result("android_task_hijacking2", "error", "app/src/main/ExtraManifest.xml"))
        self.assertEqual(1, len(policy.check(doc, BASELINE)[2]))

    def test_duplicate_inherited_error_still_blocks(self):
        doc = sarif()
        doc["runs"][0]["results"].append(copy.deepcopy(doc["runs"][0]["results"][0]))
        self.assertEqual(1, len(policy.check(doc, BASELINE)[2]))

    def test_empty_report_may_pass_but_does_not_claim_remediation(self):
        self.assertEqual(([], [], []), policy.check(sarif([]), BASELINE))

    def test_scanner_upgrade_fails_closed(self):
        for change in ("version", "name", "noresults", "sarif", "extra_run", "invalid_run"):
            with self.subTest(change=change):
                doc = sarif()
                if change == "version":
                    doc["runs"][0]["tool"]["driver"]["version"] = "1.0.1"
                if change == "name":
                    doc["runs"][0]["tool"]["driver"]["name"] = "unexpected"
                if change == "noresults":
                    del doc["runs"][0]["results"]
                if change == "sarif":
                    doc["version"] = "1.9"
                if change == "extra_run":
                    doc["runs"].append(doc["runs"][0])
                if change == "invalid_run":
                    doc["runs"] = "invalid"
                with self.assertRaises(policy.ScanError):
                    policy.check(doc, BASELINE)

    def test_baseline_duplicate_entry_refused(self):
        base = copy.deepcopy(BASELINE)
        base["inherited"].append(copy.deepcopy(base["inherited"][0]))
        with self.assertRaises(policy.ScanError):
            policy.check(sarif(), base)

    def test_baseline_larger_allowance_refused(self):
        for limit in (0, 2, True):
            with self.subTest(limit=limit):
                base = copy.deepcopy(BASELINE)
                base["inherited"][0]["limit"] = limit
                with self.assertRaises(policy.ScanError):
                    policy.check(sarif(), base)

    def test_baseline_invalid_fields_refused(self):
        for field in ("schema", "source_commit", "review_issue", "inherited", "tool"):
            with self.subTest(field=field):
                base = copy.deepcopy(BASELINE)
                base[field] = None
                with self.assertRaises(policy.ScanError):
                    policy.check(sarif(), base)

    def test_baseline_rule_severity_and_path_are_checked(self):
        for field, value in (("level", "critical"), ("ruleId", ""), ("path", ""), ("limit", 2)):
            with self.subTest(field=field):
                base = copy.deepcopy(BASELINE)
                base["inherited"][0][field] = value
                with self.assertRaises(policy.ScanError):
                    policy.check(sarif(), base)

    def test_baseline_extra_key_refused(self):
        base = copy.deepcopy(BASELINE)
        base["inherited"][0]["untrusted"] = True
        with self.assertRaises(policy.ScanError):
            policy.check(sarif(), base)

    def test_malformed_sarif_finding_refused(self):
        for field in ("ruleId", "level", "locations"):
            with self.subTest(field=field):
                doc = sarif()
                if field == "ruleId":
                    del doc["runs"][0]["results"][0][field]
                elif field == "level":
                    doc["runs"][0]["results"][0][field] = "fatal"
                else:
                    doc["runs"][0]["results"][0][field] = []
                with self.assertRaises(policy.ScanError):
                    policy.check(doc, BASELINE)

    def test_unexpected_location_never_inherits_exception(self):
        doc = sarif()
        doc["runs"][0]["results"][0]["locations"][0]["physicalLocation"]["artifactLocation"]["uri"] = "file:///tmp/untrusted/AndroidManifest.xml"
        self.assertEqual(1, len(policy.check(doc, BASELINE)[2]))

    def test_relative_file_path_normalization(self):
        self.assertEqual("app/a.xml", policy.location(result("x", "warning", "app/a.xml")))
        self.assertEqual("app/a.xml", policy.location({"locations": [{"physicalLocation": {"artifactLocation": {"uri": "./app/a.xml"}}}]}))

    def test_parse_json_rejects_bad_payloads(self):
        for raw in ("invalid", "[]", "null", "1"):
            with self.subTest(raw=raw):
                with self.assertRaises(policy.ScanError):
                    policy.parse_json(raw, "report")
        self.assertEqual({}, policy.parse_json("{}", "baseline"))

    def test_broken_scan_step_refused_even_with_valid_report(self):
        status, _, err = self.invoke(outcome="failure")
        self.assertEqual(2, status)
        self.assertIn("did not succeed", err)

    def test_missing_and_malformed_reports_refused(self):
        for kwargs in ({"report_present": False}, {"baseline_present": False},
                       {"malformed_report": True}, {"malformed_baseline": True}):
            with self.subTest(kwargs=kwargs):
                status, _, err = self.invoke(**kwargs)
                self.assertEqual(2, status)
                self.assertIn("HOLD_MOBSF_RISK_GATE", err)

    def test_valid_cli_prints_all_unresolved_findings(self):
        status, output, err = self.invoke()
        self.assertEqual(0, status, err)
        self.assertIn("MOBSF_INHERITED_UNRESOLVED=7", output)
        self.assertIn("MOBSF_NEW_BLOCKING=0", output)
        self.assertIn("RESULT=PASS_MOBSF_BASELINE_AWARE_GATE", output)

    def test_cli_refuses_file_path_arguments(self):
        with patch.object(sys, "argv", ["check_mobsf_findings.py", "--scan-outcome", "success", "--report", "/etc/passwd"]), redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as caught:
                policy.main()
        self.assertEqual(2, caught.exception.code)

    def test_workflow_retains_sarif_and_enforces_scan(self):
        workflow = (ROOT / ".github/workflows/security.yml").read_text(encoding="utf-8")
        self.assertIn("--type android --no-fail", workflow)
        self.assertIn("--scan-outcome", workflow)
        self.assertIn("steps.mobsfscan.outcome", workflow)
        self.assertIn("sarif_file: mobsfscan-results.sarif", workflow)
        self.assertNotIn("--baseline config/mobsf-known-findings.json", workflow)


if __name__ == "__main__":
    unittest.main()
