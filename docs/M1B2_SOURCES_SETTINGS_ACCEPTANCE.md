# M1-B2 — Sources and Settings acceptance

Parent: `7bcdaf8589401b128a5bb745e1b350f8c94ef8af` (merged PR #10).
This delivery boundary follows the owner's later roadmap: compact Sources and
Settings before live Dashboard metrics (M1-C). No live metrics or new permissions.

- Sources shows current platform/read access independently of companion confirmation.
- Explicit Refresh local records uses existing bounded M1-A metadata query; no auto sync.
- Scan metadata shared across wizard Verify, overview and detail; replaced when access
  changes or is rechecked (including resume), not persisted. A scan older than 24h
  is labelled stale when rendered; this is scan age, not a medical freshness rule.
- Coverage detail groups Activity/Sleep/Vitals, shows writer packages and last UTC,
  retains review-before-share, and uses the canonical setup entry for permission actions.
- Compact paired actions; existing header, themes, keyboard focus, >=48dp targets,
  widget picker, demo/empty mode, device profile and preference keys preserved.
- Isolated original M1-A proof-screen harness remains for prior tests; production
  routing exclusively uses the shared wizard-linked coverage page.

## Owner acceptance (Q25 and Titan 2)

1. Existing dashboard configuration and onboarding remain intact.
2. Sources shows actual allowed/support counts; unavailable/unknown never implies access.
3. Companion pairing confirmation is separate from record observation.
4. Refresh explicitly scans granted READ data; no permission means wizard, no fake zero.
5. Overview, Verify and details reflect shared results during the same session.
6. Record categories, writer package, last observed UTC and scan limitations are visible.
7. Recheck/resume/revocation clears previous snapshot and report preview.
8. Empty/error scans are actionable, no invented device identity or sync time.
9. Share available only after report preview; inspect metadata before sharing.
10. Settings setup routes, theme, demo/no records, widgets, profile and privacy still work.
11. Q25 compact overview, header, touch/DPAD/trackpad, Back, large fonts and rotation.
12. Titan 2 normal spacing, IJKL/trackpad, touch, Back and existing five tabs.

Full local preflight and changed-code estimate >=80% before any push;
85% preferred. Actual Sonar quality gate and Android/Security CI mandatory.
Existing MobSF issue #6 remains a public-distribution HOLD. No automatic signing,
installation, permission grant or merge. Mainline CI must be confirmed by owner.
