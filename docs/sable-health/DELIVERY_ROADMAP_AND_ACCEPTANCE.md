# Sable Health — staged delivery plan and quality gates

**Status:** Roadmap recommended from approved product strategy; scope can be refined only with documented owner decisions.

## M0/R5.4 baseline — HOLD for merge

Current app supports keyboard-first, theme, synthetic Dashboard and diagnostic-only Sources. The user reported the overview pages improved but details remain text-heavy. **No more Sources/Settings UI changes until product/data scope is validated.** Existing draft PR #1 remains under owner merge control. Historical CI pass claims do not absolve MobSF, SonarCloud and physical acceptance.

## M1-A — read-only Health Connect record-coverage probe (**next**)

**Goal:** prove access rather than infer it from installed apps or declared permissions.

- Per-type *user-initiated* READ request for selected steps, HR, sleep, HRV, resting HR, exercise, respiration, SpO2, temperature as available/declared; no WRITE.
- SDK availability, denied/granted, history/permission boundaries and feature support checked distinctly.
- For authorized classes read **counts, earliest/latest timestamp, source package(s), record subtype and coverage**; no personal metric values in shared report, logs or PRs.
- Query Android aggregate totals appropriately without conflating a record count with a health zero; isolated error handling; repeated scan is idempotent.
- No silent network, cloud OAuth, BLE, telemetry, background task or account setup.
- Export only a redacted **coverage report** with explicit user action and preview. Persist no health measurements until M1-B storage threat model.

**Acceptance:** Q25 actual user-granted API test succeeds or gives precise failure; counts/permissions/origins correspond to HC; no fabricated metrics; teardown/revoke tested; typed and contract tests/CI pass; physical keyboard and touchscreen verified.

## M1-B — local import and provenance foundation

- Canonical schema in [Data Architecture](DATA_ARCHITECTURE_AND_RECONCILIATION.md).
- Local secure storage model and retention/deletion policy, migrations and stable IDs; timestamps, units, methods, source app vs hardware distinction.
- Read-only imported data displays on existing widgets, with data-age labels, no-record and partial-coverage states.
- Synthetic onboarding/permission UX proof; physical Q25 + Titan2 tests.

**Acceptance:** no duplicate on re-import, correct source attribution, crash/permission-revoke recovery, missing data shown truthfully.

## M2 — evidence-based charts + minimal transparent metrics

- Honest daily/hourly steps (Android Aggregate API), sleep wake-day/naps, stage timeline, provenance-aware HRV/RHR trends.
- Local sleep efficiency and HR-zone calculators with method details. Raw RR/IBI RMSSD only if actually available.
- Versioned synthetic golden tests, null/partial/gap behavior.

**Acceptance:** no fake hourly steps, no interpolated absent HRV, no SDNN/RMSSD mixing.

## M3 — source reconciliation across apps (same phone)

- Per-metric source priority and decision trace, consent-aware source list, workout candidate matching, sleep overlap handling.
- Health Connect priority respected for Activity/Sleep aggregates. Conflicting sources flagged, not deleted.
- Source detail UI compact and keyboard-first.

**Acceptance:** synthetic duplicate matrices pass; raw records remain available; decisions reproducible and inspectable.

## M4 — recovery/strain/sleep intelligence (licensed reuse)

- Pin and evaluate Pulse Mobile / GenieMax first. Separate clearly licensed code + references.
- Expose underlying HRV/RHR/sleep components; optional composite only with sufficient historical baseline.
- Dataset/method compatibility, algorithm versioning, tests, non-diagnostic language and known limitations.

**Acceptance:** license review, golden-vector parity, insufficient-data abstention, sensitivity and baseline tests.

## M5 — companion ecosystem expansion

- FitMesh-inspired compatibility matrix with supported/conditional/unverified/unsupported status, verification date and route.
- Samsung Health Data SDK feasibility, smart-ring official route, legacy export adapters.
- Use current Google Health API via OAuth only after HC limitations are demonstrated. No legacy Fitbit Web API.
- Multi-route duplicate checks for same observation via HC and API.

**Acceptance:** separate consent/permissions, no phantom support, route lineage/test coverage.

## M6 — opt-in homelab multi-phone history

- Two phones, each owns its local HC; authenticated encrypted transport to Linux homelab.
- Review Open Wearables (MIT) vs Open Health Sync (AGPL), PostgreSQL history, tombstones, export/delete, backup/restoration, audit.
- Explicit user opt-in to upload categories and retention, server-offline graceful operation, no Mac Docker dependency.

**Acceptance:** repeated imports idempotent, app works offline, deletion propagation/scope documented, tested backups and security review.

## M7 — user-controlled AI and reporting

- Reviewable local wellness summary; share/copy to user-selected AI, or opt-in personal AI Gateway.
- Structured source citations, algorithm versions, uncertainty and non-medical limits. No invented diagnoses.
- Export and data portability without forcing vendor cloud account.

**Acceptance:** no automatic health-data upload, access revocation respected, privacy/consent controls and report reproducibility.

## Cross-cutting release gate (every milestone)

- **Branch discipline:** new feature branch + draft PR; deliver exact-source/full-file ZIP to owner; owner applies/builds/tests locally and alone merges. No force merge.
- **Android quality:** compile, assemble debug/unsigned release when relevant, JUnit, lint, Detekt, ktlint and source contracts; do not add suppressions just to pass.
- **Security/privacy:** secrets scanning, dependency scan, MobSF findings triaged rather than disabled; encryption/privacy tests; no personal health data in logs, fixtures, screenshots or PR artifacts.
- **Device:** Q25 720×720/Android 17 + Titan 2 keyboard/trackpad/touch; font scales, rotation, focus, Back; optional other phones as available.
- **Evidence:** every API/vendor assertion labeled and backed with device result or dated source. No claim of "working" based only on a manifest.
- **Provenance:** each analytic result contains source(s), method, timestamp, coverage, baseline and algorithm version.
- **Compliance:** wellness-only copy, data access consent, verified dependencies and attribution; no private premium feature bypass.

## Immediate work queue

1. Keep R5.4 UI frozen and PR draft.
2. Deliver M1-A source ZIP/PR with explicit controlled record-coverage and zero measurement logging.
3. Owner runs on Q25 after consent; separate test later on Galaxy phone with Samsung Health.
4. Publish results into evidence register **without user identifiers or metric values**.
5. Decide first eligible metrics from actual data, pin upstream candidates and perform license/security/test-vector review.
6. Only then approve the setup wizard and compact Sources/Settings design in [UX Contract](UX_ONBOARDING_AND_ACCESSIBILITY.md).
