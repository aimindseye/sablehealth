# FitMesh blog deep dive — technical lessons for Sable Health

**Reviewed:** 2026-11-23. This is an original technical synthesis, not a copy of FitMesh articles or app code. The [FitMesh blog index](https://www.fitmesh.fit/en/blog) contains a larger catalog; the articles below were selected for deep review based on our Sable Health roadmap. Article descriptions are FITMESH DOCUMENTED statements unless additionally cited to primary documentation. Dates are article publication dates, not proof of real-device operation.

## Priority lessons and actionable requirements

### A. Release 3.11.0 (November 20)
[FitMesh 3.11.0: sleep, charts and measurements](https://www.fitmesh.fit/en/blog/fitmesh-sync-3-11-0)

- Daily sleep totals include overnight sleep and afternoon naps without counting intervening awake breaks.
- Session-level visualization aligns sleep stages and heart rate on one timeline; session summaries show low/high/average heart rate when available.
- HRV charts connect only truly adjacent verified hourly timestamps; missing readings remain gaps. Hourly SpO2 min/max bars require actual source samples. Workout details show source; steps have hourly vs cumulative display and comparable-day count. Calorie estimates are labeled.
- Temperature/SpO2/respiratory features require a compatible source with relevant timestamped records. The article notes sign-in fixes and iOS availability fixes; *does not guarantee* every Android provider writes them.
- **Sable requirement:** a `coverage mask` for every chart, an honest sleep-day assignment (including naps), cross-metric session overlay, source labels and explicit estimated-data wording.
- **Acceptance:** synthetic wake-after-sleep + nap must not double-count awake time; sparse HRV displays gaps; no chart extrapolates absent samples.

### B. Apple Watch vs Galaxy Watch (October 21)
[Data interchange comparison](https://www.fitmesh.fit/en/blog/apple-watch-galaxy-watch-fitmesh-data)

- Wearable sensor capability, data in the manufacturer's app, data in a system health store, and data actually readable by a third-party app are **four different states**.
- Apple Watch HRV stored in Apple Health is **SDNN**; Health Connect HRV type is **RMSSD**. No fixed conversion factor; Samsung Health-to-Health Connect HRV is not listed in the Samsung export table consulted. Do not compare values just because both units are ms.
- Composite scores (Apple Readiness/Vitals and Samsung Energy Score) have no equivalent standard HealthKit/Health Connect record type; article notes Samsung SDK as a distinct route for some scores.
- Exercise metadata, ECG, blood pressure, SpO2 and temperature availability vary by model, app and country.
- **Sable requirement:** `metric_method` required for HRV; route statuses DOCUMENTED/CONDITIONAL/UNVERIFIED/UNSUPPORTED; brand scores must not be presented as imported.
- **Acceptance:** refuse SDNN/RMSSD aggregation; permit same-method trends only, with context notes.

### C. Duplicate steps and workout records (June 10; reverified July 13)
[Health Connect duplicate-data guide](https://www.fitmesh.fit/en/blog/multiple-smartwatches-duplicate-data)

- Multiple source entries can coexist while Activity/Sleep aggregates are deduplicated according to **user-configured priority**. Duplicated workout sessions and re-import loops require separate handling.
- Different operations: (i) exclude source from totals; (ii) revoke WRITE access; (iii) delete stored record. They are not interchangeable.
- Diagnose writer and route before deletion. Prefer Aggregate API for cumulative step total; capture raw provenance separately for explanation and audit.
- FitMesh describes choosing a source for cumulative metrics, reconciling overlapping sleep, and comparing session time/type for workouts. It describes optional, default-off write-back with anti-loop controls.
- **Sable requirement:** raw records immutable, canonical view separate, full provenance, duplicate decision reason, route identity and write-back strictly out of scope for M1.
- **Acceptance:** two simultaneous step sources must not sum blindly; two workouts from distinct apps flagged probable duplicate rather than silently deleted; repeated imports idempotent.

### D. Why the daily total and hourly steps chart can disagree (September 8) + 3.10 release (September 10)
[Steps total vs hourly detail](https://www.fitmesh.fit/en/blog/steps-total-vs-hourly-chart) · [3.10 release notes](https://www.fitmesh.fit/en/blog/past-days-clearer-data) · [build 190 notes](https://www.fitmesh.fit/en/blog/more-reliable-steps-update)

- FitMesh says it does **not** pick the largest daily step count or sum sources: it evaluates source consistency and hourly coverage for the day. Conflicted days can be marked unclear.
- Daily aggregate and hourly chart need compatible provenance; v3.10 permits **explicitly labeled partial charts** if they refer to the same reconciled source, but hides misleading charts where provenance cannot be verified.
- Missing hourly steps must **never be spread across hours** simply to make the chart add up; any day-average comparison should disclose number of comparable days.
- Historical date selection must change date-dependent labels, context and scores (not use today's rolling window on an old date).
- **Sable requirement:** no fabricated hourly distribution, date-consistent analytics, transparent partial coverage; keep Health Connect's priority-based aggregate as an authoritative Android total when appropriate.
- **Nuance:** FitMesh's described app-specific per-day selection is not identical to Android's own Aggregate API behavior. Sable must define its policy explicitly rather than treating them as the same algorithm.

### E. Device handoff and multi-phone coverage
[Ring and watch pass the baton](https://www.fitmesh.fit/en/blog/smart-ring-and-watch-real-scenarios) · [Multi-device dashboard](https://www.fitmesh.fit/en/blog/multi-device-health-dashboard) · [Switch watch without losing history](https://www.fitmesh.fit/en/blog/switch-smartwatch-without-losing-data)

- Real-life cases: watch charging, ring taking over at night, chest strap for workouts, one device temporarily absent, intermittent Bluetooth, phone off.
- For cumulative steps, FitMesh describes **whole-day source choice** when a single source fully covers the day, rather than indiscriminate hourly stitching; other metric types can use source-specific preferences.
- Switching vendors should preserve historical data and baseline comparability, but changed sensors/methods may warrant a baseline reset or annotation.
- **Sable requirement:** per-metric and sometimes per-session policies; represent coverage and source transitions, not a naive universal `pick max` rule.
- **Multi-phone implication:** Q25 and Galaxy phone Health Connect databases are distinct. Cross-phone history needs user-controlled transfer/sync; an account alone is not proof of a complete merge.

### F. Samsung pathways / sleep detail
[Galaxy Watch sleep pipeline](https://www.fitmesh.fit/en/blog/galaxy-watch-sleep-tracking-health-connect) · [FitMesh vs Samsung Health](https://www.fitmesh.fit/en/blog/fitmesh-vs-samsung-health) · [Galaxy Watch new models](https://www.fitmesh.fit/en/blog/galaxy-watch-ultra2-watch9-health-connect)

- Standard pipeline: Galaxy Watch → Samsung Health companion → Health Connect → third-party reader. Sleep sessions may include Awake/Light/Deep/REM intervals, arriving **after companion sync**, not live.
- FitMesh describes a second *read-only* **Samsung Health Data SDK** path on compatible Samsung phones, requiring separate registration and consent. Sable SDK access is **NOT YET VERIFIED**; standard Health Connect must work independently.
- Samsung proprietary Energy Score and related scores are not assumed to be available through standard Health Connect. Availability of ECG and other specialized measures is restricted by model and market.
- **Sable requirement:** show the exact route for each metric, identify Samsung SDK as OPTIONAL/CONDITIONAL, do not imply a Galaxy Watch is directly connected to Q25.

### G. Ring-specific and vendor interoperability
[Galaxy Ring](https://www.fitmesh.fit/en/blog/galaxy-ring-android-health-connect) · [Oura Ring](https://www.fitmesh.fit/en/blog/oura-ring-health-connect-android) · [Garmin Body Battery](https://www.fitmesh.fit/en/blog/garmin-body-battery-health-connect) · [Integration matrix](https://www.fitmesh.fit/en/integrations)

- A device may measure HRV, SpO2 and temperature without its companion exporting all of those types. Proprietary readiness and Body Battery scores remain in origin apps unless a separate authorized interface provides them.
- FitMesh lists Oura and Garmin data routes as conditional in its detailed matrix. Not all Oura metrics (notably temperature deviation) map to the Health Connect record type read by a given app.
- **Publication inconsistency to resolve:** the Oura article's short TL;DR claims SpO2 export, while its detailed per-type section says Oura's Health Connect documentation **does not confirm SpO2 export**. Prefer latest official provider documentation and an actual Sable record-coverage probe over either phrasing.
- **Sable requirement:** compatibility records carry source URL, vendor app version, verified date, permission category, observed status and limitation.

### H. Recovery, sleep and training calculations
[Recovery signals](https://www.fitmesh.fit/en/blog/recovery-metrics-hrv-sleep-heart-rate) · [Sleep score vs timing](https://www.fitmesh.fit/en/blog/sleep-score-vs-circadian-score) · [Zone 2 methods](https://www.fitmesh.fit/en/blog/why-zone-2-differs-watches-apps) · [Labs](https://www.fitmesh.fit/en/labs)

- Recovery is **not automatically one trustworthy number**. HRV, RHR, sleep duration, efficiency and timing can disagree; expose components before any optional composite.
- Sleep duration ≠ sleep efficiency ≠ regularity. Proprietary sleep-score weights cannot be reconstructed exactly from marketing descriptions.
- The same `Zone 2` label can mean different BPM ranges under %maxHR versus %heart-rate-reserve (Karvonen). Record method and parameters.
- Labs claims its **three** calculators run in browser memory without transmitting inputs: RMSSD from RR/IBI intervals; sleep efficiency; and heart rate zones. It is **not** an open-source library or an automatic license to copy site code.
- **Sable requirement:** publish algorithm specification, version, reference, input sample quality, baseline horizon, normalization units and missing-input behavior. Show uncertainty or abstain.

### I. Data plumbing, privacy and optional cloud
[Four sync architectures](https://www.fitmesh.fit/en/blog/sync-smartwatch-dashboard-2026) · [Android Health Connect guide](https://www.fitmesh.fit/en/blog/how-does-health-connect-work) · [Android to Apple cloud bridge](https://www.fitmesh.fit/en/blog/smart-ring-data-apple-health) · [Self-hosting status](https://www.fitmesh.fit/en/self-host) · [Alternatives](https://www.fitmesh.fit/en/blog/fitmesh-sync-vs-alternatives)

- Distinguish OS health store, bridge app, independent dashboard, and manual export/API. Health Connect is **on-device**, **not** a cloud synchronization service.
- FitMesh's cross-platform bridge uses its managed cloud to move data to iPhone; it is optional but **not** local-only.
- **Critical correction to earlier exploration:** FitMesh's public **self-hosting page says custom Supabase configuration is NOT a complete public self-host solution**: fitness-data writes/export/deletion still depend on managed backend. The mere presence of a `Self-hosting + dev options` setting or compiled module must not be treated as functional self-host.
- Gadgetbridge is a useful open-source privacy comparator; FitMesh, Health Sync and FitnessSyncer are proprietary. FitMesh Labs' browser-local calculator privacy statement does **not** mean the FitMesh Sync app is fully offline.
- **Sable requirement:** local analytics by default; opt-in encrypted homelab transport later, with explicit upload inventory, authenticated device identities, deletion/export, key rotation, and no implicit cloud destination.

### J. AI summaries and support troubleshooting
[Bring your own AI](https://www.fitmesh.fit/en/ai) · [Google Health missing data](https://www.fitmesh.fit/en/blog/google-health-not-syncing) · [Health Connect not syncing](https://www.fitmesh.fit/en/blog/health-connect-not-syncing)

- A curated, user-reviewed wellness summary can be copied into the AI of choice. FitMesh's public page does not claim its own proprietary chatbot does this work.
- Sync diagnosis is a **pipeline**: wearable → vendor companion → Health Connect/HealthKit write → Sable READ permission → records → freshness → UI/algorithm readiness. A single `Connected` indicator hides most failures.
- **Sable requirement:** setup wizard and troubleshooting screen should identify the failing leg without requesting sensitive logs or displaying a wall of generic text; AI summary never auto-sends.

## Relevant supplementary articles reviewed

- [Pixel Watch + Health Connect](https://www.fitmesh.fit/en/blog/pixel-watch-data-personal-dashboard) — vendor companion is an essential intermediate.
- [Google Fit API retirement](https://www.fitmesh.fit/en/blog/google-fit-shutting-down-alternative) — distinguish Google Fit, Health Connect, Google Health API and Health Services; confirm timelines with Google primary docs.
- [FitMesh pricing/founder program](https://www.fitmesh.fit/en/blog/fitmesh-sync-vs-alternatives) — user's lifetime Founder access is confirmed **by user**, not device metadata.

## Claims we explicitly do NOT promote to product facts

1. A record permission is declared -> data exists.
2. Fitbit Air is paired -> all premium/proprietary metrics are exported.
3. Health Connect `READ_HEART_RATE_VARIABILITY` -> same physiological method as Apple SDNN, or beat-to-beat RR intervals are accessible.
4. Google Health/FitMesh provides a proprietary score -> Sable can legally or technically read/clone it.
5. A FitMesh module/string mentions self-host -> self-hosted processing is fully functional.
6. A nominally supported source in the vendor matrix -> verified on the Q25 or Galaxy phone.
7. Every FitMesh article statement is consistent: provider exports and labels must be checked against official documentation when they contradict or change.
8. A published formula or an open repository is automatically safe to copy without license and scientific-input review.

**Next step:** implement the permissioned M1-A Health Connect record-coverage probe; then update this research record with observed metric-by-metric evidence and an upstream calculator acceptance matrix.
