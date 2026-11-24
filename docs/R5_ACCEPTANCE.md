# R5 acceptance — Q25 compact, daily Today, semantic charts

Status: **REQUIRES CI + ON-DEVICE ACCEPTANCE**. All chart values are synthetic M0 demo fixtures.

## Approved R5 contract (owner revisions, 2026-11-23)

- Primary accent remains Sable Blue `#2E9BD6` in dark mode, light accent `#1A6FA0`.
- `Today` **does not** contain seven-day/historical graphs. It shows either:
  - `Text` (default): Steps, last-night sleep, resting HR, HRV in compact two-up cards, a short withheld-baseline line and source-status link.
  - `Graphs`: **today's steps vs chosen goal** and **last night's sleep-stage composition**. No resting-HR 7-day chart here.
- Mode selection is remembered independently for every primary tab; selecting Graphs in Move must not change Today.
- Sleep: nightly detail and composition; Move: daily values and 7-day steps; Vitals: daily readings and 7-day HR history; Trends: comparative historical charts.
- Q25: tighter header, section gaps and bottom tabs; no change to minimum 48dp tappable targets. Titan 2 keeps R4's roomier chrome.
- Q25 Move Graphs: two summary metrics + a **fully visible** compact steps chart; bottom navigation is structurally outside the scroll area. No graph content obscured permanently by bottom tabs.
- Charts use blue (informational), green (explicit **demo goal met**), amber (explicit **demo below plan**), and red (**synthetic example outlier**). **No real clinical inference or undocumented thresholds.** Labels and accessibility descriptions carry the meaning without relying on color.
- In no-record mode, no graphs or invented readings appear.

## Manual device acceptance

1. Q25/SableOS 2.0, 720x720/280 dpi: launch, confirm automatic Q25 identification. Compare header/nav height against R4 and ensure all targets remain clickable and trackpad-focusable.
2. Today/Text: confirm four values on two rows; no historical charts. Today/Graphs: confirm only today's progress and last-night composition and no 7-day plots. Both should be visible or scrollable without hidden portions.
3. Move/Graphs: confirm compact bar chart is entirely visible and all weekday labels can be read; navigate and scroll to the actual bottom if needed. No portion should be covered by tabs.
4. Vitals/Graphs and Trends/Graphs: see actual seven-day charts. Check blue/green/amber/red presentation, contrast and written legend.
5. Each primary tab: select Text, navigate elsewhere, return and verify selection persists; force-stop/reopen, verify it persists. Today must default to Text on a clean install.
6. Change Data mode to 'No records': no demo bars/values, no misleading zero or health-risk colors. Restore synthetic demo and verify each section.
7. Titan 2: confirm header/nav spacing remains R4-sized, two-up layout persists, keyboard I/J/K/L works, no new clipping. Q25 trackpad/DPAD, Enter/Space and Back remain functional.
8. Test system light/dark, increased font size, rotation, app resume and navigation via keyboard without touch. High font scaling can require scroll / one-column fallback.
9. Screenshots should not contain actual health measurements. The demo mode is intentionally marked as synthetic.

## Technical acceptance

- `:core:test`, `:app:assembleDebug`, `:app:lintDebug`, `detekt`, `ktlintCheck` all pass on latest commit; CodeQL and MobSF reviewed and passing or explicitly documented by policy with owner sign-off. Current R5 is **HOLD** pending those checks.
- Wrapper JAR, `gradlew` and `gradlew.bat` must be committed (after local hash verification) before merge. No release APK is signed by CI.
- R5 UI design visuals are concept references. Browser screenshot/preview is not proof of Android hardware behavior.
