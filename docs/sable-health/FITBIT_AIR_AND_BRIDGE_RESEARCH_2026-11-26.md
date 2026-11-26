# Research supplement: Fitbit Air interoperability and health-data evidence (2026-11-26)

**Scope:** Sable Health and Sable Health Bridge discussions, November 23–26, 2026. Documentation and attribution only. Preserves the November 23 research baseline; does not approve implementation, Bluetooth permissions, session takeover, cloud changes or release. No personal measurements, account identifiers, device addresses, tokens, APK contents or diagnostic dumps are included.

## 1. Evidence gained since the baseline

| Observation | Evidence level | Engineering consequence |
|---|---|---|
| Fitbit-origin steps and heart-rate records were readable from Health Connect on a physical Q25 | VERIFIED — prior owner diagnostic, summarized without counts or values | Distinguish actual records from pairing, companion sync, permissions and SDK availability. The writer is Fitbit's Android app; writer identity alone is not hardware identity. |
| Sleep, HRV and resting heart rate were initially absent from that Health Connect probe | VERIFIED — limited observation window, not universal incompatibility | Missing is not zero; record-level capability and freshness remain separate. |
| Permission, clock/timezone, date-window and companion-sync troubleshooting did not resolve missing sleep in the tested setup | REPORTED — prior troubleshooting; root cause unproven | Google Health export gap was the leading hypothesis, not a confirmed platform-wide defect. Avoid destructive reset/reinstall as default. |
| SB-0 retrieved Fitbit sleep through the authorized Google Health API while the corresponding Health Connect data was absent | VERIFIED — owner-side qualification reported Nov 25 | Official API is a useful complementary route; historical data still depends on authorization and actual provider availability. |
| SB-R1 manually imported an eligible sleep session into Health Connect and Sable Health displayed it | VERIFIED — owner-side end-to-end report Nov 26 | Preserve the Bridge route and source identity; no claim that all sleep, all vendors or background synchronization work. |
| Initial sleep display remained incorrect despite data becoming available | REPORTED — owner visual feedback | Verify stage duration/placement and semantics independently of import; no chart that implies unobserved stages or uninterrupted sleep. |
| R7A design scope extended Sources/Settings and secondary screens with six approved SVG references and planned 44 visual captures | DECIDED — prior design discussion; execution/CI not asserted | Preserve compact keyboard-first layout, consistent Sable theme, accessible focus/targets, daily-only Dashboard; use synthetic screenshot fixtures. |
| SB-R2A background access research is distinct from automatic import | DOCUMENTED scope — see Bridge repository and current PRs for actual status | No automatic write authorization inferred from background read success; preserve owner consent, revocation and fail-closed boundaries. |

### Data contract additions

- Official Google Health API sleep reconciliation endpoint: https://health.googleapis.com/v4/users/me/dataTypes/sleep/dataPoints:reconcile ; read-only scope: https://www.googleapis.com/auth/googlehealth.sleep.readonly . References: [Google Health API](https://developers.google.com/health/), [data types](https://developers.google.com/health/data-types), [Sable Health Bridge](https://github.com/aimindseye/sablehealth-bridge). The earlier SB-0/SB-R1 result does not qualify every API type.
- Manual Bridge import must retain source identity, compare time bounds and stages, skip identical records, block ambiguities, never mutate foreign records, and reread before write. A new external copy may permit removal of the Bridge's own identical record only with explicit review. There is no cross-app atomic transaction. See Bridge README for the precise SB-R1 contract.
- Sleep visualization: awake/light/deep/REM must use actual timestamped intervals and correct local wake-day assignment; do not invent missing stages, fill gaps or double-count naps/awake time. Handle offsets, midnight, ambiguous overlaps, unknown stages and absent data explicitly. Keep duration semantics distinct (session span vs asleep vs time in bed).
- Candidate HC types: steps, distance, exercise, calories, HR, resting HR, HRV, respiration, skin temperature, sleep and VO2 max where supported. SpO2 and proprietary sleep/cardio scores remain conditional/unverified for the tested route; do not infer RR intervals or medical meaning from BPM. Fitbit Air sleep breathing quality support was not established by Sable testing.
- First-launch wizard pattern from [TenSeven Launcher](https://github.com/aimindseye/tenseven-launcher/tree/fbe341ecaf472f88fbc5cecace3d170a5685ec06): explain permission purpose, request only necessary record types, allow retry/re-entry from settings, distinguish denied/absent/stale, optional HRV. Core candidate preset steps, HR, sleep, resting HR, exercise; scope remains milestone-specific.

## 2. Earlier direct Fitbit Air research

[ThoreKoritzius/fitbit-air](https://github.com/ThoreKoritzius/fitbit-air) was reviewed in the Nov 25 conversation as an experimental Flutter/Android integration (MIT reported in that review; reverify actual license and revision before reuse). Reported capabilities include haptics, battery/charging, firmware, connection, find-device and live HR. Historical sleep and direct steps were not established. This is a complementary research reference, not proof on Q25/Titan 2. Movement reminders could be generated locally with explicit opt-in haptic output; never assume the wearable supports direct steps, historical sync or native reminder scheduling.

Prior comparison named Airlift, SoFit and Open Health Sync as interoperability references. No verified drop-in Android Health Sync replacement was established. Exact Airlift/SoFit repository identities and license versions were not recovered in this review; do not adopt or claim credit for specific code until pinned and verified.

## 3. Fitbit Air Mac: pinned upstream review

[Raphael Hauser — fitbit-air-mac](https://github.com/raphaelhauser/fitbit-air-mac), reviewed Nov 26, 2026. [Revision reviewed](https://github.com/raphaelhauser/fitbit-air-mac/tree/6f17660a7a09a44f75ca97c383c8753fb0de68b8). Repository metadata dates publication to Nov 26. Author reports testing GW9C8/model 67, firmware 67.20001.253.2, using macOS CoreBluetooth/Bleak, C11 Golden Gate, DTLS 1.2 and CoAP. Static review of README, findings, license, build configuration and selected Python/C implementation; no independent radio/build/device validation or complete native security audit.

| Capability | Upstream finding | Sable disposition |
|---|---|---|
| Standard BLE heart rate | 0x180D service / 0x2A37 measurement, about 1 Hz; reported packets lack RR/contact data | P1 read-only Android foreground candidate; do not infer HRV, wrist contact or complete historical coverage |
| Device telemetry | GATT device info; CoAP /device/info returns battery %, mV and firmware fields | P1 after security/session validation; display verified timestamp and unavailable/stale states |
| Wrist events | Author observed wrist-off event; code handles wrist-on, which needs separate validation | Experimental; do not infer sleep/wear solely from HR |
| Haptics | Settings intensity changes trigger pulses; original value read first, OFF respected, restoration attempted | P2 experimental only; not a dedicated supported notification API |
| Exercise | Capabilities list, empty idle sessions, metrics error when idle | P3; no demonstrated active workout metrics |
| RSSI | Approximately 1 Hz connected on Mac, less frequent ads | Low priority; proximity not precise distance |
| IMU/double-tap | 869 read-only CoAP route probes and captures did not expose raw motion/double-tap traffic on tested firmware | Defer; firmware-specific absence, not universal proof |
| Historical data | Sync dump encrypted; live activity, device state, sync status/config, alarms, time and peer metadata restricted | No replacement for authorized API/HC; no secret extraction, unauthorized access or encrypted dump ingestion |
| Additional GATT service | Undocumented service with seven characteristics reported; purpose unconfirmed | Do not probe writes or firmware operations |

The author reports that enabling companion heart-rate sharing exposes services and that only one active Golden Gate session is available, requiring phone Bluetooth off in the Mac workflow. A clean handoff is attempted on exit. Do not assume Android coexistence, automatic reconnect or official sync preservation. Public bootstrap material is not device-specific authentication. No protocol bypass, key extraction or vendor-private data access is approved. Standard BLE HR is separable from Golden Gate and should be studied first using user-owned hardware, explicit device selection and Android's supported APIs.

### Source map and attribution

- [Findings](https://github.com/raphaelhauser/fitbit-air-mac/blob/6f17660a7a09a44f75ca97c383c8753fb0de68b8/docs/FINDINGS.md): firmware-specific capability/limitation map.
- [Live monitor](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/live_monitor.py): standard HR and event parser reference.
- [Native core](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/core/gg_explorer.c): transport, lifecycle and native memory-safety study.
- [Controller](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/fitbit_controller.py): connection, read-only telemetry, state and local API patterns.
- [Haptics](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/fitbit_buzz.py): read-before-write/restore concept, not production-safe.
- [Protocol decoder](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/proto_dump.py), [scanner](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/coap_deep_scan.py), [RSSI probe](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/rssi_probe.swift), [dashboard](https://github.com/raphaelhauser/fitbit-air-mac/blob/main/static/index.html): research/UX references, not dependencies.
- [Golden Gate](https://github.com/Google-Health-API/golden-gate): upstream stack, Apache-2.0 per project notice; build script pins b8a6f2552e12aaf92c1549dc88a41c9b7e73f1aa. [MIT license](https://github.com/raphaelhauser/fitbit-air-mac/blob/6f17660a7a09a44f75ca97c383c8753fb0de68b8/LICENSE) explicitly includes third-party notices for Golden Gate, Mbed TLS (Apache-2.0) and lwIP (BSD-3-Clause). GitHub metadata did not recognize the compound license. Verify full pinned dependency licenses before reuse.
- Upstream acknowledges [airbuzz-app](https://github.com/octavi42/airbuzz-app), [pebble](https://github.com/medpar/pebble), [fitbit-air-ble-protocol](https://github.com/jotx19/fitbit-air-ble-protocol), [fitbit-air-notification-bridge](https://github.com/paceaitian/fitbit-air-notification-bridge), [AirBridge](https://github.com/ashu-sa/AirBridge). These are indirect leads from upstream documentation, not independently reviewed or licensed for incorporation.

### Security/quality findings

No visible tests/CI in the reviewed tree; upstream disables native unit tests in build. Controller can return haptic success after an unverified corrective restoration; standalone CLI prints final state but does not qualify failure recovery. Local unauthenticated HTTP and WebSocket expose state/actions, despite loopback bind. Device discovery by name is ambiguous; broad exception handling, private Bleak API, lower-bound dependencies and vendor checkout not revalidated are additional concerns. Default hover/wrist-off actions are enabled in controller. Native thread/request lifecycle and memory safety need dedicated audit. Do not import the daemon, scanner or native code directly into Android. No remote service required for a future Android device adapter.

### Recommended architecture and roadmap (not approved implementation)

Keep authorized companion/Health Connect and official Google Health API -> Bridge -> HC -> Sable as historical routes; optional local adapter provides explicitly attributed live/device data. Keep live sessions ephemeral initially, separate from historical records; no silent HC write or duplicate counting. Preserve daily Dashboard and compact Q25 navigation; future Heart live session and Sources device details screens are illustrative concepts only, with freshness, disconnected/unknown states and keyboard access. Future optional haptics: hydration/movement reminders, timers, workout intervals, breathing cues; not medical alerts. TenSeven/Sable OS notification integration is a later opt-in idea. No cloud AI or homelab dependency.

- P0: existing R7/R11 and Bridge milestones retain their actual release/CI gates. Do not change current work based on research alone.
- P1: standard BLE HR, explicit device identity, permission/revocation, foreground disconnect/reconnect/coexistence; then separately qualify read-only battery/firmware/wrist telemetry and session handoff.
- P2: wearable interaction, only after explicit consent and safe state recovery. Respect OFF; verify original settings and final restoration; fail closed on timeout/disconnect, no automatic retries of ambiguous writes.
- P3: exercise capability research; encrypted historical access remains research-only via authorized supported routes.

Acceptance for future code: genuine >=80% new-code test coverage, SonarCloud/Detekt/ktlint/Lint and applicable security scans, deterministic synthetic parser/edge-case fixtures, permission/no-network/privacy tests, firmware compatibility and physical Q25/Titan 2 tests, visual regression, official sync and Health Connect/Bridge non-regression. Native integration additionally needs sanitizer/concurrency tests and dependency review. No personal health values in CI or public issues. No persistent settings changes or fabricated readings. No medical claims.

## 4. Additional research references to preserve

- [FitMesh Sync](https://www.fitmesh.fit/en/integrations), [Labs](https://www.fitmesh.fit/en/labs), [AI](https://www.fitmesh.fit/en/ai): proprietary benchmark, not reusable source code; local transparent calculations, per-source reconciliation, honest chart gaps, manual reviewed AI handoff. Existing [deep dive](FITMESH_BLOG_DEEP_DIVE_2026-11-23.md) remains authoritative for the earlier study.
- [Wellness Project AI](https://wellnessproject.ai/): prior review identified a public MIT MCP integration with hosted service, not a reusable local analytics engine or full self-host solution. Exact repository revision not recovered; no code adoption approved.
- [NOOP](https://github.com/ryanbr/noop) and [Hælan](https://github.com/bardesss/haelan): architecture/UI inspiration; WHOOP protocol is not Fitbit Air protocol, server companion is not Sable's required architecture. Noncommercial/AGPL obligations prohibit silent incorporation into an Apache-licensed component.
- Vitality and Health Connect KMP were named additional evaluation candidates (Apache-2.0 reported previously); exact repository identity and revision not recovered, so no reuse or license assurance. Existing [analytics evaluation](ANALYTICS_AND_LICENSE_EVALUATION.md) records Pulse/Pulse Mobile, GenieMax, OpenStrap, NeuroKit2, OpenVitals, Open Wearables, Open Health Sync and Gadgetbridge. No license-compatible production reuse inferred.
- [Sable Games](https://github.com/aimindseye/sablegames), [Phosphor Icons](https://github.com/phosphor-icons/core), and TenSeven Launcher inform shared design, icon attribution and onboarding; distinguish internal project design references from incorporated assets. See existing Phosphor notice in docs/licenses.

## 5. Maintenance

Keep dated append-only evidence supplements and README acknowledgements. Each new finding needs source link, revision/version if known, review date, status (reported/verified/conditional/unsupported), test/falsifier, licensing disposition and roadmap implication. Owner hardware observations are summarized without raw data. Exact unrecovered references and unverified claims remain explicit; do not treat prior chat recollections as independent source verification. Existing product decisions remain controlling until owner approves changes. Current code/CI status belongs to the actual PR, not this research supplement.
