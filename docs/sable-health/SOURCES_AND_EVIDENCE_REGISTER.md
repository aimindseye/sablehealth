# Sable Health — sources, evidence, unresolved claims and audit trail

**As-of 2026-11-23.** Links are references to published material; do not reproduce full articles in this repository. Source contents and licenses can change. Primary official developer documentation takes precedence over secondary blog claims when they conflict.

## Direct user/device evidence (not public attachments)

| Evidence | Observed scope | Supported finding | Does NOT establish |
|---|---|---|---|
| Q25 read-only preflight, 2026-11-23 | Android package metadata, installed apps and manifest requests | Zinwa Q25 Android 17 SDK 37, 720×720, density 280; Google Health/Fitbit/Google Fit and Health Connect components present; Sable M0 present | Per-record data exists; read authorization; active Fitbit Health Connect export; premium entitlements |
| FitMesh phone preflight, 2026-11-23 | Different Samsung Android 17 phone, app metadata | FitMesh v3.11.0 build 192; 16 health-record READ declarations + one background health read, 5 WRITE declarations; Samsung Health installed | Actual record grants despite package-manager flags; record availability; cloud operation |
| User confirmation | Personal report, not API test | Fitbit Air is paired/synced to Google Health on Q25; Founder lifetime FitMesh entitlement remains recognized on other phone | What raw records and proprietary scores Sable can access |
| FitMesh 3.11.0 APK static examination (previous discussion) | Five split APKs, compiled symbols/manifest indicators | Flutter/native libs; provider callbacks; modules named for reconciliation/recovery/provenance and settings | Algorithms, server-side behavior, account access, working OAuth integration or permission to copy code |
| Q25 R5.3/R5.4 screenshots + feedback | Physical user visual test | Main Dashboard accepted; R5.4 overview better; secondary Sources/Settings too verbose for compact screen | M1 data access, general accessibility correctness on all font scales |

**Privacy:** never commit device serials, raw health values, tokens, complete ADB reports containing identifiers, or proprietary APKs to a public repository. Source report filenames may be retained in private handoffs.

## FitMesh primary public site (publisher-authored, not independent audit)

- [Blog index](https://www.fitmesh.fit/en/blog) — dated article inventory; reviewed selected high-relevance articles below.
- [Integrations and compatibility selector](https://www.fitmesh.fit/en/integrations) — vendor-specific source routes and verification dates. Dynamic selector status must not be assumed to cover Fitbit Air hardware specifically.
- [Labs overview](https://www.fitmesh.fit/en/labs) — claim of browser-local calculation; formulas and limitations, not source-code reuse license.
- [Labs RMSSD](https://www.fitmesh.fit/en/labs/hrv-rmssd-calculator) — requires true RR/IBI intervals.
- [Labs sleep efficiency](https://www.fitmesh.fit/en/labs/sleep-efficiency-calculator) — depends on credible time asleep and time in bed.
- [Labs HR zones](https://www.fitmesh.fit/en/labs/heart-rate-zones-calculator) — %maxHR vs Karvonen.
- [Bring your own AI](https://www.fitmesh.fit/en/ai) — user-reviewed summary and manual handoff.
- [Self-hosting status](https://www.fitmesh.fit/en/self-host) — explicitly says custom Supabase backend is **not** a complete user-supported self-host solution; managed data writes/export/deletion remain.
- [Privacy policy](https://www.fitmesh.fit/en/privacy) — recheck privacy claims if considering any managed FitMesh service as benchmark.

### Deep-reviewed FitMesh blog articles by design topic

| Topic | Articles (publisher links) | Engineering takeaway |
|---|---|---|
| Recent updates | [3.11.0 (Nov 20)](https://www.fitmesh.fit/en/blog/fitmesh-sync-3-11-0), [3.10.0](https://www.fitmesh.fit/en/blog/past-days-clearer-data), [build 190](https://www.fitmesh.fit/en/blog/more-reliable-steps-update) | naps, honest chart gaps, provenance, day-consistent history |
| Watch interoperability | [Apple vs Galaxy](https://www.fitmesh.fit/en/blog/apple-watch-galaxy-watch-fitmesh-data), [Samsung HC data](https://www.fitmesh.fit/en/blog/galaxy-watch-ultra2-watch9-health-connect), [Samsung overview](https://www.fitmesh.fit/en/blog/fitmesh-vs-samsung-health) | source route != wearable capability; SDNN/RMSSD methods |
| Aggregation | [duplicates guide](https://www.fitmesh.fit/en/blog/multiple-smartwatches-duplicate-data), [hourly vs total steps](https://www.fitmesh.fit/en/blog/steps-total-vs-hourly-chart), [global dashboard](https://www.fitmesh.fit/en/blog/multi-device-health-dashboard) | HC aggregate totals vs raw records; no fabricated hours |
| Real-world multi-wearable | [ring/watch handoff](https://www.fitmesh.fit/en/blog/smart-ring-and-watch-real-scenarios), [switching wearables](https://www.fitmesh.fit/en/blog/switch-smartwatch-without-losing-data) | coverage, source continuity, retention across vendors |
| Sleep & wellness | [Galaxy sleep](https://www.fitmesh.fit/en/blog/galaxy-watch-sleep-tracking-health-connect), [sleep score](https://www.fitmesh.fit/en/blog/sleep-score-vs-circadian-score), [recovery components](https://www.fitmesh.fit/en/blog/recovery-metrics-hrv-sleep-heart-rate) | real sleep stages, naps, regularity and component-based recovery |
| Exercise | [Zone 2 variation](https://www.fitmesh.fit/en/blog/why-zone-2-differs-watches-apps) | distinguish HR-zone method and thresholds |
| Data routes & failures | [sync architectures](https://www.fitmesh.fit/en/blog/sync-smartwatch-dashboard-2026), [HC guide](https://www.fitmesh.fit/en/blog/how-does-health-connect-work), [HC troubleshooting](https://www.fitmesh.fit/en/blog/health-connect-not-syncing), [Google Health sync](https://www.fitmesh.fit/en/blog/google-health-not-syncing) | device → companion → store → consent → app → metric |
| Rings and provider limitations | [Galaxy Ring](https://www.fitmesh.fit/en/blog/galaxy-ring-android-health-connect), [Oura Ring](https://www.fitmesh.fit/en/blog/oura-ring-health-connect-android), [Garmin Body Battery](https://www.fitmesh.fit/en/blog/garmin-body-battery-health-connect), [Pixel Watch](https://www.fitmesh.fit/en/blog/pixel-watch-data-personal-dashboard) | per-type export needs verification; proprietary scores remain locked |
| Cloud + privacy + alternatives | [Android-to-Apple bridge](https://www.fitmesh.fit/en/blog/smart-ring-data-apple-health), [FitMesh vs alternatives](https://www.fitmesh.fit/en/blog/fitmesh-sync-vs-alternatives), [smartwatch migration](https://www.fitmesh.fit/en/blog/switch-smartwatch-without-losing-data) | optional managed bridge is cloud, not local; self-hosting not complete |
| Platform migration | [Google Fit API deprecation](https://www.fitmesh.fit/en/blog/google-fit-shutting-down-alternative) | verify actual Fitbit/Google API endpoint dates with Google |

### Not exhaustively reviewed

The FitMesh index also includes device-buying guides, pricing posts, GDPR commentary, Wear OS/summer articles, Colmi model-specific tutorials, family features and other editorial posts. Their existence is cataloged at the index; **no claim of line-by-line exhaustive audit of every article** is made here. For new models/services, add targeted review and verification dates.

## Higher-authority documentation

### Google / Android
- [Health Connect raw data reads](https://developer.android.com/health-and-fitness/health-connect/read-data): permissions and raw reads; cumulative steps should use `aggregate()` to avoid double counting.
- [Health Connect aggregation](https://developer.android.com/health-and-fitness/health-connect/aggregate-data): data-origin filters; activity/sleep priority-aware dedup.
- [Health Connect synchronization](https://developer.android.com/health-and-fitness/health-connect/sync-data): IDs, versioning, metadata and secure sync patterns.
- [Health Connect sleep experiences](https://developer.android.com/health-and-fitness/health-connect/experiences/sleep): sleep sessions/stages and overlaps.
- [Health Connect record metadata](https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/metadata/Metadata): writer origin, IDs, device info and modification timestamps.
- [Google Health API home](https://developers.google.com/health/) and [About / retirement dates](https://developers.google.com/health/about): current API, Google OAuth; Fitbit Web API/oauth endpoints retired November 2, 2026.
- [Google Health API setup](https://developers.google.com/health/setup) and [types](https://developers.google.com/health/data-types): OAuth configuration, scopes and actual supported values, never assume full Fitbit data exports.
- [Samsung Health Data SDK](https://developer.samsung.com/health/data-sdk/overview.html) and [Samsung HC FAQ](https://developer.samsung.com/health/blog/en/health-connect-faq-to-access-samsung-health-data.html): separate registration/permissions and version-dependent sharing (verify before implementing).
- [Oura Health Connect](https://support.ouraring.com/hc/en-us/articles/10786105824531-Health-Connect-by-Android-Integration): preferred source over secondary contradictory blog language; check current support.

### Upstream reusable code and licenses
- [Pulse Mobile (PolyForm Noncommercial)](https://github.com/adityaongit/pulse-mobile)
- [Pulse server (PolyForm Noncommercial)](https://github.com/adityaongit/pulse)
- [GenieMax Core (MIT)](https://github.com/satayutata/geniemax-core)
- [OpenStrap Analytics (AGPL)](https://github.com/OpenStrap/analytics)
- [NeuroKit2 (MIT)](https://github.com/neuropsychology/NeuroKit)
- [OpenVitals (AGPL-3.0+)](https://github.com/Hale-MTU/OpenVitals)
- [Open Wearables (MIT)](https://github.com/the-momentum/open-wearables)
- [Open Health Sync (AGPL-3.0; pre-1.0)](https://github.com/soma-intelligence/open-health-sync)
- [Gadgetbridge (AGPL)](https://codeberg.org/Freeyourgadget/Gadgetbridge)

No upstream has been pinned, tested or integrated by this documentation PR. License labels are a research snapshot and must be checked at selected commit.

## Contradictions / follow-up verification register

| ID | Finding | Status | Required next action |
|---|---|---|---|
| E01 | Oura FitMesh blog TL;DR implies SpO2 export, but detailed section does not confirm | CONFLICT / OFFICIAL CHECK | Oura official Aug 19 help lists export of activity, sleep, HR, HRV; **not SpO2**; live records still needed |
| E02 | FitMesh compiled `self-host` label vs published self-hosting limitations | RESOLVED AS LIMITATION | Not a full self-host; keep Sable homelab independent |
| E03 | FitMesh main integration counts and per-provider marketing may change | OPEN | Do not hard-code vendor claims; maintain dated per-metric matrix |
| E04 | Fitbit Air Google Health export might omit certain measured types | OPEN | Q25 permissioned record-coverage probe |
| E05 | Samsung SDK route may unlock specific extra categories, requires official SDK registration | OPEN | Investigate SDK access and verify on Galaxy phone |
| E06 | Daily/hourly chart and step merge method differs between Android aggregate priority and FitMesh app-level selection | DESIGN DECISION | Explicit policy with test cases; do not conflate |
| E07 | Recovered premium scores from standard HC | NOT SUPPORTED | Use independent derived metrics, never claim bypass |
| E08 | Health Connect older history and Android 17 limits | OPEN | Test actual user-consented history on physical Q25 |
| E09 | Remote provider APIs may duplicate same HC observations | OPEN | Design route lineage and idempotent import |
| E10 | Pulse/GenieMax algorithm applicability with Q25 data | OPEN | Check real inputs and license, then golden-vector tests |
| E11 | Existing R5.4 code merge/security quality | HOLD | Owner acceptance and unresolved findings triage |
| E12 | FitMesh source parser or API metadata proves real feature functionality | NOT PROVEN | Require runtime, avoid speculative implementation claim |\n| E13 | Oura blog implies subscription-free data, official Oura HC help requires active membership for Gen3+ | CONFLICT / OFFICIAL CHECK | Treat Gen3+ membership requirement as documented; verify on relevant device before promising access |

## Maintenance convention

For each new conclusion add: source URL, publisher date, date reviewed, status (DOCUMENTED / VERIFIED / UNVERIFIED / CONTRADICTED / DECIDED), implicated metric/data route, and what test would falsify it. Change architecture decisions only after owner approval and record the delta in [Product Charter](PRODUCT_CHARTER_AND_DECISIONS.md).

**Scope:** product/engineering knowledge retention only; no health values, device IDs, or proprietary APK contents stored in this public docs directory.

## Evidence supplement — 2026-11-26

See [dated Fitbit Air and Bridge research supplement](FITBIT_AIR_AND_BRIDGE_RESEARCH_2026-11-26.md) for the later owner-side observations (sanitized), pinned upstream source review, security and firmware limitations, previously missing product/analytics references and remaining unknowns. Earlier entries are historical snapshots, not assertions about present CI or data availability.
