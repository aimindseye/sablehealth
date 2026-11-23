# Sable Health

Keyboard-first, privacy-first Android wellness viewer for Zinwa Q25, Unihertz Titan 2, Zinwa P26 and other Android phones. Intended for data shared by Google Health through Health Connect; **no direct Fitbit Air BLE pairing**. Not a medical device.

## Status: M0 foundation — DRAFT / local hardware validation required

- Sable UI: exact Sable Games Blue `#2E9BD6` (dark), `#1A6FA0` (light), with Sable black/white backgrounds; follow-system, light and dark modes.
- Three hardware profiles + generic keyboard and touch fallback. Defaults follow `sablegames` mappings; overrides persist locally.
- Touch and keyboard-accessible onboarding, Today, Sleep, Move, Vitals, Trends, Settings, Sources, Help.
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
