# PR #7 SonarCloud quality-gate repair — 2026-11-23

**Parent:** draft PR #7 at `a0837bb68b4392c2f93b34ab959660e258f5d1ac`; base `main` contains merged PR #1 and PR #2. **Scope:** CI, security policy checker and its synthetic unit tests only. This is not an Android health-data behavior change.

## Failure evidence

- [PR #7 SonarCloud run 45322892428](https://github.com/aimindseye/sablehealth/actions/runs/45322892428): Android, CodeQL and MobSF jobs separately PASS; SonarCloud FAIL.
- SonarCloud's Sonar Way PR gate showed 0.0% coverage on **212 new lines**, required 80%, plus **security rating C**, required A. A screen of the issue identified the Python `pythonsecurity:S8707` taint finding, originating from unrestricted CLI-supplied input filenames in `tools/check_mobsf_findings.py`.
- The previous Sonar scanner imported a **valid JaCoCo XML**, but no Python Cobertura XML. This did not measure the newly added Python security policy implementation.

## Remediation and invariants

1. **No caller-defined files.** The checker resolves `config/mobsf-known-findings.json` and `mobsfscan-results.sarif` relative to its own source file's repository root. Its only CLI argument is `--scan-outcome`. Arbitrary `--report`, `--baseline` and `--summary` arguments are rejected. It still fails closed for missing or malformed files, mismatched scanner version and new SARIF warning/error results. CI writes a summary outside the checker after the Python process completes.
2. **Real Python coverage.** Coverage.py **7.13.3** installed from binary wheels only (`--only-binary :all:`), branch coverage, repo-relative source paths, a Cobertura `python-coverage.xml` report, measured local threshold of 85%, and a check for the exact `tools/check_mobsf_findings.py` source filename. SonarCloud imports this via `sonar.python.coverage.reportPaths` alongside existing core JaCoCo.
3. **No scope reduction, no waiver.** SonarCloud analysis sources/exclusions and the Sonar Way Quality Gate remain as before. CodeQL still builds uncached Kotlin. The original MobSF SARIF remains uploaded, and the seven inherited findings remain unresolved in [issue #6](https://github.com/aimindseye/sablehealth/issues/6).
4. **Stacked PR qualification.** SonarCloud now runs on PRs whose base is a feature branch, as Security already does, so a corrective PR can qualify before its owner-controlled integration into PR #7.

## Local validation performed on the exact proposed Python sources

- **26/26** synthetic unit tests PASS with coverage.py 7.13.3.
- Measured production-checker coverage: **99% combined checker coverage (139/140 statements; 71/72 branches)**.
- Replayed the original PR #7 MobSF SARIF artifact: **7 inherited, 0 new blocking; PASS**.
- Tested CLI arbitrary path rejection; malformed baseline/report, scan failure, version drift, duplicated errors and new warning/error all remain fail-closed.
- Python source hashes and tests in GitHub PR must match the local tested blobs.

## Owner qualification / merge HOLD

- [ ] PR's **new SonarCloud scan** reports successful Python coverage import and gate A / >=80%. Do not infer success from presence of XML alone.
- [ ] No new SonarCloud security issues or regressions. If one persists, collect its exact rule and dataflow; do not disable the rule.
- [ ] Security (CodeQL + MobSF), Android, and document quality gates pass on the feature branch after integration.
- [ ] Owner applies supplied guarded handoff, builds/tests locally as needed, reviews PR and decides merges; no assistant auto-merge.

**Note:** This fixes CI **measurement** and removes a real arbitrary-path capability. It does not alter the app's privacy or health readings, and does not constitute a security certification.

## Second CI review (PR #9)

A read-only SonarCloud gate API diagnostic identified **79.6%** new-code coverage (80% required) and new Security Rating **C**. The security issue was `githubactions:S8541`: a pinned pip install still permitted source distributions and setup scripts. The workflow now uses `--only-binary :all:`, preserving the pinned dependency and eliminating that execution route. Five `python:S5778` test assertions now construct their fixture *outside* the exception context; the `python:S3776` 32-point checker function has been split into separate small validators, preserving each original rejection. Three additional synthetic negative tests raise local coverage from 92% to **99% combined**. None of these changes relaxes a gate. A fresh SonarCloud result is required before merge.
