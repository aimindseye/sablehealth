# Sable Health — release signing, GitHub Releases and quality gates

**Status: infrastructure candidate; no key provisioned or release authorized.**  
**Package:** `io.github.aimindseye.sablehealth` (owner reports registered in Android Developer Console).  
**Version currently configured:** `0.1.0-m0`, versionCode `1`; this is a prototype baseline, not a public-release recommendation.  
**Reference:** TenSeven Launcher `tools/build-release.sh`, `tools/verify-release-apks.sh`, Gradle release signing, Sonar workflow and AGENTS.md. Sable Health uses its **own** key, fingerprint and app identity.

## 1. Prerequisites and certificate identity (owner action)

Android Developer Console package registration is not enough to infer which signing certificate was registered. Before creating a key, check whether the existing registration already specifies an eligible signing certificate **SHA-256**. If yes, reuse that exact private key. Do not generate a new key unless registration permits binding it.

If this package has **no existing release key**, on your Mac generate the keystore **once**, outside the repo, after deciding its long-term backup location. The example uses the default interactive keytool password prompt; never paste secrets into chat or CI:

```bash
mkdir -p "$HOME/.config/sablehealth/signing"
chmod 700 "$HOME/.config/sablehealth/signing"

keytool -genkeypair \
  -keystore "$HOME/.config/sablehealth/signing/SableHealth-release.jks" \
  -alias sablehealth \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -storetype PKCS12

keytool -list -v \
  -keystore "$HOME/.config/sablehealth/signing/SableHealth-release.jks" \
  -alias sablehealth
```

Read the **SHA256** certificate fingerprint from the keytool output and compare it against the certificate in Android Developer Console. Register/verify that **public certificate** for `io.github.aimindseye.sablehealth` using the developer console's supported process. If the console needs an ownership APK, create it only after the matching key is established; don't assume package name registration proves a chosen key matches. Never use TenSeven's certificate digest for Sable Health unless the console explicitly binds that same certificate.

After verifying the exact identity, add a **public one-line** `config/release-certificate.sha256` (64 lowercase hex characters, no colons), e.g. via your editor. It is expected to be **tracked and reviewed in Git**. **No such certificate fingerprint is pinned by this initial implementation**: signed-release promotion must HOLD until you supply it. Keep the private keystore and backups outside GitHub.

For a new app only (not an installed build's update) Android's debug key and owner's release key differ; `adb install -r` cannot normally update between differently signed APKs. Do not uninstall an existing test build or erase private health data without planning the migration.

## 2. Local signing configuration

Use **one** of these mechanisms; all four values are required.

**Option A: untracked local `keystore.properties` at repo root**, which the updated `.gitignore` excludes. Protect the file with `chmod 600 keystore.properties` and ensure Mac backups are encrypted. Its contents:

```properties
storeFile=/absolute/path/outside/repository/SableHealth-release.jks
storePassword=REPLACE_LOCALLY_NOT_IN_GIT
keyAlias=sablehealth
keyPassword=REPLACE_LOCALLY_NOT_IN_GIT
```

**Option B: owner-only environment** (inject from your Mac Keychain or secure password manager, not shell history):

- `SABLE_HEALTH_KEYSTORE` — path
- `SABLE_HEALTH_KEYSTORE_PASSWORD`
- `SABLE_HEALTH_KEY_ALIAS`
- `SABLE_HEALTH_KEY_PASSWORD`

Never store a keystore, store passwords, serialized credentials, or signing env variables in GitHub, Actions logs or PR artifacts. Sable's Gradle build intentionally produces **unsigned** release candidates when no signing inputs exist. Incomplete signing config is an error, not silent fallback.

## 3. Preflight and release build

```bash
cd "$HOME/Documents/projects/sablehealth-m1a"   # replace with your clean release-candidate worktree

python3 tools/quality_preflight.py --quick
python3 tools/quality_preflight.py

# Only after commits, local quality, CI, MobSF/CodeQL, Sonar, source review and Q25 acceptance:
python3 tools/build-release.py
```

`build-release.py` refuses dirty/untracked work, requires the committed public certificate pin, runs the full preflight again, requires Gradle's `verifyRegisteredReleaseKey` task, builds one signed release APK, and checks the result through `tools/verify-release-apk.py`.

It writes under `~/Downloads/sablehealth-release/<version>/`: signed APK, `.sha256`, `SOURCE_COMMIT.txt` and `REGISTERED_CERT_SHA256.txt`. It **never** publishes automatically. The signed verifier checks package ID, minSdk, version, v2/v3 signature, exactly one signer, and the pinned certificate.

Before publishing, verify a clean installation on a separate test device and update behavior on any existing release-signed installation. Review versionCode monotonicity and note that `0.1.0-m0` is a testing version.

## 4. GitHub Releases — manual only

Once owner-approved, use GitHub Releases UI or authenticated GitHub CLI. For example, for a **draft** release from the reviewed source commit:

```bash
# Example only, after replacing the version/tag with the approved release.
gh release create "v0.1.0-m0" \
  "$HOME/Downloads/sablehealth-release/0.1.0-m0/SableHealth-0.1.0-m0-vc1.apk" \
  --repo aimindseye/sablehealth \
  --draft --title "Sable Health 0.1.0-m0" \
  --notes "Prototype / device validation build. Not a medical device."
```

Do not run that example until the actual source tag/commit, certificate, binary checksum and release notes are verified. Do not `git add` APKs; use GitHub Release assets instead. Public signed releases remain blocked if the certificate identity or security gates are unresolved.

## 5. SonarCloud setup (owner action)

1. Create/import the `aimindseye/sablehealth` project in **SonarQube Cloud**, using **CI-based analysis**, and turn off **Automatic Analysis** if enabled.
2. Confirm that `sonar.projectKey=aimindseye_sablehealth` and `sonar.organization=aimindseye` in `sonar-project.properties` exactly match the project. Change them in a reviewed commit if Sonar generated different values.
3. In GitHub repo **Settings → Secrets and variables → Actions → Secrets**, add `SONAR_TOKEN` (Sonar token). Do not paste the token in chat.
4. In **Variables**, set existing `SONAR_ENABLED=true`. Until set, scanner job is intentionally **SKIPPED / NOT RUN**. A skipped Sonar workflow never satisfies a release quality gate.
5. Re-run the workflow on the intended PR. The scanner waits for Sonar's actual quality gate. Verify project binding/new-code policy/coverage and check that PR decoration works.
6. Do not change Sonar coverage/quality thresholds to force green. `core:jacocoTestReport` generates genuine core JVM coverage; Compose/Android UI needs separate physical/instrumented test work, not a blanket coverage exclusion.

The Sonar secret is used **only** in scanner/authentication steps. Fork-origin PRs do not receive it. The workflow handles both feature-to-feature stacked PRs and PRs to main when Sonar is enabled.

## 6. Consistent workflows and blocking statuses

| Check | When | Status semantics |
|---|---|---|
| Local `quality_preflight.py` | After last source edit, before every push | Required PASS before PR |
| Android quality | Every PR, push main | Required PASS: contracts, unit, JaCoCo, both builds/lints, Detekt, ktlint, unsigned identity |
| Security | Every PR, push main | CodeQL/Gitleaks + existing MobSF gate; preexisting failures remain HOLD until triaged |
| SonarCloud | Every PR when enabled | Until configured NOT RUN; afterward actual Sonar quality gate must PASS |
| Release identity | Owner's Mac only | Missing public cert pin, wrong key, unsigned APK or wrong version => fail closed |
| Physical Q25/Titan 2 | Features and release | Owner acceptance required; not implied by CI |
| GitHub Release | Owner manually, post approval | No automatic publish; verifies source SHA, cert and APK sha256 |

**No signing key was created, supplied, registered or installed by this PR.** The app remains development-only until owner validates release identity.
