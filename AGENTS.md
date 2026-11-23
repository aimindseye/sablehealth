# Sable Health engineering policy (M0)

- **Branch/PR workflow:** one scoped feature branch and draft PR per deliverable. Do not merge to main. User builds/tests locally and explicitly approves final merge.
- **Design contract:** follow `docs/DESIGN_CONTRACT.md`. Exact Sable Blue: dark `#2E9BD6`, light `#1A6FA0`; backgrounds `#000000` / `#F5F7F9`. No green/teal default accents. Focus must be visible.
- **Architecture:** Kotlin/Compose Android app; pure deterministic Kotlin profile/keymap core; no React/Expo, server or dependency on TenSeven/SableOS.
- **No scope inflation:** M0 is demo UI + Health Connect availability/permission-count diagnostic, not biometric record ingestion, BLE pairing, live pulse tracking or proprietary score recreation.
- **Privacy:** never commit genuine health records, tokens, keystores, identifiable screenshots or unredacted logcat. Synthetic fixtures only. No telemetry, silent uploads or source rewriting.
- **Interaction:** every action keyboard and touch reachable, trackpad DPAD on Q25; I/J/K/L on Titan 2/P26; modifiers and text inputs must not be stolen. Confirm actions must not repeat.
- **Testing:** unit-test profile detection, keyboard mapping, adverse/missing data, text safety; Android build/lint, Detekt, ktlint, CodeQL, MobSF and Sonar. Do not weaken gates or add blanket suppressions for green CI.
- **Supply chain:** verified GitHub Actions SHAs, pinned dependencies, Gradle wrapper checksum; no release signing secrets in repo, no auto-merge or publishing without validation.
- **Derived metrics:** never infer clinical diagnoses. Missing != 0. M1+ must track source provenance, timezone, partial coverage and duplicates. All scoring independent of NOOP/Hælan source code.
- **Review:** PR must include Design Compliance table, source/test scope, CI results and physical device matrix, with untested cases labeled PENDING.
