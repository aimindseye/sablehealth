# M1-C combined real data candidate

Version 0.1.2-m1c / 3. Keep existing demo/empty choices and four widget preferences. Enable Real data explicitly in Settings or setup. Only foreground reads; no persistence, logs, export of values, cloud, network, write or background permissions. Add READ_DISTANCE and READ_TOTAL_CALORIES_BURNED to the existing read-only catalog. Health Connect SDK remains 1.1.0. Companion sync is separate from app refresh.

## Acceptance (Q25 and Titan 2)
- [ ] In-place signed update preserves preferences, widgets, keyboard navigation, and setup flow.
- [ ] Real mode loads on launch and resume; refresh control, pull gesture, R shortcut and foreground timer update all tabs. Interval Off/1/5/15/30 min works. No background measurement collection; midnight and zone change invalidate snapshot.
- [ ] Switch to Demo/No records; no live reads or real values. Revoke a read permission: values disappear; other permitted metrics still load. Regrant and refresh.
- [ ] Compare daily steps, distance, active and total calories to Health Connect, not a sum of writer totals. Current day uses device midnight; history uses provider-local calendar buckets (travel may differ). Missing values stay unavailable, not zero.
- [ ] Latest heart-rate sample and other vitals show date/time and writer when present. Daily history uses latest observation, not average of samples; mean is across observed completed days only.
- [ ] Exercise duration is recorded session time, not Fitbit Active Zone Minutes.
- [ ] Dashboard has four selected slots; no historical data graph on home. Steps graph uses today's hourly aggregates; sleep graph uses exported stages only.
- [ ] Sleep and historical tabs render real sessions, gaps, unknown stages, dates, graph/text and 7/30 days. Session time is not time asleep. Assign completed sessions to wake date; choose longest nonoverlapping session when sources compete (not equivalent to platform writer priority).
- [ ] Check no sleep records case, explicit diagnostics and selectable 1/3/7/14/30 day coverage scan with UTC boundaries. Query failures distinct from denied/missing. Scan cap 500 records per type; live pages capped at 20 x 200 with partial status. Never create synthetic records on the primary device.
- [ ] Actual Fitbit sleep export remains OPEN: Google Health contains sleep, read/write permission and device clock verified, manual sync/reset did not resolve missing Health Connect category. Test synthetic sessions on isolated fixture/emulator; qualify actual device sleep only when exported.
- [ ] Quick/full quality preflight, Detekt/ktlint/Lint, unit tests, fresh genuine coverage >=80%, CI security/Android/SonarCloud, signed device testing. Public distribution remains held by issue #6.

All views are wellness information, not medical conclusions. Report actual omissions or errors before publication; do not claim missing measurements are synced.
