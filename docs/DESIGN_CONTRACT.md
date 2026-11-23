# Sable Health — Approved UI/UX v1.0 with Sable Blue amendment

**Approval:** 2026-11-23. Screens and interaction contract approved. The sole requested change was to replace original teal/green aesthetics with established Sable theme and Sable Blue.

## Source of visual truth

`aimindseye/sablegames` `design/src/main/java/org/sableos/design/SableTheme.kt` and `SableSystemTokens.kt`.

| Role | Dark | Light |
|---|---|---|
| Accent/focus | `#2E9BD6` | `#1A6FA0` |
| Background | `#000000` | `#F5F7F9` |
| Surface | `#141414` | `#FFFFFF` |
| Raised surface | `#262626` | `#E9EDF2` |
| Text | `#FFFFFF` | `#12171C` |
| Secondary text | `#A6A6A6` | `#515B66` |

Use Source Sans 3 when installed; system sans-serif fallback. Corners 8/12/16 dp; touch targets at least 48 dp, focus outline 2 dp with sufficient contrast. Do not force dynamic material palettes to override Sable Blue. Follow system dark/light until user chooses otherwise.

## Information architecture

Today / Sleep / Move / Vitals / Trends are five permanent primary tabs. Sources and Settings are contextual. Baseline-withheld and missing-data states are explicit. Onboarding explains independent setup, demo and Health Connect diagnostic; M1 adds granular consent.

## Responsive profiles

- Q25: square window with trackpad/DPAD, two-up overview tiles only when the available dp supports them; accessible compact detail lists.
- Titan 2: square window with I/J/K/L, Enter/Space, and touch parity.
- P26: passport-style keyboard I/J/K/L defaults; do not claim keyboard touch gestures without validation.
- Other keyboard/touch devices: safe generic fallback and profile override.

The **window width and font scale** decide layout; device identity only selects default key bindings and hints.

## Interactions

- DPAD or I/J/K/L (for letter-navigation profiles): move focus.
- Enter/trackpad click/Space: activate the focused button.
- Android Back/Escape: return, preserving app context where practical.
- `T`: next main tab; `R`: refresh diagnostics; `?`: help.
- Never intercept modifiers, native text editing, accessibility commands. Repeated confirmation/refresh must not execute repeatedly.
- Keyboard/touch parity; no inaccessible off-screen focus. Honor user's system font scale/reduced motion.

## Data quality contract

Missing never becomes zero. Distinguish observed time from import time, consent denial, unavailable source, partial readings, staleness, synthetic demo and insufficient-baseline states. M0 uses demo readings only; M1+ must implement provenance and source reconciliation before derived scores.

## Implementation review matrix

- [x] Sable Blue palette and Sable surfaces are defined as code constants.
- [x] Five primary screens plus Sources/Settings/Help present.
- [x] Hardware input profiles implemented with deterministic tests.
- [x] M0 demo explicitly labeled and no live score fabricated.
- [ ] Physical Q25 trackpad validation — user test.
- [ ] Physical Titan 2 validation — user test.
- [ ] Physical P26 validation — user test.
- [ ] Full Android build and CI — pending access to SDK / GitHub workflow run.
- [ ] Health Connect M1 permission and data validation — out of scope for M0.
