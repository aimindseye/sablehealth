#!/usr/bin/env python3
"""Browser smoke check and screenshot captures for compact M0 demo charts."""
import os
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / 'docs/compact-r4-preview.html'
OUT = ROOT / 'docs/screenshots'
OUT.mkdir(parents=True, exist_ok=True)
with sync_playwright() as playwright:
    browser = playwright.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH', '/usr/bin/chromium'), headless=True, args=['--no-sandbox'])
    page = browser.new_page(viewport={'width': 510, 'height': 560}, device_scale_factor=2)
    errors = []
    page.on('pageerror', lambda error: errors.append(str(error)))
    page.set_content(HTML.read_text(), wait_until="load")
    for tab in ('Sleep', 'Move', 'Vitals', 'Trends'):
        page.locator('[data-tab="%s"]' % tab).click()
        assert page.locator('#screen .title').inner_text() == tab
        assert page.locator('#screen .demo').count() >= 1
        if tab in ('Sleep','Move','Vitals'):
            assert page.locator('.metric').count() in (3,4)
        page.locator('.viewport').screenshot(path=str(OUT / ('compact-r4-' + tab.lower() + '-dark.png')))
    page.locator('#theme').click()
    assert page.evaluate('document.body.classList.contains("light")')
    page.locator('[data-tab="Sleep"]').click()
    page.locator('.viewport').screenshot(path=str(OUT / 'compact-r4-sleep-light.png'))
    assert not errors, errors
    browser.close()
print('PASS_COMPACT_PREVIEW: 4 dark screens + light; synthetic graph labels verified')
