# Sable Health R5.4 — Sources & Settings UI/UX proposal (approved for R5.4 implementation)

**Reference:** Four Q25 photographs supplied on 2026-11-23, after the owner accepted R5.3 Dashboard and other main screens. **R5.4 approved design reference. Implementation tracked separately; see R54_SOURCES_SETTINGS_ACCEPTANCE.md.**

## 1. Findings from the Q25 screenshots

- The R5.3 header with **Sable Health**, **Sources** and **Settings** is now present and should be preserved.
- Sources currently displays Health Connect availability, 0 health metric permissions, no imported records and privacy as individually tall light-gray notice cards. The `Check again` and `Back` buttons are pushed below the fold.
- Settings currently displays an oversized Dashboard widget shortcut, an explanatory data-status card, many device-profile buttons, appearance/data-mode options, and unrelated status notices in one long scroll.
- The data displayed by M0 is diagnostic: Health Connect available; metric permissions not requested; zero imported records because M0 does not query any health measurements. This is *not* evidence that the Fitbit Air failed to sync.

## 2. Shared design contract

- **Sable Games theme:** dark black background, dark gray surfaces, Sable Blue `#2E9BD6`; light mode `#F5F7F9` with `#1A6FA0` accent; system theme default.
- Same calm, compact card language as the approved Dashboard, compact Q25 spacing, recognizable **Sable Health** title at the top and both sources/settings actions bounded to their own targets.
- Dedicated back arrow next to the app name on Sources, Settings and their subpages. Android Back / Q25 hardware Back and keyboard navigation also work. Returning to Dashboard must be possible without finding an off-screen Back button.
- Main Sources and Settings *overview* should fit within a 411dp × 411dp Q25 review viewport at normal font scale, without scrolling in the normal state. Details may scroll naturally. Large font/accessibility modes must allow scrolling without clipping.
- Render compact visual surfaces inside **at least 48dp touch and focus targets in production**, maintain visible Sable Blue focus, full keyboard / trackpad / touch parity, and meaningful TalkBack labels. Concept HTML is a presentation prototype, not verified Android implementation of these requirements.
- Use restrained green for available/confirmed status; amber for unverified export; reserve red for independently justified alerts, not for normal measurements. Never rely on color alone.

## 3. Sources overview

**Title:** `Data connections` · `M0 diagnostic` badge.

1. **Health Connect** compact connection card: `Available`, followed by `Read access not requested · M0`. Tapping opens a short details page.
2. **Google Health · Fitbit Air** connection card: `Paired & synced · user confirmed`; supporting note `Health Connect export not verified`. Do *not* claim a direct Sable Health → Fitbit Air BLE connection, that M0 read Fitbit metrics, or that the official companion's sync time is known to Sable Health.
3. Two compact, side-by-side readouts: `Permissions — 0 granted` and `Records imported — None`. These refer to Sable Health M0 read permissions/imports and are not health metric zero values.
4. **Privacy & diagnostics** single collapsed row; details provide explicit local-only/no telemetry/no account/no direct Fitbit Bluetooth behavior.
5. **Check availability again** control; in M0 this reruns platform availability diagnostics only, not a health-record synchronization/import. Display accessible feedback for success, unsupported platform, or error.

**Source detail (secondary screen):** platform status, permissions, imports, what the app can and cannot verify, and the future M1 read-only permission/data provenance path. No promise of Google-only Fitbit metrics being exported through Health Connect.

### Sources state handling

- Available / update required / unavailable / error / checking for Health Connect platform state.
- Permission count unknown / not requested / granted / denied, independent from platform availability.
- Import count None (never imported) / known count with freshness, independent from Google's own Fitbit sync status.
- If the user reports paired and synced, the UI may display **user-confirmed** setup progress, never imply the app verified Bluetooth or Google Health records.

## 4. Settings overview

**Title:** `Customize your experience` · `Local` badge.

1. **Dashboard:** one row, `Customize widgets · 4 selected`; opens existing four-widget picker. Preserve all widget choices, order, display style, save/reset/cancel and local persistence.
2. **Appearance:** a single segmented row `System / Light / Dark`; system is default. Persist current choice.
3. **Data mode:** one segmented row `Demo / No records`. This changes only the M0 presentation and must never masquerade as an actual synced-data on/off toggle.
4. **Phone, input & privacy:** one compact row with current Q25 profile summary. Opens secondary settings; avoids listing every device profile and informational notice on the first screen.

**Phone & input (secondary screen):** detected vs chosen profile; automatic detection; Zinwa Q25, Titan 2, P26 (and full supported profile set in production); keyboard/trackpad help. Custom remapping remains explicitly `Planned` until implemented.

**Privacy & about (secondary screen):** M0 local-only/privacy explanation and clear data-access distinctions. Keep future permissions under Sources once M1 is implemented, not hidden behind an unrelated setting.

## 5. Fitbit Air readiness boundary

As of the user's message: **Google Fit and Google Health are installed on Q25; Fitbit Air is paired/synced to Google Health, per user confirmation.** Sable Health's M0 Sources page only diagnoses availability, not records. This design therefore differentiates:

1. User-reported paired/synced in the official companion.
2. Health Connect platform availability.
3. Permission to read specific Health Connect record types.
4. Actual imported records, source provenance, and timestamps in Sable Health.

**M1, separate milestone:** Health Connect read-only permissions, record coverage, late sync handling, records provenance, and real-data rendering. No fabricated readings or arbitrary clinical status colors.

## 6. Acceptance criteria

- [ ] On Q25, brand/header + Sources + Settings and Back are visible and reachable simultaneously.
- [ ] Both overview screens show essential information without vertical scrolling at normal Q25 font scale. At larger font scale, controls scroll/focus into view and nothing is clipped.
- [ ] Health Connect availability distinct from Fitbit Air syncing and Sable Health permission/import state.
- [ ] An unverified export never appears as `Connected to Sable Health`.
- [ ] Existing M0 read-only diagnostic and privacy contract preserved.
- [ ] Widgets/settings preserve values after restart and have safe back/restore behavior.
- [ ] Correct Q25 DPAD/trackpad, Titan 2 IJKL keyboard, Enter, Back, touch, TalkBack and focus handling.
- [ ] Existing Dashboard, Sleep, Move, Vitals, Trends, semantic colors and Sable Blue branding unaffected.
- [ ] Existing static/CI/security quality gates remain enforced; UI redesign does not waive MobSF concerns.
- [ ] Test on Titan 2 with normal spacing; Q25-specific density must not shrink Titan 2 unnecessarily.

## 7. Review artifacts

- `preview.html`: interactive design illustration (Sources, Settings, Health Connect detail, Phone & keyboard, Dashboard widgets, Privacy). Tabs and several controls are demonstrations only, not integrated with Android or Health Connect.
- `mockup_sources.png`, `mockup_settings.png`: exact browser-rendered 411dp-square concept overview screens.
- `mockup_source_detail.png`, `mockup_device.png`, `mockup_widgets.png`, `mockup_privacy.png`: proposed secondary layouts.
- `mockup_sources_light.png` and `mockup_settings_light.png`: Sable light-theme variants.
- `Sable_Health_Sources_Settings_Design_v1.png`: contact sheet of the six dark-mode concepts.
- `concept_direction.png`: artistic two-screen visual direction, secondary to the precise labeled screenshots.

## 8. Decision

**Approved by owner.** Screens implemented in R5.4; physical acceptance and security/CI gates remain separate.
