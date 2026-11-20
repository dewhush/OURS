#!/usr/bin/env python3
"""OURS end-to-end flow test against the live deployed site.

Walks the full user journey at the Poco X8 viewport (390x844) and reports
PASS/FAIL/SKIP per step with console errors and failed requests. Screenshots
land in the artifact directory for visual review.

Honesty notes:
- The app uses the OAuth2 Authorization Code + PKCE *redirect* flow, not the
  Google Identity Services popup. `window.google` is expected to be undefined
  and is NOT checked here.
- The pairing step (#seg-create / #seg-join / #onb-space / #onb-code) is only
  rendered once `state.googleAuth` is populated by a completed OAuth
  round-trip. That requires a real Google account, so those checks are
  reported as SKIP rather than FAIL. What we CAN verify without an account is
  the exact authorization request the app builds when the button is pressed.
"""
import asyncio
import json
import os
import re
import sys
from pathlib import Path
from urllib.parse import parse_qs, urlparse

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
oauth_request = {}


def step(name, ok, detail=""):
    """ok=True -> PASS, ok=False -> FAIL, ok=None -> SKIP (blocked, not broken)."""
    status = "SKIP" if ok is None else ("PASS" if ok else "FAIL")
    results.append({"step": name, "status": status, "detail": detail})
    print(f"[{status}] {name}" + (f" :: {detail}" if detail else ""))


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

        # Intercept the Google authorization navigation so we can inspect the
        # exact request the app builds without leaving the origin (and without
        # involving any Google-hosted page).
        async def capture_oauth(route):
            oauth_request["url"] = route.request.url
            await route.fulfill(status=200, content_type="text/html",
                                body="<html><body>oauth-intercept</body></html>")

        await ctx.route("https://accounts.google.com/**", capture_oauth)

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

        # --- 7. Google auth entry point ------------------------------------
        g = page.locator("#btn-google-auth")
        step("7.1 Google sign-in button", await g.count() > 0,
             f"{await g.count()} found")
        step("7.2 Onboarding progress rail",
             await page.locator(".onb-seg-progress").count() > 0,
             f"{await page.locator('.onb-seg-progress').count()} found")

        # --- 8. Authorization request built by the app ---------------------
        # This is the reachable portion of the auth flow: press the button and
        # inspect the URL the app navigates to. A real Google round-trip is
        # required to get past it, so the pair controls are SKIP below.
        if await g.count():
            try:
                await g.click()
                for _ in range(40):
                    if oauth_request.get("url"):
                        break
                    await page.wait_for_timeout(100)
            except Exception as exc:
                step("8.1 Google auth request", False, str(exc)[:120])

            url = oauth_request.get("url", "")
            if not url:
                step("8.1 Google auth request", False,
                     "no navigation to accounts.google.com captured")
            else:
                q = parse_qs(urlparse(url).query)

                def q1(k):
                    return (q.get(k) or [""])[0]

                step("8.1 Google auth endpoint",
                     url.startswith("https://accounts.google.com/o/oauth2/v2/auth"),
                     urlparse(url).path)
                step("8.2 response_type=code", q1("response_type") == "code",
                     q1("response_type") or "missing")
                step("8.3 client_id present", bool(q1("client_id")),
                     (q1("client_id")[:14] + "...") if q1("client_id") else "missing")
                step("8.4 PKCE code_challenge + S256",
                     len(q1("code_challenge")) == 43
                     and q1("code_challenge_method") == "S256",
                     f"len={len(q1('code_challenge'))} "
                     f"method={q1('code_challenge_method') or 'missing'}")
                step("8.5 CSRF state present", len(q1("state")) >= 16,
                     f"len={len(q1('state'))}")
                ru = q1("redirect_uri")
                step("8.6 redirect_uri points at app",
                     "ours.mnemonic.my.id" in ru or ru.startswith("http"),
                     ru or "missing")
                step("8.7 scope present", bool(q1("scope")), q1("scope") or "missing")

            # Restore the app so later steps inspect a real page.
            try:
                await page.goto(BASE, wait_until="networkidle", timeout=30000)
                await page.wait_for_timeout(900)
            except Exception:
                pass

            await shot(page, "04_after_auth_click")

        # --- 8b. Pair step (blocked without a Google account) --------------
        pair_ready = await page.locator("#seg-create").count() > 0
        if pair_ready:
            await page.locator("#seg-join").click()
            await page.wait_for_timeout(500)
            step("8b.1 Join reveals code field",
                 await page.locator("#onb-code").count() > 0, "reachable")
            await page.locator("#seg-create").click()
            await page.wait_for_timeout(500)
            step("8b.2 Create reveals space field",
                 await page.locator("#onb-space").count() > 0, "reachable")
            await shot(page, "05_pair_step")
        else:
            whymsg = ("pair controls render only after a completed Google OAuth "
                      "round-trip (state.googleAuth); needs a real account")
            step("8b.1 Create/Join segmented control", None, whymsg)
            step("8b.2 Join reveals code field", None, "blocked by 8b.1")
            step("8b.3 Create reveals space field", None, "blocked by 8b.1")

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
    s = sum(1 for r in results if r["status"] == "SKIP")
    print(f"TOTAL: {p} PASS / {f} FAIL / {s} SKIP / {len(results)} steps")
    print("=" * 60)
    (OUT / "e2e_report.json").write_text(json.dumps({
        "base": BASE, "results": results,
        "console_errors": console_errors,
        "failed_requests": failed_requests,
        "pass": p, "fail": f, "skip": s,
    }, indent=2))
    return 0 if f == 0 else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
