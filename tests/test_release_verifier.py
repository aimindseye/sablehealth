#!/usr/bin/env python3
"""Offline, fake-Android-SDK tests for fail-closed APK identity and certificate checks."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
VERIFIER = ROOT / "tools/verify-release-apk.py"
CERT = "a" * 64

AAPT = """#!/usr/bin/env python3
import os
print("package: name='" + os.getenv("TEST_PACKAGE", "io.github.aimindseye.sablehealth") +
      "' versionCode='" + os.getenv("TEST_CODE", "1") +
      "' versionName='" + os.getenv("TEST_NAME", "0.1.0-m0") + "'")
print(os.getenv("TEST_SDK_LABEL", "sdkVersion") + ":'" + os.getenv("TEST_SDK", "30") + "'")
"""
SIGN = """#!/usr/bin/env python3
import os, sys
if os.getenv("TEST_CORRUPT") == "1":
    sys.exit(4)
print("Verifies")
print("Verified using v2 scheme (APK Signature Scheme v2): " +
      ("false" if os.getenv("TEST_SCHEME") == "v1" else "true"))
print("Verified using v3 scheme (APK Signature Scheme v3): false")
print("Number of signers: " + os.getenv("TEST_SIGNERS", "1"))
print(os.getenv("TEST_LABEL", "Signer #1") + " certificate SHA-256 digest: " +
      os.getenv("TEST_CERT", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
"""


class VerifyReleaseApkTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        tools = self.root / "build-tools/36.0.0"
        tools.mkdir(parents=True)
        for name, content in (("aapt2", AAPT), ("apksigner", SIGN)):
            path = tools / name
            path.write_text(content, encoding="utf-8")
            path.chmod(0o755)
        self.apk = self.root / "SableHealth-release.apk"
        self.apk.write_bytes(b"synthetic, never distribute")
        self.cert = self.root / "registered.sha256"
        self.cert.write_text(CERT + "\n", encoding="utf-8")

    def run_verifier(self, *, signed=True, **environment):
        apk = self.apk
        args = [sys.executable, str(VERIFIER)]
        if not signed:
            apk = self.root / "app-release-unsigned.apk"
            apk.write_bytes(b"synthetic unsigned, never distribute")
            args += ["--ci-unsigned"]
        args += ["--certificate", str(self.cert), str(apk)]
        env = {**os.environ, "ANDROID_HOME": str(self.root), **environment}
        return subprocess.run(args, env=env, capture_output=True, text=True, timeout=30)

    def test_registered_single_signer(self):
        self.assertEqual(0, self.run_verifier().returncode)

    def test_signed_rejects_different_key(self):
        self.assertNotEqual(0, self.run_verifier(TEST_CERT="b" * 64).returncode)

    def test_signed_rejects_multiple_signers(self):
        self.assertNotEqual(0, self.run_verifier(TEST_SIGNERS="2").returncode)

    def test_signed_rejects_v1_only(self):
        self.assertNotEqual(0, self.run_verifier(TEST_SCHEME="v1").returncode)

    def test_signed_rejects_tampering(self):
        self.assertNotEqual(0, self.run_verifier(TEST_CORRUPT="1").returncode)

    def test_signed_rejects_mismatched_package(self):
        self.assertNotEqual(0, self.run_verifier(TEST_PACKAGE="com.example.other").returncode)

    def test_modern_aapt2_min_sdk_version_label(self):
        result = self.run_verifier(signed=False, TEST_SDK_LABEL="minSdkVersion")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_signed_rejects_mismatched_sdk(self):
        self.assertNotEqual(0, self.run_verifier(TEST_SDK="29").returncode)

    def test_signed_rejects_incorrect_certificate_file(self):
        self.cert.write_text("UNREGISTERED\n", encoding="utf-8")
        self.assertNotEqual(0, self.run_verifier().returncode)

    def test_unsigned_mode_requires_unsigned_name(self):
        self.assertEqual(0, self.run_verifier(signed=False).returncode)
        self.assertNotEqual(0, self.run_verifier(signed=False, TEST_PACKAGE="bad").returncode)

    def test_version_metadata_contract(self):
        args = [sys.executable, str(VERIFIER), "--expected-code", "1",
                "--expected-name", "0.1.0-m0", "--certificate", str(self.cert), str(self.apk)]
        env = {**os.environ, "ANDROID_HOME": str(self.root)}
        result = subprocess.run(args, env=env, capture_output=True, text=True, timeout=30)
        self.assertEqual(0, result.returncode)
        env["TEST_CODE"] = "2"
        result = subprocess.run(args, env=env, capture_output=True, text=True, timeout=30)
        self.assertNotEqual(0, result.returncode)


if __name__ == "__main__":
    unittest.main()
