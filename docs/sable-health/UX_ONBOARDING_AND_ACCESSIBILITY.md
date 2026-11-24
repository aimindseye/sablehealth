# Sable Health — first-run wizard and compact Sources/Settings design contract

**Stage:** Requirements only. No new design has been approved or applied beyond the existing R5.4 UI.

## Why R5.4 is not the final UX

Q25 physical 720×720 at density 280 ≈ 411dp × 411dp before safe insets. User accepted main R5.3 Dashboard visual direction and R5.4 Sources/Settings **overview**, but reported that their secondary pages were wordy and confusing on small screens; settings and source setup should become a guided process. A slab phone can display detailed explanations; Q25 should not require reading many cards or scrolling to find Back/Next.

**Guiding rule:** one decision or one status per screen; prioritize status and action, put details behind `Why?` or `Details`.

## Proposed one-time wizard

1. **Welcome** — explain local-first, wellness-only, supported modes; choose **Use my data** or **Explore demo**; never ask for cloud sign-in upfront.
2. **Find local data** — check Health Connect availability and surface platform-specific guidance. Label vendor companion detected vs platform available vs records verified separately.
3. **Grant selected permissions** — concise, reasoned user-triggered Health Connect per-metric READ request. Default reasonable minimum scopes (steps, sleep, heart rate; optional HRV/RHR etc. based on actual capability). Allow Skip, Retry and per-category controls; do not request WRITE in M1.
4. **Check actual records** — permitted read-only counts, origin app names and freshness/date ranges; progress, no-record and denied states. Report what is present, not what vendor marketing claims.
5. **Build my Dashboard** — choose 4 widgets, or accept suggested layout based only on verified records. Keep the existing four-slot persistence, save/reset/cancel/order/style.
6. **Finish** — clear summary: `3 sources available / 2 metrics have records` only if evidenced, explicit `Some data unavailable`, actions `Open Dashboard` and `Fix missing data`.

On subsequent launches: go directly to Dashboard. Reopen setup through Sources → Manage or Settings → Setup & permissions; never force wizard due to a temporarily missing source. Automatic retry cannot grant permissions by itself.

## Proposed navigation model

**Dashboard:** critical 4 widgets + optional explain/coverage, no duplicate config controls.

**Sources overview** (short):
- `Health data: Ready / Action needed / Not connected / Unsupported` with one primary action.
- `Last checked`, `Metrics with data` and `Source details` (when real), never a false "Fitbit connected" claim from user confirmation.
- `Manage sources` enters wizard steps; `Explain my sources` reveals provenance, priority and latency.
- Advanced view includes per-metric routes (Q25: Google Health → local HC; Galaxy: Samsung Health → local HC), authorization and freshness; no unbounded paragraphs.

**Settings overview** (short):
- `Dashboard widgets`
- `Appearance` (System/Light/Dark)
- `Data mode` (Real if M1 verified, Demo, No records; never ambiguously label this as sync on/off)
- `Keyboard & device` (Q25, Titan2, P26, auto detection)
- `Privacy & backup` (local default, optional later server)
- `Advanced / Help` nested. Preserve saved prefs and reduce content on first screen.

**Analytics Lab** (future): show result, short formula label, source, method, data window and `How calculated`. Can always show `Insufficient data`. No pseudo-medical colors.

## Compact UI hard requirements

- At least **48dp** actionable touch/focus targets with compact visual pills, not 36dp touch areas.
- Preserve full `Sable Health` title with bounded Sources/Settings actions, visible Back and keyboard/trackpad focus.
- Ensure Tab/DPAD, Enter, arrow keys and Titan2 I/J/K/L navigation do not double-activate; Back navigation is consistent, no off-screen Back button.
- Normal Q25 first-run step fits within safe 411dp-square bounds if practical; scrolling at large font scales must work and focus must bring active control into view.
- Shared Sable black/white/Sable Blue appearance, but contrast and selected state may not depend on color alone.
- Avoid oversized prose. Use one-line labels and explicit `Details` rather than squeezing explanations to unreadable type.
- On slab phones, responsive containers may show more description without changing the underlying consent or data-validation semantics.

## State/wording matrix

| System condition | Correct short label | Do not say |
|---|---|---|
| Health Connect API installed, no Sable grants | `Health Connect available · Access not granted` | `Fitbit connected` |
| Permission granted, no records | `Allowed · No records found` | `0 steps` |
| User marks companion synced but API unverified | `Sync reported · Export not checked` | `Records imported` |
| Some records have origins and dates | `Data available · Last record [date]` | `Live syncing` |
| Incomplete day or sparse samples | `Partial coverage` | Smoothed/full chart |
| Provider or SDK not supported | `Not supported on this phone` | `Connected` |
| Same HRV unit, incompatible method | `Different HRV methods` | Combined HRV average |
| Analytics inputs insufficient | `Not enough data yet` | `Normal recovery` |
| User denies health access | `Access not granted · You can still use demo` | `Error / Broken` |

## No invisible behavior

- Demo is always visibly synthetic and never mixed into real charts.
- Pressing `Check again` only performs the operation stated (SDK test in M0, local records test in M1), not BLE pairing or cloud sync.
- Permission requests happen on user action; provide the precise reason and data category.
- BYO-AI summary is review/copy/share only; no automatic transmission.
- Optional local-first homelab sync must show which metrics leave the device, retention, destination and how to disconnect/delete.

## UX acceptance test set (to approve after M1 proof)

- New install on Q25, no permissions: user can enter demo, navigate, retry, and later complete setup.
- Grant only steps: dashboard has real steps, other selected widgets correctly say no data; no synthetic values leak into real mode.
- Simulate stale HC data / revoked permissions: show one actionable status, not 10 diagnostic cards.
- Physical Q25 and Titan2 keyboard + focus + font scale; no truncated actions; settings persistence; Back returns to invoking screen.
- Dynamic source list from *observed* records and permissions rather than hard-coded "Fitbit" based on installed packages.
- Bypass M1 Google Health OAuth; only HC first. Any vendor API becomes a separate consent step.
