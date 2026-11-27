#!/usr/bin/env python3
"""Fail-closed differential JaCoCo gate for changed Kotlin/Java production code."""
import argparse
from pathlib import Path
import re
import subprocess
import sys
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REPORTS = {
    "core": "core/build/reports/jacoco/test/jacocoTestReport.xml",
    "app": "app/build/reports/coverage/test/debug/report.xml",
}
HUNK = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")
MINIMUM_PERCENT = 80


def git(root, *args):
    return subprocess.run(
        ["git", *args], cwd=root, check=True, capture_output=True, text=True,
    ).stdout


def added_lines(patch):
    lines = set()
    for line in patch.splitlines():
        match = HUNK.match(line)
        if match:
            start = int(match[1])
            count = int(match[2]) if match[2] is not None else 1
            lines.update(range(start, start + count))
    return lines


def changed_sources(root, base, head):
    before = git(root, "rev-parse", "--verify", "--end-of-options", base + "^{commit}").strip()
    after = git(root, "rev-parse", "--verify", "--end-of-options", head + "^{commit}").strip()
    git(root, "merge-base", "--is-ancestor", before, after)
    paths = git(
        root, "diff", "--no-renames", "--name-only", "--diff-filter=AM", "-z",
        before, after, "--", "app/src/main", "core/src/main",
    ).split("\0")
    for name in paths:
        path = Path(name)
        if path.suffix not in {".kt", ".java"}:
            continue
        if len(path.parts) < 6 or path.parts[3] not in {"java", "kotlin"}:
            raise ValueError("Unexpected production source layout: " + name)
        patch = git(
            root, "diff", "--no-ext-diff", "--no-renames", "--unified=0",
            before, after, "--", name,
        )
        lines = added_lines(patch)
        if lines:
            yield path, lines


def source_counts(report, path, added):
    package = "/".join(path.parts[4:-1])
    matches = [
        source
        for entry in report.findall("package") if entry.get("name") == package
        for source in entry.findall("sourcefile") if source.get("name") == path.name
    ]
    if len(matches) != 1:
        raise ValueError("Missing or duplicate JaCoCo source mapping: " + str(path))
    covered = total = 0
    seen = set()
    for line in matches[0].findall("line"):
        nr = int(line.attrib["nr"])
        if nr in seen or nr < 1:
            raise ValueError("Invalid or duplicate JaCoCo line: " + str(path))
        seen.add(nr)
        mi, ci, mb, cb = (int(line.attrib[key]) for key in ("mi", "ci", "mb", "cb"))
        if min(mi, ci, mb, cb) < 0:
            raise ValueError("Negative JaCoCo counter: " + str(path))
        if nr in added:
            covered += int(ci > 0) + cb
            total += int(mi + ci > 0) + mb + cb
    if not seen:
        raise ValueError("Empty JaCoCo source mapping: " + str(path))
    return covered, total


def evaluate(root, base, head):
    counts = {module: [0, 0] for module in REPORTS}
    reports = {}
    for path, lines in changed_sources(root, base, head):
        module = path.parts[0]
        if module not in reports:
            report = ET.parse(root / REPORTS[module]).getroot()
            if report.tag != "report":
                raise ValueError("Invalid JaCoCo report for " + module)
            reports[module] = report
        covered, total = source_counts(reports[module], path, lines)
        counts[module][0] += covered
        counts[module][1] += total
        print(f"{path}: {covered}/{total}")
    return counts


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", default="HEAD")
    args = parser.parse_args(argv)
    try:
        counts = evaluate(ROOT, args.base, args.head)
    except (OSError, ValueError, KeyError, ET.ParseError, subprocess.CalledProcessError) as error:
        print(f"HOLD_NEW_CODE_COVERAGE_EVIDENCE: {error}", file=sys.stderr)
        return 2
    passed = True
    for module, (covered, total) in counts.items():
        if total == 0:
            print(f"{module.upper()}_NEW_CODE_COVERAGE=NO_EXECUTABLE_CHANGES")
        else:
            print(f"{module.upper()}_NEW_CODE_COVERAGE={100 * covered / total:.2f}% ({covered}/{total})")
            passed = passed and covered * 100 >= total * MINIMUM_PERCENT
    print("RESULT=PASS_NEW_CODE_COVERAGE" if passed else "RESULT=HOLD_NEW_CODE_COVERAGE")
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
