# Sable Health M0 R4 compact-screen acceptance — Q25 and Titan 2

**Candidate:** M0 R4, designed from real Q25 and Titan 2 screenshots on 2026-11-23. All checks PENDING device rerun until the user reports them.

## Header and navigation

- [ ] On Q25, Sable Health + Sources/Settings fit on one compact header row without cutting text; actions remain at least 48dp tall.
- [ ] Bottom Today/Sleep/Move/Vitals/Trends tabs fit one row and are at least 48dp tall; trackpad (Q25) or IJKL (Titan 2) focuses each tab visibly.
- [ ] T cycles tabs; Back/Escape, Sources, Settings and keyboard focus retain old behavior.
- [ ] 1.3x and 1.5x text scaling: content remains reachable, falling back to one column where needed.

## Metrics and graphs

- [ ] Sleep: time asleep, deep, REM, light are 2 × 2 cards with the stage chart visible in one viewport on Q25 if possible.
- [ ] Move: steps, active minutes, distance shown two across with a seven-day steps bar chart; last-day bar uses Sable Blue.
- [ ] Vitals: HR, resting HR, HRV, unavailable SpO2 and a seven-day resting HR blue line chart; missing SpO2 is displayed as “—”, never zero.
- [ ] Trends: two *synthetic* charts; bars/points show correctly with no dark/light contrast or drawing glitches.
- [ ] Switch to no-data mode: metric values and ALL sample charts disappear; no fabricated live readings.
- [ ] Dark mode: #2E9BD6 marks, blue/gray tonal stages, readable text. Light mode: #1A6FA0 primary mark, readable contrast.
- [ ] All charts labeled DEMO, and TalkBack announces synthetic chart summaries.

## Build and scope gate

- [ ] Local `:core:test :app:assembleDebug :app:lintDebug detekt ktlintCheck` pass.
- [ ] GitHub Android, CodeQL, MobSF and configured SonarCloud quality/security checks reviewed.
- [ ] `gradlew`, `gradlew.bat` and verified `gradle-wrapper.jar` committed before merge.
- [ ] Fitbit Air setup NOT REQUIRED for M0. No actual Health Connect measurements imported.

**Hold for merge** until code gates and both device checks are approved by owner. Screenshots or logs must not include personal health data.
