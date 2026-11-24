# Sable Health — ingestion, canonical schema and source reconciliation

**Status:** Proposed engineering contract. M1 must verify actual available record types before adding production analytics. See [Product Charter](PRODUCT_CHARTER_AND_DECISIONS.md) and [FitMesh Deep Dive](FITMESH_BLOG_DEEP_DIVE_2026-11-23.md).

## Boundary: device -> companion -> system store -> Sable

```text
Fitbit Air -> Google Health on Q25 -> Health Connect on Q25 --(consented read)--> Sable Q25
Galaxy Watch -> Samsung Health on Galaxy phone -> HC on Galaxy phone --> Sable Galaxy
Other rings -> their authorized companion -> local HC or vendor API --> Sable adapter

[per-phone local canonical history + analytics]
           | OPTIONAL explicit user action / opt-in authenticated sync
           v
[Linux homelab: multi-phone reconciliation, history, backup, heavy analytics]
           |
           v
[Q25 and other Sable clients: local display, source explanations, exports]
```

Health Connect is a local per-phone store, **not** a cross-phone sync service. Android official [read](https://developer.android.com/health-and-fitness/health-connect/read-data), [aggregate](https://developer.android.com/health-and-fitness/health-connect/aggregate-data), and [sync](https://developer.android.com/health-and-fitness/health-connect/sync-data) guidance is primary authority.

### Source types

- **HC_LOCAL** — permissioned local record read; a record's `dataOrigin.packageName` is the *writer app*, not necessarily the original wearable hardware.
- **GOOGLE_HEALTH_API** — future optional Google OAuth source; must attribute imported remote data and avoid re-importing the same event via HC.
- **SAMSUNG_SDK** — future conditional official SDK; separate permission/registration and dedup against HC route.
- **OTHER_VENDOR_API** — only with official authorization, terms and purpose bound scopes.
- **MANUAL_IMPORT** — later offline exports with declared parse formats and uncertainties.
- **USER_ENTRY** — first-party journaling/measurements explicitly marked manually entered.
- **DERIVED** — Sable algorithms, never masquerading as hardware or vendor measurements.

For each route store `documented_support`, `actual_permission_status`, `record_coverage`, `freshness`, `last_success`, `error_reason`, and `evidence` separately. `Installed` is never a synonym for `Connected`.

## Proposed canonical entities

```text
SourceConnection {
  source_connection_id, connector_type, host_device_id_pseudonym,
  vendor_app_id?, source_vendor?, route_id, permission_scope_set,
  consent_version, last_attempt, last_success, visibility, verification_status
}
RawObservation {
  observation_id, source_connection_id, origin_app_package?,
  originating_device_label?, upstream_record_id?, upstream_version?,
  metric_type, unit, measurement_method?, aggregation_kind,
  start_utc, end_utc?, zone_offset_at_record?, value_or_stage_series,
  recording_kind, source_modified_at?, imported_at, sample_coverage?,
  data_quality_flags, imported_content_hash?
}
ReconciliationDecision {
  canonical_subject_key, metric_type, time_window,
  policy_id, policy_version, candidates[], selected_candidates[],
  excluded_candidates[], reason_codes[], coverage, ambiguity,
  user_source_override?, computed_at
}
DerivedMetric {
  derived_metric_id, algorithm_id, algorithm_version, inputs[],
  window, baseline_definition, output_unit?, output_value?,
  insufficient_inputs_reason?, source_reconciliation_versions[],
  coverage, computed_at, references[]
}
```

Never use real credentials, account identifiers or personal health values in version-controlled test fixtures. Private local store implementation should be encrypted at rest according to a reviewed threat model; source/device identifiers are pseudonymized when synced.

## Separation of record views

1. **Raw/imported:** source-preserving original data and metadata. Import corrections create explicit revisions/tombstones; do not silently rewrite history.
2. **Canonical/reconciled:** a deterministic view with decision trace. Can be recomputed when source-priority rules or time-zone normalization changes.
3. **Derived:** versioned analytics from canonical inputs. Stale/unknown inputs yield unavailable/provisional output, not a guaranteed normal score.
4. **Presentation:** chart as a projection with a `coverage_mask`, `estimated` flag, `method`, and visible source identity.

A step total, an hourly graph, a sleep stage chart, and an HRV trend can have *different* evidentiary status. Do not fabricate hourly buckets when only a daily aggregate exists.

## Core reconciliation rules

### Steps
- On one Android phone, call Health Connect `aggregate()` for official priority-aware cumulative totals; reference `dataOriginFilter` for single-source diagnostics.
- Never sum overlapping raw step-count intervals across devices. Do not pick the numerically largest total as the winner by default.
- For *multi-phone* canonical totals, develop a separate deterministic policy using intervals, coverage, user preferences and conflicts; Health Connect cannot arbitrate across different phone stores.
- Source selection/coverage policies must be documented and measured on synthetic disjoint/overlapping cases, including partial days, timezone changes and clock skew.
- If daily total exists but provenance-compatible hourly detail does not: show total, omit or label partial chart, and show reason. Never distribute unknown steps evenly across hours.
- Treat `0` as a real measured zero only when a source reports it for an observed interval; otherwise use `No data`.

### Sleep
- Ingest `SleepSessionRecord` with stages/awakenings when provided.
- Define local **wake-day** association; include confirmed naps without double counting intervals, exclude awake portions when computing time asleep; retain explicit time-in-bed distinction.
- Overlapping sessions from two wearables: compare supported stage detail, data completeness and source preference; do not merge stage states from different sensors indiscriminately.
- Missing/unknown stage is not deep/light sleep. Spurious duplicate or clock-overlap remains flagged rather than deleted.

### Workout sessions
- Candidate duplicates by normalized exercise type, interval overlap, source route and metadata.
- Distinguish re-import of same upstream identity from two different apps independently representing the same physical event.
- Preserve both upstream records, mark one canonical / ambiguous. A matching start/end/type is a candidate signal, not infallible proof.

### Point samples (HR / SpO2 / temperature / respiration)
- Maintain actual sample timestamps; no interpolation through long gaps or fake "continuous" coverage.
- Per-metric source selection may differ (e.g., chest strap during workout, ring at night, watch by day); switching should be explainable and versioned.
- Compare measurement contexts and methods; core vs skin temperature, absolute vs deviation, and distinct sampling periods are not directly interchangeable.

### HRV
- **Method identity is mandatory:** RMSSD, SDNN, or other. Apple's SDNN must not be pooled with Android RMSSD, even if both are expressed in ms.
- RR/IBI raw-interval calculations require real intervals. Daily RMSSD summary does not supply beat-to-beat intervals.
- Preserve measurement context (overnight, short resting test, daytime; device; acquisition protocol) and baseline per device/method when needed.

### Data quality
Use explicit flags: `MISSING_SOURCE`, `PERMISSION_DENIED`, `NO_RECORDS`, `STALE`, `PARTIAL_COVERAGE`, `UNVERIFIED_ORIGIN`, `CONFLICTING_SOURCES`, `METHOD_MISMATCH`, `IMPLAUSIBLE_SAMPLE`, `AMBIGUOUS_DUPLICATE`, `UNKNOWN_TIME_ZONE`, `LOW_BASELINE_HISTORY`.

## Anti-loop / export rules

- **M1 is strictly READ only** and has no writing/deletion path. No automatic data copy from one HC back to itself.
- Later write-back requires its own settings, typed scopes, user confirmation, a source `owned_by_sable` marker, stable `clientRecordId`, versioning, export destination and audit trail.
- Do not create a second copy of data merely because it came through cloud and local adapters. Source provenance must track `lineage`/upstream IDs and route.
- Deletion requests must explicitly separate "remove from Sable cache/history", "delete Sable-owned HC records", and "delete provider-origin records" (the third is **not Sable's default capability**).
- No silent delete from another provider. No bidirectional export until independent loop and failure-injection tests pass.

## Optional self-hosted mode

- On-device scoring and charts function while offline; server never becomes a required dependency for daily health display.
- Homelab may later provide durable cross-phone history, deduplicated multi-phone canonical timelines, optional Python analytics/NeuroKit and encrypted backup.
- Proposed candidate infrastructure: [Open Wearables](https://github.com/the-momentum/open-wearables) (MIT), [Open Health Sync](https://github.com/soma-intelligence/open-health-sync) (AGPL-3.0); neither accepted without security, compatibility, data-model and license review.
- Require mutual authentication, TLS/Tailscale or similar private transport, encryption-at-rest, explicit opt-in, recovery drills, deletion/export, retention, and telemetry minimization. No credentials/health records in GitHub Actions.
- The [FitMesh self-host page](https://www.fitmesh.fit/en/self-host) explicitly notes its custom Supabase setting is *not* a complete public self-host solution; don't copy that dependency model.

## First validation suite (all synthetic)

1. Two identical step intervals from separate apps: no double total.
2. Morning watch + afternoon ring: retain disjoint contributions only if independently supported.
3. Full-day single source + partial duplicate source: deterministic priority with explanation.
4. Daily total and incomplete/mismatched hourly chart: label partial or suppress.
5. Overnight sleep plus 45-minute nap: sum valid asleep intervals, not wake gaps.
6. Two overlapping sleep sources disagreeing on stages: do not count both.
7. Duplicate workout through direct API and HC: one canonical event, two raw inputs.
8. Same record re-imported across Q25/Galaxy: idempotent canonical view.
9. SDNN + RMSSD: refuse cross-method mean/composite.
10. No data/permission denied/error/partial/stale: distinct states with non-misleading UI.
11. Daylight-saving change and UTC/local wake-day: stable assignment.
12. Provider deletes or revises record: canonical/derived invalidation while preserving trace.

**Open proof:** run permissioned M1-A coverage probe on Q25 and Galaxy separately. No part of this document implies those physical tests have already passed.
