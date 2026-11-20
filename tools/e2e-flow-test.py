#!/usr/bin/env python3
"""OURS end-to-end flow test against the live deployed site.

Walks the full user journey at the Poco X8 viewport (390x844) and reports
PASS/FAIL per step with console errors and failed requests. Screenshots land
in the artifact directory for visual review.
"""
import asyncio
import json
import os
import re
import sys
from pathlib import Path

from playwright.async_api import async_playwright

BASE = os.environ.get("OURS_URL", "https://ours.mnemonic.my.id")
OUT = Path(os.environ.get(
    "OURS_ARTIFACTS",
    "/home/dewhush/.gemini/antigravity/brain/06de2091-74e7-4f7a-a609-b2d3382f29f2",
))
OUT.mkdir(parents=True, exist_ok=True)
VIEWPORT = {"width": 390, "height": 844}

results = []
console_errors = []
failed_requests = []


def step(name, ok, detail=""):
    results.append({"step": name, "status": "PASS" if ok else "FAIL", "detail": detail})
    print(f"[{'PASS' if ok else 'FAIL'}] {name}" + (f" :: {detail}" if detail else ""))


async def shot(page, name):
    path = OUT / f"e2e_{name}.png"
    await page.screenshot(path=str(path))
    return path


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(args=["--no-sandbox"])
        ctx = await browser.new_context(
            viewport=VIEWPORT,
            device_scale_factor=2,
            is_mobile=True,
            has_touch=True,
            user_agent=("Mozilla/5.0 (Linux; Android 14; Poco X8) AppleWebKit/537.36 "
                        "(KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"),
        )
        page = await ctx.new_page()
        page.on("console", lambda m: console_errors.append(m.text)
                if m.type == "error" else None)
        page.on("requestfailed", lambda r: failed_requests.append(
            f"{r.method} {r.url} :: {r.failure}"))

        # --- 1. Landing / onboarding load -----------------------------------
        try:
            resp = await page.goto(BASE, wait_until="networkidle", timeout=30000)
            step("1.1 Landing loads", resp is not None and resp.status == 200,
                 f"HTTP {resp.status if resp else 'none'}")
        except Exception as exc:
            step("1.1 Landing loads", False, str(exc))
            await browser.close()
            return report()

        await page.wait_for_timeout(1500)
        await shot(page, "01_landing")

        body = await page.inner_text("body")
        step("1.2 Body renders text", len(body.strip()) > 20,
             f"{len(body.strip())} chars")

        # --- 2. Strict-CSP hygiene -----------------------------------------
        html = await page.content()
        inline_script = len(re.findall(r"<script(?![^>]*\bsrc=)[^>]*>", html))
        inline_handler = len(re.findall(r"\son(click|load|error|input|change)\s*=", html))
        em_dash = html.count("\u2014")
        step("2.1 No inline <script>", inline_script == 0, f"found {inline_script}")
        step("2.2 No inline on* handlers", inline_handler == 0, f"found {inline_handler}")
        step("2.3 No em dashes", em_dash == 0, f"found {em_dash}")

        # --- 3. Assets resolve ---------------------------------------------
        assets = ["/styles.css", "/components.css", "/app.js", "/components.js",
                  "/manifest.webmanifest", "/sw.js"]
        missing = []
        for a in assets:
            r = await page.request.get(BASE + a)
            if r.status != 200:
                missing.append(f"{a}={r.status}")
        step("3.1 Core assets 200", not missing, ", ".join(missing) or "all ok")

        # --- 4. JS boot without fatal errors -------------------------------
        fatal = [e for e in console_errors if "SyntaxError" in e or "TypeError" in e
                 or "ReferenceError" in e]
        step("4.1 No fatal JS errors", not fatal, "; ".join(fatal[:3]) or "clean")

        # --- 5. App is interactive ------------------------------------------
        app_html = await page.inner_html("#app") if await page.query_selector("#app") else ""
        step("5.1 #app mounted", len(app_html) > 100, f"{len(app_html)} bytes")
        await shot(page, "02_app_mounted")

        # --- 6. Language toggle --------------------------------------------
        try:
            id_btn = page.locator("#onb-lang-id")
            if await id_btn.count():
                await id_btn.click()
                await page.wait_for_timeout(600)
                body_id = await page.inner_text("body")
                step("6.1 Language switch to ID", "Tetap dekat" in body_id
                     or "Lokasi" in body_id or "kalian" in body_id,
                     "id copy visible" if "Tetap dekat" in body_id else "copy unchanged")
                await shot(page, "03_lang_id")
                await page.locator("#onb-lang-en").click()
                await page.wait_for_timeout(400)
            else:
                step("6.1 Language switch to ID", False, "#onb-lang-id absent")
        except Exception as exc:
            step("6.1 Language switch to ID", False, str(exc)[:120])

        # --- 7. Google auth button present ---------------------------------
        g = page.locator("#btn-google-auth")
        step("7.1 Google sign-in button", await g.count() > 0,
             f"{await g.count()} found")

        # --- 8. Pair flow: create / join segmented control ------------------
        no_email = page.locator("#btn-onb-no-email")
        if await no_email.count():
            await no_email.click()
            await page.wait_for_timeout(900)
            await shot(page, "04_pair_step")
            seg_join = page.locator("#seg-join")
            step("8.1 Create/Join segmented control", await seg_join.count() > 0,
                 f"{await seg_join.count()} found")
            if await seg_join.count():
                await seg_join.click()
                await page.wait_for_timeout(600)
                code_in = page.locator("#onb-code")
                step("8.2 Join reveals code field", await code_in.count() > 0,
                     f"{await code_in.count()} found")
                await shot(page, "05_join_mode")
                await page.locator("#seg-create").click()
                await page.wait_for_timeout(500)
                space_in = page.locator("#onb-space")
                step("8.3 Create reveals space field", await space_in.count() > 0,
                     f"{await space_in.count()} found")
                await shot(page, "06_create_mode")
        else:
            step("8.1 Create/Join segmented control", False,
                 "#btn-onb-no-email absent (may require Google)")
            # Try to reach it directly anyway
            await shot(page, "04_pair_step")

        # --- 9. Dark mode --------------------------------------------------
        try:
            await page.emulate_media(color_scheme="dark")
            await page.wait_for_timeout(700)
            await shot(page, "07_dark_mode")
            bg = await page.evaluate(
                "getComputedStyle(document.body).backgroundColor")
            step("9.1 Dark mode applies", "rgb(9, 11, 16)" in bg
                 or "rgb(17, 19, 24)" in bg, f"body bg {bg}")
            await page.emulate_media(color_scheme="light")
            await page.wait_for_timeout(400)
        except Exception as exc:
            step("9.1 Dark mode applies", False, str(exc)[:120])

        # --- 10. Partner status surface ------------------------------------
        await shot(page, "08_final_state")
        text_all = await page.inner_text("body")
        step("10.1 Partner/waiting surface", any(
            k.lower() in text_all.lower() for k in
            ["partner", "waiting", "live", "offline", "paused", "invite"]),
            "status copy present")

        await browser.close()

    # --- network summary ---------------------------------------------------
    step("11.1 No failed requests", not failed_requests,
         "; ".join(failed_requests[:3]) or "none")
    step("11.2 No console errors", not console_errors,
         "; ".join(console_errors[:3]) or "none")

    return report()


def report():
    print("\n" + "=" * 60)
    p = sum(1 for r in results if r["status"] == "PASS")
    f = sum(1 for r in results if r["status"] == "FAIL")
    print(f"TOTAL: {p} PASS / {f} FAIL / {len(results)} steps")
    print("=" * 60)
    (OUT / "e2e_report.json").write_text(json.dumps({
        "base": BASE, "results": results,
        "console_errors": console_errors,
        "failed_requests": failed_requests,
        "pass": p, "fail": f,
    }, indent=2))
    return 0 if f == 0 else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
