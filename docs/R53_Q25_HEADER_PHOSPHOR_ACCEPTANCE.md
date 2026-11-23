# R5.3 — Q25 header recovery + Sable Games/Phosphor icon system

Owner test input: Q25/SableOS 2.0 photo taken November 23, 2026. R5.2 shows an oversized **Sources** button filling the entire width; the Sable Health app name and Settings are hidden. Steps icon is not recognizable as footprints. User additionally requests same open-source icon family/style as Sable Games for Android launcher.

## Actual fix (not a spacing-only workaround)

`HealthHeader` now measures the **two Q25 actions at fixed 76dp each** inside a bounded full-width `Row`, giving the Sable Health title its weighted remainder. The underlying issue was `Q25ChromeButton`'s `fillMaxWidth()` child receiving unbounded width, which expanded Sources and crowded out the other controls. Q25 painted pills remain 36dp but their separate clickable areas stay >=48dp high. Settings/Sources are both on screen and keyboard-focusable. Q25 other pages also retain a recognizable **Sable Health** title, with their page names in the subtitle. Titan 2 styling and controls are unchanged.

Steps uses the **original MIT-licensed Phosphor regular `footprints`** glyph rather than bespoke ovals, within the same Sable Blue progress ring. Red heart widget remains as accepted in R5.2 (color is a category icon, not an alert). The launcher icon uses **Phosphor regular `heartbeat`** as a separate white-on-black health mark with Sable Blue underline: the same icon family/composition as Sable Games, but distinct from any Games app. Includes default/round adaptive launcher icons and monochrome themed layer. Phosphor attribution and unmodified MIT license are included.

## Q25 (priority)

- [ ] Dashboard header **shows all three**: Sable Health, Sources, Settings in one row. Sources is **not** full width; both buttons respond to touch and trackpad click
- [ ] Navigate to Settings and Sources; brand and page context remain identifiable; Back returns to Dashboard
- [ ] Q25 full-width mode, portrait, system font scaling and dark/light: no header overlap; at high font scale, labels may ellipsize but must remain accessible via TalkBack
- [ ] Steps inside ring: recognizable two-foot Phosphor outline, centered, no clipping of blue ring
- [ ] Bottom nav retains five tabs and at least 48dp tap targets; dashboard widgets/sleep chart/status not obscured
- [ ] Launcher and app switcher: Sable Health heartbeat icon on black with Sable Blue underline; round and themed monochrome variants apply when OS/launcher supports them
- [ ] No crash or Android resource resolution error; install over R5.2 without deleting widget configuration
- [ ] Demo/no-data mode and four-widget customization still persist; no real Fitbit values or fabricated zeros in M0

## Titan 2

- [ ] Header still normal-sized, branded and fully reachable; trackpad/keyboard I/J/K/L unaffected
- [ ] Step glyph and launcher icons render as above; spacing unchanged
- [ ] Settings/notification/navigation and existing graphs unaffected

## Fitbit Air setup / M1 preflight (no protocol claim in R5.3)

Fitbit Air arrived and a **Fit app is installed on Q25**. This alone does **not** confirm pairing, supported Android/Google Health version, or Health Connect record export. First pair/sync using the device's official companion, then inspect Health Connect data-source permissions and actual record categories. R5.3 is UI-only; app does not pair the Air directly or read records. Do not label heart rate live from historical samples. Never upload health screenshots or raw health records to public PRs.

## Local build

```sh
cd ~/Documents/projects/sablehealth
git switch feature/sablehealth-m0-foundation-20261123
git pull --ff-only origin feature/sablehealth-m0-foundation-20261123
./gradlew :core:test :app:assembleDebug :app:lintDebug detekt ktlintCheck
adb devices
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

PR #1 remains draft, and merges are owner-controlled after security, SonarCloud/wrapper, and physical acceptance checks.
