# M1-B1 — re-run Setup from Settings and verify Health Connect permissions

**Status:** Owner-requested requirement, not implemented by the release-engineering PR. Track code with [Issue #4](https://github.com/aimindseye/sablehealth/issues/4) and review the [M1-B design spec](https://github.com/aimindseye/sablehealth/blob/docs/sable-health-research-architecture-20261123/docs/sable-health/M1B_SETUP_SOURCES_SETTINGS_DESIGN.md).

## Three direct entries from Settings → Setup & Permissions

1. **Check permissions:** show what Health Connect **actually grants now**, grouping Activity / Sleep / Vitals; show `Allowed`, `Not granted`, `No records`, `Stale`, and an explicit **Manage Android permissions** action. A user can request missing READ scopes individually, or select `All supported READ` with one Android consent action. Re-evaluate grants on return and on app resume.
2. **Run complete setup:** reopen the six-step wizard (Welcome, Sources, Read permissions, Verify, Personalize, Review) with existing preferences pre-filled. **Do not** silently reset settings, widget choices, consent, or previous screen.
3. **Choose setup steps:** multi-select any **one or more** of those six stages, execute only selected stages in canonical order, with Back/Next/Skip; Review & Finish appears at the end even when not selected so changes are explicit. Examples: `Permissions + Verify`, `Sources only`, `Personalize only`. Existing setup-completed marker remains true, even if a selected stage is skipped.

Additional **Sources → Manage** deep link starts the same `Permissions + Verify` flow, not a duplicate permission engine.

## Required state and persistence

- Wizard entry modes: `FIRST_RUN`, `REVIEW_ALL`, `SELECTED_STEPS`, and `PERMISSIONS_ONLY`.
- Stage IDs/ordering are stable; saved selections survive Android system permission sheet, rotation and Activity recreation. Source of truth for permission status is Health Connect's `getGrantedPermissions()`, **never** the wizard's saved preferences.
- First-run wizard is shown only when no existing `onboarded` marker; preserve R5.4 M0 installed user settings and all four dashboard widgets on upgrade. User may safely reopen setup on demand.
- `Check permissions` does not trigger a scan or permission request automatically. Every permission grant is a user action. Display real record coverage separately.
- Dynamic metric catalog is aligned with implemented records: Steps, HR, Sleep, RHR, Exercise, optional HRV/SpO2/etc.; Distance and Total Calories after record support is actually added. READ only: no write/delete/background/extended-history requests.
- **No phantom completion:** a user can Skip and finish, but missing permissions/data still appear as actionable warnings, never a green Connected status.
- UI: Q25 411dp viewport, scrollable content, persistent footer, >=48dp targets, keyboard/trackpad focus and Titan 2 navigation. Back from selected-steps flow returns to Settings, not forced first-run state.
- Privacy: no raw metric values in setup logs, PRs, diagnostic exports or state snapshots.

## Tests required

- First install → wizard; existing install → dashboard unaffected; Settings reentry all and selected stages.
- Single-step and multi-step selection order; skip/back/next; rotation/permission sheet return; permission deny/revoke and requery.
- Shared Sources/Settings permission state; no duplicate permission dialogs, no auto grant or WRITE.
- Preserve widget order, theme, demo/no-data and device profiles after rerun.
- Q25/Titan2 acceptance, static contract, JUnit/UI tests, Android Lint, Detekt, ktlint, security and Sonar.
