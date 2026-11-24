# Sable Health — product charter, decisions and scope

**Stage:** Direction approved in conversation; detailed M1 design subject to data discovery.

## Positioning

Sable Health is a **keyboard-first, privacy-first, local-first, source-aware wellness intelligence platform**, not merely a smaller Google Health UI. It should combine Fitbit, Samsung and eventually ring/watch data while preserving **where each number came from, why it was selected, what it actually means, and whether inputs are sufficient**.

Primary differentiation:
1. **Cross-ecosystem data ownership** — history and exports under user control, later spanning phones and vendors.
2. **Explainable multi-source reconciliation** — prevent double counting, preserve raw provenance, expose decisions and missing/uncertain intervals.
3. **Auditable, reusable analytics** — recovery, sleep, activity and training methods with inputs, units, versions, references, coverage and missing-data abstentions.
4. **Simple hardware-appropriate interaction** — guided initial setup, excellent 720×720 Q25/keyboard navigation, plus slab phone experience.
5. **Opt-in self-hosted intelligence** — never require a homelab or AI service to see basic data and scores.

This is **not** a claim to obtain proprietary or subscription-only vendor scores. Fitbit/Google Health Premium, Samsung Energy Score, Garmin Body Battery and Oura Readiness are not unlocked automatically by Health Connect. Sable may offer independently computed estimates when underlying permitted measurements suffice; never mislabel an estimate as a vendor's score or assert clinical validity.

## Recorded decisions

| ID | Status | Decision | Implication |
|---|---|---|---|
| D01 | DECIDED | Keep R5.4 Sources/Settings UI unchanged until capability and data contracts are settled | No additional visual refactor yet |
| D02 | DECIDED | Health Connect first on Android, per-type read-only access in M1 | No record ingestion has been proven by M0 |
| D03 | DECIDED | Google Health API is optional second path, not legacy Fitbit Web API | OAuth and provider policies are separate work |
| D04 | DECIDED | Support Samsung watches and smart rings, eventually reconciling across devices | Multi-phone transport and duplicate handling needed |
| D05 | DECIDED | Reuse suitable licensed, published analytics rather than invent all scoring | Evaluate Pulse/GenieMax/OpenStrap/NeuroKit; qualify licenses and tests |
| D06 | DECIDED | Transparent FitMesh Labs-like calculators, including local execution | Public formula != permission to copy proprietary source |
| D07 | DECIDED | Future first-run wizard and compact Sources/Settings | Progressive disclosure, not walls of text |
| D08 | DECIDED | Optional Linux homelab for longer history, processing and phone sync | No mandatory project Docker on Mac; protect health data |
| D09 | DECIDED | User-controlled ZIP/local test/feature branch/draft PR; owner merges only | Never auto-merge unfinished work |
| D10 | DECIDED | No commercial distribution is planned | Still review noncommercial/AGPL obligations before code inclusion |
| D11 | RECOMMENDED | Read-only initially; any Health Connect write-back separate, opt-in, anti-loop | M1 requests zero WRITE permissions |
| D12 | RECOMMENDED | Local-only, reviewable AI summary then optional BYO AI/AI Gateway | No automatic transmission of raw records to models |

## Known current application state (do not overstate)

- M0/R5.4: Dashboard/Sleep/Move/Vitals/Trends, widget picker and compact Sources/Settings, local demo/no-record state, keyboard profiles. The M0 Health Connect API path **checks availability and existing permission count only; does not read metrics**.
- Q25: Zinwa Q25 / SableOS 2.0, Android 17 (SDK 37), display 720×720 at density 280. The user reports Fitbit Air paired and synced with Google Health; not a verified Health Connect export. Google Health, Google Fit, Fitbit/companion packages and Health Connect components were detected in prior read-only preflight. The public repo must **not** contain raw diagnostic serials or health data.
- FitMesh benchmark: user has FitMesh 3.11.0 build 192 on a **different** Samsung Android 17 phone, with lifetime Founder entitlement recognized by the user. The ADB report confirms declaration of 16 health record READ categories + background read permission and 5 WRITE categories; effective grants and actual records were **not** demonstrated.
- Previous FitMesh APK inspection (five splits) identified Flutter, native SQLite, vendor callbacks, potential source-fusion and recovery modules. These are **static indicators**, not permission to reuse code, proof of algorithms, or verification of account features.
- Prior M0 draft PR #1 stays separate; successful build CI on some revisions does not close MobSF, wrapper, SonarCloud or physical testing concerns automatically.

## Product non-goals / constraints

- No direct unauthorized Fitbit BLE pairing, vendor-private data extraction, credential scraping, paid-feature bypass, fabricated data, or confident medical conclusions.
- No premium score cloning based on names alone; do not average Apple SDNN with Android RMSSD.
- No automatic uploading of health measurements to CI, issues, analytics, logs or AI; demos use synthetic fixtures.
- No unqualified vendor claim: `installed` != `authorized` != `records present` != `fresh` != `analytics-ready`.
- No silent writes or deletion in Health Connect; raw records remain immutable in the Sable canonical store.
- No user-facing health score unless all necessary inputs and method/baseline thresholds are present.

## Questions requiring real evidence

1. Which record classes on Q25 are actually populated by Google Health after Fitbit Air sync? Is HRV RMSSD present? Are sleep stages, RHR, respiration, SpO2 or temperature populated? What is their source/timestamp coverage?
2. Which records can Sable Health read after user consent? How do Android 17 permissions and history limits behave on Q25?
3. Which canonical metric semantics and thresholds are portable between Fitbit and Samsung? Which remain device-specific?
4. What data does Samsung Health actually write to Health Connect on the separate Galaxy phone? Is Samsung Health Data SDK authorization possible for an independent app?
5. Which open analytics modules are verifiably correct and compatible with data available, and with Sable's intended license/distribution?
6. What is the user's desired privacy boundary for eventual home-server sync, backup and optional AI summaries?
7. When to add cross-phone transfer: after trustworthy on-device records, not before.

**Rule for future discussions:** treat this as the controlling product charter. Update this document and the decisions/roadmap when a new milestone is approved; do not rely on chat history for engineering requirements.
