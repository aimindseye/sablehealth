# Sable Health — release signing, GitHub Releases and quality gates

**Status: infrastructure candidate; no key provisioned or release authorized.**  
**Package:** `io.github.aimindseye.sablehealth` (owner reports registered in Android Developer Console).  
**Version currently configured:** `0.1.0-m0`, versionCode `1`; this is a prototype baseline, not a public-release recommendation.  
**Reference:** TenSeven Launcher, [Sable Games](https://github.com/aimindseye/sablegames/blob/main/tools/build-release.sh), and [Sable Watch](https://github.com/aimindseye/sable-watch/blob/main/tools/build-release.sh) interactive local signing patterns; Sonar workflow and AGENTS.md. Sable Health uses its **own** key, fingerprint and app identity.

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

## 2. Owner-only interactive signing

**Canonical command:**

```bash
bash tools/build-release.sh
```

There is **no `keystore.properties` file, environment password or Python release entrypoint** required. With a clean committed source checkout, pinned public registered certificate, Gradle 8.13 wrapper and Android SDK Build Tools 36.0.0, the script:

1. Runs the exact local quality preflight, builds the **unsigned** APK and verifies package/minSdk/version.
2. Prompts for **keystore location**, then **key alias**.
3. Prompts for the hidden **keystore password**, then hidden **key password** (press Enter if it is the same).
4. Signs with `apksigner`, passing passwords through **standard input only**, never in arguments, an environment variable, a settings file or CI.
5. Verifies the signed APK's v2/v3 signature, single signer, package, version and matching **registered public SHA-256 certificate**.
6. Writes to `~/Downloads/sablehealth-release/<version>/`: the APK, `.sha256`, `SOURCE_COMMIT.txt` and `REGISTERED_CERT_SHA256.txt`.

No keystore path, key alias, password or signed artifact is ever written into GitHub. The temporary signed file is removed after verification and copy. **This command never pushes or publishes a GitHub Release.**

### Registered certificate and shared-key option

Sable Watch's [releasing documentation](https://github.com/aimindseye/sable-watch/blob/main/docs/RELEASING.md) confirms that Sable Watch, Sable Games and TenSeven Launcher use a shared registered certificate. You **may use the same existing keystore and alias** for Sable Health **if its package registration explicitly uses that identical certificate**. We do not assume it does or pin another app's fingerprint automatically.

The one-line public `config/release-certificate.sha256` file must be committed before the script can sign. It is not a secret. If the private key doesn't match that digest, the APK is refused and no release asset is accepted.

**No keystore/password input is needed for ordinary Gradle builds or CI.** `./gradlew :app:assembleRelease` now produces only `app-release-unsigned.apk`; the signing step is restricted to the owner-run Bash script.

### Preparation and local checks

```bash
cd "$HOME/Documents/projects/sablehealth-m1b0"  # or an approved clean release worktree

python3 tools/quality_preflight.py --quick
python3 tools/quality_preflight.py

# Only after PR review, security/CI/Sonar and device acceptance, plus certificate registration:
bash tools/build-release.sh
```

The script deliberately refuses dirty/untracked source or an untracked certificate pin. Running it in an uncommitted ZIP handoff review worktree will produce **HOLD**, not a signed release. Commit and approve the release source first. Do not uninstall debug-signed Q25 builds to test release signing without a data migration plan.

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
