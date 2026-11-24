# M1-A — Health Connect read-only coverage experiment

**Status:** Draft, NOT device-validated and NOT a production health-data release.  
**Exact parent:** `d0e26078112ed56a496f9f0df3de8cf2acf29150` (R5.4 draft).  
**Integration:** `Sources → M1-A record coverage` while R5.4 Sources/Settings overviews and Dashboard remain unchanged.

## Scope and boundaries

- **Only** user-selected **READ** permissions, using the Android Health Connect permission screen.
- The app may inspect **steps, sleep sessions, heart rate, HRV RMSSD, resting heart rate, exercise, respiratory rate, oxygen saturation, body temperature, skin temperature, active calories** when the user grants each permission.
- A separate, explicit **Scan allowed records** tap triggers a foreground-only read of the **last 30 days**.
- Pages are capped at **500 record objects per category**, paginated 100 at a time, newest first. Display count is marked **at least** if capped. Records are not individual HR samples or step totals.
- The in-app temporary summary shows counts, available record types and **writer app packages**, plus earliest/latest observed UTC timestamps in the optional metadata-only report.
- **No metric values**, per-record IDs, hardware IDs, direct wearable pairing, historical grant request, cloud/API OAuth, network/BLE permission, background collection, Health Connect write/delete, durable measurement storage or Dashboard data import.
- Grant flags alone never imply records exist. An installed companion is not proof of Health Connect export. Source writer package is not necessarily the physical watch/ring.
- Permission denied, no records, read error, and partial/capped coverage are distinct.
- Report sharing is an explicit user action through Android's share sheet. It contains **record counts, timestamps and writer package names**—still personal metadata. Preview and redact further before sending it outside your phone.

## Local validation commands (on new isolated worktree)

```bash
cd "$HOME/Documents/projects/sablehealth-m1a"

python3 tools/check-structure.py
python3 tools/check-r5-contract.py
python3 tools/check-r51-contract.py
python3 tools/check-r52-contract.py
python3 tools/check-r53-contract.py
python3 tools/check-r54-contract.py
python3 tools/check-m1a-contract.py

./gradlew --no-daemon --console=plain \
  :core:test :app:assembleDebug :app:lintDebug detekt ktlintCheck
```

If the wrapper was not checked into R5.4, use the verified Gradle 8.13 via the repository bootstrap instructions rather than a global Gradle 9 CLI. Never disable lint/Detekt/ktlint to manufacture a pass. Confirm `./gradlew :core:test` runs new `HealthCoverageContractTest`, not only cached M0 tests.

## Physical Q25 test matrix

1. Install debug APK over same package; open existing R5.4 Dashboard, widgets, Sources, Settings, themes: unchanged.
2. Sources → M1-A coverage. Verify Q25 scroll, 48dp targets, keyboard/trackpad focus, Back to Sources.
3. Select only **Steps**, tap Request. Android Health Connect permission screen appears, permission can be granted or denied. There are **no write** permissions.
4. Tap Scan. Unselected categories should say **Read access not granted**; selected granted category should say records, no records, or a legitimate error. Report must not display actual step counts or biometric values.
5. Grant **Sleep + Heart rate**, then optionally **HRV RMSSD / RHR / oxygen / respiratory / temperature**. Repeat scan. Source writer app package(s) and count presence only are expected; do not assume Fitbit Air supplies every class.
6. If a category has 500+ records, verify the lower-bound label; never treat `500` as complete history or as measurement value.
7. Inspect report **privately**. If sharing with assistant, keep only non-sensitive metadata; strip or generalize UTC timestamps if concerned about personal patterns. Avoid screenshots of measured health values.
8. Revoke a selected permission in Android Health Connect, return, rescan: category no longer read; denied state. Retrying after permission changes should not crash.
9. Simulate Health Connect unavailable on other device if available; verify no request/scan attempts.
10. Restart app: no prior record coverage or measurement values persisted, no background sync.
11. Confirm Q25 remains stable: no unexpected permission prompt on launch or when opening ordinary Sources/Settings. M0 Dashboard still clearly synthetic / no records.
12. Repeat a small subset on Titan 2 and/or Galaxy phone later, but keep device record counts separate.

## When reporting results

Paste only `SABLE_HEALTH_M1A_COVERAGE_V1` from deliberate share, **after inspecting it**. It is okay to send only category + state + count + writer package and omit timestamps entirely. Do not upload Fitbit/Health Connect DBs, private health screenshots, debug logcat or authentication material.

## Quality and merge gates

- **Pending**: compile, tests, lint, ktlint, Detekt on actual local Android build and CI.
- **Pending**: physical Q25 permission grant/revoke and per-type data coverage.
- **Pending**: review inherited R5.4 Security workflow failure. M1-A does not close that automatically.
- **Owner only** merges PRs after acceptance; M1-A remains a **stacked draft** against R5.4 feature branch, not a merge to main.
- M1-A diagnostic does not prove cross-device sync, dedup correctness, HRV beat-to-beat availability or analytics readiness. Those remain separate milestones.

## Primary API references

- https://developer.android.com/health-and-fitness/health-connect/get-started
- https://developer.android.com/health-and-fitness/health-connect/read-data
- https://developer.android.com/health-and-fitness/health-connect/aggregate-data
- https://developer.android.com/health-and-fitness/health-connect/data-types
