# Sable Health — MobSF security baseline and escalation policy

**Status: CI baseline only; seven inherited findings remain UNRESOLVED.**
This baseline is **not** a security certification, mitigation, waiver, or release approval.

## Evidence and scope

- Anchor: first populated `main` after owner merged foundation PR #1, commit `3095fc96df438426beac6b678c36bf57f2bf992e`
- Mainline source scan: [Security run 45318348399](https://github.com/aimindseye/sablehealth/actions/runs/45318348399), original `mobsfscan-sarif` artifact
- Compared with [PR #7 run 45320228578](https://github.com/aimindseye/sablehealth/actions/runs/45320228578)
- Scanner in both SARIF files: `mobsfscan` **0.4.5**; report: SARIF 2.1.0
- Result identity: same seven `ruleId + SARIF level + normalized location` fingerprints, one instance each.
- Owner-facing ongoing triage: [issue #6](https://github.com/aimindseye/sablehealth/issues/6)

| Rule | SARIF level | Evidence-based assessment to complete |
|---|---|---|
| `android_task_hijacking2` | **error**, `app/src/main/AndroidManifest.xml` | Review exported launcher activity / task-affinity and OS-version-specific attackability; scanner's description mentions target SDK below 29, but app declares targetSdk 36 |
| `android_prevent_screenshot` | note | Decide real-health-data recent-task preview and user-initiated screenshot privacy UX |
| `android_tapjacking` | note | Review overlay attacks on any sensitive in-app controls; Health Connect system consent belongs to Android |
| `android_root_detection` | note | Evaluate support for custom ROM/rooted devices against realistic threat model |
| `android_ssl_pinning` | note | Confirm no app-controlled TLS endpoints or INTERNET use before classifying not applicable |
| `android_certificate_transparency` | note | Same: applicability depends on actual network clients, not local Health Connect IPC |
| `android_safetynet` | note | Legacy SafetyNet attestation is not a default requirement for a local-only app; reassess if backend trust becomes relevant |

## CI behavior

`config/mobsf-known-findings.json` pins each exact inherited fingerprint to the observed mainline source commit and open issue. `tools/check_mobsf_findings.py` checks every original result, never removes anything from SARIF, and prints counts to logs and job summary.

- **PASS (CI only):** scanner actually succeeded; SARIF was valid and from the expected scanner/version; all `error`/`warning` results were among the pinned inherited fingerprints; newly found `note`/`none` results remain visible for human review.
- **FAIL:** scanner action fails/skips; SARIF missing, malformed, wrong version/tool, or missing result schema; baseline duplicates/broad allowances; any **new** `error` or `warning` result (including a second occurrence or changed location of an inherited error).
- **NOT ACCEPTED:** even when CI passes, the seven inherited results remain tracked as unresolved. A passing ratchet is not permission to publish a signed health-data app.
- The pinned MobSF 0.4.5 CLI is invoked with `--no-fail` because finding-only reports otherwise produce exit code 1; that switch does **not** filter or suppress SARIF. The independent Python gate still checks `steps.mobsfscan.outcome`, scanner identity/version, every original result and new warning/error regressions. Runtime scanner failures remain blocking.
- The full `mobsfscan-results.sarif` is uploaded to GitHub Code Scanning and as a workflow artifact. CodeQL/Gitleaks/Sonar remain independent, with no thresholds relaxed.
- Changes to the scanner version, baseline, or severity policy require an explicit scoped PR reviewing the new SARIF and tests; do not update the baseline just to clear a failure.

**Limitation:** MobSF 0.4.5 emits generic root-relative `.` locations for six best-practice checks and no partial fingerprints. The ratchet cannot detect a replacement finding with an identical `ruleId + level + path` if a previous instance disappears. This is why SARIF inspection and periodic review remain mandatory.

## Remaining release acceptance

Review every inherited rule in issue #6, mitigate any real risk with targeted Android tests (including Q25), document explicit owner decisions for inapplicable/accepted risks, then rerun the security gate and SonarCloud. New high-risk regressions always block. No security claims are inferred from an informational-only scan.

## Scope control

This policy deliverable changes only MobSF CI classification/evidence, its own checked baseline and tests. It does **not** add root detection, forced screenshot blocking, Play Integrity/SafetyNet, network permissions, or unsafe risk suppressions. It does not modify the Android application, signing flow or Gradle wrapper.
