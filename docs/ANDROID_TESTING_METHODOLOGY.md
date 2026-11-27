# Android testing and release qualification methodology

**Status:** normative policy proposal for owner review, 2026-11-27. This document establishes the next implementation requirements. It does not claim that emulator instrumentation, golden screenshots or automated release-device tests already exist or pass. Existing executable gates remain mandatory during migration.

## 1. Repository and delivery discipline

- Canonical checkout: `~/Documents/projects/sablehealth`. Work on a clean, explicitly selected feature branch; record parent, candidate and tracked/untracked state. Temporary worktrees require a reason; do not delete or overwrite existing work.
- One consolidated complete-file ZIP and matching draft PR per implementation deliverable; document apply/verify/rollback, changed files and exact candidate. Owner builds, signs, validates and alone merges. A documentation-only PR need not rebuild or install a previously qualified APK.
- Keep `tools/build-release.sh` as owner-only signing entrypoint. Preserve the existing commit-scoped archive `~/Downloads/sablehealth-release/<version>/<commit>/` and pinned certificate. Record APK SHA-256, package, version and source commit in the release receipt. Never select an APK solely by version or mtime.
- Resolve the Android SDK and paths from the repository or environment, not milestone worktree names or reused shell variables. Fail with actionable missing-path diagnostics. Never reset, clean, uninstall or clear physical app data implicitly.

## 2. Reference implementation

Reviewed [Vaachak Mobile main at 9611076d402e5220b7c9e1db96816465190ed814](https://github.com/vaachak-platform/vaachak-mobile/tree/9611076d402e5220b7c9e1db96816465190ed814), including its [Sonar workflow](https://github.com/vaachak-platform/vaachak-mobile/blob/9611076d402e5220b7c9e1db96816465190ed814/.github/workflows/sonar.yml), [instrumented runner](https://github.com/vaachak-platform/vaachak-mobile/blob/9611076d402e5220b7c9e1db96816465190ed814/tools/ci/run-sonar-instrumented-coverage.sh) and PR #58 remediation documents. [Hosted run 45851824138](https://github.com/vaachak-platform/vaachak-mobile/actions/runs/45851824138) passed its emulator, JaCoCo verification and Sonar quality-gate steps on 2026-11-26. The owner already uses a local Pixel 7 AVD for Vaachak. Adopt the patterns, not a runtime dependency or a copy of its authentication/PIN setup.

## 3. Initial emulator profile: Q25

| Property | Required initial setting |
| --- | --- |
| AVD name | Sable_Q25_API35 |
| Runtime | Android API 35 Google APIs |
| Screen | 720 x 720 pixels, 280 dpi (approx. 411 x 411 dp before insets) |
| Font scale | 1.0 baseline; larger font separately |
| Hardware keyboard | Enabled locally; verify key-event navigation in tests |
| Resources | Proposed 4 GiB RAM, 2 cores |
| macOS | Existing SDK and native host ABI (arm64-v8a on Apple Silicon; x86_64 on Intel) |
| CI | Linux x86_64, hardware-accelerated KVM, generic AVD with explicit size/density |

Version-control the Q25 profile and provisioning instructions. Provision/start/stop only an explicitly selected test emulator; verify serial, API, ABI, boot completion, package manager readiness, actual wm size/density, font scale, usable bounds and insets. Refuse ambiguous targets and physical devices for emulator-only operations. Do not mutate the owner's Pixel 7 AVD. CI invokes one checked-in script via a single command, not multiline runner continuations or unavailable Pixel hardware IDs. Pin third-party actions by commit, retain minimal permissions and record emulator/image version. Use disposable test data and bounded waits rather than unbounded retries. Preserve the repository's build SDK/toolchain; runtime API 35 is not a compile SDK downgrade. API 36, API 30, Titan 2 and P26 follow Q25 qualification; do not claim their acceptance from a square emulator. Firmware, hardware keys, sensor/Health Connect and Fitbit behavior require real hardware.

## 4. Test layers and implementation requirements

1. **Existing local and CI gates:** retain JVM golden vectors, Robolectric, contract tests, lint, Detekt, ktlint, dependency/security scans, unsigned assembly and Sonar. No blanket suppressions or gate reductions.
2. **Native instrumentation:** add AndroidJUnitRunner, test-only dependencies and a debug-only isolated Compose host; assert a real semantics root attaches within a bounded timeout. Keep the visible test host awake without bypassing keyguard. Render the actual production composables with injected deterministic fixtures, not duplicated mock UI. Add full-app navigation/lifecycle tests as well as isolated component tests. Use centralized stable test tags (Tid); preserve meaningful accessibility content descriptions; expose stable resource IDs for external automation where needed.
3. **Visual regression:** capture fixed synthetic fixture/date/time-zone/locale/theme/font-scale/profile states after assertion and render-idle. Use approved baselines and documented environment-specific tolerances; capture actual/reference/diff on failure. Do not update goldens automatically to pass. Prefer CI-generated baselines for CI and separately qualified local renderers where needed. Assert usable bounds, no clipping or overlap, visible essential controls, >=48dp targets and keyboard focus order. At accessibility scales or with an IME, allow intentional scrolling/pagination rather than clipping.
4. **Coverage:** add actual app instrumented JaCoCo reporting while retaining existing core JVM and app JVM coverage. Generate fresh reports for the exact candidate; validate XML, expected executed classes/source mappings, test counts and failures/skips; reject missing, stale, ambiguous or zero-execution inputs. Combine execution/report data without double counting and preserve line+condition denominator and per-module differential coverage. Send correct module report paths to Sonar; require real quality gate. Minimum 80% new Kotlin/Java line+condition coverage per changed module, Sonar new-code >=80%, Security A, Reliability A, duplication <=3%; retain any stricter existing repository Python or cumulative thresholds. Docs-only changes explicitly have no executable denominator. Never fabricate coverage.
5. **Signed release:** separate external ADB/UI automation targets the already installed release, not an instrumented debug replacement. Resolve the archive by exact installed APK SHA-256 and verify package, source receipt, version, user and device; fail closed on mismatches or unsupported split identity. Automate semantic navigation, clicks, text entry, ordinary scrolls and screenshot capture with bounded waits and screen assertions; no blind fixed coordinate sequences. Capture failure evidence safely, classify automation/navigation errors separately from functional failures. Manual fallback only for unsupported navigation/scrolling, user consent, sensitive permission/system operations or actual wearable actions. Restore only settings changed with explicit consent; never reset data, change security or grant/revoke permissions silently. Mark unsupported/unobserved checks NOT OBSERVED, not PASS.

## 5. Proposed command contract (not implemented by this policy)

From canonical checkout, provision/reuse the verified local AVD and execute the same instrumentation tasks as CI. Proposed `bash tools/test-local.sh --unit|--emulator|--visual|--coverage|--full` and `bash tools/test-device.sh --q25`. Options denote alternatives. Existing documented preflight and build commands remain the current entrypoints until the new scripts are implemented and qualified. CI requires Q25 instrumentation and visual regression for applicable executable PRs after migration; no green claim from documentation, previews, JVM tests alone or skipped emulator jobs. Expanded Android/device matrix follows separately.

## 6. Evidence, privacy and acceptance

- Per candidate: record baseline/head, clean source identity, test/profile/environment, expected and executed counts, outcomes, report paths/hashes, coverage and CI status. Distinguish automatic assertions, owner observations and unsupported steps.
- Synthetic data only in CI, repository goldens, logs and uploaded artifacts. No genuine health values, source record IDs, account names/tokens, device serials, private keys, or raw logcat. On physical hardware store screenshots and diagnostics locally with restricted permissions under `~/Downloads/sable-device-evidence-runs/`; do not upload automatically. Review/redact before sharing and preserve hashes for the reviewed package. Do not capture unrelated foreground apps or system screens containing sensitive data.
- CI and emulator evidence do not certify real device behavior. Exact-head green CI and local qualification precede owner-signed installation; required physical checks and accepted visual baselines precede release approval. Keep draft/hold until blockers are resolved; no public distribution or security acceptance is inferred.

## 7. Sable Health M2.1 application-specific acceptance

Preserve M2 PR #14 at d91291108524675c2f17c4e16c3e6755b843bfb7 as the separately qualified calculation candidate until owner disposition. Do not alter its historical evidence. Implement adaptive analytics in the next consolidated candidate based on the accepted M2 revision; do not regress existing four-slot daily Dashboard or read-only privacy boundary.

- Q25 default viewport: Dashboard, Sleep, Move, Vitals and single-dataset Trends fit without vertical scrolling. Compact two-column text metrics; chart sizing from usable content bounds; one selected dataset/period; Methods and Trends visible in text and graph; selected metric/date preserved. Details and long lists may scroll; accessible fallback at larger fonts. Existing Sable Blue, compact branded header and navigation, keyboard focus, icons and 48dp targets retained. Real/demo/no-records share the same layout; honest missing-vs-zero semantics and visibly synthetic demo data.
- Instrumentation matrix: light/dark, text/graph, populated/partial/no records; dashboard widget selection, mode persistence, method/trend routes, chart axes and selected point, date changes, Sources/Settings and wizard; keyboard/D-pad and back. Validate both valid HR zone methods, invalid/clear inputs, exact sleep ratio vs withheld efficiency, HRV RMSSD and resting HR data/missing states, no interpolation. Investigate failed Vitals checkpoints; missing screenshots are not proof of a defect or pass.
- Synthetic Health Connect seams only in CI. Physical Q25 verifies actual synced sleep, latest data and explicit permission revocation/regrant if approved. Titan 2/P26 remain separate device acceptance; Sable Health never writes health records. Existing design, release and security policies still apply. M2.1 changes must not imply new data sources, network, permissions or calculation semantics without separate approval.
