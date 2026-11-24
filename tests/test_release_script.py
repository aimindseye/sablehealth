"""Fail-closed checks for Sable Health's canonical Bash owner release workflow."""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
RELEASE_SCRIPT = ROOT / "tools/build-release.sh"
BUILD_GRADLE = ROOT / "app/build.gradle.kts"


class OwnerReleaseWorkflowTest(unittest.TestCase):
    def test_bash_syntax_is_valid(self):
        result = subprocess.run(
            ["bash", "-n", str(RELEASE_SCRIPT)],
            capture_output=True,
            text=True,
            check=False,
            timeout=10,
        )
        self.assertEqual(0, result.returncode, result.stderr)

    def test_noninteractive_signing_is_refused(self):
        result = subprocess.run(
            ["bash", str(RELEASE_SCRIPT)],
            input="",
            capture_output=True,
            text=True,
            check=False,
            timeout=10,
        )
        self.assertEqual(2, result.returncode)
        self.assertIn("Interactive terminal required", result.stderr)
        self.assertNotIn("PASS_SABLE_HEALTH_OWNER_SIGNED_RELEASE", result.stdout)

    def test_secrets_are_only_prompted_and_sent_to_apksigner_stdin(self):
        source = RELEASE_SCRIPT.read_text(encoding="utf-8")
        self.assertIn("Keystore location", source)
        self.assertIn("Key alias:", source)
        self.assertIn("read -r -s -p", source)
        self.assertIn("--ks-pass stdin", source)
        self.assertIn("--key-pass stdin", source)
        self.assertIn("unset store_pass key_pass", source)
        self.assertNotIn("--ks-pass pass:", source)
        self.assertNotIn("--key-pass pass:", source)
        self.assertNotIn("export store_pass", source)
        self.assertNotIn("env:SABLE_HEALTH_KEYSTORE_PASSWORD", source)

    def test_gradle_produces_unsigned_release_only(self):
        source = BUILD_GRADLE.read_text(encoding="utf-8")
        self.assertIn('applicationId = "io.github.aimindseye.sablehealth"', source)
        self.assertNotIn("signingConfigs", source)
        self.assertNotIn("keyPassword", source)
        self.assertNotIn("storePassword", source)


if __name__ == "__main__":
    unittest.main()
