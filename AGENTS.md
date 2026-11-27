# Sable Health — engineering policy for contributors and coding agents

**Read before every source or CI edit.** These rules extend the original M0 policy with lessons from TenSeven Launcher, while keeping Sable Health's device, design, privacy and local-execution requirements. Executable tests and owner instructions take precedence over assumptions; do not change them to hide failures.

## 1. Repo/PR discipline
- Record `git status -sb`, `git branch --show-current`, `git rev-parse HEAD`, tracked diff and untracked files. Never reset, clean, restore, stash, switch, rebase, overwrite or discard existing owner work without permission.
- One scoped feature branch and draft PR per deliverable, generally stacked on the accepted prior milestone. Owner applies exact-file ZIP, builds/tests on Mac/Q25/Titan 2 and **alone merges**. No silent commit, push, release, install or permission grant.
- Do not reapply previously accepted R5.4 or M1-A changes. Preserve approved functionality and preference keys. State the exact parent commit, file scope, rollback and validation matrix.

## 2. Design and Android engineering
- Follow `docs/DESIGN_CONTRACT.md` and current approved screens. **Sable Blue** dark `#2E9BD6`, light `#1A6FA0`; black/`#F5F7F9` backgrounds, no default teal/green accent. Color cannot be the only indicator of readiness. Visible keyboard focus.
- Q25 720×720 (approx. 411dp) is first-class; preserve compact header, anchored actions and >=48dp touch/focus targets; primary summaries and single-dataset graphs must fit without scrolling at default settings after M2.1, with detailed lists and accessibility fallback allowed. Test trackpad, DPAD, keyboard and Back, as well as Titan 2 I/J/K/L and large fonts.
- Kotlin 4-space formatting, UTF-8/LF final newline, official ktlint, Detekt, explicit imports, named constants and short focused functions. `@Composable` naming remains Compose-style; non-Compose naming stays enforced. Avoid deprecations when a supported API exists.
- Split pure policy logic from Android APIs/Compose. Every new feature requires tests; permission and navigation changes require physical/instrumented verification. No blanket suppressions, fake test coverage, baseline rewrites or repository-wide autoformat on a dirty worktree.

## 3. Health and source safety
- Sable Health is a local-first wellness app, not a diagnostic tool. Never fabricate health measurements, infer access from a manifest, or present absent records as zero. Keep Demo visibly synthetic and never mix it with real data.
- Treat platform available, read permissions granted, actual records present, freshness, writer package, physical wearable and analytics readiness as **different** states. `com.fitbit.FitbitMobile` is a writer identity, not a verified physical device ID.
- Health Connect READ access is user-granted. No WRITE/DELETE, background/history/Internet/Bluetooth or external AI/cloud data export without a new explicit scope, consent and security review.
- No genuine health values, record IDs, device identifiers, signed APK keys, OAuth tokens, passwords, full unredacted logcat or sensitive screenshots in GitHub, tests, PRs or CI logs. Synthetic fixtures only.
- Setup must never silently prompt Android permissions. Reopened setup should reuse the same selection/permission state as Sources and Settings; grant/revoke status must be queried from Android.

## 4. **Mandatory local preflight before every push**
```bash
python3 tools/quality_preflight.py --quick
python3 tools/quality_preflight.py
```
- Use the repository's verified Gradle **8.13 wrapper**, not system Gradle 9.x. If absent, bootstrap via `tools/bootstrap-gradle.sh` and verify its checksum.
- The shared preflight performs M0–R5.4 and M1-A contracts, release-verifier offline tests, core JUnit, genuine JaCoCo core report, debug/unsigned-release assemblies, debug/release Android Lint, Detekt and ktlint. CI runs the matching tasks with pinned 8.13.
- Check ktlint and Detekt **after the final source edit**, not only after an earlier draft. Fix only failing relevant code; rerun gates. Do not push repeated routine formatting failures to discover them in CI. Reproduce Sonar/security failures independently.
- Report exact exit codes, changed file/rule/line, tests performed and report paths; cached/up-to-date is fine only when it actually refers to the final candidate.
- CI names and PR branch triggers matter: **not run** cannot be reported as **passed**. Physical Q25 and Titan 2 acceptance is independent of JVM checks.

## 5. GitHub CI and SonarCloud
- `.github/workflows/android.yml`: all PR bases including stacked branches; runs `python3 tools/quality_preflight.py --ci`, checks release candidate is **unsigned**.
- `.github/workflows/security.yml`: CodeQL, MobSF and Gitleaks on all PRs. Existing unresolved MobSF findings must be triaged, **not** disabled to get green.
- `.github/workflows/sonar.yml`: the owner confirmed SonarCloud project setup and `SONAR_TOKEN`. Analysis now runs on each same-repo PR and push to main without a `SONAR_ENABLED` toggle; a missing token or failing quality gate is a **FAILURE**, not a skip. Never print the token.
- Sonar uses authentic core and Android JaCoCo, Python Cobertura and Android Lint XML. Release APKs must retain R8 minification and obfuscation. Seven inherited MobSF findings remain unresolved under issue #6; no public distribution until release security closure. The JVM report does not replace Android UI coverage; no blanket production exclusions to inflate scores. Sonar's quality gate remains independent.
- Never relax thresholds, patch workflows to bypass gates or use broad suppressions. If legacy findings block merging, record them and request targeted review.

## 6. Owner-only release signing
- The package `io.github.aimindseye.sablehealth` is registered with Android Developer Console. Package registration alone is **not** proof of the release certificate identity.
- The owner confirmed `io.github.aimindseye.sablehealth` is registered with the same public signing certificate as TenSeven, Sable Games and Sable Watch. The shared public SHA-256 is pinned in tracked `config/release-certificate.sha256`; owner-only private key never belongs in Git or CI.
- Signing keystore stays on the owner's Mac/encrypted backup. The canonical `bash tools/build-release.sh` prompts for path and alias and reads hidden passwords directly from the terminal; `apksigner` receives passwords only through stdin. Never put secrets into repo files, GitHub secrets, process arguments, artifacts or CI.
- All signed release builds require: clean committed source, public pinned registered cert, local full quality preflight, an **unsigned** Gradle APK, `apksigner` signing using terminal-prompted secrets, post-signature verification, `aapt2` package/version/minSdk verification and SHA-256 receipt.
- `bash tools/build-release.sh` is the **one release entrypoint**; it creates signed APK/receipts under Downloads only after local qualification. It **does not publish**. CI produces only unsigned candidates. Owner manually attaches verified APK to GitHub Releases after signoff. Do not commit APKs.
- Debug-signed and release-signed APKs generally cannot update each other in-place; do not uninstall or wipe health-app data without the owner's explicit instruction.

## 7. Completion receipt
State baseline SHA, touched tracked/untracked paths, local checks and versions, CI/Sonar/MobSF/Gitleaks results, certificate and version identity where relevant, physical-device outcomes, unresolved risks and explicit HOLD or READY FOR OWNER REVIEW. Never represent successful compilation as release certification.

## 8. Preserved M0 governance
- Pure Kotlin/Compose architecture; no server or dependency on other Sable apps. Preserve keyboard modifiers, text entry and nonrepeating confirm actions. Track provenance, timezone, partial coverage and duplicates; no clinical diagnoses or proprietary score recreation. Include Design Compliance and device matrix in each PR.

## 9. Release security ratchet
- Run full-history Gitleaks locally before every push (including after committing), using redaction; CI independently scans history. Only exact reviewed historical fingerprints may be ignored. See docs/RELEASE_ENGINEERING.md.
- Production APK verifier accepts fixed repository-relative artifact locations only; no caller-supplied certificate trust anchor or SDK command arguments. Reject symlinked paths and test negative boundaries. Keep R8 enabled.
- Keep Python cognitive complexity at most 15 and measured local coverage at least 85%; do not relax Sonar gates or suppress security findings.

## 10. Android testing methodology

Read [docs/ANDROID_TESTING_METHODOLOGY.md](docs/ANDROID_TESTING_METHODOLOGY.md) before each deliverable. Adopt Q25-first local/CI instrumentation, visual regression and external signed-release qualification as the next implementation requirements. Preserve existing executable gates, exact M2 baseline, owner-only release and independent device acceptance. Do not claim unimplemented gates passed.
