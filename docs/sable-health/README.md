# Sable Health — canonical product and architecture record

**Baseline date:** 2026-11-23  
**Status:** Research + approved direction; **not** an implemented M1 feature.  
**Repository:** [aimindseye/sablehealth](https://github.com/aimindseye/sablehealth).  
**Code baseline:** M0/R5.4 remains a draft pending owner device/security acceptance.  
**Safety:** wellness information only, not diagnostic, therapeutic, or a medical device.

## Purpose

This directory preserves the decisions and research accumulated across the Sable Health project conversation: Q25 UI testing; Fitbit Air/Google Health setup; Samsung/FitMesh investigations; FitMesh 3.11.0 APK static inspection; Google Health/Health Connect access; published analytics and licensing; multi-wearable reconciliation; local-first privacy; and optional homelab/AI features.

**Read these in order:**

1. [PRODUCT_CHARTER_AND_DECISIONS.md](PRODUCT_CHARTER_AND_DECISIONS.md) — product value proposition, confirmed choices, constraints, unanswered questions.
2. [FITMESH_BLOG_DEEP_DIVE_2026-11-23.md](FITMESH_BLOG_DEEP_DIVE_2026-11-23.md) — article-by-article lessons and shortcomings.
3. [DATA_ARCHITECTURE_AND_RECONCILIATION.md](DATA_ARCHITECTURE_AND_RECONCILIATION.md) — data flows, provenance, merge semantics, and no-loop rules.
4. [ANALYTICS_AND_LICENSE_EVALUATION.md](ANALYTICS_AND_LICENSE_EVALUATION.md) — candidate codebases, licenses, inputs, test plan.
5. [UX_ONBOARDING_AND_ACCESSIBILITY.md](UX_ONBOARDING_AND_ACCESSIBILITY.md) — wizard and Settings/Sources redesign requirements (no changes yet).
   - [M1B_SETUP_SOURCES_SETTINGS_DESIGN.md](M1B_SETUP_SOURCES_SETTINGS_DESIGN.md) — TenSeven-informed six-stage first-run wizard, permission presets, Sources/Settings 2.0, phased implementation acceptance.
6. [DELIVERY_ROADMAP_AND_ACCEPTANCE.md](DELIVERY_ROADMAP_AND_ACCEPTANCE.md) — staged milestones, exit criteria and quality gates.
7. [SOURCES_AND_EVIDENCE_REGISTER.md](SOURCES_AND_EVIDENCE_REGISTER.md) — public source URLs, direct observations, confidence/discrepancy log.

## Evidence language

- **DECIDED** — user explicitly approved the direction or process.
- **RECOMMENDED** — design proposal requiring owner review.
- **VERIFIED — device/report** — supported by the user's supplied local diagnostic.
- **DOCUMENTED — vendor** — reported by a vendor or official API documentation; does not prove operation on user devices.
- **DOCUMENTED — FitMesh** — FitMesh's description of its own product; not an independent audit.
- **APK INDICATOR** — evidence of declared capabilities or compiled identifiers, **not** demonstration of running behavior.
- **NOT VERIFIED** — absent direct data or proof; do not mark as working.
- **HOLD** — blocking quality, consent, license, privacy, or correctness work.

Do not automatically promote a DOCUMENTED route to VERIFIED when a new device is connected. Health permissions, records, timestamp precision, origin, and coverage all require in-app confirmation.

## Preservation / workflow

This is a documentation-only branch and draft PR to `main`; it does not alter application code, merge R5.4, create release artifacts, or perform health-data access. The owner alone reviews and merges PRs. Keep this record versioned, update the source registry as interfaces change, and append decision changes rather than rewriting history.

**Last research verification:** 2026-11-23. Links and licenses may change; pin any upstream code version before integration.

## Research update — 2026-11-26

[Fitbit Air interoperability and Bridge research supplement](FITBIT_AIR_AND_BRIDGE_RESEARCH_2026-11-26.md) records newer device evidence, official API/manual Bridge validation, sleep display concerns, experimental direct BLE/Golden Gate research, additional references, security/quality requirements and proposed roadmap. It supplements rather than retroactively changes the Nov 23 baseline. Implementation/release status is not inferred.
