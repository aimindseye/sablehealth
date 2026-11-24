# M1-B design proposal — first-run setup, Sources and Settings 2.0

**Design status:** PROPOSED for owner approval; **not implemented** in this documentation PR.  
**Research baseline:** 2026-11-23.  
**Code implementation baseline:** M1-A draft [PR #3](https://github.com/aimindseye/sablehealth/pull/3), whose head was `26ffa516cee9302c0692f223583eda1a047f8b19` at review.  
**TenSeven source inspected:** [TenSeven Launcher](https://github.com/aimindseye/tenseven-launcher/tree/fbe341ecaf472f88fbc5cecace3d170a5685ec06) commit `fbe341ecaf472f88fbc5cecace3d170a5685ec06`.  
**Scope:** replace the simplistic two-button onboarding with a real wizard; update Sources/Settings to show permissioned source-aware records; stage live data display after verified queries. This proposal intentionally does not copy TenSeven source or permissions verbatim.

## 1. Direct code observations from TenSeven

The following are **verified repository design patterns**:

- [OnboardingActivity.kt](https://github.com/aimindseye/tenseven-launcher/blob/fbe341ecaf472f88fbc5cecace3d170a5685ec06/app/src/main/java/io/github/aimindseye/tenseven/OnboardingActivity.kt) uses user-triggered permission/Settings requests, displays a rationale for each, refreshes access status on Activity resume, supports individual step links and saves first-run choices. Its current no-extra-step entry redirects to the categorized Launcher Settings permissions flow; **do not assume the whole legacy class is always shown as a wizard**.
- [Setup2Plan.kt](https://github.com/aimindseye/tenseven-launcher/blob/fbe341ecaf472f88fbc5cecace3d170a5685ec06/app/src/main/java/io/github/aimindseye/tenseven/onboarding/Setup2Plan.kt) provides a pure seven-stage mapping with Review; [OnboardingPlan.kt](https://github.com/aimindseye/tenseven-launcher/blob/fbe341ecaf472f88fbc5cecace3d170a5685ec06/app/src/main/java/io/github/aimindseye/tenseven/onboarding/OnboardingPlan.kt) separates what is granted from user configuration and allows skipping optional steps.
- [LauncherSettingsActivity.kt](https://github.com/aimindseye/tenseven-launcher/blob/fbe341ecaf472f88fbc5cecace3d170a5685ec06/app/src/main/java/io/github/aimindseye/tenseven/LauncherSettingsActivity.kt) uses categorized settings, a persistent page destination across permissions activity pause/resume, Back routing, **scrollable content with a non-scrolling footer**, and rechecks permission state on resume.
- [Settings2Flow.kt](https://github.com/aimindseye/tenseven-launcher/blob/fbe341ecaf472f88fbc5cecace3d170a5685ec06/app/src/main/java/io/github/aimindseye/tenseven/model/Settings2Flow.kt) centralizes page order/previous/next; [CompanionWizardActivity.kt](https://github.com/aimindseye/tenseven-launcher/blob/fbe341ecaf472f88fbc5cecace3d170a5685ec06/app/src/main/java/io/github/aimindseye/tenseven/CompanionWizardActivity.kt) uses saved state, preset/custom/review pages, and Save & finish.
- [D1 acceptance](https://github.com/aimindseye/tenseven-launcher/blob/fbe341ecaf472f88fbc5cecace3d170a5685ec06/docs/tenseven-2/D1_SETUP_SETTINGS_ACCEPTANCE.md) requires selections and existing behavior preserved after update, Back/Next behavior, deep-linked permission help, physical keyboard tests and no fake unsupported features.

**Adaptation to Sable Health:** use a single shared source/permission state service to drive wizard, Sources, and Settings rather than presenting three contradictory summaries. Unlike TenSeven's launcher-role case, **no Health Connect permission is technically required to start Sable Health**; every consent step is optional and the user can enter Demo or No Records mode.

## 2. Six-stage initial setup wizard

A fresh install opens this before Dashboard; existing users' `onboarded=true` preference **is preserved** on upgrade, so never unexpectedly force them back into setup. Offer a prominent **Complete health setup** entry in Sources/Settings if their health permissions or coverage are incomplete.

| Step | Name | Primary interaction | Real completion state |
|---|---|---|---|
| 1 | Welcome | **Use my health data** (preferred), **Try demo**, **Not now** | Chosen presentation preference only; no implied authorization |
| 2 | Connect sources | Health Connect availability, on-device route, instructions for Google Health/Fitbit, **Open Health Connect** / **Open companion** only if supported | Platform status, not an invented pairing result |
| 3 | Read permissions | Preset **Recommended**, **All supported read metrics**, **Choose individually**; open standard Android Health Connect request UI **only after tap** | Re-read actual granted set when returning; permit denial/partial |
| 4 | Verify data | **Scan now**; short results for Steps/HR/Sleep and number of authorized categories, origin and most recent record; **Manage write/export in companion** link if empty | `Allowed + records` / `Allowed + none` / `Not granted` / `Unavailable` / `Error`; never fake a metric zero |
| 5 | Personalize | Theme System/Light/Dark; Q25/keyboard auto detection; choose four Dashboard widgets with existing controls; Demo/Real/No Records clearly distinguished | Persist existing widget/appearance/profile preferences, no parallel keys |
| 6 | Review & finish | Concise summary: permission categories granted; categories with recent entries; source writer(s); optional unresolved actions; **Save & open Dashboard** | Commit onboarding completion **only when explicitly finished** |

**Navigation:** `Back`, `Next` and `Skip for now` anchored beneath scrollable content. Step counter (e.g. `3 / 6`) and progress accessible by text. Hardware Back returns to previous step; resume after Android system permission sheet without losing wizard progress, chosen preset, or current step. Rotation/process recreation should retain wizard page and selected metrics as far as platform saved state allows. Do not request anything without a user action or mark an unfinished wizard completed on Back.

**Recommended READ preset:** Steps, Heart Rate, Sleep, Resting Heart Rate and Exercise. HRV and other optional types can be offered based on planned features.  
**All supported READ preset:** every currently implemented/declared Sable Health metric, with explicit category list and one standard Health Connect request; no blanket request for irrelevant medical records.  
**Custom:** per-category selections and granular purpose text, preserved until next visit. Granting one type never silently broadens to other categories. On Android, manifest declaration alone never grants access.

**Permission catalog change:** M1-A currently declares 11 read types; when the corresponding current features are implemented, add `READ_DISTANCE` and `READ_TOTAL_CALORIES_BURNED` (distance/calories were visible in the companion but not exposed by M1-A). Do not label Active Calories and Total Calories as interchangeable. Additional types (weight/height/BMR/floors) are candidates requiring justified features and updated manifest/review, *not* automatic permission requests. No WRITE, DELETE, background reads or extended-history access in this onboarding milestone.

## 3. Updated Sources overview (Q25-first)

The compact overview should fit a 411dp × 411dp Q25 viewport at default font setting with the existing **Sable Health / Sources / Settings** header.

```text
  Data sources                                  [Manage]
  Health Connect         Ready · Read permitted
  Fitbit (via HC)        Records verified · Last ...
  [Permissions 5/11]     [Metrics with records 2]
  [Refresh local records]  [Help with sync]
```

- Show verified writer app package(s) as **source app(s)**, not necessarily physical hardware identity. `com.fitbit.FitbitMobile` indicates Fitbit wrote a record; it does not prove which Fitbit device produced that record.
- **Refresh local records** is a permissioned local query of Health Connect, **not** a button that forces Google Health/Fitbit sync. Separate troubleshooting action opens the official companion/export settings if supported.
- Counts/coverage shown in source detail are metadata only, distinguish records from samples, avoid displaying all 11 types on root. A per-type detail page groups **Activity, Sleep, Vitals**, shows granted status, observed/empty, writer app, last UTC and scan limitations.
- Remove all outdated R5.4 wording (`M0 DIAGNOSTIC`, `Read access not requested`, `Records imported: None` when actual reads occur). Be precise: scanned records != durably imported measurements.
- Permissions control launches the **same** wizard's permissions page; never maintain a separate conflicting permission list.
- Badge text and colors must not indicate success when there are zero records or the last check is stale; show actionable **Action needed** instead.
- If Google Health has dashboard data but HC has none: show `Check companion export/write permissions`—Sable cannot grant another application's WRITE permissions itself.

## 4. Updated Settings overview

```text
  Settings
  Data & permissions     Health Connect · 5/11 granted
  Dashboard widgets      4 chosen
  Appearance             System / Light / Dark
  Phone & keyboard       Zinwa Q25 · Auto
  Privacy & backup       Local only
  Setup wizard           Resume or review
```

- Category-based, compact, not a long vertical information dump; keep the R5.4 approved Sable Blue/gray visual language and >=48dp interactive targets.
- Data & permissions summary links to the **same** source/permission service as Sources; explicit `Review permissions`, `Request missing permissions`, `Recheck records`, `Open Android Health Connect`.
- **Data display mode:** Real (only when actual measurement ingestion is implemented) / Demo (visibly synthetic) / No Records. Do not label M1-A metadata-only scan as live Dashboard ingestion. Never silently turn Demo into Real during setup before M1-B data display exists.
- Dashboard widget order/style, theme, keyboard profile and existing SharedPreferences keys survive upgrade and re-entering setup.
- Privacy details: no network, no automatic sharing, health reads on device, original companion writes, report review before sharing, precise deletion/export scope; no fictional cloud backup.
- Settings subpages have visible Back/Next only when following setup flow; ordinary Settings navigation has Back to Settings and does not unexpectedly advance steps.

## 5. Shared implementation contracts

```text
OnboardingSetupPlan (pure Kotlin + tests)
  WizardStep, preset selection, allowed skip/review, progress, persisted status
HealthConnectPermissionRepository
  SDK availability -> grantedPermissionSet -> delta of missing scopes
HealthConnectCoverageRepository (reuse/extend M1HealthConnectProbe)
  bounded local query -> type-specific status, count, writer origin, freshness
SetupUiState / SourceUiState (single state owner)
  used by first-run Wizard, Sources detail and Settings data-permissions category
LocalPreferenceRepository (existing keys)
  onboarded, demo, appearance, widgets, profile + wizard resume marker
```

- Do **not** implement a separate permission store claiming a grant; always query Android. Refresh after permission UI returns and when the app resumes. A revoked grant never remains displayed as ready.
- Use the current `PermissionController.createRequestPermissionResultContract()` on a direct tap; show reviewable category purpose before invoking Android consent.
- No personal measurements in logs, PR artifacts or screenshot fixtures. The optional coverage report remains explicitly review-before-share and metadata-only.
- Sable Health can query Health Connect **locally** when opening/resuming the app; that does not control Fitbit/Google Health's separate export schedule.
- All source labels: **Installed / Health Connect available / Read permission granted / Records present / Fresh / Analytics ready** must remain separate states.
- Avoid duplicate record reads from multiple observers on every recomposition; refresh on explicit action and deliberate lifecycle boundaries only.
- Do not copy TenSeven's launcher-specific special-access/notification permissions; Health Connect's own Android permission flow applies.

## 6. Acceptance matrix for the first implementation

1. **Fresh Q25 install** shows wizard only once; no permission dialog before explicit tap. Full preset opens Android's Health Connect **READ** selection, not WRITE. Skip proceeds to a usable no-data/demo experience.
2. **Existing Q25 install** preserves R5.4 settings, widgets and `onboarded=true`; can launch Setup from Settings/Sources without uninstall/clear-data.
3. **Return from Android Health Connect:** step index, selected categories and theme remain stable, permission statuses recheck accurately. Deny/partial/revoke all work; no false "connected".
4. **Verified Fitbit write:** sources lists writer as `com.fitbit.FitbitMobile` (when observed) and data-present categories; no assumption the Fitbit Air supplies sleep/HRV/RHR without actual records.
5. **No records:** show export/write-access troubleshooting instructions rather than "0 steps." Distinguish error, stale, not granted and empty.
6. **Q25 720×720 / ~411dp** no clipped header/footer; Back/Next fixed within safe insets; scrollable content and keyboard focus bring controls onscreen. Titan 2 touch, trackpad, D-pad/IJKL and large-font/rotation also pass.
7. **Regression:** Dashboard/Widgets/5 tabs, themes, device profile, 48dp targets, synthetic mode and legacy preferences unchanged until live metrics are independently tested.
8. **Quality:** add pure setup-plan JUnit tests and instrumentation UI navigation/permission state tests; M0–R5.4 contracts, M1-A read-only guard, Android Lint, Detekt, ktlint, CI, privacy/security review all green with **no suppressions**.
9. **Release gate:** draft stacked PR, owner local build/Q25 acceptance, owner-only merge. Existing inherited Security CI HOLD remains separate.

## 7. Milestone sequencing

- **M1-B1: Setup + Sources/Settings integration** — canonical permissions and coverage, wizard, compact redesigned pages, restore/review/retry paths. Live Dashboard still off until secure ingestion contract exists.
- **M1-B2: Live dashboard metrics** — import/render permitted Steps and Heart Rate with timestamps/source provenance and correct Android aggregation; no synthetic overlap. Then expand to Sleep, Distance and calories only where actual records exist.
- **M2+:** reconciliation and analytics only once real source data is proven.

The two stages are **delivery boundaries**, not two different onboarding experiences. The end state is one polished Sable Health first-run experience, one permission management point and real-data Dashboard; no additional competing Settings interface.
