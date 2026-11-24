# M1-B1-R2 — CI qualification

Parent: acd28d4e3ef76095b83c516e3c283289bf602f9e (owner accepted Q25/Titan 2).
Local PR coverage estimate: 81.90%; actual SonarCloud result remains unqualified.

Robolectric 4.16.1 resolves instrumented framework jars through its own Maven
fetcher during test execution. The Sonar job has repeatedly failed there
with HTTP 429 / artifact retrieval errors. Resolving the same pinned SDK
artifacts through Gradle makes them eligible for its existing dependency cache
and prevents runtime downloads from the test JVM. Missing jars fail closed.
API 30 and 34 are the existing test sandbox levels; API 36 provides target
framework resources. Versions are from the upstream 4.16.1 DefaultSdkProvider.
No SDK filter or test skip is introduced.

The scanner sees separate app and core modules. Preserve the existing source,
test and exclusion scope and the actual Sonar quality gate; specify absolute
JaCoCo paths at invocation, with module-specific paths, and retain local
relative defaults. Require nonzero executed coverage for the new wizard
sources and core SetupPlan in addition to the prior M1-A checks.

Acceptance:
- Prepare SDKs and run the full local preflight; every existing test must pass.
- Run fresh coverage for the entire PR against original main; estimate >=80%.
- Verify Android and security CI and fresh Sonar analysis for the exact head.
- Verify the scanner imported both reports, not merely their existence.
- Actual Sonar new-code coverage >=80%; all other existing conditions pass.
- Production Kotlin, manifest, resources, release dependencies and signing
  unchanged. Prior device acceptance applies to the unchanged application.
- Do not merge on stale or missing CI evidence. Issue #6 still holds public release.

R2 first run: the new Gradle preparation task failed closed on HTTP 429
from repo.maven.apache.org for the API 34 artifact. R2A restricts the Google
Cloud-hosted Maven Central mirror to the three pinned SDK artifact versions
in settings.gradle.kts. All other repository resolution is unchanged.
Reference: https://docs.cloud.google.com/docs/buildpacks/java#mirror_repositories_in_gradle
No checksum sidecars were independently retrieved in the author environment;
the existing Gradle HTTPS dependency trust model is retained. A mirror failure
must still fail the job; no skipped tests, fabricated reports or relaxed gate.
