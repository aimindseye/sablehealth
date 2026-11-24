# Sable Health — reusable analytics and licensing decision record

**Stage:** Candidate evaluation, not approval to copy production code. We seek reliable mathematics with transparent inputs, not a proprietary-score clone.

## Two separate tasks

1. **Measurement calculations**: independently reproducible formulas such as RMSSD from beat-to-beat RR intervals, sleep efficiency (asleep / in-bed), resting heart rate trends, daily/weekly step aggregates, percent-max-heart-rate and Karvonen zones. Public formulas can be implemented and tested independently, with original sources cited.
2. **Composite/behavioral analytics**: recovery/readiness, strain, sleep regularity, training load, relative temperature and physiological trends. These involve normalization choices, baseline windows, weighting and data-quality thresholds. Port from license-compatible projects or implement original documented methods, and test substantially before suggesting any health interpretation.

A score from Fitbit Premium, WHOOP, Garmin, Oura or Samsung is NOT a standard API field simply because the component observations exist. Sable-derived numbers must be distinctly labeled and never presented as medically validated.

## Candidate comparison (status 2026-11-23)

| Candidate | Published license | Environment | Potential contribution | Disposition |
|---|---|---|---|---|
| [Pulse Mobile](https://github.com/adityaongit/pulse-mobile) | PolyForm Noncommercial 1.0.0 | Mobile, Health Connect/Fitbit-oriented | Recovery/sleep/strain and local on-device ideas | **Evaluate first for Fitbit inputs**, but segregate NC components and license obligations |
| [Pulse (self-hosted)](https://github.com/adityaongit/pulse) | PolyForm Noncommercial 1.0.0 | Web + self-hosted service | Fitbit-derived scores and server calculation reference | Compare optional personal backend; not a drop-in Apache component |
| [GenieMax Core](https://github.com/satayutata/geniemax-core) | MIT | Pure Swift; WHOOP-focused sensors | Recovery, HRV, sleep and golden vectors | **Evaluate tests/formula portability**; not drop-in Kotlin |
| [OpenStrap Analytics](https://github.com/OpenStrap/analytics) | AGPL-3.0 | Dart, WHOOP 1 Hz raw sensor stream | Signal-processing/strain/HRV | Advanced reference only when raw sample rates exist; AGPL obligations |
| [NeuroKit2](https://github.com/neuropsychology/NeuroKit) | MIT | Python | Mature physiological signal-processing methods | Candidate for optional homelab if actual RR/PPG/ECG data exists |
| [OpenVitals](https://github.com/Hale-MTU/OpenVitals) | AGPL-3.0-or-later | Native Android / Health Connect | Read-only local charts/permission patterns and local wellness work | Reference or separate study; license review for reuse |
| [Open Wearables](https://github.com/the-momentum/open-wearables) | MIT | Self-hosted Python/PostgreSQL | Normalization, multi-provider API and history | Leading backend proof-of-concept candidate, not automatically selected |
| [Open Health Sync](https://github.com/soma-intelligence/open-health-sync) | AGPL-3.0 | Go/Postgres, pre-1.0 | Self-hosted adapters, REST/exports/MCP | Compare maturity/security/data schema; license and pre-1.0 risk |
| [FitMesh Labs](https://www.fitmesh.fit/en/labs) | Proprietary site; public calculators | Local browser | Published formula explanations, input validation and privacy model | **Reimplement published mathematical methods**, do not copy their code |
| [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) | AGPL-3.0 | Android/BLE | Privacy-first direct wearable integration model | Architecture comparator; not a planned Fitbit Air protocol shortcut |

**License rule:** an upstream README's license may change by commit; before using code pin a SHA/tag, read the **actual license text and per-file exceptions**, record origin/attribution and whether distributing the resulting application triggers obligations. Noncommercial intent does NOT turn PolyForm components into Apache-2.0 code, and linking AGPL components can impose obligations. Seek proper review where unclear.

## Algorithm gate — required metadata

Every analytic module must specify:
- algorithm ID + version + implementation origin/license + references;
- input record types (including HRV method) and minimum sample/time coverage;
- units, timezone handling, baseline horizon, per-device context and outlier policy;
- missing-input and stale-data behavior (not zero by default);
- deterministic golden vectors, boundary tests, invalid input tests and differential tests versus independently implemented references;
- time complexity/storage and whether on-device computation is practical;
- textual explanation of meaning and **non-medical limitations**.

### Candidate v1 local metrics (no composite required)

| Metric | Inputs | First presentation | Block if |
|---|---|---|---|
| Steps | Health Connect aggregate or vetted merged intervals | Count + source + day coverage | No authorized records or unresolved conflict |
| Sleep duration | Sleep sessions/stages | Overnight + naps with wake intervals excluded | Missing/untrusted interval semantics |
| Sleep efficiency | Asleep time and time in bed | Ratio with duration detail | Time-in-bed not actually known |
| Sleep regularity | Multiple days of bedtime/wake timing | Trend, not health-risk claim | Insufficient comparable nights |
| RHR trend | Timestamped resting HR, consistent method | Baseline delta and trend | Single isolated/mismatched samples |
| HRV trend | RMSSD (or separate SDNN) summaries | Same-method personal trend | Missing method / mixed methods / insufficient baseline |
| RMSSD calculation | True sequential RR/IBI intervals | Exact formula + validity criteria | Only summary HRV available |
| Heart rate zones | Max HR and optional resting HR | Explicit %HRmax/Karvonen alternative | Unbounded/invalid inputs or unidentified method |
| Recovery estimate | Independently qualified HRV/RHR/sleep/etc. | Optional score with **component trace** | Missing inputs/baseline; do not infer false precision |
| Workout strain | Workout duration and appropriate HR/context | Versioned model, comparison to own baseline | Sparse HR or mismatched workout data |
| Physiological signal processing | Beat-to-beat/PPG/ECG appropriate samples | Optional advanced service | Health Connect only provides daily summary |

## The Pulse evaluation plan

1. Fix a repository revision/tag for Pulse Mobile and Pulse server; collect license + notices, transitive dependencies, actual test fixtures and algorithm IDs.
2. Map each method to Health Connect record types, checking whether Fitbit Air through Q25 produces the required inputs. Distinguish pulse `Google Health API` path from local HC path.
3. Run synthetic fixtures on reference project and proposed Kotlin core. Compare exact outputs and tolerances. Include outlier, missing/partial, short-history and method mismatch cases.
4. If code reuse is allowed and useful, decide between **separate NC service/module** and original Kotlin implementation of public mathematical concepts; never silently paste licensed source into Apache files.
5. If provenance/quality or license cannot be demonstrated, mark that calculation **NOT READY** and display underlying raw trends instead.

## The GenieMax evaluation plan

- Identify pure mathematical routines independent of WHOOP-specific sensor plumbing.
- Pin its MIT revision and golden vectors; port with attribution where appropriate.
- Add cross-language parity tests and privacy/no-network checks.
- Avoid claiming WHOOP's raw 1 Hz inputs are available from Fitbit Air or Health Connect without actual evidence.

## Lab features inspired by FitMesh

- A **Calculate & Verify** details page for RMSSD, sleep efficiency and heart-rate zones, using local-only computation and clear formulas. Explain why a calculation cannot run.
- A provenance panel with source/time range, input count, method, precision and formula reference.
- Optional copyable analysis report to ChatGPT/Claude/Gemini only after deliberate review and manual share; do not call remote AI automatically.
- Charts must handle partial coverage, unsupported methods and no-record states gracefully. No invented reference ranges or clinical flags.

## Backend selection is independent

Do not choose Open Wearables/Open Health Sync merely because it advertises many connectors. Evaluate live supported provider APIs, modern Google Health API migration, secrets handling, per-source permission revocation, history backfill, tombstones, idempotency, encryption, schema migrations, and homelab operational overhead. The first deliverable should work without backend and without cloud accounts.

## Proof required to move to implementation

**Gate A:** real HC record coverage and origin verified (counts/types/dates only, no measurements in diagnostic report).  
**Gate B:** at least two qualified transparent metrics with synthetic test vectors and a defined no-data UX.  
**Gate C:** documented source reconciliation precedes composite scores.  
**Gate D:** dependency/attribution/license/security review signed off before any upstream source is incorporated.  
**Gate E:** physical Q25 + Titan2 acceptance, Android lint/Detekt/ktlint, regression checks and privacy gate pass.

**No scoring has been committed by this document.** A published technique is a candidate, not a validated medical recommendation.
