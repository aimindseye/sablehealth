#!/usr/bin/env python3
"""Local browser-only smoke test for approved design preview, optional Playwright."""
from pathlib import Path
import os
from playwright.sync_api import sync_playwright

root = Path(__file__).resolve().parents[1]
html = (root / 'docs/design-preview.html').read_text()
shots = root / 'docs/screenshots'
shots.mkdir(exist_ok=True)
with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or None, headless=True, args=['--no-sandbox'])
    page = browser.new_page(viewport={'width': 1366, 'height': 900}, device_scale_factor=1)
    for device in ['q25', 'titan2', 'p26']:
        page.set_content(html)
        page.evaluate("device => { S.device = device; S.theme = 'dark'; S.screen = 'today'; render(); }", device)
        page.wait_for_selector('#phone')
        page.locator('#phone').screenshot(path=str(shots / f'{device}-today-sable-blue.png'))
        assert page.locator('#phone').evaluate("e=>getComputedStyle(e).getPropertyValue('--accent').trim()") == '#2e9bd6'
    browser.close()
print('PASS_DESIGN_SABLE_BLUE_PREVIEW')
