"""Execute the real repository contracts against valid and synthetic adverse inputs."""
import contextlib
import io
from pathlib import Path
import runpy
import unittest
from unittest.mock import patch
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'app/src/main/java/io/github/aimindseye/sablehealth'
MANIFEST = ROOT / 'app/src/main/AndroidManifest.xml'


def execute(script):
    with contextlib.redirect_stdout(io.StringIO()) as output:
        runpy.run_path(str(ROOT / 'tools' / script), run_name='__main__')
    return output.getvalue()


def altered_source(path, original, replacement):
    read = Path.read_text

    def read_text(self, *args, **kwargs):
        content = read(self, *args, **kwargs)
        if self == path:
            if original not in content:
                raise ValueError('Negative fixture anchor missing')
            return content.replace(original, replacement, 1)
        return content

    return patch.object(Path, 'read_text', read_text)


class ContractTests(unittest.TestCase):
    def test_real_source_passes_all_contracts(self):
        for script, marker in (
            ('check-m1a-contract.py', 'PASS_M1A_READ_ONLY_CONTRACT'),
            ('check-r5-contract.py', 'PASS_R51_DASHBOARD'),
            ('check-structure.py', 'PASS_M0_STATIC_CONTRACT'),
        ):
            with self.subTest(script=script):
                self.assertIn(marker, execute(script))

    def test_manifest_permissions_reject_extras_and_missing(self):
        original = MANIFEST.read_text()
        permission = '<uses-permission android:name="android.permission.health.READ_STEPS" />'
        parse = ET.parse
        for invalid in (
            original.replace(permission, ''),
            original.replace(permission, permission + '\n<uses-permission android:name="android.permission.INTERNET" />'),
            original.replace(permission, permission.replace('READ_STEPS', 'WRITE_STEPS')),
        ):
            with self.subTest(invalid=invalid[:80]):
                def parse_manifest(source, *args, **kwargs):
                    if source == MANIFEST:
                        return parse(io.StringIO(invalid), *args, **kwargs)
                    return parse(source, *args, **kwargs)

                with patch.object(ET, 'parse', parse_manifest), self.assertRaises(AssertionError):
                    execute('check-m1a-contract.py')

    def test_malformed_manifest_fails(self):
        parse = ET.parse

        def parse_manifest(source, *args, **kwargs):
            if source == MANIFEST:
                return parse(io.StringIO('<manifest>'), *args, **kwargs)
            return parse(source, *args, **kwargs)

        with patch.object(ET, 'parse', parse_manifest), self.assertRaises(ET.ParseError):
            execute('check-m1a-contract.py')

    def test_missing_rationale_and_unsafe_probe_fail(self):
        cases = (
            ('M1PermissionRationaleActivity.kt', 'Health Connect privacy', 'Privacy'),
            ('M1HealthConnectProbe.kt', 'client.readRecords(', 'client.insertRecords('),
            ('M1HealthConnectProbe.kt', 'client.readRecords(', 'client.readRecords( /* Log. */'),
            ('MainActivity.kt', 'getGrantedPermissions', 'readRecords('),
            ('M1DiscoveryPage.kt', 'Intent.ACTION_SEND', 'Intent.ACTION_VIEW'),
        )
        for name, old, new in cases:
            with self.subTest(name=name, old=old):
                with altered_source(APP / name, old, new), self.assertRaises(AssertionError):
                    execute('check-m1a-contract.py')

    def test_dashboard_contract_rejects_unsafe_entrypoint(self):
        with altered_source(APP / 'MainActivity.kt', 'getGrantedPermissions', 'getGrantedPermissions /* readRecords */'), self.assertRaises(AssertionError):
            execute('check-r5-contract.py')

    def test_structure_contract_rejects_unsafe_probe(self):
        with altered_source(APP / 'M1HealthConnectProbe.kt', 'client.readRecords(', 'client.deleteRecords('), self.assertRaises(AssertionError):
            execute('check-structure.py')


if __name__ == '__main__':
    unittest.main()
