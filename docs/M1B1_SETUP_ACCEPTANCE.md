# M1-B1 Setup Wizard acceptance

Candidate only until local tests, signed device checks and SonarCloud new-code coverage >=80% pass.

- First run: no automatic permission dialog; six stages in order; Back/Next/Skip and Finish work.
- Existing install: no forced wizard; four widgets, appearance, data mode and companion confirmation retained.
- Settings: Check permissions, Run complete setup, Choose setup steps; Sources uses the same wizard.
- Selected steps run in canonical order with mandatory Review; cancellation returns to the originating page.
- Recommended selects five core metrics; All supported excludes unavailable skin temperature; customize categories.
- Permission request checks current platform access immediately before launching and excludes already granted scopes.
- Deny all, allow some, grant remaining, revoke via Manage Android permissions, resume/recheck; no phantom access. Activity, Sleep and Vitals groups show actual grants.
- Optional Verify scan uses the unchanged bounded M1-A metadata-only reader, with writer and last observed UTC metadata. No inferred freshness threshold.
- Test Q25 and Titan 2 DPAD/trackpad/touch, focus, rotation, Android permission sheet recreation, small viewport.
- No manifest or dependency changes; versionCode 2 supports signed update with the same certificate.
- Personalization and companion confirmation are pending until Finish; cancel re-entry discards draft choices. Android permission grants are controlled by Android and cannot be rolled back by Cancel.
- No public release until inherited MobSF issue #6 is closed.
