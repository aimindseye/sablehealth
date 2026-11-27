# Sable Health

Keyboard-first, privacy-first Android wellness viewer for Zinwa Q25, Unihertz Titan 2, Zinwa P26 and other Android phones. Intended for data shared by Google Health through Health Connect; **no direct Fitbit Air BLE pairing**. Not a medical device.

## Status: M0 foundation — DRAFT / local hardware validation required

- Sable UI: exact Sable Games Blue `#2E9BD6` (dark), `#1A6FA0` (light), with Sable black/white backgrounds; follow-system, light and dark modes.
- Three hardware profiles + generic keyboard and touch fallback. Defaults follow `sablegames` mappings; overrides persist locally.
- Touch and keyboard-accessible onboarding, Dashboard, Sleep, Move, Vitals, Trends, Settings, Sources, Help.
- Synthetic demo data is permanently marked. Live placeholder contains **no** measurements.
- R4 compact mode after Q25/Titan 2 feedback: 2-up cards for Sleep/Move/Vitals, smaller header/tab chrome, demo-only stage bar, steps bars and resting-HR line charts. Browser preview: `docs/compact-r4-preview.html`; device acceptance: `docs/R4_COMPACT_ACCEPTANCE.md`.
- Health Connect diagnostic only: SDK status and number of already-granted permissions. **No health records are read, imported or stored in M0.**
- M1 adds granular read permissions, history access, actual data import, provenance and caching.
- No accounts, telemetry, background sync or remote service.

## Build on macOS

Requires Android SDK platform 36, JDK 17+ (JDK 21 compatible) and Android Studio. The application runs on **Gradle 8.13** with Android Gradle Plugin 8.13.2. A globally installed Gradle 9.7 CLI is **not** compatible with the Android project itself. The one-time `tools/bootstrap-gradle.sh` uses the global Gradle CLI **only** inside a temporary empty project to generate a launcher for Gradle 8.13, then runs Gradle 8.13 to generate the official wrapper JAR and verifies it against Gradle's published SHA-256. No project Gradle plugins are evaluated by the global CLI. The wrapper is still a local generated artifact until checked into the review branch.

```bash
cd sablehealth
bash tools/bootstrap-gradle.sh
./gradlew --version   # must report Gradle 8.13, not global Gradle 9.7
./gradlew :core:test :app:assembleDebug :app:lintDebug detekt ktlintCheck
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For CI, `gradle/actions/setup-gradle` installs Gradle 8.13 directly and invokes `gradle`. After bootstrapping, the owner should commit the checksum-verified wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) to **the M0 feature branch**, not to `main`, so later developers do not need the bootstrap step. The binary cannot be represented as GitHub text; bootstrap it locally and push the three files.

## Package registration and release-signing identity

Register **`io.github.aimindseye.sablehealth`** in Android developer verification with the **existing Sable Games / TenSeven release-signing certificate** (SHA-256 `9b2047390e5aa77537b8e8875cdfd3c2c72d0a0715e172a7f77457fc4300f292`). Do not create a new key.

M0 debug APKs are signed with the local Android debug certificate, **not** the registered release certificate; that is expected for local USB testing. Production release APKs will be built unsigned and signed locally with the existing keystore/alias via Android `apksigner`, then verified against the exact certificate fingerprint before publication. No keystore, password, key alias, or health record should be committed or sent to CI. Certificate sharing does not share app storage or permissions. See `docs/RELEASE_SIGNING.md`.

## Design contract

See `docs/DESIGN_CONTRACT.md`. The visual layouts remain approved v1.0; only colors changed to Sable Blue and Sable black/white. M0 has the committed prototype in `docs/design-preview.html` with revised Sable tokens.

## Validation

Follow `docs/TITAN2_ACCEPTANCE.md` to test the debug APK on Titan 2, Q25 and P26 when available. CI green does not replace hardware validation. No actual Fitbit health data belongs in issues, logs or test fixtures.

## License

Apache-2.0 proposed for independent Sable implementation. No source or formulas have been copied from NOOP (PolyForm Noncommercial), Hælan (AGPL-3.0) or Pulse. Licensing notice and full text to be finalized before public release.

## R5 approved UI contract — daily Today, independent view modes

R5 keeps the two-up Today cards as the default **Text** view. **Graphs** on Today display only today's step-goal progress and last-night stage composition. The seven-day steps and resting-heart-rate graphs live in Move, Vitals and Trends only, not on Today. View mode persists separately for each tab. Q25 has more compact application chrome while preserving minimum 48dp interactive targets; Titan 2 chrome is unchanged. Color-coded demo bars communicate informational/goal/below-plan/illustrative-outlier statuses only; no medical risk thresholds or diagnosis are inferred. See `docs/R5_ACCEPTANCE.md` and the local illustrative `docs/r5-preview.html`.

## R5.1 approved Dashboard implementation

**First tab is Dashboard, replacing Today.** The owner-provided design is the reference: two visual hero cards, a wide last-night sleep visualization and a compact fourth card. It contains **no seven-day histories**. The R5 global Text/Graphs toggle has been removed from Dashboard; individual Graph/Text styles can be selected on graph-capable widgets. Other tabs retain independently saved Text/Graphs choices.

Settings → **Customize Dashboard widgets** offers a 20-item catalog and exactly four saved slots. Default order: Steps, Heart rate, Sleep stages, HRV. Slot order affects presentation (two hero, one wide, one compact); picker supports selection, deselection, reorder, per-widget style, restore defaults, Save/Cancel. A mandatory non-widget status always says M0 has never imported live records.

All readings displayed in demo mode are synthetic. Unsupported or unverified proposals remain `No demo sample`, never invented measurements. Selecting No Records leaves the configured dashboard visible with missing-data placeholders. Actual Health Connect types/availability must be confirmed in M1 before enabling live measurement widgets.

Acceptance details: `docs/R51_DASHBOARD_ACCEPTANCE.md`; static gate: `python3 tools/check-r51-contract.py`. This is a design-implementation candidate, not a Fitbit health-data release.

## R5.2 Q25 icon and chrome refinements

The owner-confirmed Q25 Dashboard screenshot exposed tiny font glyphs in the steps ring and heart widget and excessive apparent chrome height. R5.2 uses two checked-in Android vector assets for a larger Sable Blue step/footprint ring and a distinct **red heart** with a white pulse. Red is the heart **category icon**, **not an adverse reading or warning**. Q25 reclaims one Dashboard header row by putting its screen title/demo state into the app subtitle; header and bottom tab pills are visibly 36dp tall inside **48dp** focusable/tappable targets. Titan 2 retains its accepted header/nav sizing. No new permissions, sensor reads or medical judgments.

See `docs/R52_Q25_ICON_CHROME_ACCEPTANCE.md` for device testing, `docs/r52-preview.html` for the clearly labeled browser concept, and `python3 tools/check-r52-contract.py` for source-specific static checks.


## M0 R5.3 — Q25 branded header + shared Phosphor icon family

R5.3 corrects Q25 Sources taking the full header width: brand, Sources and Settings now each have bounded space. Steps uses the exact Phosphor regular `footprints` icon and the launcher icon uses Phosphor regular `heartbeat` in Sable Games' black/white/blue composition. Android adaptive/round/monochrome assets and MIT attribution are included. Existing R5.1 four-widget behavior, Fitbit data boundaries and Titan 2 spacing remain unchanged. See `docs/R53_Q25_HEADER_PHOSPHOR_ACCEPTANCE.md`.

## R5.4 — Sources and Settings overview

The compact Sources screen separates Health Connect platform availability, permissions granted to Sable Health, and **zero imported records in M0** from the *user's declaration* of Google Health / Fitbit Air sync. In Sources → Connection details, users can mark whether they have paired and synced the official companion. This declaration is stored locally, is off by default for all new installations, and **never** verifies Fitbit data or Health Connect exports. Check availability again reads only SDK status and this app's granted-permission count. M1 will handle actual read-only metric import after per-record validation.

The compact Settings screen groups four-widget Dashboard customization, System/Light/Dark appearance, Demo/No records presentation, phone & keyboard, and local-only privacy. Device profiles and detailed explanations are in secondary screens. The Q25 top bar retains Sable Health, Back, Sources and Settings; focus/keyboard and Titan 2 behavior are preserved.

Review the [R5.4 acceptance checklist](docs/R54_SOURCES_SETTINGS_ACCEPTANCE.md) and the [approved UI/UX specification](docs/R54_APPROVED_DESIGN.md). Draft PR quality/security gates remain enforced.

## Research references and acknowledgements

Sable Health gratefully acknowledges the projects and publishers whose public documentation, designs, interoperability research and analytics concepts informed evaluation. A reference is not an endorsement, dependency, partnership or claim that code was copied.

- Wearable interoperability: [fitbit-air-mac (Raphael Hauser)](https://github.com/raphaelhauser/fitbit-air-mac), [fitbit-air (Thore Koritzius)](https://github.com/ThoreKoritzius/fitbit-air), [Google Golden Gate](https://github.com/Google-Health-API/golden-gate), [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge), [NOOP](https://github.com/ryanbr/noop), [Hælan](https://github.com/bardesss/haelan). Upstream research also credits [airbuzz-app](https://github.com/octavi42/airbuzz-app), [pebble](https://github.com/medpar/pebble), [fitbit-air-ble-protocol](https://github.com/jotx19/fitbit-air-ble-protocol), [fitbit-air-notification-bridge](https://github.com/paceaitian/fitbit-air-notification-bridge) and [AirBridge](https://github.com/ashu-sa/AirBridge); these are research leads, not independently qualified dependencies.
- Analytics and data architecture: [Pulse Mobile](https://github.com/adityaongit/pulse-mobile), [Pulse](https://github.com/adityaongit/pulse), [GenieMax Core](https://github.com/satayutata/geniemax-core), [OpenStrap Analytics](https://github.com/OpenStrap/analytics), [NeuroKit2](https://github.com/neuropsychology/NeuroKit), [OpenVitals](https://github.com/Hale-MTU/OpenVitals), [Open Wearables](https://github.com/the-momentum/open-wearables), [Open Health Sync](https://github.com/soma-intelligence/open-health-sync).
- Product benchmarks: [FitMesh Sync](https://www.fitmesh.fit/en/integrations), [FitMesh Labs](https://www.fitmesh.fit/en/labs), [FitMesh blog](https://www.fitmesh.fit/en/blog), [Wellness Project AI](https://wellnessproject.ai/). Public formulas/concepts are research references; proprietary code and hosted services are not incorporated by this acknowledgement.
- Platform documentation: [Android Health Connect](https://developer.android.com/health-and-fitness/health-connect), [Google Health API](https://developers.google.com/health/), [Samsung Health Data SDK](https://developer.samsung.com/health/data-sdk/overview.html).
- Sable family: [Sable Games](https://github.com/aimindseye/sablegames), [TenSeven Launcher](https://github.com/aimindseye/tenseven-launcher), [Sable Health Bridge](https://github.com/aimindseye/sablehealth-bridge); [Phosphor Icons](https://github.com/phosphor-icons/core) (see [MIT asset notice](docs/licenses/PHOSPHOR_MIT.txt)).

Research archive: [index](docs/sable-health/README.md), [evidence register](docs/sable-health/SOURCES_AND_EVIDENCE_REGISTER.md), [analytics/license evaluation](docs/sable-health/ANALYTICS_AND_LICENSE_EVALUATION.md), [November 26 interoperability and recent findings](docs/sable-health/FITBIT_AIR_AND_BRIDGE_RESEARCH_2026-11-26.md). Verify licenses at the selected upstream revision before copying code; credits are not a license grant. Direct BLE/Golden Gate remains research only, not a current capability or replacement for the authorized historical-data path.

## Android testing and release qualification methodology

See [Android testing methodology](docs/ANDROID_TESTING_METHODOLOGY.md) for the Q25-first emulator, instrumentation, visual regression and signed-device qualification requirements. This policy defines the migration; existing executable gates remain mandatory and the new runner is not yet implemented.
