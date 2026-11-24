"""Exercise the shared preflight without invoking Gradle or signing."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, ROOT / path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class PreflightTest(unittest.TestCase):
    def setUp(self):
        self.module = load("preflight", "tools/quality_preflight.py")

    def main(self, args):
        with patch("sys.argv", ["quality_preflight.py", *args]):
            return self.module.main()

    def test_local_plan_and_quick(self):
        self.assertEqual(0, self.main(["--plan", "--quick"]))
        self.assertEqual(0, self.main(["--plan"]))

    def test_ci_plan_and_missing_gradle(self):
        with patch.object(self.module.shutil, "which", return_value="/fake/gradle"):
            self.assertEqual(0, self.main(["--ci", "--plan"]))
        with patch.object(self.module.shutil, "which", return_value=None):
            self.assertEqual(2, self.main(["--ci", "--plan"]))

    def test_missing_wrapper(self):
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(self.module, "ROOT", Path(directory)):
                self.assertEqual(2, self.main(["--plan"]))

    def test_execution_stops_at_first_failure(self):
        with patch.object(self.module, "invoke", return_value=False) as call:
            self.assertEqual(1, self.main(["--quick"]))
            self.assertEqual(1, call.call_count)

    def test_invocation_exit_status(self):
        for code in (0, 1):
            with patch.object(self.module.subprocess, "run", return_value=subprocess.CompletedProcess([], code)):
                self.assertEqual(code == 0, self.module.invoke(["fake"], plan=False))
        with patch.object(self.module.subprocess, "run") as call:
            self.assertTrue(self.module.invoke(["fake"], plan=True))
            call.assert_not_called()

    def test_local_history_scan_is_mandatory(self):
        with patch.object(self.module, "invoke", return_value=True) as call:
            self.assertEqual(0, self.main([]))
        commands = [entry.args[0] for entry in call.call_args_list]
        self.assertEqual("gitleaks", commands[-3][0])
        self.assertIn("--redact=100", commands[-3])

    def test_missing_command_fails_closed(self):
        with patch.object(self.module.subprocess, "run", side_effect=FileNotFoundError("missing")):
            self.assertFalse(self.module.invoke(["gitleaks"], plan=False))

    def test_full_ci_command_order(self):
        with patch.object(self.module.shutil, "which", return_value="/fake/gradle"):
            with patch.object(self.module, "invoke", return_value=True) as call:
                self.assertEqual(0, self.main(["--ci"]))
        commands = [entry.args[0] for entry in call.call_args_list]
        self.assertIn(":app:createDebugUnitTestCoverageReport", commands[-2])
        self.assertIn(":app:assembleRelease", commands[-2])
        self.assertIn("--ci-unsigned", commands[-1])


class UnsignedBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.module = load("apk_verifier", "tools/verify-release-apk.py")
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.apk = Path(temp.name) / "app-release-unsigned.apk"

    def test_manifest_required(self):
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("other", b"synthetic")
        with self.assertRaisesRegex(ValueError, "manifest"):
            self.module.verify_unsigned(self.apk)

    def test_apk_signing_block_refused(self):
        import struct
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("AndroidManifest.xml", b"synthetic")
        data = self.apk.read_bytes()
        end = data.rfind(b"PK\x05\x06")
        offset = struct.unpack_from("<I", data, end + 16)[0]
        trailer = bytearray(data[end:])
        block = b"APK Sig Block 42"
        struct.pack_into("<I", trailer, 16, offset + len(block))
        self.apk.write_bytes(data[:offset] + block + data[offset:end] + trailer)
        with self.assertRaisesRegex(ValueError, "Signing Block"):
            self.module.verify_unsigned(self.apk)

    def test_invalid_zip_refused(self):
        self.apk.write_bytes(b"invalid")
        with self.assertRaises(zipfile.BadZipFile):
            self.module.verify_unsigned(self.apk)
