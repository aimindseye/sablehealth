"""Offline SDK fixtures; no real credentials or external APKs."""
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("verifier", ROOT / "tools/verify-release-apk.py")
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)
CERT = "a" * 64
AAPT = '''#!/usr/bin/env python3
import os
print("package: name='" + os.getenv("TEST_PACKAGE", "io.github.aimindseye.sablehealth") +
      "' versionCode='" + os.getenv("TEST_CODE", "1") + "' versionName='" + os.getenv("TEST_NAME", "0.1.0-m0") + "'")
print(os.getenv("TEST_SDK_LABEL", "sdkVersion") + ":'" + os.getenv("TEST_SDK", "30") + "'")
'''
SIGN = '''#!/usr/bin/env python3
import os, sys
if os.getenv("TEST_CORRUPT") == "1": sys.exit(4)
if os.getenv("TEST_NOVERIFIES") != "1": print("Verifies")
print("Verified using v2 scheme (APK Signature Scheme v2): " + ("false" if os.getenv("TEST_SCHEME") == "v1" else "true"))
print("Verified using v3 scheme (APK Signature Scheme v3): false")
print("Number of signers: " + os.getenv("TEST_SIGNERS", "1"))
print("Signer #1 certificate SHA-256 digest: " + os.getenv("TEST_CERT", "a" * 64))
'''


class VerifyReleaseApkTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name).resolve()
        tools = self.root / "build-tools/36.0.0"
        tools.mkdir(parents=True)
        for name, content in (("aapt2", AAPT), ("apksigner", SIGN)):
            path = tools / name
            path.write_text(content)
            path.chmod(0o755)
        self.release = self.root / VERIFIER.RELEASE
        self.release.mkdir(parents=True)
        self.apk = self.release / ".owner-signing/app-release-signed.apk"
        self.apk.parent.mkdir()
        self.apk.write_bytes(b"synthetic")
        self.unsigned = self.release / "app-release-unsigned.apk"
        with zipfile.ZipFile(self.unsigned, "w") as archive:
            archive.writestr("AndroidManifest.xml", b"synthetic")
        self.cert = self.root / "config/release-certificate.sha256"
        self.cert.parent.mkdir()
        self.cert.write_text(CERT + "\n")

    def run_verifier(self, *, signed=True, extra=(), **environment):
        args = ["verify-release-apk.py", *([] if signed else ["--ci-unsigned"]), *extra]
        with patch.object(VERIFIER, "ROOT", self.root), patch.object(sys, "argv", args), patch.dict(os.environ, {"ANDROID_HOME": str(self.root), "ANDROID_SDK_ROOT": str(self.root), **environment}):
            return VERIFIER.entrypoint()

    def test_registered_single_signer(self):
        self.assertEqual(0, self.run_verifier())

    def test_signed_rejects_different_key(self):
        self.assertEqual(2, self.run_verifier(TEST_CERT="b" * 64))

    def test_signed_rejects_multiple_signers(self):
        self.assertEqual(2, self.run_verifier(TEST_SIGNERS="2"))

    def test_signed_rejects_v1_only(self):
        self.assertEqual(2, self.run_verifier(TEST_SCHEME="v1"))

    def test_signed_rejects_tampering(self):
        self.assertEqual(2, self.run_verifier(TEST_CORRUPT="1"))

    def test_signed_rejects_mismatched_package(self):
        self.assertEqual(2, self.run_verifier(TEST_PACKAGE="bad"))

    def test_modern_aapt2_min_sdk_version_label(self):
        self.assertEqual(0, self.run_verifier(signed=False, TEST_SDK_LABEL="minSdkVersion"))

    def test_signed_rejects_mismatched_sdk(self):
        self.assertEqual(2, self.run_verifier(TEST_SDK="29"))

    def test_signed_rejects_incorrect_certificate_file(self):
        self.cert.write_text("UNREGISTERED\n")
        self.assertEqual(2, self.run_verifier())

    def test_unsigned_mode_requires_unsigned_name(self):
        self.assertEqual(0, self.run_verifier(signed=False))
        self.unsigned.rename(self.release / "wrong.apk")
        self.assertEqual(2, self.run_verifier(signed=False))

    def test_unsigned_rejects_v1_signature(self):
        with zipfile.ZipFile(self.unsigned, "a") as archive:
            archive.writestr("META-INF/CERT.RSA", b"synthetic")
        self.assertEqual(2, self.run_verifier(signed=False))

    def test_version_metadata_contract(self):
        args = ("--expected-code", "1", "--expected-name", "0.1.0-m0")
        self.assertEqual(0, self.run_verifier(extra=args))
        self.assertEqual(2, self.run_verifier(extra=args, TEST_CODE="2"))
        self.assertEqual(2, self.run_verifier(extra=args, TEST_NAME="wrong"))

    def test_arbitrary_paths_and_certificate_cli_refused(self):
        for args in (("../../outside.apk",), ("--certificate", "/tmp/other"), ("--expected-c", "1"), ("--helpful",)):
            with self.assertRaises(SystemExit) as error:
                self.run_verifier(extra=args)
            self.assertEqual(2, error.exception.code)

    def test_symlinked_apk_refused(self):
        self.apk.unlink()
        self.apk.symlink_to(self.unsigned)
        self.assertEqual(2, self.run_verifier())

    def test_symlinked_certificate_refused(self):
        alternate = self.root / "alternate"
        self.cert.rename(alternate)
        self.cert.symlink_to(alternate)
        self.assertEqual(2, self.run_verifier())

    def test_traversal_helper_refused(self):
        with patch.object(VERIFIER, "ROOT", self.root):
            with self.assertRaises(ValueError):
                VERIFIER.fixed_file("../outside")
            with self.assertRaises(ValueError):
                VERIFIER.sdk_tool("../../sh")

    def test_invalid_metadata_and_missing_signature(self):
        self.assertEqual(2, self.run_verifier(TEST_SDK_LABEL="bad"))
        self.assertEqual(2, self.run_verifier(TEST_CODE="0"))
        self.assertEqual(2, self.run_verifier(TEST_NOVERIFIES="1"))

    def test_missing_pin(self):
        self.cert.unlink()
        self.assertEqual(2, self.run_verifier())
