# M0 device acceptance checklist (all PENDING user hardware test)

## Install and startup

- [ ] `:app:assembleDebug`, `:core:test`, `:app:lintDebug`, Detekt and ktlint pass locally.
- [ ] Install APK on Titan 2. First-run welcome explains Health Connect diagnostic vs real import.
- [ ] Launch synthetic demo. All sample measurements are explicitly marked demo.
- [ ] Switch data mode to no records; no fabricated measurements remain visible.
- [ ] Force close and reopen: onboarding completed, appearance/profile/data-mode settings persist.

## Keyboard + touch

- [ ] Titan 2 I/J/K/L focus navigation works; Enter/Space activates selected controls.
- [ ] T moves across Today/Sleep/Move/Vitals/Trends; R opens Sources; Escape/Back returns.
- [ ] Holding Enter or R does not invoke repeated actions; Alt/Ctrl chords not stolen.
- [ ] All screens and settings accessible by both touch and keyboard; focused control visibly outlined in Sable Blue.
- [ ] Adjust Android system font scale; no inaccessible clipped action or obscured bottom bar.

## Sable theme

- [ ] Dark mode shows #000000 background, #141414 cards and #2E9BD6 accent/focus.
- [ ] Light mode shows #F5F7F9 background and #1A6FA0 accent/focus.
- [ ] System mode follows OS dark/light; settings persist across restart.

## Health Connect diagnostic

- [ ] Sources reports Available / Update required / Unavailable accurately.
- [ ] If available, granted-permission count is readable; M0 does **not** request permissions or read records.
- [ ] Denial/unavailability does not crash; no API keys/accounts/remote calls.

## Other profiles

- [ ] Q25: trackpad arrows and click work; compact square layout and text visible.
- [ ] P26: I/J/K/L basic keys work on real hardware; keyboard gestures are not promised.
- [ ] Other phone: touch navigation works; profile override is reversible.

**Do not paste any personal health values or full logcat into a public PR.** Use PASS / FAIL / NOT_TESTED for each item. CI cannot substitute physical validation.
