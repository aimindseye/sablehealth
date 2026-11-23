# R5.4 — Sources & Settings device acceptance

**Branch:** `feature/sablehealth-m0-foundation-20261123`  
**Review:** UI/UX R5.4 Sources & Settings v1 approved by owner (Q25 screenshot review).  
**Status:** PENDING physical testing on Q25/SableOS 2.0 and Titan 2. No health data read in M0.

## A. Sources overview (Q25 first)

- [ ] Open Sources from the compact header. `Sable Health`, back arrow, Sources, Settings remain visible and reachable together.
- [ ] Overview fits on standard Q25 font scale without vertical scrolling; accessible large fonts may scroll with all controls visible on focus.
- [ ] Health Connect card says AVAILABLE / update required / unavailable / diagnostic error accurately. Never claims imported health data.
- [ ] Google Health / Fitbit Air status initially says `Setup not confirmed in this app`, not that every user has a synced device.
- [ ] Open connection detail. Manually confirm **I paired and synced in Google Health**; return to Sources: label becomes `User-confirmed sync · export unverified`.
- [ ] Restart app. Manual confirmation persists. Toggle off again; status reverts to unconfirmed.
- [ ] Permission count describes permissions granted **to Sable Health**, not Google Health's permissions. No M0 permission request is initiated.
- [ ] `Records imported` is **None · M0**, even when the wearable has synced with Google Health. No fabricated health-record count.
- [ ] `Privacy & diagnostics` opens its secondary page; Back returns to Sources.
- [ ] `Check availability again` refreshes platform diagnostic, not wearable/Health Connect records.

## B. Settings overview

- [ ] Settings accessible from every tab; app title/back/Settings remain visible.
- [ ] Overview shows Dashboard widgets, Appearance, Data mode, Phone & keyboard, Privacy & about without the old long list of device choices.
- [ ] `Customize widgets` opens the existing picker; save/cancel/reset and ordering remain intact across process restart.
- [ ] System/Light/Dark selection changes and persists. Contrast, Sable Blue focus and label truncation remain readable.
- [ ] Demo/No records toggles only synthetic presentation; switching modes does not request health permissions or claim imported data.
- [ ] Phone & keyboard opens secondary page. Automatic detection and manual Q25/Titan2/P26 selection persist. Help opens and Back returns through the expected path.
- [ ] Privacy & about has distinct local-only, records, demo and M1 disclosures; Back returns to Settings.

## C. Interaction and regression

- [ ] Q25 trackpad/DPAD: can focus cards, segments, detail rows, back, and header controls; Enter selects once.
- [ ] Titan 2: I/J/K/L and arrow support, focus, Back and no unexpected compact Q25 styling.
- [ ] Touch targets >= 48dp, distinct Sable Blue focus, accessibility labels, font scaling and rotation retained.
- [ ] Main Dashboard, four-widget configuration, Sleep/Move/Vitals/Trends and semantic graph colors unchanged.
- [ ] No direct Fitbit BLE, telemetry, health-record imports, credentials, signer changes or network permissions introduced.

## Quality and release

- [ ] R5.4 source contract and JUnit tests pass.
- [ ] Latest Android assemble + lint + Detekt + ktlint pass.
- [ ] CodeQL pass. MobSF seven earlier findings reviewed and dispositioned; SonarCloud configured or explicitly documented as open.
- [ ] Verified Gradle wrapper files checked into feature branch; owner completes physical acceptance before any merge.

**Fitbit Air:** User reported Google Health + Google Fit installed and Fitbit Air paired/synced with Google Health on Q25. This is external user-reported setup, not a Sable Health M0 API verification.
