# Sable Health — package identity and local release signing

Android application ID to register: **`io.github.aimindseye.sablehealth`**.

Signing policy: use the existing Sable Games / TenSeven **release** certificate, fingerprint:

```text
9b2047390e5aa77537b8e8875cdfd3c2c72d0a0715e172a7f77457fc4300f292
```

This identity comes from `aimindseye/sablegames/docs/RELEASING.md` (reviewed 2026-11-23). Do not create a new key and do not copy any keystore into the repository, CI, a ZIP, chat, or an APK asset. Reusing the key is an explicit owner decision and expands the blast radius of compromise: guard the shared private key and offline backups accordingly.

## M0 development

`./gradlew :app:assembleDebug` produces a **debug-key-signed** APK for Q25/Titan 2 testing. That is distinct from release signing and does **not** need the registered certificate. Do not treat an installed debug APK as a release-signature qualification.

## Future local production release

Use the existing Sable Games keystore and alias **locally**. Build the unsigned release APK, run Android SDK Build Tools `apksigner sign` using the owner's keystore/alias with password provided by prompt/environment only, then run:

```bash
bash tools/verify-release-apk.sh /absolute/path/to/sablehealth-release-signed.apk
```

The verifier requires the exact package ID and registered signing fingerprint. It will fail closed on mismatches or missing tools. Never publish the unsigned APK. The release-signing implementation and independent qualification remain part of the later release milestone, not M0 acceptance.
