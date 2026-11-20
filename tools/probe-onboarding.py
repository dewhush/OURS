#!/usr/bin/env python3
"""Probe the live onboarding DOM to see exactly which controls exist."""
import asyncio, json, os
from playwright.async_api import async_playwright

BASE = os.environ.get("OURS_URL", "https://ours.mnemonic.my.id")


async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--no-sandbox"])
        ctx = await b.new_context(viewport={"width": 390, "height": 844},
                                  is_mobile=True, has_touch=True)
        pg = await ctx.new_page()
        await pg.goto(BASE, wait_until="networkidle", timeout=30000)
        await pg.wait_for_timeout(2000)

        info = await pg.evaluate("""() => {
          const ids = [...document.querySelectorAll('[id]')].map(e => e.id);
          const btns = [...document.querySelectorAll('button')].map(e => ({
            id: e.id, text: e.innerText.trim().slice(0,40),
            cls: e.className.slice(0,40), hidden: !e.offsetParent
          }));
          const app = document.getElementById('app');
          return {
            title: document.title,
            appClass: app ? app.className : null,
            appText: app ? app.innerText.slice(0, 400) : null,
            ids: ids,
            buttons: btns,
            bodyBg: getComputedStyle(document.body).backgroundColor,
            hasGis: !!window.google,
          };
        }""")
        print(json.dumps(info, indent=2))
        await b.close()

asyncio.run(main())
