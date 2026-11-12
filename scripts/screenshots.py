"""Take professional screenshots of the live DiviStay site in English and Hebrew.
Saves full-page and first-screen images into docs/ (2x resolution)."""
from pathlib import Path
from playwright.sync_api import sync_playwright

URL = "https://divistay.onrender.com/"
OUT = Path(__file__).resolve().parent.parent / "docs"

with sync_playwright() as p:
    browser = p.chromium.launch()
    for lang, suffix in (("en", ""), ("he", "-he")):
        page = browser.new_page(viewport={"width": 1440, "height": 900}, device_scale_factor=2, reduced_motion="reduce")
        print(f"Opening {lang} (the first load can take up to a minute)...")
        page.goto(f"{URL}?lang={lang}", wait_until="networkidle", timeout=180_000)
        page.wait_for_timeout(2500)
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        page.wait_for_timeout(1500)
        page.evaluate("window.scrollTo(0, 0)")
        page.wait_for_timeout(1000)
        page.screenshot(path=OUT / f"site{suffix or '-en'}.png")
        page.screenshot(path=OUT / f"site-full{suffix or '-en'}.png", full_page=True)
        print(f"Saved docs/site{suffix or '-en'}.png and docs/site-full{suffix or '-en'}.png")
        page.close()
    browser.close()
