# M0 R5.2 — Q25 hero-icon and chrome refinement

Owner feedback: 2026-11-23 Q25/SableOS 2.0 physical screenshot. Steps ring and heart glyph looked compressed. App header and bottom navigation still occupied substantial vertical/visual space. Titan 2 spacing had already been accepted.

## Design/implementation contract

- **Default Dashboard:** Steps ring + distinct footprint SVG/vector, a larger red heart with white pulse accent, wide last-night sleep visualization, and fourth configurable card. No weekly graphs or added metrics.
- **Red heart:** category artwork only. **Never** interpret the red heart as an abnormal heart-rate indication. Actual anomaly colors remain explicitly baseline/metric-specific; no medical evaluation in M0.
- **Q25 only:** compact brand title and subtitle. Display `DASHBOARD · DEMO` or `DASHBOARD · NO RECORDS` in the header, without repeating the separate two-line Dashboard heading. This reclaims about one heading's height. All five tabs remain visible and reachable.
- **Q25 header/tab visuals:** 36dp painted chips centered in independent >=48dp clickable/focusable areas. The actual touch target must not shrink below 48dp. Remove unnecessary external Q25 padding. This makes chrome visibly less bulky without violating accessibility.
- **Hero proportions:** ring 44dp Q25 / 48dp standard, footprint 25dp Q25 / 30dp standard, heart 37dp Q25 / 43dp standard. Q25 hero height 116dp to prevent squeezing its label, icon, value and description. Every icon is a drawable vector, not a Unicode glyph whose metrics depend on the device font.
- **Titan 2:** retain its accepted standard header/tab dimensions, grid spacing and existing keyboard rules. Larger vector icons intentionally improve legibility here too.
- **No feature scope drift:** four configurable slots and ordering/display persistence unchanged. Health Connect remains diagnostic-only in M0; no real Fitbit Air sensor reads, Bluetooth transport, telemetry, new network access, or medical scoring.

## Acceptance test — Q25 first, then Titan 2

- [ ] Q25: red heart is recognizably proportioned; white pulse accent is visible in both light and dark themes.
- [ ] Q25: blue circular step-progress ring is smooth with a centered footprint that does not overlap the ring. No squashed/misaligned Unicode glyph appears.
- [ ] Q25: compact subtitle displays Dashboard/demo state; no second Dashboard heading repeats it.
- [ ] Q25: top Sources/Settings controls and all five tabs have shorter painted outlines, remain legible, focusable and easy to tap/trackpad-click. At least 48dp hit area remains.
- [ ] Q25: four widgets, full sleep graphic and data-status message remain reachable, without being clipped behind the bottom tabs.
- [ ] Q25: dark/light, rotation and accessibility font scaling; no overlapping or inaccessible labels. When font scale is large, scrolling is allowed to protect readability.
- [ ] Titan 2: normal/roomy header/tab dimensions unchanged; footprint and heart improve over Unicode. Keyboard I/J/K/L, Back, focus remain functional.
- [ ] Both: Settings widget customization (select four, reorder, save, cancel, reset), demo/no-record state, Sleep/Move/Vitals/Trends and seven-day graphs unchanged.
- [ ] CI: Android build, core tests, Lint, Detekt, ktlint and CodeQL must pass against exact commit. MobSF seven findings remain independent triage blockers; SonarCloud setup and Gradle wrapper check-in remain pending unless separately resolved.

## Manual local build

```bash
cd ~/Documents/projects/sablehealth
git switch feature/sablehealth-m0-foundation-20261123
git pull --ff-only origin feature/sablehealth-m0-foundation-20261123
./gradlew :core:test :app:assembleDebug :app:lintDebug detekt ktlintCheck
adb devices
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

Only the owner merges draft PR #1 after the required security/build/device evidence is dispositioned. No release signing material belongs in Git.
