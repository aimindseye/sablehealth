"""Exercise the actual differential gate, including real Git diff fixtures."""
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from xml.etree import ElementTree as ET

from tools import check_new_code_coverage as gate


class DifferentialGateTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        gate.git(self.root, "init", "-q")
        gate.git(self.root, "config", "user.name", "Fixture")
        gate.git(self.root, "config", "user.email", "fixture@example.invalid")
        self.core = Path("core/src/main/kotlin/example/core/Example.kt")
        self.app = Path("app/src/main/java/example/app/Example.kt")
        self.write(self.core, "package example.core\nfun sample() = 1\n")
        self.write(self.app, "package example.app\nfun sample() = 1\n")
        self.base = self.commit("base")
        self.write(self.core, "package example.core\nfun sample() = 2\n")
        self.write(self.app, "package example.app\nfun sample() = 2\n")
        self.write(Path("app/src/main/res/values/strings.xml"), "<resources/>")
        self.head = self.commit("change")

    def write(self, path, content):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")

    def commit(self, name):
        gate.git(self.root, "add", ".")
        gate.git(self.root, "commit", "-qm", name)
        return gate.git(self.root, "rev-parse", "HEAD").strip()

    def report(self, module, line):
        self.write(
            Path(gate.REPORTS[module]),
            f'<report><package name="example/{module}"><sourcefile name="Example.kt">'
            + line + "</sourcefile></package></report>",
        )

    def main(self, base=None):
        with patch.object(gate, "ROOT", self.root):
            return gate.main(["--base", base or self.base, "--head", self.head])

    def test_actual_git_diff_and_coverage_boundary(self):
        self.assertEqual({self.core: {2}, self.app: {2}}, dict(
            gate.changed_sources(self.root, self.base, self.head),
        ))
        self.report("core", '<line nr="2" mi="0" ci="1" mb="0" cb="4"/>')
        self.report("app", '<line nr="2" mi="0" ci="1" mb="1" cb="3"/>')
        self.assertEqual({"core": [5, 5], "app": [4, 5]}, gate.evaluate(
            self.root, self.base, self.head,
        ))
        self.assertEqual(0, self.main())
        self.report("app", '<line nr="2" mi="1" ci="0" mb="1" cb="3"/>')
        self.assertEqual(1, self.main())

    def test_missing_report_mapping_and_invalid_counter_fail_closed(self):
        self.assertEqual(2, self.main())
        self.report("app", '<line nr="2" mi="0" ci="1" mb="0" cb="0"/>')
        self.report("core", '<line nr="2" mi="-1" ci="1" mb="0" cb="0"/>')
        self.assertEqual(2, self.main())
        self.report("core", '<line nr="2" mi="0" ci="1" mb="0" cb="0"/>')
        self.write(Path(gate.REPORTS["app"]), "<report/>")
        self.assertEqual(2, self.main())
        self.report("app", "")
        self.assertEqual(2, self.main())
        self.write(Path(gate.REPORTS["app"]), "<not-report/>")
        self.assertEqual(2, self.main())
        self.write(Path(gate.REPORTS["app"]), "<broken")
        self.assertEqual(2, self.main())

    def test_duplicate_lines_and_source_entries_are_rejected(self):
        line = '<line nr="2" mi="0" ci="1" mb="0" cb="0"/>'
        self.report("core", line + line)
        report = ET.parse(self.root / gate.REPORTS["core"]).getroot()
        with self.assertRaises(ValueError):
            gate.source_counts(report, self.core, {2})
        report.find("package").append(report.find("package/sourcefile"))
        with self.assertRaises(ValueError):
            gate.source_counts(report, self.core, {2})

    def test_unchanged_lines_do_not_count_and_empty_diff_is_explicit(self):
        self.report("core", '<line nr="1" mi="0" ci="1" mb="0" cb="0"/>')
        self.report("app", '<line nr="2" mi="1" ci="0" mb="0" cb="0"/>')
        self.assertEqual({"core": [0, 0], "app": [0, 1]}, gate.evaluate(
            self.root, self.base, self.head,
        ))
        self.assertEqual(0, self.main(self.head))
        self.assertEqual(2, self.main("nonexistent"))

    def test_unexpected_production_layout_is_refused(self):
        self.write(Path("app/src/main/Other.kt"), "fun another() = 3\n")
        self.head = self.commit("invalid-layout")
        self.assertEqual(2, self.main())

    def test_zero_context_hunks_cover_additions_and_not_deletions(self):
        self.assertEqual({3, 8, 9}, gate.added_lines(
            "@@ -3 +3 @@\n-old\n+new\n@@ -7,0 +8,2 @@\n+a\n+b\n@@ -12 +13,0 @@\n-c",
        ))


if __name__ == "__main__":
    unittest.main()
