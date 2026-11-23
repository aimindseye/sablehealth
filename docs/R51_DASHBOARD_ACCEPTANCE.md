# R5.1 — Dashboard & four configurable widgets

**Status:** implementation candidate, owner acceptance + security closure required. Design approved 2026-11-23. Primary design reference: owner's dark screen with two upper hero cards and one wide Sleep waveform card; do not reproduce proprietary imagery.

## Contract

1. **Dashboard**, not Today, is the first/default tab. All existing navigation, Q25 trackpad/DPAD and Titan 2 keyboard behavior remain.
2. Dashboard uses exactly four independent widget slots: hero left, hero right, wide visual, compact fourth. On small windows or accessibility scaling, hero widgets stack. The status message is mandatory and **does not consume a slot**.
3. Default sequence: **Steps, Heart rate, Sleep stages, HRV**. Steps progress ring and Heart icon are informational Sable Blue; Sleep is a **synthetic illustrative** last-night timeline, never a validated signal waveform. No seven-day chart exists on Dashboard.
4. Settings → **Customize Dashboard widgets** lists 20 proposed widget types by category with explicit availability status. Some are unverified/future; selection does **not** mean Health Connect exports them. Only explicitly supported widget types allow **Text/Graph** style selection. Choose exactly four; reorder with Up/Down; restore defaults; Save or Cancel.
5. Save persists the four IDs in order and display style through process restart; Cancel never persists. Corrupted/missing settings recover safely to four unique widgets. Existing R5 visual mode choices for Sleep, Move, Vitals and Trends remain independent.
6. Synthetic values are labeled **Demo**. Unverified/future demo tiles show **No demo sample**; they never receive guessed values. In no-data mode, all four slots show `—`/`Not synced`; nothing becomes zero or falsely live.
7. Always show a separate, small data-status row: no live records imported in M0; no fabricated last sync time. Health Connect diagnostic remains in Sources. No direct BLE or Health Connect read permission added.
8. On Q25, preserve compact header/tabs and 48dp minimum click targets; on Titan 2 preserve current chrome. Avoid truncated selected names; scrolling is acceptable in Settings and at larger accessibility scales. All displayed content must remain reachable via DPAD / keyboard and touch.
9. Move, Vitals, Trends retain seven-day charts and Sable Blue/goal/low-plan/synthetic-outlier legend. No new medical-score inference. No red/green health interpretation until metric-specific baseline logic is validated.

## Physical-device checklist

- [ ] Fresh install (or clear storage on a **test build only**): first tab Dashboard; four defaults and sleep visual present in demo.
- [ ] Q25/SableOS 2.0: hero cards, wide sleep visual, compact HRV, data status and bottom tabs fit; content is reachable and not hidden by navigation.
- [ ] Titan 2: roomier chrome unchanged, I/J/K/L and DPAD behavior work, 48dp targets remain easy to tap.
- [ ] Settings → Customize: remove one widget, select another (e.g., Active Minutes), reorder it, choose a supported style, Save; verify new Dashboard layout.
- [ ] Force-stop and reopen: same four widgets, same order and supported styles.
- [ ] Customize again: use Restore defaults and Save. Reopen: all four defaults restored.
- [ ] Customize then Cancel: original widgets remain, including order and styles.
- [ ] Four full: trying to add fifth requires removing one; cannot Save fewer than four.
- [ ] Select `SpO₂` or `Readiness` as future/unverified: shows no fabricated reading and status explains unverified availability.
- [ ] Switch Settings Data mode to No records: all four slots remain visible, show missing-data placeholders (never zero) and status says no imports.
- [ ] Contrast and TalkBack, light/dark, device rotation, large text, Q25 trackpad and Titan 2 letter keys.
- [ ] Sleep / Move / Vitals / Trends functionality unchanged; history remains outside Dashboard.

## Engineering and security gates

- `:core:test :app:assembleDebug :app:lintDebug detekt ktlintCheck` must be green on the exact reviewed SHA; CodeQL must pass.
- MobSF seven findings require a documented mitigation/accepted-risk classification and explicit owner approval; **never suppress rules simply for green CI**.
- SonarCloud project setup and quality-gate evidence remain separate pending items.
- Gradle 8.13 verified wrapper scripts and JAR must be committed on the feature branch before merge.
- Release signer stays identical to Sable Games/TenSeven, but no key or password enters Git/CI. No auto-merge.
